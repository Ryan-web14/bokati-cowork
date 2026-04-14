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

    Optional<StockLevel> findByItemAndLocation(InventoryItem item, InventoryLocation location);

    List<StockLevel> findAllByLocation(InventoryLocation location);

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
}
