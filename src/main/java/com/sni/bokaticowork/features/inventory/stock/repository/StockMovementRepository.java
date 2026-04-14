package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    boolean existsByMovementCode(String movementCode);

    Optional<StockMovement> findByMovementCode(String movementCode);

    Optional<StockMovement> findFirstByReversalOfMovementOrderByPerformedAtDesc(StockMovement movement);

    @Query("""
            SELECT movement FROM StockMovement movement
            JOIN movement.item item
            LEFT JOIN movement.locationFrom locationFrom
            LEFT JOIN movement.locationTo locationTo
            WHERE (:itemCode IS NULL OR item.itemCode = :itemCode)
              AND (:locationCode IS NULL OR locationFrom.locationCode = :locationCode OR locationTo.locationCode = :locationCode)
              AND (:movementType IS NULL OR movement.movementType = :movementType)
              AND (:referenceType IS NULL OR movement.referenceType = :referenceType)
              AND (:referenceCode IS NULL OR movement.referenceCode = :referenceCode)
              AND (:fromDate IS NULL OR movement.performedAt >= :fromDate)
              AND (:toDate IS NULL OR movement.performedAt <= :toDate)
            """)
    Page<StockMovement> search(@Param("itemCode") String itemCode,
                               @Param("locationCode") String locationCode,
                               @Param("movementType") StockMovementType movementType,
                               @Param("referenceType") StockReferenceType referenceType,
                               @Param("referenceCode") String referenceCode,
                               @Param("fromDate") Instant fromDate,
                               @Param("toDate") Instant toDate,
                               Pageable pageable);

    @Query("""
            SELECT movement.movementType, COUNT(movement), COALESCE(SUM(movement.quantity), 0), COALESCE(SUM(movement.totalCost), 0)
            FROM StockMovement movement
            JOIN movement.item item
            LEFT JOIN movement.locationFrom locationFrom
            LEFT JOIN movement.locationTo locationTo
            WHERE (:itemCode IS NULL OR item.itemCode = :itemCode)
              AND (:locationCode IS NULL OR locationFrom.locationCode = :locationCode OR locationTo.locationCode = :locationCode)
              AND (:fromDate IS NULL OR movement.performedAt >= :fromDate)
              AND (:toDate IS NULL OR movement.performedAt <= :toDate)
            GROUP BY movement.movementType
            """)
    List<Object[]> movementReport(@Param("itemCode") String itemCode,
                                  @Param("locationCode") String locationCode,
                                  @Param("fromDate") Instant fromDate,
                                  @Param("toDate") Instant toDate);

    long countByAllowNegativeOverrideTrue();

    long countByMovementTypeInAndQuantityGreaterThan(java.util.Collection<StockMovementType> movementTypes, BigDecimal quantity);
}
