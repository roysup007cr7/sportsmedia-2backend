package com.supriyoroy.sportsmedia.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Ad network code (Adsterra, Monetag, AdSense...) pasted from the admin panel.
 * placement maps to a container in the page, so you never touch HTML to move an ad.
 */
@Entity @Table(name = "ad_slots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdSlot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** header | under-hero | in-feed | sidebar | above-player | below-player | footer | popunder */
    @Column(nullable = false)
    private String placement;

    /** Raw <script> or <ins> snippet from the network. */
    @Column(length = 8000)
    private String code;

    private Boolean active = true;
    private Integer sortOrder = 0;
}
