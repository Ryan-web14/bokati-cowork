package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.stock.enums.InventoryPeriodStatus;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InventoryPeriodRepository extends JpaRepository<InventoryPeriod, Long> {

    Optional<InventoryPeriod> findByPeriodCode(String periodCode);

    boolean existsByPeriodCode(String periodCode);

    List<InventoryPeriod> findAllByOrderByStartDateDesc();

    List<InventoryPeriod> findAllByStatusOrderByStartDateDesc(InventoryPeriodStatus status);

    /** Periode couvrant la date fournie, s'il en existe une. */
    @Query("""
            SELECT period FROM InventoryPeriod period
            WHERE period.startDate <= :date AND period.endDate >= :date
            """)
    Optional<InventoryPeriod> findCovering(@Param("date") LocalDate date);

    /** Periodes chevauchant l'intervalle fourni, hors periode donnee. */
    @Query("""
            SELECT period FROM InventoryPeriod period
            WHERE period.startDate <= :endDate
              AND period.endDate >= :startDate
              AND (:excludedId IS NULL OR period.id <> :excludedId)
            """)
    List<InventoryPeriod> findOverlapping(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("excludedId") Long excludedId);
}
