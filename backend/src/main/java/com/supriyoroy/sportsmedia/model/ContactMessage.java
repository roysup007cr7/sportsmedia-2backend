package com.supriyoroy.sportsmedia.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity @Table(name = "contact_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String subject;

    @Column(length = 4000)
    private String body;

    private Boolean read = false;
    private Instant createdAt = Instant.now();
}
