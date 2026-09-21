package com.supriyoroy.sportsmedia.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Viewer opinions. Held as approved=false until you approve them in the admin panel. */
@Entity @Table(name = "comments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, length = 1500)
    private String body;

    /** Null = a general site comment rather than a match comment. */
    private Long matchId;

    private Boolean approved = false;
    private Boolean pinned = false;
    private String ipHash;
    private Instant createdAt = Instant.now();
}
