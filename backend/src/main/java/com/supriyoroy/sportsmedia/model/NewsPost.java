package com.supriyoroy.sportsmedia.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity @Table(name = "news_posts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NewsPost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    private String summary;
    private String coverUrl;
    private String sportSlug;

    @Column(length = 20000)
    private String body;

    private Boolean published = true;
    private Instant createdAt = Instant.now();
}
