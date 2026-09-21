package com.supriyoroy.sportsmedia.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** All request/response shapes in one place. */
public final class Dtos {

    private Dtos() {}

    // ---------- auth ----------
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String token, String displayName, long expiresInSeconds) {}

    // ---------- public reads ----------
    public record SportView(Long id, String name, String slug, String icon, String accentColor) {}

    public record LeagueView(Long id, String name, String slug, String country,
                             String logoUrl, String sportSlug) {}

    public record MatchView(
            Long id, String homeTeam, String awayTeam, String homeLogo, String awayLogo,
            String description, Instant kickoffUtc, String status,
            String homeScore, String awayScore, String posterUrl,
            boolean featured, long views,
            String leagueName, String leagueSlug, String leagueLogo,
            String sportName, String sportSlug,
            /** Only filled when the match is actually live — otherwise null. */
            String streamUrl,
            List<StreamOption> extraStreams,
            String shareUrl
    ) {}

    public record StreamOption(String label, String url) {}

    public record NewsView(Long id, String title, String slug, String summary,
                           String coverUrl, String sportSlug, String body, Instant createdAt) {}

    public record CommentView(Long id, String author, String body, boolean pinned, Instant createdAt) {}

    public record AdSlotView(String placement, String code) {}

    public record HomePayload(
            List<MatchView> live,
            List<MatchView> upcoming,
            List<MatchView> finished,
            List<MatchView> featured,
            List<SportView> sports,
            List<LeagueView> leagues,
            List<NewsView> news,
            Map<String, String> settings,
            List<AdSlotView> ads,
            Instant serverTime
    ) {}

public String extStream1;
public String extStream2;
public String extStream3;

    // ---------- public writes ----------
    public record CommentRequest(@NotBlank @Size(max = 60) String author,
                                 @NotBlank @Size(max = 1500) String body,
                                 Long matchId) {}

    public record ContactRequest(@NotBlank String name, @Email String email,
                                 String subject, @NotBlank @Size(max = 4000) String body) {}

    public record PaymentIntentRequest(@NotBlank String planCode, @NotNull BigDecimal amount,
                                       String method, String customerName,
                                       String customerEmail, String customerPhone) {}

    public record PaymentIntentResponse(String orderRef, String status, BigDecimal amount,
                                        String currency, String upiDeepLink, String note) {}

    // ---------- admin writes ----------
    public record MatchRequest(
            @NotBlank String homeTeam, @NotBlank String awayTeam,
            String homeLogo, String awayLogo, String description,
            @NotNull Instant kickoffUtc, String status,
            String homeScore, String awayScore,
            String streamUrl, String backupStreams, String posterUrl,
            Boolean featured, Boolean visible, Long leagueId
    ) {}

    public record LeagueRequest(@NotBlank String name, String slug, String country,
                                String logoUrl, String externalCode,
                                Integer sortOrder, Boolean active, @NotNull Long sportId) {}

    public record SportRequest(@NotBlank String name, String slug, String icon,
                               String accentColor, Integer sortOrder, Boolean active) {}

    public record AdSlotRequest(String name, @NotBlank String placement, String code,
                                Boolean active, Integer sortOrder) {}

    public record NewsRequest(@NotBlank String title, String slug, String summary,
                              String coverUrl, String sportSlug, String body, Boolean published) {}

    public record SettingsRequest(Map<String, String> settings) {}

    public record DashboardStats(long totalMatches, long liveNow, long upcoming,
                                 long pendingComments, long unreadMessages,
                                 long totalViews, Instant lastSync) {}

    public record ApiMessage(boolean ok, String message) {}
}
