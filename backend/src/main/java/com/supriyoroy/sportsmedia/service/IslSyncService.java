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
import java.util.Set;

@Service
public class IslSyncService {

    @Value("${api-football.enabled:true}")
    private boolean enabled;

    @Value("${api-football.key:a4759894ef3edc59dc1d51149919b1ee}")
    private String apiKey;

    @Value("${api-football.base-url:https://v3.football.api-sports.io}")
    private String baseUrl;

    private final MatchRepository matchRepo;
    private final LeagueRepository leagueRepo;
    private final SportRepository sportRepo;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> LIVE_STATUSES = Set.of("1H", "HT", "2H", "ET", "BT", "P", "SUSP", "INT", "LIVE");
    private static final Set<String> UPCOMING_STATUSES = Set.of("NS", "TBD");

    public IslSyncService(MatchRepository matchRepo, LeagueRepository leagueRepo, SportRepository sportRepo) {
        this.matchRepo = matchRepo;
        this.leagueRepo = leagueRepo;
        this.sportRepo = sportRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        syncMatches();
    }

    @Scheduled(cron = "0 0/15 * * * *")
    public void syncMatches() {
        if (!enabled || apiKey == null || apiKey.isBlank()) return;

        try {
            Sport football = sportRepo.findBySlug("football")
                    .orElseGet(() -> sportRepo.save(Sport.builder().name("Football").slug("football").sortOrder(1).active(true).build()));

            int currentYear = LocalDate.now().getYear();

            System.out.println("Starting sync for Live & Upcoming fixtures via direct API-Sports...");

            // 1. UEFA Nations League (ID: 5)
            fetchAndSaveUpcomingAndLive(5, "UEFA Nations League", "nations-league", "Europe", football, currentYear);

            Thread.sleep(1200);

            // 2. Indian Super League (ID: 323)
            fetchAndSaveUpcomingAndLive(323, "Indian Super League", "isl", "India", football, currentYear);

            System.out.println("Sync finished successfully.");

        } catch (Exception e) {
            System.err.println("Error during syncMatches: " + e.getMessage());
        }
    }

    private void fetchAndSaveUpcomingAndLive(int apiLeagueId, String name, String slug, String country, Sport sport, int season) {
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

            // Query upcoming fixtures and live fixtures
            String upcomingUrl = baseUrl + "/fixtures?league=" + apiLeagueId + "&season=" + season + "&status=NS";
            String liveUrl = baseUrl + "/fixtures?league=" + apiLeagueId + "&live=all";

            processEndpoint(upcomingUrl, league);
            processEndpoint(liveUrl, league);

        } catch (Exception e) {
            System.err.println("Sync Error for " + name + ": " + e.getMessage());
        }
    }

    private void processEndpoint(String url, League league) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-apisports-key", apiKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode responseArr = root.get("response");

                if (responseArr != null && responseArr.isArray()) {
                    int count = 0;
                    for (JsonNode item : responseArr) {
                        JsonNode fixture = item.get("fixture");
                        JsonNode teams = item.get("teams");
                        JsonNode goals = item.get("goals");

                        if (fixture == null || teams == null) continue;

                        String statusShort = (fixture.get("status") != null && fixture.get("status").get("short") != null)
                                ? fixture.get("status").get("short").asText() : "";

                        // Filter strictly for LIVE and UPCOMING (ignores FT, PEN, AET)
                        if (!LIVE_STATUSES.contains(statusShort) && !UPCOMING_STATUSES.contains(statusShort)) {
                            continue;
                        }

                        MatchStatus status = LIVE_STATUSES.contains(statusShort) ? MatchStatus.LIVE : MatchStatus.UPCOMING;

                        String externalId = "api_football_" + fixture.get("id").asText();
                        MatchEntity match = matchRepo.findByExternalId(externalId).orElse(new MatchEntity());

                        match.setExternalId(externalId);
                        match.setHomeTeam(teams.get("home").get("name").asText());
                        match.setHomeLogo(teams.get("home").get("logo").asText());
                        match.setAwayTeam(teams.get("away").get("name").asText());
                        match.setAwayLogo(teams.get("away").get("logo").asText());
                        match.setHomeScore(goals != null && !goals.get("home").isNull() ? goals.get("home").asText() : null);
                        match.setAwayScore(goals != null && !goals.get("away").isNull() ? goals.get("away").asText() : null);
                        match.setStatus(status);
                        match.setKickoffUtc(Instant.parse(fixture.get("date").asText()));
                        match.setLeague(league);

                        matchRepo.save(match);
                        count++;
                    }
                    System.out.println("Saved/Updated " + count + " matches for " + league.getName() + " via " + url);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch fixtures from " + url + ": " + e.getMessage());
        }
    }
}