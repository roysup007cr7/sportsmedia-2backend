package com.supriyoroy.sportsmedia.model;

import jakarta.persistence.*;
import lombok.*;

/** Simple key/value store: social links, WhatsApp + Telegram popup text, page copy, toggles. */
@Entity @Table(name = "site_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SiteSetting {
    @Id
    @Column(name = "setting_key", length = 100)
    private String key;

    @Column(name = "setting_value", length = 10000)
    private String value;
}
