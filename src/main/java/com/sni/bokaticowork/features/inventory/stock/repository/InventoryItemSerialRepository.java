package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.enums.InventorySerialStatus;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryItemSerial;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryItemSerialRepository extends JpaRepository<InventoryItemSerial, Long> {

    boolean existsByItemAndSerialNumber(InventoryItem item, String serialNumber);

    Optional<InventoryItemSerial> findByItemAndSerialNumberAndStatus(InventoryItem item, String serialNumber,
                                                                     InventorySerialStatus status);

    @Query("""
            SELECT s FROM InventoryItemSerial s
            JOIN s.item item
            WHERE item.itemCode = :itemCode
              AND (:locationCode IS NULL OR s.location.locationCode = :locationCode)
              AND (:status IS NULL OR s.status = :status)
            ORDER BY s.receivedAt ASC
            """)
    List<InventoryItemSerial> findSerialsForItem(@Param("itemCode") String itemCode,
                                                 @Param("locationCode") String locationCode,
                                                 @Param("status") InventorySerialStatus status);

    List<InventoryItemSerial> findAllByItemAndLocationAndStatus(InventoryItem item, InventoryLocation location,
                                                                InventorySerialStatus status);
}
