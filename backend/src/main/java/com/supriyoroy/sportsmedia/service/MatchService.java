package com.supriyoroy.sportsmedia.service;

import com.supriyoroy.sportsmedia.dto.Dtos.*;
import com.supriyoroy.sportsmedia.model.*;
import com.supriyoroy.sportsmedia.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepo;
    private final SettingsService settings;

    /**
     * The stream URL only leaves the server while the match is LIVE.
     * That keeps links out of page source before kick-off.
     */
    // public MatchView toView(MatchEntity m) {
    //     boolean live = m.getStatus() == MatchStatus.LIVE;
    //     League l = m.getLeague();
    //     Sport s = l != null ? l.getSport() : null;
    //     String base = settings.get("site.baseUrl", "");

    //     return new MatchView(
    //             m.getId(), m.getHomeTeam(), m.getAwayTeam(), m.getHomeLogo(), m.getAwayLogo(),
    //             m.getDescription(), m.getKickoffUtc(), m.getStatus().name(),
    //             m.getHomeScore(), m.getAwayScore(), m.getPosterUrl(),
    //             Boolean.TRUE.equals(m.getFeatured()), m.getViews() == null ? 0 : m.getViews(),
    //             l != null ? l.getName() : null,
    //             l != null ? l.getSlug() : null,
    //             l != null ? l.getLogoUrl() : null,
    //             s != null ? s.getName() : null,
    //             s != null ? s.getSlug() : null,
    //             live ? m.getStreamUrl() : null,
    //             live ? parseBackups(m.getBackupStreams()) : List.of(),
    //             base.isBlank() ? null : base + "/#match-" + m.getId()   // blank -> the page builds its own link
    //     );
    // }
public MatchView toView(MatchEntity m) {
        boolean live = m.getStatus() == MatchStatus.LIVE;
        League l = m.getLeague();
        Sport s = l != null ? l.getSport() : null;
        String base = settings.get("site.baseUrl", "");

        return new MatchView(
                m.getId(), m.getHomeTeam(), m.getAwayTeam(), m.getHomeLogo(), m.getAwayLogo(),
                m.getDescription(), m.getKickoffUtc(), m.getStatus().name(),
                m.getHomeScore(), m.getAwayScore(), m.getPosterUrl(),
                Boolean.TRUE.equals(m.getFeatured()), m.getViews() == null ? 0 : m.getViews(),
                l != null ? l.getName() : null,
                l != null ? l.getSlug() : null,
                l != null ? l.getLogoUrl() : null,
                s != null ? s.getName() : null,
                s != null ? s.getSlug() : null,
                live ? m.getStreamUrl() : null,
                live ? parseBackups(m.getBackupStreams()) : List.of(),
                base.isBlank() ? null : base + "/#match-" + m.getId(),
                live ? m.getExtStream1() : null,
                live ? m.getExtStream2() : null,
                live ? m.getExtStream3() : null
        );
    }


matchEntity.setExtStream1(dto.extStream1);
matchEntity.setExtStream2(dto.extStream2);
matchEntity.setExtStream3(dto.extStream3);

    private List<StreamOption> parseBackups(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<StreamOption> out = new ArrayList<>();
        for (String line : raw.split("\\r?\\n")) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\|", 2);
            out.add(parts.length == 2
                    ? new StreamOption(parts[0].trim(), parts[1].trim())
                    : new StreamOption("Server " + (out.size() + 2), parts[0].trim()));
        }
        return out;
    }

    public List<MatchView> toViews(List<MatchEntity> list) {
        return list.stream().map(this::toView).toList();
    }

    @Transactional
    public void countView(Long id) {
        matchRepo.incrementViews(id);
    }
}
