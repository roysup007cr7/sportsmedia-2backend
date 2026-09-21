package com.supriyoroy.sportsmedia.config;

import com.supriyoroy.sportsmedia.model.*;
import com.supriyoroy.sportsmedia.repo.*;
import com.supriyoroy.sportsmedia.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** First-boot data: admin account, sports, a few leagues, sample fixtures, default settings. */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class DataSeeder {

    private final SportRepository sports;
    private final LeagueRepository leagues;
    private final MatchRepository matches;
    private final AdminUserRepository admins;
    private final AdSlotRepository ads;
    private final SettingsService settings;
    private final PasswordEncoder encoder;

    @Value("${app.admin.username}") private String adminUser;
    @Value("${app.admin.password}") private String adminPass;

    @Bean
    public ApplicationRunner seed() {
        return args -> {
            if (admins.findByUsername(adminUser).isEmpty()) {
                admins.save(AdminUser.builder()
                        .username(adminUser)
                        .passwordHash(encoder.encode(adminPass))
                        .displayName("Supriyo Roy")
                        .role("ADMIN").enabled(true).build());
                log.info("Admin account created: {} — change the password after first login", adminUser);
            }

            seedSettings();
            if (sports.count() > 0) return;

            Sport football = sports.save(Sport.builder().name("Football").slug("football")
                    .icon("⚽").accentColor("#16E6A0").sortOrder(1).active(true).build());
            Sport cricket = sports.save(Sport.builder().name("Cricket").slug("cricket")
                    .icon("🏏").accentColor("#FFB020").sortOrder(2).active(true).build());
            sports.save(Sport.builder().name("Kabaddi").slug("kabaddi")
                    .icon("🤼").accentColor("#7C5CFF").sortOrder(3).active(true).build());
            sports.save(Sport.builder().name("Tennis").slug("tennis")
                    .icon("🎾").accentColor("#4FC3F7").sortOrder(4).active(true).build());

            League laliga = leagues.save(League.builder().name("LaLiga").slug("laliga")
                    .country("Spain").externalCode("PD").sport(football).sortOrder(1).active(true).build());
            League ucl = leagues.save(League.builder().name("UEFA Champions League").slug("champions-league")
                    .country("Europe").externalCode("CL").sport(football).sortOrder(2).active(true).build());
            League isl = leagues.save(League.builder().name("Indian Super League").slug("isl")
                    .country("India").sport(football).sortOrder(3).active(true).build());
            leagues.save(League.builder().name("Premier League").slug("premier-league")
                    .country("England").externalCode("PL").sport(football).sortOrder(4).active(true).build());
            leagues.save(League.builder().name("I-League").slug("i-league")
                    .country("India").sport(football).sortOrder(5).active(true).build());
            leagues.save(League.builder().name("Indian Premier League").slug("ipl")
                    .country("India").sport(cricket).sortOrder(6).active(true).build());

            Instant now = Instant.now();
            matches.save(MatchEntity.builder().homeTeam("Real Madrid").awayTeam("Barcelona")
                    .description("El Clasico · Santiago Bernabeu").league(laliga)
                    .kickoffUtc(now.minus(35, ChronoUnit.MINUTES)).status(MatchStatus.LIVE)
                    .homeScore("1").awayScore("1").featured(true).visible(true)
                    .streamUrl("").adminLocked(true).build());
            matches.save(MatchEntity.builder().homeTeam("Bayern Munich").awayTeam("Arsenal")
                    .description("Champions League · Quarter-final, 1st leg").league(ucl)
                    .kickoffUtc(now.plus(3, ChronoUnit.HOURS)).status(MatchStatus.UPCOMING)
                    .featured(true).visible(true).adminLocked(true).build());
            matches.save(MatchEntity.builder().homeTeam("Mohun Bagan SG").awayTeam("East Bengal")
                    .description("Kolkata Derby · Salt Lake Stadium").league(isl)
                    .kickoffUtc(now.plus(26, ChronoUnit.HOURS)).status(MatchStatus.UPCOMING)
                    .visible(true).adminLocked(true).build());
            matches.save(MatchEntity.builder().homeTeam("Atletico Madrid").awayTeam("Sevilla")
                    .description("LaLiga · Matchday 6").league(laliga)
                    .kickoffUtc(now.minus(1, ChronoUnit.DAYS)).status(MatchStatus.FINISHED)
                    .homeScore("2").awayScore("0").visible(true).adminLocked(true).build());

            if (ads.count() == 0) {
                ads.save(AdSlot.builder().name("Header banner").placement("header").active(false).sortOrder(1).code("").build());
                ads.save(AdSlot.builder().name("Above player").placement("above-player").active(false).sortOrder(2).code("").build());
                ads.save(AdSlot.builder().name("In feed").placement("in-feed").active(false).sortOrder(3).code("").build());
                ads.save(AdSlot.builder().name("Footer").placement("footer").active(false).sortOrder(4).code("").build());
            }
            log.info("Seed data loaded.");
        };
    }

    private void seedSettings() {
        settings.seed("site.name", "Sports Media");
        settings.seed("site.tagline", "Every kick-off, in your time zone.");
        settings.seed("site.baseUrl", ""); // left blank on purpose: share links use whatever URL the site is served from
        settings.seed("site.owner", "Supriyo Roy");
        settings.seed("social.facebook", "");
        settings.seed("social.instagram", "");
        settings.seed("social.youtube", "");
        settings.seed("social.x", "");
        settings.seed("social.whatsapp", "https://whatsapp.com/channel/0029VbDbRJg9MF94wETZ7u0l");
        settings.seed("social.telegram", "https://t.me/sportsmedialive1");
        settings.seed("popup.enabled", "true");
        settings.seed("popup.title", "Never miss a kick-off");
        settings.seed("popup.body", "Join the WhatsApp and Telegram channels for match links and schedule changes.");
        settings.seed("popup.delaySeconds", "4");
        settings.seed("payments.upiId", "");
        settings.seed("payments.payeeName", "Supriyo Roy");
        settings.seed("contact.email", "contact@yoursite.com");
        settings.seed("page.about", "Sports Media is run by Supriyo Roy. It collects football and cricket "
                + "fixtures in one schedule, shows every kick-off in your own device time, and links to the "
                + "broadcasts we are authorised to carry.");
        settings.seed("page.disclaimer", "Sports Media does not host, upload or record any video. Listings point "
                + "to third-party broadcasts. All competition names, crests and footage belong to their rights "
                + "holders. If you own rights to something listed here, write to us and it will be removed.");
        settings.seed("page.privacy", "We store the name you type on a comment and an anonymised form of your IP "
                + "address for moderation. Advertising partners may set their own cookies. We do not sell personal data.");
        settings.seed("comments.autoApprove", "false");
    }
}
