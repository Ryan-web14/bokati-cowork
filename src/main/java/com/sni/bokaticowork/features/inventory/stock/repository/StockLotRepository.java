package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockLotRepository extends JpaRepository<StockLot, Long> {
    Optional<StockLot> findByItemAndLocationAndLotNumber(InventoryItem item, InventoryLocation location, String lotNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT lot FROM StockLot lot
            WHERE lot.item = :item
              AND lot.location = :location
              AND lot.remainingQuantity > 0
              AND lot.active = true
              AND lot.quarantined = false
            ORDER BY CASE WHEN lot.expiryDate IS NULL THEN 1 ELSE 0 END ASC, lot.expiryDate ASC, lot.receivedAt ASC
            """)
    List<StockLot> findConsumableLotsForUpdate(@Param("item") InventoryItem item, @Param("location") InventoryLocation location);

    @Query("""
            SELECT lot FROM StockLot lot
            WHERE lot.expiryDate IS NOT NULL
              AND lot.expiryDate <= :date
              AND lot.remainingQuantity > :zero
              AND lot.active = true
            """)
    List<StockLot> findExpiringLots(@Param("date") LocalDate date, @Param("zero") BigDecimal zero);

    @Query("""
            SELECT lot FROM StockLot lot
            WHERE lot.item = :item
              AND lot.location = :location
              AND lot.lotNumber = :lotNumber
              AND (:expiryDate IS NULL OR lot.expiryDate = :expiryDate)
            """)
    Optional<StockLot> findMatchingLot(@Param("item") InventoryItem item,
                                       @Param("location") InventoryLocation location,
                                       @Param("lotNumber") String lotNumber,
                                       @Param("expiryDate") LocalDate expiryDate);

    @Query("""
            SELECT lot FROM StockLot lot
            JOIN lot.item item
            JOIN lot.location location
            WHERE (:itemCode IS NULL OR item.itemCode = :itemCode)
              AND (:locationCode IS NULL OR location.locationCode = :locationCode)
              AND (:lotNumberPattern IS NULL OR UPPER(lot.lotNumber) LIKE :lotNumberPattern)
              AND (:expiringBefore IS NULL OR lot.expiryDate <= :expiringBefore)
              AND (:active IS NULL OR lot.active = :active)
              AND (:remainingOnly IS NULL OR :remainingOnly = FALSE OR lot.remainingQuantity > 0)
            """)
    Page<StockLot> search(@Param("itemCode") String itemCode,
                          @Param("locationCode") String locationCode,
                          @Param("lotNumberPattern") String lotNumberPattern,
                          @Param("expiringBefore") LocalDate expiringBefore,
                          @Param("active") Boolean active,
                          @Param("remainingOnly") Boolean remainingOnly,
                          Pageable pageable);

    long countByExpiryDateLessThanEqualAndRemainingQuantityGreaterThanAndActiveTrue(LocalDate expiryDate, BigDecimal remainingQuantity);

    @Query("""
            SELECT lot FROM StockLot lot
            JOIN lot.item item
            WHERE item.itemCode = :itemCode
              AND (:locationCode IS NULL OR lot.location.locationCode = :locationCode)
              AND (:activeOnly IS NULL OR :activeOnly = FALSE OR (lot.active = true AND lot.remainingQuantity > 0))
            ORDER BY lot.expiryDate ASC NULLS LAST, lot.receivedAt ASC
            """)
    List<StockLot> findLotsForItem(@Param("itemCode") String itemCode,
                                   @Param("locationCode") String locationCode,
                                   @Param("activeOnly") Boolean activeOnly);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT lot FROM StockLot lot
            WHERE lot.item = :item
              AND lot.location = :location
              AND lot.lotNumber = :lotNumber
              AND lot.active = true
            """)
    Optional<StockLot> findActiveLotForUpdate(@Param("item") InventoryItem item,
                                              @Param("location") InventoryLocation location,
                                              @Param("lotNumber") String lotNumber);
}
