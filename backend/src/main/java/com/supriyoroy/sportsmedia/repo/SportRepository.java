package com.supriyoroy.sportsmedia.repo;

import com.supriyoroy.sportsmedia.model.Sport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SportRepository extends JpaRepository<Sport, Long> {
    Optional<Sport> findBySlug(String slug);
    List<Sport> findByActiveTrueOrderBySortOrderAsc();
}
