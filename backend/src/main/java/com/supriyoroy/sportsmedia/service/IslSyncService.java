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

    @Value("${api-football.key:3b8be5131amshec4623e52edf98bp1aecd5jsn82aa6eb1ec6a}")
    private String apiKey;

    @Value("${api-football.base-url:https://api-football-v1.p.rapidapi.com/v3}")
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

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        syncMatches();
    }

@Scheduled(cron = "0 0 * * * *")
public void syncMatches() {
    if (!enabled || apiKey == null || apiKey.isBlank()) return;

    try {
        Sport football = sportRepo.findBySlug("football")
                .orElseGet(() -> sportRepo.save(Sport.builder().name("Football").slug("football").sortOrder(1).active(true).build()));

        int currentYear = LocalDate.now().getYear();

        // 1. Sync Indian Super League (ID: 323) for current season
        fetchAndSave(baseUrl + "/fixtures?league=323&season=" + currentYear, 323, "Indian Super League", "isl", "India", football);

        // 2. Sync UEFA Nations League (ID: 5) - query live and next 20 fixtures directly
        fetchAndSave(baseUrl + "/fixtures?league=5&live=all", 5, "UEFA Nations League", "nations-league", "Europe", football);
        fetchAndSave(baseUrl + "/fixtures?league=5&next=20", 5, "UEFA Nations League", "nations-league", "Europe", football);

    } catch (Exception e) {
        System.err.println("Error in syncMatches: " + e.getMessage());
    }
}

    private void syncUpcomingAndLive(int apiLeagueId, String name, String slug, String country, Sport sport, String season) {
        // Fetch LIVE matches first for current season
        fetchAndSave(baseUrl + "/fixtures?league=" + apiLeagueId + "&season=" + season + "&live=all", apiLeagueId, name, slug, country, sport);
        // Fetch UPCOMING matches only (next 20 fixtures) for current season
        fetchAndSave(baseUrl + "/fixtures?league=" + apiLeagueId + "&season=" + season + "&next=20", apiLeagueId, name, slug, country, sport);
    }

    private void syncLeagueBySeason(int apiLeagueId, String name, String slug, String country, Sport sport, String season) {
        fetchAndSave(baseUrl + "/fixtures?league=" + apiLeagueId + "&season=" + season, apiLeagueId, name, slug, country, sport);
    }

    private void fetchAndSave(String url, int apiLeagueId, String name, String slug, String country, Sport sport) {
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

            // RapidAPI Proxy Headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-rapidapi-key", apiKey);
            headers.set("x-rapidapi-host", "api-football-v1.p.rapidapi.com");
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

                        String statusShort = fixture.get("status").get("short").asText();

                        // SKIP FINISHED MATCHES
                        if (statusShort.matches("FT|AET|PEN")) {
                            continue;
                        }

                        MatchStatus status = statusShort.matches("1H|2H|HT|ET|P|LIVE") ? MatchStatus.LIVE : MatchStatus.UPCOMING;

                        String externalId = "api_football_" + fixture.get("id").asText();
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
            System.err.println("Sync Error for " + name + ": " + e.getMessage());
        }
    }
}