package com.supriyoroy.sportsmedia.repo;

import com.supriyoroy.sportsmedia.model.AdSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdSlotRepository extends JpaRepository<AdSlot, Long> {
    List<AdSlot> findByActiveTrueOrderBySortOrderAsc();
}
