package com.supriyoroy.sportsmedia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supriyoroy.sportsmedia.model.League;
import com.supriyoroy.sportsmedia.model.MatchEntity;
import com.supriyoroy.sportsmedia.model.MatchStatus;
import com.supriyoroy.sportsmedia.model.Sport;
import com.supriyoroy.sportsmedia.repo.LeagueRepository;
import com.supriyoroy.sportsmedia.repo.MatchRepository;
import com.supriyoroy.sportsmedia.repo.SportRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;

@Service
public class IslSyncService {

    @Value("${api-football.enabled:true}")
    private boolean enabled;

    @Value("${api-football.key:24e721b245957d50afaa28d812a53baf}")
    private String apiKey;

    @Value("${api-football.base-url:https://v3.football.api-sports.io}")
    private String baseUrl;

    private final MatchRepository matchRepo;
    private final LeagueRepository leagueRepo;
    private final SportRepository sportRepo;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IslSyncService(MatchRepository matchRepo, LeagueRepository leagueRepo, SportRepository sportRepo) {
        this.matchRepo = matchRepo;
        this.leagueRepo = leagueRepo;
        this.sportRepo = sportRepo;
    }

    // Trigger sync immediately upon application startup
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        syncMatches();
    }

    // Scheduled hourly run
    @Scheduled(cron = "0 0 * * * *")
    public void syncMatches() {
        if (!enabled || apiKey == null || apiKey.isBlank()) return;

        try {
            Sport football = sportRepo.findBySlug("football")
                    .orElseGet(() -> sportRepo.save(Sport.builder().name("Football").slug("football").sortOrder(1).active(true).build()));

            int currentYear = LocalDate.now().getYear();

            // 1. Sync Indian Super League (ID: 323)
            syncLeague(323, "Indian Super League", "isl", "India", football, String.valueOf(currentYear));

            // 2. Sync UEFA Nations League (ID: 5)
            syncLeague(5, "UEFA Nations League", "nations-league", "Europe", football, String.valueOf(currentYear));

        } catch (Exception e) {
            System.err.println("Error in syncMatches: " + e.getMessage());
        }
    }

    private void syncLeague(int apiLeagueId, String name, String slug, String country, Sport sport, String season) {
        try {
            League league = leagueRepo.findBySlug(slug)
                    .orElseGet(() -> leagueRepo.save(League.builder()
                            .name(name)
                            .slug(slug)
                            .country(country)
                            .externalCode(String.valueOf(apiLeagueId))
                            .sport(sport)
                            .active(true)
                            .build()));

            String url = baseUrl + "/fixtures?league=" + apiLeagueId + "&season=" + season;

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-apisports-key", apiKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode responseArr = root.get("response");

                if (responseArr != null && responseArr.isArray()) {
                    for (JsonNode item : responseArr) {
                        JsonNode fixture = item.get("fixture");
                        JsonNode teams = item.get("teams");
                        JsonNode goals = item.get("goals");

                        String externalId = "api_football_" + fixture.get("id").asText();
                        String statusShort = fixture.get("status").get("short").asText();

                        MatchStatus status = MatchStatus.UPCOMING;
                        if (statusShort.matches("1H|2H|HT|ET|P|LIVE")) {
                            status = MatchStatus.LIVE;
                        } else if (statusShort.matches("FT|AET|PEN")) {
                            status = MatchStatus.FINISHED;
                        }

                        MatchEntity match = matchRepo.findByExternalId(externalId).orElse(new MatchEntity());
                        match.setExternalId(externalId);
                        match.setHomeTeam(teams.get("home").get("name").asText());
                        match.setHomeLogo(teams.get("home").get("logo").asText());
                        match.setAwayTeam(teams.get("away").get("name").asText());
                        match.setAwayLogo(teams.get("away").get("logo").asText());
                        match.setHomeScore(goals.get("home").isNull() ? null : goals.get("home").asText());
                        match.setAwayScore(goals.get("away").isNull() ? null : goals.get("away").asText());
                        match.setStatus(status);
                        match.setKickoffUtc(Instant.parse(fixture.get("date").asText()));
                        match.setLeague(league);

                        matchRepo.save(match);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Sync Error for " + name + " (Season " + season + "): " + e.getMessage());
        }
    }
}