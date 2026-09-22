// package com.supriyoroy.sportsmedia.service;

// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.supriyoroy.sportsmedia.model.*;
// import com.supriyoroy.sportsmedia.repo.*;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import org.springframework.web.client.RestClient;

// import java.time.Instant;
// import java.time.LocalDate;
// import java.time.temporal.ChronoUnit;
// import java.util.Optional;

// /**
//  * Pulls fixtures from football-data.org and keeps the schedule current.
//  *
//  * Rules that protect your work:
//  *  - a fixture is matched by externalId, so re-running never duplicates
//  *  - streamUrl is NEVER touched by the sync
//  *  - if adminLocked is true, kick-off/status/score are left alone too
//  */
// @Service
// @Slf4j
// @RequiredArgsConstructor
// public class ScheduleSyncService {

//     private final MatchRepository matchRepo;
//     private final LeagueRepository leagueRepo;
//     private final SportRepository sportRepo;
//     private final ObjectMapper mapper = new ObjectMapper();

//     @Value("${football.api.enabled}") private boolean enabled;
//     @Value("${football.api.token}")   private String token;
//     @Value("${football.api.base-url}") private String baseUrl;
//     @Value("${football.api.competitions}") private String competitions;
//     @Value("${football.api.days-ahead}") private int daysAhead;

//     private volatile Instant lastSync;

//     public Instant lastSync() { return lastSync; }

//     @Scheduled(cron = "${football.api.cron}")
//     public void scheduledSync() {
//         if (!enabled) return;
//         syncAll();
//     }

//     /** Also callable from the admin panel: POST /api/admin/sync */
//     @Transactional
//     public int syncAll() {
//         if (!enabled || token == null || token.isBlank()) {
//             log.info("Fixture sync skipped: set FOOTBALL_API_ENABLED=true and FOOTBALL_API_TOKEN");
//             return 0;
//         }
//         int total = 0;
//         RestClient client = RestClient.builder().baseUrl(baseUrl).build();
//         LocalDate from = LocalDate.now();
//         LocalDate to = from.plusDays(daysAhead);

//         for (String code : competitions.split(",")) {
//             code = code.trim();
//             if (code.isEmpty()) continue;
//             try {
//                 String body = client.get()
//                         .uri("/competitions/{code}/matches?dateFrom={from}&dateTo={to}", code, from, to)
//                         .header("X-Auth-Token", token)
//                         .retrieve()
//                         .body(String.class);
//                 total += ingest(code, mapper.readTree(body));
//             } catch (Exception e) {
//                 log.warn("Sync failed for {}: {}", code, e.getMessage());
//             }
//         }
//         lastSync = Instant.now();
//         log.info("Fixture sync complete: {} fixtures touched", total);
//         return total;
//     }

//     private int ingest(String code, JsonNode root) {
//         JsonNode fixtures = root.path("matches");
//         if (!fixtures.isArray()) return 0;

//         League league = leagueRepo.findByExternalCode(code).orElseGet(() -> {
//             String name = root.path("competition").path("name").asText(code);
//             Sport football = sportRepo.findBySlug("football").orElseThrow();
//             return leagueRepo.save(League.builder()
//                     .name(name).slug(slugify(name)).externalCode(code)
//                     .country(root.path("competition").path("area").path("name").asText(null))
//                     .logoUrl(root.path("competition").path("emblem").asText(null))
//                     .sport(football).active(true).sortOrder(50).build());
//         });

//         int count = 0;
//         for (JsonNode f : fixtures) {
//             String extId = code + "-" + f.path("id").asText();
//             Optional<MatchEntity> existing = matchRepo.findByExternalId(extId);

//             MatchEntity m = existing.orElseGet(MatchEntity::new);
//             if (existing.isPresent() && Boolean.TRUE.equals(m.getAdminLocked())) continue;

//             m.setExternalId(extId);
//             m.setLeague(league);
//             m.setHomeTeam(f.path("homeTeam").path("name").asText("TBD"));
//             m.setAwayTeam(f.path("awayTeam").path("name").asText("TBD"));
//             m.setHomeLogo(f.path("homeTeam").path("crest").asText(null));
//             m.setAwayLogo(f.path("awayTeam").path("crest").asText(null));
//             m.setKickoffUtc(Instant.parse(f.path("utcDate").asText()));
//             m.setStatus(mapStatus(f.path("status").asText("SCHEDULED")));

