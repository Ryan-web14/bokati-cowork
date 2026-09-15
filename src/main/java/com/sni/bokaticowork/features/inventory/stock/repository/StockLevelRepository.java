package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT level FROM StockLevel level WHERE level.item = :item AND level.location = :location")
    Optional<StockLevel> findByItemAndLocationForUpdate(@Param("item") InventoryItem item,
                                                        @Param("location") InventoryLocation location);

    /**
     * Lecture seule. Toute ecriture sur un niveau de stock doit passer par
     * {@link #findByItemAndLocationForUpdate(InventoryItem, InventoryLocation)}, sans quoi deux
     * operations concurrentes sur le meme couple article et emplacement peuvent produire un stock faux.
     */
    @Query("SELECT level FROM StockLevel level WHERE level.item = :item AND level.location = :location")
    Optional<StockLevel> findByItemAndLocationReadOnly(@Param("item") InventoryItem item,
                                                       @Param("location") InventoryLocation location);

    List<StockLevel> findAllByLocation(InventoryLocation location);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM stock_level
            WHERE item_id = :itemId
            ORDER BY quantity_available ASC
            """)
    List<StockLevel> findAllByItemIdOrderByQuantityAvailableAsc(@Param("itemId") Long itemId);

    List<StockLevel> findAllByItemAndQuantityAvailableGreaterThanOrderByQuantityAvailableDesc(InventoryItem item, java.math.BigDecimal quantity);

    @Query("""
            SELECT level FROM StockLevel level
            JOIN level.item item
            JOIN level.location location
            LEFT JOIN item.category category
            WHERE (:itemCode IS NULL OR item.itemCode = :itemCode)
              AND (:locationCode IS NULL OR location.locationCode = :locationCode)
              AND (:categoryCode IS NULL OR category.code = :categoryCode)
              AND (:availableOnly IS NULL OR :availableOnly = FALSE OR level.quantityAvailable > 0)
              AND (:lowStock IS NULL OR :lowStock = FALSE OR level.quantityAvailable <= 0)
            """)
    Page<StockLevel> search(@Param("itemCode") String itemCode,
                            @Param("locationCode") String locationCode,
                            @Param("categoryCode") String categoryCode,
                            @Param("availableOnly") Boolean availableOnly,
                            @Param("lowStock") Boolean lowStock,
                            Pageable pageable);

    long countByQuantityAvailableLessThanEqual(java.math.BigDecimal quantity);

    @Query("""
            SELECT COALESCE(SUM(level.quantityOnHand * COALESCE(level.averageCost, 0)), 0)
            FROM StockLevel level
            """)
    Long totalStockValue();

    /**
     * Compare le stock enregistre au stock recalcule depuis le journal de mouvements, et ne renvoie
     * que les couples article et emplacement divergents.
     *
     * <p>Un mouvement contre-passe reste dans la somme : la contre-passation cree un mouvement
     * compensatoire distinct, exclure l'original reviendrait a compter la correction deux fois.</p>
     *
     * <p>Colonnes renvoyees, dans l'ordre : itemCode, itemName, locationCode, locationName,
     * recordedQuantity, expectedQuantity, difference, lastMovementAt.</p>
     */
    @Query(value = """
            WITH movement_delta AS (
                SELECT movement.item_id, movement.location_to_id AS location_id, movement.quantity AS delta
                FROM stock_movement movement
                WHERE movement.movement_type IN ('IN', 'ADJUSTMENT_IN', 'TRANSFER')
                  AND movement.location_to_id IS NOT NULL
                UNION ALL
                SELECT movement.item_id, movement.location_from_id AS location_id, -movement.quantity AS delta
                FROM stock_movement movement
                WHERE movement.movement_type IN ('OUT', 'ADJUSTMENT_OUT', 'TRANSFER')
                  AND movement.location_from_id IS NOT NULL
            ),
            expected AS (
                SELECT item_id, location_id, SUM(delta) AS expected_quantity
                FROM movement_delta
                GROUP BY item_id, location_id
            )
            SELECT
                item.item_code,
                item.name,
                location.location_code,
                location.name,
                COALESCE(level.quantity_on_hand, 0),
                COALESCE(expected.expected_quantity, 0),
                COALESCE(level.quantity_on_hand, 0) - COALESCE(expected.expected_quantity, 0),
                level.last_movement_at
            FROM expected
            FULL OUTER JOIN stock_level level
                ON level.item_id = expected.item_id
               AND level.location_id = expected.location_id
            JOIN inventory_item item
                ON item.id = COALESCE(level.item_id, expected.item_id)
            JOIN inventory_location location
                ON location.id = COALESCE(level.location_id, expected.location_id)
            WHERE COALESCE(level.quantity_on_hand, 0) <> COALESCE(expected.expected_quantity, 0)
              AND (CAST(:itemCode AS varchar) IS NULL OR item.item_code = :itemCode)
              AND (CAST(:locationCode AS varchar) IS NULL OR location.location_code = :locationCode)
            ORDER BY ABS(COALESCE(level.quantity_on_hand, 0) - COALESCE(expected.expected_quantity, 0)) DESC,
                     item.item_code ASC
            """, nativeQuery = true)
    List<Object[]> findReconciliationDivergences(@Param("itemCode") String itemCode,
                                                 @Param("locationCode") String locationCode);

    /**
     * Nombre de couples article et emplacement examines par la reconciliation, qu'ils divergent ou non.
     */
    @Query(value = """
            WITH movement_pairs AS (
                SELECT DISTINCT movement.item_id, movement.location_to_id AS location_id
                FROM stock_movement movement
                WHERE movement.location_to_id IS NOT NULL
                UNION
                SELECT DISTINCT movement.item_id, movement.location_from_id AS location_id
                FROM stock_movement movement
                WHERE movement.location_from_id IS NOT NULL
                UNION
                SELECT level.item_id, level.location_id
                FROM stock_level level
            )
            SELECT COUNT(*) FROM movement_pairs
            """, nativeQuery = true)
    long countReconciliationPairs();
}
