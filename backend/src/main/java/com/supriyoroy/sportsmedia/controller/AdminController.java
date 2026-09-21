package com.supriyoroy.sportsmedia.controller;

import com.supriyoroy.sportsmedia.dto.Dtos.*;
import com.supriyoroy.sportsmedia.model.*;
import com.supriyoroy.sportsmedia.repo.*;
import com.supriyoroy.sportsmedia.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Everything behind the admin login. Requires: Authorization: Bearer <token> */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MatchRepository matchRepo;
    private final LeagueRepository leagueRepo;
    private final SportRepository sportRepo;
    private final CommentRepository commentRepo;
    private final AdSlotRepository adRepo;
    private final NewsPostRepository newsRepo;
    private final ContactMessageRepository contactRepo;
    private final PaymentOrderRepository paymentRepo;
    private final SettingsService settings;
    private final ScheduleSyncService sync;

    @GetMapping("/stats")
    public DashboardStats stats() {
        long views = matchRepo.findAll().stream()
                .mapToLong(m -> m.getViews() == null ? 0 : m.getViews()).sum();
        return new DashboardStats(
                matchRepo.count(),
                matchRepo.findByStatus(MatchStatus.LIVE).size(),
                matchRepo.findByStatus(MatchStatus.UPCOMING).size(),
                commentRepo.countByApprovedFalse(),
                contactRepo.countByReadFalse(),
                views,
                sync.lastSync());
    }

    // ---------- matches ----------

    @GetMapping("/matches")
    public List<MatchEntity> allMatches() { return matchRepo.findAll(); }

    @PostMapping("/matches")
    public MatchEntity create(@Valid @RequestBody MatchRequest req) {
        return matchRepo.save(apply(new MatchEntity(), req));
    }

    @PutMapping("/matches/{id}")
    public ResponseEntity<MatchEntity> update(@PathVariable Long id, @Valid @RequestBody MatchRequest req) {
        return matchRepo.findById(id)
                .map(m -> ResponseEntity.ok(matchRepo.save(apply(m, req))))
                .orElse(ResponseEntity.notFound().build());
    }

    /** The one you'll use most: paste a stream link onto an existing fixture. */
    @PatchMapping("/matches/{id}/stream")
    public ResponseEntity<MatchEntity> setStream(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body) {
        return matchRepo.findById(id).map(m -> {
            m.setStreamUrl(body.get("streamUrl"));
            if (body.containsKey("backupStreams")) m.setBackupStreams(body.get("backupStreams"));
            m.setAdminLocked(true);
            return ResponseEntity.ok(matchRepo.save(m));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/matches/{id}/status")
    public ResponseEntity<MatchEntity> setStatus(@PathVariable Long id, @RequestParam String status) {
        return matchRepo.findById(id).map(m -> {
            m.setStatus(MatchStatus.valueOf(status.toUpperCase()));
            m.setAdminLocked(true);
            return ResponseEntity.ok(matchRepo.save(m));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/matches/{id}")
    public ApiMessage deleteMatch(@PathVariable Long id) {
        matchRepo.deleteById(id);
        return new ApiMessage(true, "Match deleted.");
    }

    private MatchEntity apply(MatchEntity m, MatchRequest r) {
        m.setHomeTeam(r.homeTeam());
        m.setAwayTeam(r.awayTeam());
        m.setHomeLogo(r.homeLogo());
        m.setAwayLogo(r.awayLogo());
        m.setDescription(r.description());
        m.setKickoffUtc(r.kickoffUtc());
        if (r.status() != null && !r.status().isBlank())
            m.setStatus(MatchStatus.valueOf(r.status().toUpperCase()));
        m.setHomeScore(r.homeScore());
        m.setAwayScore(r.awayScore());
        m.setStreamUrl(r.streamUrl());
        m.setBackupStreams(r.backupStreams());
        m.setPosterUrl(r.posterUrl());
        m.setFeatured(r.featured() != null && r.featured());
        m.setVisible(r.visible() == null || r.visible());
        m.setAdminLocked(true);
        if (r.leagueId() != null) leagueRepo.findById(r.leagueId()).ifPresent(m::setLeague);
        return m;
    }

    // ---------- catalogue ----------

    @PostMapping("/sports")
    public Sport createSport(@Valid @RequestBody SportRequest r) {
        return sportRepo.save(Sport.builder()
                .name(r.name())
                .slug(r.slug() == null || r.slug().isBlank() ? ScheduleSyncService.slugify(r.name()) : r.slug())
                .icon(r.icon()).accentColor(r.accentColor())
                .sortOrder(r.sortOrder() == null ? 0 : r.sortOrder())
                .active(r.active() == null || r.active()).build());
    }

    @DeleteMapping("/sports/{id}")
    public ApiMessage deleteSport(@PathVariable Long id) {
        sportRepo.deleteById(id);
        return new ApiMessage(true, "Category deleted.");
    }

    @PostMapping("/leagues")
    public ResponseEntity<League> createLeague(@Valid @RequestBody LeagueRequest r) {
        return sportRepo.findById(r.sportId()).map(sp -> ResponseEntity.ok(leagueRepo.save(League.builder()
                .name(r.name())
                .slug(r.slug() == null || r.slug().isBlank() ? ScheduleSyncService.slugify(r.name()) : r.slug())
                .country(r.country()).logoUrl(r.logoUrl()).externalCode(r.externalCode())
                .sortOrder(r.sortOrder() == null ? 0 : r.sortOrder())
                .active(r.active() == null || r.active()).sport(sp).build())))
                .orElse(ResponseEntity.badRequest().build());
    }

    @DeleteMapping("/leagues/{id}")
    public ApiMessage deleteLeague(@PathVariable Long id) {
        leagueRepo.deleteById(id);
        return new ApiMessage(true, "League deleted.");
    }

    // ---------- comments ----------

    @GetMapping("/comments/pending")
    public List<Comment> pending() { return commentRepo.findByApprovedFalseOrderByCreatedAtDesc(); }

    @GetMapping("/comments")
    public List<Comment> allComments() { return commentRepo.findAll(); }

    @PatchMapping("/comments/{id}/approve")
    public ApiMessage approve(@PathVariable Long id) {
        commentRepo.findById(id).ifPresent(c -> { c.setApproved(true); commentRepo.save(c); });
        return new ApiMessage(true, "Comment approved.");
    }

    @PatchMapping("/comments/{id}/pin")
    public ApiMessage pin(@PathVariable Long id) {
        commentRepo.findById(id).ifPresent(c -> { c.setPinned(!Boolean.TRUE.equals(c.getPinned())); commentRepo.save(c); });
        return new ApiMessage(true, "Pin toggled.");
    }

    @DeleteMapping("/comments/{id}")
    public ApiMessage deleteComment(@PathVariable Long id) {
        commentRepo.deleteById(id);
        return new ApiMessage(true, "Comment deleted.");
    }

    // ---------- ads ----------

    @GetMapping("/ads")
    public List<AdSlot> ads() { return adRepo.findAll(); }

    @PostMapping("/ads")
    public AdSlot createAd(@Valid @RequestBody AdSlotRequest r) {
        return adRepo.save(AdSlot.builder().name(r.name()).placement(r.placement()).code(r.code())
                .active(r.active() == null || r.active())
                .sortOrder(r.sortOrder() == null ? 0 : r.sortOrder()).build());
    }

    @PutMapping("/ads/{id}")
    public ResponseEntity<AdSlot> updateAd(@PathVariable Long id, @Valid @RequestBody AdSlotRequest r) {
        return adRepo.findById(id).map(a -> {
            a.setName(r.name()); a.setPlacement(r.placement()); a.setCode(r.code());
            a.setActive(r.active() == null || r.active());
            a.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder());
            return ResponseEntity.ok(adRepo.save(a));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/ads/{id}")
    public ApiMessage deleteAd(@PathVariable Long id) {
        adRepo.deleteById(id);
        return new ApiMessage(true, "Ad slot deleted.");
    }

    // ---------- news, messages, settings, sync ----------

    @PostMapping("/news")
    public NewsPost createNews(@Valid @RequestBody NewsRequest r) {
        return newsRepo.save(NewsPost.builder().title(r.title())
                .slug(r.slug() == null || r.slug().isBlank() ? ScheduleSyncService.slugify(r.title()) : r.slug())
                .summary(r.summary()).coverUrl(r.coverUrl()).sportSlug(r.sportSlug())
                .body(r.body()).published(r.published() == null || r.published()).build());
    }

    @DeleteMapping("/news/{id}")
    public ApiMessage deleteNews(@PathVariable Long id) {
        newsRepo.deleteById(id);
        return new ApiMessage(true, "Post deleted.");
    }

    @GetMapping("/messages")
    public List<ContactMessage> messages() { return contactRepo.findAllByOrderByCreatedAtDesc(); }

    @PatchMapping("/messages/{id}/read")
    public ApiMessage markRead(@PathVariable Long id) {
        contactRepo.findById(id).ifPresent(m -> { m.setRead(true); contactRepo.save(m); });
        return new ApiMessage(true, "Marked as read.");
    }

    @GetMapping("/payments")
    public List<PaymentOrder> payments() { return paymentRepo.findAllByOrderByCreatedAtDesc(); }

    @GetMapping("/settings")
    public Map<String, String> settings() { return settings.all(); }

    @PutMapping("/settings")
    public ApiMessage saveSettings(@RequestBody SettingsRequest req) {
        settings.putAll(req.settings());
        return new ApiMessage(true, "Settings saved.");
    }

    @PostMapping("/sync")
    public ApiMessage syncNow() {
        int n = sync.syncAll();
        return new ApiMessage(true, n + " fixtures pulled from the schedule API.");
    }
}
