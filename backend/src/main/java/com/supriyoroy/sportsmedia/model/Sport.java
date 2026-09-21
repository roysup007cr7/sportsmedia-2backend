package com.supriyoroy.sportsmedia.model;

import jakarta.persistence.*;
import lombok.*;

/** Top-level category: Football, Cricket, Kabaddi, Tennis... */
@Entity @Table(name = "sports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String icon;          // emoji or icon class shown in the category strip
    private String accentColor;   // hex, used for the category chip
    private Integer sortOrder = 0;
    private Boolean active = true;
}
