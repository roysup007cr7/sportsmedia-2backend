package com.supriyoroy.sportsmedia.controller;

import com.supriyoroy.sportsmedia.dto.Dtos.*;
import com.supriyoroy.sportsmedia.model.*;
import com.supriyoroy.sportsmedia.repo.*;
import com.supriyoroy.sportsmedia.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** Everything the website itself calls. No auth. */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final MatchRepository matchRepo;
    private final LeagueRepository leagueRepo;
    private final SportRepository sportRepo;
    private final CommentRepository commentRepo;
    private final AdSlotRepository adRepo;
    private final NewsPostRepository newsRepo;
    private final ContactMessageRepository contactRepo;
    private final MatchService matchService;
    private final SettingsService settings;
    private final PaymentService payments;

    /** One call fills the whole home page — the refresh button re-hits this. */
    @GetMapping("/home")
    public HomePayload home() {
        return new HomePayload(
                matchService.toViews(matchRepo.findByVisibleTrueAndStatusOrderByKickoffUtcAsc(MatchStatus.LIVE)),
                matchService.toViews(matchRepo.findByVisibleTrueAndStatusOrderByKickoffUtcAsc(MatchStatus.UPCOMING)),
                matchService.toViews(matchRepo.findByVisibleTrueAndStatusOrderByKickoffUtcAsc(MatchStatus.FINISHED)),
                matchService.toViews(matchRepo.findByVisibleTrueAndFeaturedTrueOrderByKickoffUtcAsc()),
                sportRepo.findByActiveTrueOrderBySortOrderAsc().stream()
                        .map(s -> new SportView(s.getId(), s.getName(), s.getSlug(), s.getIcon(), s.getAccentColor())).toList(),
                leagueRepo.findByActiveTrueOrderBySortOrderAsc().stream()
                        .map(this::leagueView).toList(),
                newsRepo.findByPublishedTrueOrderByCreatedAtDesc().stream().limit(8).map(this::newsView).toList(),
                settings.all(),
                adRepo.findByActiveTrueOrderBySortOrderAsc().stream()
                        .map(a -> new AdSlotView(a.getPlacement(), a.getCode())).toList(),
                Instant.now()
        );
    }

    @GetMapping("/matches")
    public List<MatchView> matches(@RequestParam(required = false) String sport,
                                   @RequestParam(required = false) String league,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "24") int size) {
        MatchStatus st = status == null || status.isBlank() ? null
                : MatchStatus.valueOf(status.toUpperCase());
        return matchService.toViews(matchRepo
                .browse(blankToNull(sport), blankToNull(league), st, PageRequest.of(page, size))
                .getContent());
    }

    @GetMapping("/matches/{id}")
    public ResponseEntity<MatchView> match(@PathVariable Long id) {
        return matchRepo.findById(id)
                .filter(m -> Boolean.TRUE.equals(m.getVisible()))
                .map(m -> { matchService.countView(id); return ResponseEntity.ok(matchService.toView(m)); })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<MatchView> search(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) return List.of();
        return matchService.toViews(matchRepo.search(q.trim(), PageRequest.of(0, 30)));
    }

    @GetMapping("/sports")
    public List<SportView> sports() {
        return sportRepo.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(s -> new SportView(s.getId(), s.getName(), s.getSlug(), s.getIcon(), s.getAccentColor())).toList();
    }

    @GetMapping("/leagues")
    public List<LeagueView> leagues(@RequestParam(required = false) String sport) {
        var list = (sport == null || sport.isBlank())
                ? leagueRepo.findByActiveTrueOrderBySortOrderAsc()
                : leagueRepo.findBySportSlugAndActiveTrueOrderBySortOrderAsc(sport);
        return list.stream().map(this::leagueView).toList();
    }

    @GetMapping("/news")
    public List<NewsView> news() {
        return newsRepo.findByPublishedTrueOrderByCreatedAtDesc().stream().map(this::newsView).toList();
    }

    @GetMapping("/settings")
    public Map<String, String> settings() { return settings.all(); }

    // ---------- comments ----------

    @GetMapping("/comments")
    public List<CommentView> comments(@RequestParam(required = false) Long matchId) {
        var list = matchId == null
                ? commentRepo.findTop50ByApprovedTrueOrderByPinnedDescCreatedAtDesc()
                : commentRepo.findByApprovedTrueAndMatchIdOrderByPinnedDescCreatedAtDesc(matchId);
        return list.stream().map(c -> new CommentView(c.getId(), c.getAuthor(), c.getBody(),
                Boolean.TRUE.equals(c.getPinned()), c.getCreatedAt())).toList();
    }

    @PostMapping("/comments")
    public ApiMessage postComment(@Valid @RequestBody CommentRequest req, HttpServletRequest http) {
        boolean auto = "true".equalsIgnoreCase(settings.get("comments.autoApprove", "false"));
        commentRepo.save(Comment.builder()
                .author(strip(req.author())).body(strip(req.body()))
                .matchId(req.matchId()).approved(auto)
                .ipHash(hash(http.getRemoteAddr())).build());
        return new ApiMessage(true, auto ? "Posted." : "Sent for review — it appears once approved.");
    }

    @PostMapping("/contact")
    public ApiMessage contact(@Valid @RequestBody ContactRequest req) {
        contactRepo.save(ContactMessage.builder()
                .name(strip(req.name())).email(req.email())
                .subject(strip(req.subject())).body(strip(req.body())).build());
        return new ApiMessage(true, "Message sent. Supriyo will reply to the address you gave.");
    }

    @PostMapping("/payments/intent")
    public PaymentIntentResponse payment(@Valid @RequestBody PaymentIntentRequest req) {
        return payments.create(req);
    }

    // ---------- helpers ----------

    private LeagueView leagueView(League l) {
        return new LeagueView(l.getId(), l.getName(), l.getSlug(), l.getCountry(), l.getLogoUrl(),
                l.getSport() != null ? l.getSport().getSlug() : null);
    }

    private NewsView newsView(NewsPost n) {
        return new NewsView(n.getId(), n.getTitle(), n.getSlug(), n.getSummary(),
                n.getCoverUrl(), n.getSportSlug(), n.getBody(), n.getCreatedAt());
    }

    private String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s; }

    /** Strips tags so a comment can never inject markup into the page. */
    private String strip(String s) {
        return s == null ? null : s.replaceAll("<[^>]*>", "").trim();
    }

    private String hash(String ip) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(ip.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (Exception e) { return "unknown"; }
    }
}
