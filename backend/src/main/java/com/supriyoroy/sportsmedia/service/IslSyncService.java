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

    @Value("${api-football.key:}")
    private String apiKey;

    @Value("${api-football.base-url:https://v3.football.api-sports.io}")
    private String baseUrl;

    @Value("${api-football.isl-league-id:323}")
    private int islLeagueId;

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

    @Scheduled(cron = "0 0 * * * *")
    public void syncIslMatches() {
        if (!enabled || apiKey == null || apiKey.isBlank()) return;

        try {
            Sport football = sportRepo.findBySlug("football")
                    .orElseGet(() -> sportRepo.save(Sport.builder().name("Football").slug("football").sortOrder(1).active(true).build()));

            League isl = leagueRepo.findBySlug("isl")
                    .orElseGet(() -> leagueRepo.save(League.builder()
                            .name("Indian Super League")
                            .slug("isl")
                            .country("India")
                            .externalCode("323")
                            .sport(football)
                            .active(true)
                            .build()));

            int currentYear = LocalDate.now().getYear();
            String url = baseUrl + "/fixtures?league=" + islLeagueId + "&season=" + currentYear;

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
                        match.setLeague(isl);

                        matchRepo.save(match);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("ISL Sync Error: " + e.getMessage());
        }
    }
}