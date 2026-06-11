package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
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

    @Query(value = """
            SELECT movement.*
            FROM stock_movement movement
            JOIN inventory_item item ON item.id = movement.item_id
            LEFT JOIN inventory_location location_from ON location_from.id = movement.location_from_id
            LEFT JOIN inventory_location location_to ON location_to.id = movement.location_to_id
            WHERE (CAST(:itemCode AS varchar) IS NULL OR item.item_code = :itemCode)
              AND (CAST(:locationCode AS varchar) IS NULL OR location_from.location_code = :locationCode OR location_to.location_code = :locationCode)
              AND (CAST(:movementType AS varchar) IS NULL OR movement.movement_type = :movementType)
              AND (CAST(:referenceType AS varchar) IS NULL OR movement.reference_type = :referenceType)
              AND (CAST(:referenceCode AS varchar) IS NULL OR movement.reference_code = :referenceCode)
              AND (CAST(:fromDate AS timestamp with time zone) IS NULL OR movement.performed_at >= CAST(:fromDate AS timestamp with time zone))
              AND (CAST(:toDate AS timestamp with time zone) IS NULL OR movement.performed_at <= CAST(:toDate AS timestamp with time zone))
            ORDER BY movement.performed_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM stock_movement movement
            JOIN inventory_item item ON item.id = movement.item_id
            LEFT JOIN inventory_location location_from ON location_from.id = movement.location_from_id
            LEFT JOIN inventory_location location_to ON location_to.id = movement.location_to_id
            WHERE (CAST(:itemCode AS varchar) IS NULL OR item.item_code = :itemCode)
              AND (CAST(:locationCode AS varchar) IS NULL OR location_from.location_code = :locationCode OR location_to.location_code = :locationCode)
              AND (CAST(:movementType AS varchar) IS NULL OR movement.movement_type = :movementType)
              AND (CAST(:referenceType AS varchar) IS NULL OR movement.reference_type = :referenceType)
              AND (CAST(:referenceCode AS varchar) IS NULL OR movement.reference_code = :referenceCode)
              AND (CAST(:fromDate AS timestamp with time zone) IS NULL OR movement.performed_at >= CAST(:fromDate AS timestamp with time zone))
              AND (CAST(:toDate AS timestamp with time zone) IS NULL OR movement.performed_at <= CAST(:toDate AS timestamp with time zone))
            """,
            nativeQuery = true)
    Page<StockMovement> search(@Param("itemCode") String itemCode,
                               @Param("locationCode") String locationCode,
                               @Param("movementType") String movementType,
                               @Param("referenceType") String referenceType,
                               @Param("referenceCode") String referenceCode,
                               @Param("fromDate") Instant fromDate,
                               @Param("toDate") Instant toDate,
                               Pageable pageable);

    @Query(value = """
            SELECT movement.movement_type, COUNT(*), COALESCE(SUM(movement.quantity), 0), COALESCE(SUM(movement.total_cost), 0)
            FROM stock_movement movement
            JOIN inventory_item item ON item.id = movement.item_id
            LEFT JOIN inventory_location location_from ON location_from.id = movement.location_from_id
            LEFT JOIN inventory_location location_to ON location_to.id = movement.location_to_id
            WHERE (CAST(:itemCode AS varchar) IS NULL OR item.item_code = :itemCode)
              AND (CAST(:locationCode AS varchar) IS NULL OR location_from.location_code = :locationCode OR location_to.location_code = :locationCode)
              AND (CAST(:fromDate AS timestamp with time zone) IS NULL OR movement.performed_at >= CAST(:fromDate AS timestamp with time zone))
              AND (CAST(:toDate AS timestamp with time zone) IS NULL OR movement.performed_at <= CAST(:toDate AS timestamp with time zone))
            GROUP BY movement.movement_type
            """,
            nativeQuery = true)
    List<Object[]> movementReport(@Param("itemCode") String itemCode,
                                  @Param("locationCode") String locationCode,
                                  @Param("fromDate") Instant fromDate,
                                  @Param("toDate") Instant toDate);

    @Query(value = """
            SELECT
                COUNT(*),
                COALESCE(SUM(CASE WHEN movement.movement_type IN ('IN', 'ADJUSTMENT_IN') THEN movement.quantity ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN movement.movement_type IN ('OUT', 'ADJUSTMENT_OUT') THEN movement.quantity ELSE 0 END), 0),
                COALESCE(SUM(CASE
                    WHEN movement.movement_type IN ('IN', 'ADJUSTMENT_IN') THEN movement.quantity
                    WHEN movement.movement_type IN ('OUT', 'ADJUSTMENT_OUT') THEN -movement.quantity
                    ELSE 0
                END), 0),
                COALESCE(SUM(CASE WHEN movement.movement_type IN ('IN', 'ADJUSTMENT_IN') THEN movement.total_cost ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN movement.movement_type IN ('OUT', 'ADJUSTMENT_OUT') THEN movement.total_cost ELSE 0 END), 0),
                COALESCE(SUM(CASE
                    WHEN movement.movement_type IN ('IN', 'ADJUSTMENT_IN') THEN movement.total_cost
                    WHEN movement.movement_type IN ('OUT', 'ADJUSTMENT_OUT') THEN -movement.total_cost
                    ELSE 0
                END), 0),
                COALESCE(SUM(movement.total_cost), 0),
                MIN(movement.performed_at),
                MAX(movement.performed_at)
            FROM stock_movement movement
            JOIN inventory_item item ON item.id = movement.item_id
            LEFT JOIN inventory_location location_from ON location_from.id = movement.location_from_id
            LEFT JOIN inventory_location location_to ON location_to.id = movement.location_to_id
            WHERE (CAST(:itemCode AS varchar) IS NULL OR item.item_code = :itemCode)
              AND (CAST(:locationCode AS varchar) IS NULL OR location_from.location_code = :locationCode OR location_to.location_code = :locationCode)
              AND (CAST(:fromDate AS timestamp with time zone) IS NULL OR movement.performed_at >= CAST(:fromDate AS timestamp with time zone))
              AND (CAST(:toDate AS timestamp with time zone) IS NULL OR movement.performed_at <= CAST(:toDate AS timestamp with time zone))
            """,
            nativeQuery = true)
    Object[] movementReportSummary(@Param("itemCode") String itemCode,
                                   @Param("locationCode") String locationCode,
                                   @Param("fromDate") Instant fromDate,
                                   @Param("toDate") Instant toDate);

    @Query(value = """
            SELECT
                item.item_code,
                item.name,
                COUNT(*),
                COALESCE(SUM(CASE WHEN movement.movement_type IN ('IN', 'ADJUSTMENT_IN') THEN movement.quantity ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN movement.movement_type IN ('OUT', 'ADJUSTMENT_OUT') THEN movement.quantity ELSE 0 END), 0),
                COALESCE(SUM(CASE
                    WHEN movement.movement_type IN ('IN', 'ADJUSTMENT_IN') THEN movement.quantity
                    WHEN movement.movement_type IN ('OUT', 'ADJUSTMENT_OUT') THEN -movement.quantity
                    ELSE 0
                END), 0),
                COALESCE(SUM(movement.total_cost), 0)
            FROM stock_movement movement
            JOIN inventory_item item ON item.id = movement.item_id
            LEFT JOIN inventory_location location_from ON location_from.id = movement.location_from_id
            LEFT JOIN inventory_location location_to ON location_to.id = movement.location_to_id
            WHERE (CAST(:itemCode AS varchar) IS NULL OR item.item_code = :itemCode)
              AND (CAST(:locationCode AS varchar) IS NULL OR location_from.location_code = :locationCode OR location_to.location_code = :locationCode)
              AND (CAST(:fromDate AS timestamp with time zone) IS NULL OR movement.performed_at >= CAST(:fromDate AS timestamp with time zone))
              AND (CAST(:toDate AS timestamp with time zone) IS NULL OR movement.performed_at <= CAST(:toDate AS timestamp with time zone))
            GROUP BY item.item_code, item.name
            ORDER BY COALESCE(SUM(movement.total_cost), 0) DESC, item.name ASC
            LIMIT 20
            """,
            nativeQuery = true)
    List<Object[]> movementReportByItem(@Param("itemCode") String itemCode,
                                        @Param("locationCode") String locationCode,
                                        @Param("fromDate") Instant fromDate,
                                        @Param("toDate") Instant toDate);

    @Query(value = """
            WITH movement_locations AS (
                SELECT movement.*, location_from.location_code, location_from.name AS location_name
                FROM stock_movement movement
                JOIN inventory_item item ON item.id = movement.item_id
                LEFT JOIN inventory_location location_from ON location_from.id = movement.location_from_id
                WHERE location_from.id IS NOT NULL
                  AND (CAST(:itemCode AS varchar) IS NULL OR item.item_code = :itemCode)
                  AND (CAST(:locationCode AS varchar) IS NULL OR location_from.location_code = :locationCode)
                  AND (CAST(:fromDate AS timestamp with time zone) IS NULL OR movement.performed_at >= CAST(:fromDate AS timestamp with time zone))
                  AND (CAST(:toDate AS timestamp with time zone) IS NULL OR movement.performed_at <= CAST(:toDate AS timestamp with time zone))
                UNION ALL
                SELECT movement.*, location_to.location_code, location_to.name AS location_name
                FROM stock_movement movement
                JOIN inventory_item item ON item.id = movement.item_id
                LEFT JOIN inventory_location location_to ON location_to.id = movement.location_to_id
                WHERE location_to.id IS NOT NULL
                  AND (CAST(:itemCode AS varchar) IS NULL OR item.item_code = :itemCode)
                  AND (CAST(:locationCode AS varchar) IS NULL OR location_to.location_code = :locationCode)
                  AND (CAST(:fromDate AS timestamp with time zone) IS NULL OR movement.performed_at >= CAST(:fromDate AS timestamp with time zone))
                  AND (CAST(:toDate AS timestamp with time zone) IS NULL OR movement.performed_at <= CAST(:toDate AS timestamp with time zone))
            )
            SELECT
                location_code,
                location_name,
                COUNT(*),
                COALESCE(SUM(CASE WHEN movement_type IN ('IN', 'ADJUSTMENT_IN') THEN quantity ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN movement_type IN ('OUT', 'ADJUSTMENT_OUT') THEN quantity ELSE 0 END), 0),
                COALESCE(SUM(CASE
                    WHEN movement_type IN ('IN', 'ADJUSTMENT_IN') THEN quantity
                    WHEN movement_type IN ('OUT', 'ADJUSTMENT_OUT') THEN -quantity
                    ELSE 0
                END), 0),
                COALESCE(SUM(total_cost), 0)
            FROM movement_locations
            GROUP BY location_code, location_name
            ORDER BY COALESCE(SUM(total_cost), 0) DESC, location_name ASC
            LIMIT 20
            """,
            nativeQuery = true)
    List<Object[]> movementReportByLocation(@Param("itemCode") String itemCode,
                                            @Param("locationCode") String locationCode,
                                            @Param("fromDate") Instant fromDate,
                                            @Param("toDate") Instant toDate);

    @Query(value = """
            SELECT
                movement.movement_code,
                movement.movement_type,
                item.item_code,
                item.name,
                location_from.location_code,
                location_to.location_code,
                movement.quantity,
                movement.unit_cost,
                movement.total_cost,
                movement.reference_type,
                movement.reference_code,
                movement.performed_by,
                movement.performed_at
            FROM stock_movement movement
            JOIN inventory_item item ON item.id = movement.item_id
            LEFT JOIN inventory_location location_from ON location_from.id = movement.location_from_id
            LEFT JOIN inventory_location location_to ON location_to.id = movement.location_to_id
            WHERE (CAST(:itemCode AS varchar) IS NULL OR item.item_code = :itemCode)
              AND (CAST(:locationCode AS varchar) IS NULL OR location_from.location_code = :locationCode OR location_to.location_code = :locationCode)
              AND (CAST(:fromDate AS timestamp with time zone) IS NULL OR movement.performed_at >= CAST(:fromDate AS timestamp with time zone))
              AND (CAST(:toDate AS timestamp with time zone) IS NULL OR movement.performed_at <= CAST(:toDate AS timestamp with time zone))
            ORDER BY movement.performed_at DESC
            LIMIT 15
            """,
            nativeQuery = true)
    List<Object[]> movementReportRecentMovements(@Param("itemCode") String itemCode,
                                                 @Param("locationCode") String locationCode,
                                                 @Param("fromDate") Instant fromDate,
                                                 @Param("toDate") Instant toDate);

    long countByAllowNegativeOverrideTrue();

    long countByMovementTypeInAndQuantityGreaterThan(java.util.Collection<StockMovementType> movementTypes, BigDecimal quantity);

    @Query(value = """
            SELECT COALESCE(SUM(sm.quantity), 0)
            FROM stock_movement sm
            WHERE sm.item_id = :itemId
              AND sm.movement_type IN ('OUT', 'ADJUSTMENT_OUT')
              AND (:locationId IS NULL OR sm.location_from_id = :locationId)
              AND sm.performed_at >= :since
            """, nativeQuery = true)
    BigDecimal consumptionSince(@Param("itemId") Long itemId,
                                @Param("locationId") Long locationId,
                                @Param("since") Instant since);

    boolean existsByItemIdAndMovementTypeInAndPerformedAtAfter(Long itemId,
                                                               java.util.Collection<StockMovementType> movementTypes,
                                                               Instant performedAt);

    @Query(value = """
            SELECT COALESCE(SUM(sm.quantity), 0)
            FROM stock_movement sm
            WHERE sm.item_id = :itemId
              AND sm.movement_type IN ('OUT', 'ADJUSTMENT_OUT')
              AND (:locationId IS NULL OR sm.location_from_id = :locationId)
              AND sm.performed_at >= :from
              AND sm.performed_at < :to
            """, nativeQuery = true)
    BigDecimal consumptionBetween(@Param("itemId") Long itemId,
                                  @Param("locationId") Long locationId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to);
}
