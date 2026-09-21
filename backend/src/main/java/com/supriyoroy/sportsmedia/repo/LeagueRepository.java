package com.supriyoroy.sportsmedia.repo;

import com.supriyoroy.sportsmedia.model.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, Long> {
    Optional<League> findBySlug(String slug);
    Optional<League> findByExternalCode(String externalCode);
    List<League> findByActiveTrueOrderBySortOrderAsc();
    List<League> findBySportSlugAndActiveTrueOrderBySortOrderAsc(String sportSlug);
}
