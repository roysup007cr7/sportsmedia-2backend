// package com.supriyoroy.sportsmedia.model;

// import jakarta.persistence.*;
// import lombok.*;

// import java.time.Instant;

// /**
//  * One fixture. Kick-off is stored in UTC (Instant) and converted to the
//  * viewer's own device timezone in the browser, so nobody has to do maths.
//  */
// @Entity @Table(name = "matches", indexes = {
//         @Index(name = "idx_match_kickoff", columnList = "kickoffUtc"),
//         @Index(name = "idx_match_status", columnList = "status")
// })
// @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
// public class MatchEntity {
//     @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     @Column(nullable = false)
//     private String homeTeam;

//     @Column(nullable = false)
//     private String awayTeam;

//     private String homeLogo;
//     private String awayLogo;

//     /** Free-text note shown under the title, e.g. "Matchday 7 · Santiago Bernabeu". */
//     @Column(length = 2000)
//     private String description;

//     @Column(nullable = false)
//     private Instant kickoffUtc;

//     @Enumerated(EnumType.STRING)
//     @Column(nullable = false, length = 20)
//     private MatchStatus status = MatchStatus.UPCOMING;

//     private String homeScore;
//     private String awayScore;

//     /** The embed/iframe/HLS URL you paste in the admin panel. Never returned unless the match is live. */
//     @Column(length = 1000)
//     private String streamUrl;

//     /** Optional extra feeds: "Hindi|https://...", one per line. */
//     @Column(length = 3000)
//     private String backupStreams;

//     private String posterUrl;
//     private Boolean featured = false;
//     private Boolean visible = true;
//     private Long views = 0L;

// @Column(name = "ext_stream1")
// private String extStream1;

// @Column(name = "ext_stream2")
// private String extStream2;

// @Column(name = "ext_stream3")
// private String extStream3;

// // Add Getters and Setters for all 3 fields below
// public String getExtStream1() { return extStream1; }
// public void setExtStream1(String extStream1) { this.extStream1 = extStream1; }

// public String getExtStream2() { return extStream2; }
// public void setExtStream2(String extStream2) { this.extStream2 = extStream2; }

// public String getExtStream3() { return extStream3; }
// public void setExtStream3(String extStream3) { this.extStream3 = extStream3; }

//     /** Set when the fixture came from the schedule API, so a re-sync updates instead of duplicating. */
//     @Column(unique = true)
//     private String externalId;

//     /** True once you edit it by hand — the sync job then leaves your changes alone. */
//     private Boolean adminLocked = false;

//     @ManyToOne(fetch = FetchType.EAGER)
//     @JoinColumn(name = "league_id")
//     private League league;

//     private Instant createdAt = Instant.now();
//     private Instant updatedAt = Instant.now();

//     @PreUpdate
//     public void touch() { this.updatedAt = Instant.now(); }
// }
package com.supriyoroy.sportsmedia.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "matches")
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String homeTeam;
    private String awayTeam;
    private String homeLogo;
    private String awayLogo;

    private String leagueName;
    private String leagueSlug;
    private String sportSlug;
    private String sportName;

    private Instant kickoffUtc;

    @Enumerated(EnumType.STRING)
    private MatchStatus status = MatchStatus.UPCOMING;

    private String homeScore;
    private String awayScore;

    @Column(length = 2000)
    private String description;

    private boolean featured;
    private long views;

    @Column(length = 1000)
    private String streamUrl;

    // External Backup Streams matching Dtos (extStream1, extStream2, extStream3)
    @Column(length = 1000)
    private String extStream1;

    @Column(length = 1000)
    private String extStream2;

    @Column(length = 1000)
    private String extStream3;

    // External API Sync mapping
    private String externalId;
    private String apiProvider;

    public MatchEntity() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHomeTeam() { return homeTeam; }
    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }

    public String getAwayTeam() { return awayTeam; }
    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }

    public String getHomeLogo() { return homeLogo; }
    public void setHomeLogo(String homeLogo) { this.homeLogo = homeLogo; }

    public String getAwayLogo() { return awayLogo; }
    public void setAwayLogo(String awayLogo) { this.awayLogo = awayLogo; }

    public String getLeagueName() { return leagueName; }
    public void setLeagueName(String leagueName) { this.leagueName = leagueName; }

    public String getLeagueSlug() { return leagueSlug; }
    public void setLeagueSlug(String leagueSlug) { this.leagueSlug = leagueSlug; }

    public String getSportSlug() { return sportSlug; }
    public void setSportSlug(String sportSlug) { this.sportSlug = sportSlug; }

    public String getSportName() { return sportName; }
    public void setSportName(String sportName) { this.sportName = sportName; }

    public Instant getKickoffUtc() { return kickoffUtc; }
    public void setKickoffUtc(Instant kickoffUtc) { this.kickoffUtc = kickoffUtc; }

    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }

    public String getHomeScore() { return homeScore; }
    public void setHomeScore(String homeScore) { this.homeScore = homeScore; }

    public String getAwayScore() { return awayScore; }
    public void setAwayScore(String awayScore) { this.awayScore = awayScore; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public long getViews() { return views; }
    public void setViews(long views) { this.views = views; }

    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }

    public String getExtStream1() { return extStream1; }
    public void setExtStream1(String extStream1) { this.extStream1 = extStream1; }

    public String getExtStream2() { return extStream2; }
    public void setExtStream2(String extStream2) { this.extStream2 = extStream2; }

    public String getExtStream3() { return extStream3; }
    public void setExtStream3(String extStream3) { this.extStream3 = extStream3; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getApiProvider() { return apiProvider; }
    public void setApiProvider(String apiProvider) { this.apiProvider = apiProvider; }
}