//             JsonNode full = f.path("score").path("fullTime");
//             if (!full.path("home").isNull()) m.setHomeScore(full.path("home").asText());
//             if (!full.path("away").isNull()) m.setAwayScore(full.path("away").asText());

//             if (m.getDescription() == null || m.getDescription().isBlank()) {
//                 String md = f.path("matchday").isMissingNode() ? "" : "Matchday " + f.path("matchday").asInt();
//                 m.setDescription((league.getName() + " " + md).trim());
//             }
//             if (m.getVisible() == null) m.setVisible(true);
//             matchRepo.save(m);
//             count++;
//         }
//         return count;
//     }

//     private MatchStatus mapStatus(String s) {
//         return switch (s) {
//             case "IN_PLAY", "PAUSED" -> MatchStatus.LIVE;
//             case "FINISHED", "AWARDED" -> MatchStatus.FINISHED;
//             case "POSTPONED", "SUSPENDED" -> MatchStatus.POSTPONED;
//             case "CANCELLED" -> MatchStatus.CANCELLED;
//             default -> MatchStatus.UPCOMING;
//         };
//     }

//     /**
//      * Safety net for manually-added matches: flips UPCOMING to LIVE at kick-off,
//      * and LIVE to FINISHED 2h45m later, so badges are never stale.
//      */
//     @Scheduled(fixedDelay = 60_000)
//     @Transactional
//     public void autoStatus() {
//         Instant now = Instant.now();
//         matchRepo.findByStatusAndKickoffUtcBefore(MatchStatus.UPCOMING, now)
//                 .forEach(m -> { m.setStatus(MatchStatus.LIVE); matchRepo.save(m); });

//         matchRepo.findByStatus(MatchStatus.LIVE).stream()
//                 .filter(m -> m.getKickoffUtc().plus(165, ChronoUnit.MINUTES).isBefore(now))
//                 .forEach(m -> { m.setStatus(MatchStatus.FINISHED); matchRepo.save(m); });
//     }

//     public static String slugify(String s) {
//         return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
//     }
// }


package com.supriyoroy.sportsmedia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supriyoroy.sportsmedia.model.MatchEntity;
import com.supriyoroy.sportsmedia.model.MatchStatus;
import com.supriyoroy.sportsmedia.repo.MatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.Instant;
import java.util.Optional;

@Service
public class ScheduleSyncService {

    private final MatchRepository matchRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${football.api.key:}")
    private String apiKey;

    @Value("${football.api.url:https://api.football-data.org/v4/matches}")
    private String apiUrl;

    public ScheduleSyncService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // Runs every 60 seconds to sync live scores automatically
    @Scheduled(fixedRate = 60000)
    public void syncLiveScores() {
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode matches = root.get("matches");

                if (matches != null && matches.isArray()) {
                    for (JsonNode m : matches) {
                        String extId = m.get("id").asText();
                        Optional<MatchEntity> existing = matchRepository.findByExternalId(extId);

                        MatchEntity match = existing.orElseGet(MatchEntity::new);
                        match.setExternalId(extId);
                        match.setApiProvider("football-data.org");
                        match.setSportSlug("football");
                        match.setSportName("Football");

                        match.setHomeTeam(m.get("homeTeam").get("name").asText());
                        match.setAwayTeam(m.get("awayTeam").get("name").asText());
                        if (m.get("homeTeam").has("crest")) match.setHomeLogo(m.get("homeTeam").get("crest").asText());
                        if (m.get("awayTeam").has("crest")) match.setAwayLogo(m.get("awayTeam").get("crest").asText());

                        match.setLeagueName(m.get("competition").get("name").asText());
                        match.setLeagueSlug(m.get("competition").get("code").asText().toLowerCase());

                        String utcDate = m.get("utcDate").asText();
                        match.setKickoffUtc(Instant.parse(utcDate));

                        String statusStr = m.get("status").asText();
                        if ("IN_PLAY".equals(statusStr) || "PAUSED".equals(statusStr)) {
                            match.setStatus(MatchStatus.LIVE);
                        } else if ("FINISHED".equals(statusStr)) {
                            match.setStatus(MatchStatus.FINISHED);
                        } else {
                            match.setStatus(MatchStatus.UPCOMING);
                        }

                        JsonNode scoreNode = m.get("score").get("fullTime");
                        if (scoreNode != null && !scoreNode.get("home").isNull()) {
                            match.setHomeScore(scoreNode.get("home").asText());
                            match.setAwayScore(scoreNode.get("away").asText());
                        }

                        matchRepository.save(match);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error syncing live scores: " + e.getMessage());
        }
    }
}