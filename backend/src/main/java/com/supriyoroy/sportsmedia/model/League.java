package com.supriyoroy.sportsmedia.model;

import jakarta.persistence.*;
import lombok.*;

/** A competition inside a sport: LaLiga, Champions League, ISL, IPL... */
@Entity @Table(name = "leagues")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class League {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String country;
    private String logoUrl;

    /** football-data.org competition code, e.g. PD / CL / PL. Blank for manual leagues. */
    private String externalCode;

    private Integer sortOrder = 0;
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sport_id")
    private Sport sport;
}
