package com.supriyoroy.sportsmedia.repo;

import com.supriyoroy.sportsmedia.model.MatchEntity;
import com.supriyoroy.sportsmedia.model.MatchStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

    Optional<MatchEntity> findByExternalId(String externalId);

    List<MatchEntity> findByVisibleTrueAndStatusOrderByKickoffUtcAsc(MatchStatus status);

    List<MatchEntity> findByVisibleTrueAndFeaturedTrueOrderByKickoffUtcAsc();

    @Query("""
           select m from MatchEntity m
           where m.visible = true
             and (:sportSlug is null or m.league.sport.slug = :sportSlug)
             and (:leagueSlug is null or m.league.slug = :leagueSlug)
             and (:status is null or m.status = :status)
           order by
             case when m.status = com.supriyoroy.sportsmedia.model.MatchStatus.LIVE then 0 else 1 end,
             m.kickoffUtc asc
           """)
    Page<MatchEntity> browse(@Param("sportSlug") String sportSlug,
                             @Param("leagueSlug") String leagueSlug,
                             @Param("status") MatchStatus status,
                             Pageable pageable);

    @Query("""
           select m from MatchEntity m
           where m.visible = true and (
                 lower(m.homeTeam) like lower(concat('%', :q, '%'))
              or lower(m.awayTeam) like lower(concat('%', :q, '%'))
              or lower(m.description) like lower(concat('%', :q, '%'))
              or lower(m.league.name) like lower(concat('%', :q, '%')))
           order by m.kickoffUtc asc
           """)
    List<MatchEntity> search(@Param("q") String q, Pageable pageable);

    /** Used by the auto status job: kicked off but not yet marked live. */
    List<MatchEntity> findByStatusAndKickoffUtcBefore(MatchStatus status, Instant time);

    List<MatchEntity> findByStatus(MatchStatus status);

    @Modifying
    @Query("update MatchEntity m set m.views = m.views + 1 where m.id = :id")
    void incrementViews(@Param("id") Long id);
}
