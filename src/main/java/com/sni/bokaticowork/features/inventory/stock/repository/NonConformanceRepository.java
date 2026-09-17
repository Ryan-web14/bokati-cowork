package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.stock.model.NonConformance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NonConformanceRepository extends JpaRepository<NonConformance, Long> {

    Optional<NonConformance> findByNonConformanceCode(String nonConformanceCode);

    boolean existsByNonConformanceCode(String nonConformanceCode);

    @Query("""
            SELECT record FROM NonConformance record
            WHERE (:itemCode IS NULL OR record.item.itemCode = :itemCode)
              AND (:openOnly IS NULL OR :openOnly = FALSE OR record.closedAt IS NULL)
            ORDER BY record.detectedAt DESC
            """)
    Page<NonConformance> search(@Param("itemCode") String itemCode,
                                @Param("openOnly") Boolean openOnly,
                                Pageable pageable);
}
