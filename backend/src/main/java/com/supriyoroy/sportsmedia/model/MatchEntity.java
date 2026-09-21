package com.supriyoroy.sportsmedia.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One fixture. Kick-off is stored in UTC (Instant) and converted to the
 * viewer's own device timezone in the browser, so nobody has to do maths.
 */
@Entity @Table(name = "matches", indexes = {
        @Index(name = "idx_match_kickoff", columnList = "kickoffUtc"),
        @Index(name = "idx_match_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatchEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String homeTeam;

    @Column(nullable = false)
    private String awayTeam;

    private String homeLogo;
    private String awayLogo;

    /** Free-text note shown under the title, e.g. "Matchday 7 · Santiago Bernabeu". */
    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Instant kickoffUtc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status = MatchStatus.UPCOMING;

    private String homeScore;
    private String awayScore;

    /** The embed/iframe/HLS URL you paste in the admin panel. Never returned unless the match is live. */
    @Column(length = 1000)
    private String streamUrl;

    /** Optional extra feeds: "Hindi|https://...", one per line. */
    @Column(length = 3000)
    private String backupStreams;

    private String posterUrl;
    private Boolean featured = false;
    private Boolean visible = true;
    private Long views = 0L;

    /** Set when the fixture came from the schedule API, so a re-sync updates instead of duplicating. */
    @Column(unique = true)
    private String externalId;

    /** True once you edit it by hand — the sync job then leaves your changes alone. */
    private Boolean adminLocked = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "league_id")
    private League league;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void touch() { this.updatedAt = Instant.now(); }
}
