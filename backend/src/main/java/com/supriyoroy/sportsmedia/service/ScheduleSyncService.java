package com.supriyoroy.sportsmedia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supriyoroy.sportsmedia.model.*;
import com.supriyoroy.sportsmedia.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Pulls fixtures from football-data.org and keeps the schedule current.
 *
 * Rules that protect your work:
 *  - a fixture is matched by externalId, so re-running never duplicates
 *  - streamUrl is NEVER touched by the sync
 *  - if adminLocked is true, kick-off/status/score are left alone too
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleSyncService {

    private final MatchRepository matchRepo;
    private final LeagueRepository leagueRepo;
    private final SportRepository sportRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${football.api.enabled}") private boolean enabled;
    @Value("${football.api.token}")   private String token;
    @Value("${football.api.base-url}") private String baseUrl;
    @Value("${football.api.competitions}") private String competitions;
    @Value("${football.api.days-ahead}") private int daysAhead;

    private volatile Instant lastSync;

    public Instant lastSync() { return lastSync; }

    @Scheduled(cron = "${football.api.cron}")
    public void scheduledSync() {
        if (!enabled) return;
        syncAll();
    }

    /** Also callable from the admin panel: POST /api/admin/sync */
    @Transactional
    public int syncAll() {
        if (!enabled || token == null || token.isBlank()) {
            log.info("Fixture sync skipped: set FOOTBALL_API_ENABLED=true and FOOTBALL_API_TOKEN");
            return 0;
        }
        int total = 0;
        RestClient client = RestClient.builder().baseUrl(baseUrl).build();
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(daysAhead);

        for (String code : competitions.split(",")) {
            code = code.trim();
            if (code.isEmpty()) continue;
            try {
                String body = client.get()
                        .uri("/competitions/{code}/matches?dateFrom={from}&dateTo={to}", code, from, to)
                        .header("X-Auth-Token", token)
                        .retrieve()
                        .body(String.class);
                total += ingest(code, mapper.readTree(body));
            } catch (Exception e) {
                log.warn("Sync failed for {}: {}", code, e.getMessage());
            }
        }
        lastSync = Instant.now();
        log.info("Fixture sync complete: {} fixtures touched", total);
        return total;
    }

    private int ingest(String code, JsonNode root) {
        JsonNode fixtures = root.path("matches");
        if (!fixtures.isArray()) return 0;

        League league = leagueRepo.findByExternalCode(code).orElseGet(() -> {
            String name = root.path("competition").path("name").asText(code);
            Sport football = sportRepo.findBySlug("football").orElseThrow();
            return leagueRepo.save(League.builder()
                    .name(name).slug(slugify(name)).externalCode(code)
                    .country(root.path("competition").path("area").path("name").asText(null))
                    .logoUrl(root.path("competition").path("emblem").asText(null))
                    .sport(football).active(true).sortOrder(50).build());
        });

        int count = 0;
        for (JsonNode f : fixtures) {
            String extId = code + "-" + f.path("id").asText();
            Optional<MatchEntity> existing = matchRepo.findByExternalId(extId);

            MatchEntity m = existing.orElseGet(MatchEntity::new);
            if (existing.isPresent() && Boolean.TRUE.equals(m.getAdminLocked())) continue;

            m.setExternalId(extId);
            m.setLeague(league);
            m.setHomeTeam(f.path("homeTeam").path("name").asText("TBD"));
            m.setAwayTeam(f.path("awayTeam").path("name").asText("TBD"));
            m.setHomeLogo(f.path("homeTeam").path("crest").asText(null));
            m.setAwayLogo(f.path("awayTeam").path("crest").asText(null));
            m.setKickoffUtc(Instant.parse(f.path("utcDate").asText()));
            m.setStatus(mapStatus(f.path("status").asText("SCHEDULED")));

            JsonNode full = f.path("score").path("fullTime");
            if (!full.path("home").isNull()) m.setHomeScore(full.path("home").asText());
            if (!full.path("away").isNull()) m.setAwayScore(full.path("away").asText());

            if (m.getDescription() == null || m.getDescription().isBlank()) {
                String md = f.path("matchday").isMissingNode() ? "" : "Matchday " + f.path("matchday").asInt();
                m.setDescription((league.getName() + " " + md).trim());
            }
            if (m.getVisible() == null) m.setVisible(true);
            matchRepo.save(m);
            count++;
        }
        return count;
    }

    private MatchStatus mapStatus(String s) {
        return switch (s) {
            case "IN_PLAY", "PAUSED" -> MatchStatus.LIVE;
            case "FINISHED", "AWARDED" -> MatchStatus.FINISHED;
            case "POSTPONED", "SUSPENDED" -> MatchStatus.POSTPONED;
            case "CANCELLED" -> MatchStatus.CANCELLED;
            default -> MatchStatus.UPCOMING;
        };
    }

    /**
     * Safety net for manually-added matches: flips UPCOMING to LIVE at kick-off,
     * and LIVE to FINISHED 2h45m later, so badges are never stale.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void autoStatus() {
        Instant now = Instant.now();
        matchRepo.findByStatusAndKickoffUtcBefore(MatchStatus.UPCOMING, now)
                .forEach(m -> { m.setStatus(MatchStatus.LIVE); matchRepo.save(m); });

        matchRepo.findByStatus(MatchStatus.LIVE).stream()
                .filter(m -> m.getKickoffUtc().plus(165, ChronoUnit.MINUTES).isBefore(now))
                .forEach(m -> { m.setStatus(MatchStatus.FINISHED); matchRepo.save(m); });
    }

    public static String slugify(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
