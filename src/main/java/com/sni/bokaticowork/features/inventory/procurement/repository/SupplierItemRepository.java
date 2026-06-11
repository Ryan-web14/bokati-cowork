package com.sni.bokaticowork.features.inventory.procurement.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.procurement.model.SupplierItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierItemRepository extends JpaRepository<SupplierItem, Long> {

    @Query("""
            SELECT si FROM SupplierItem si
            WHERE si.item = :item
              AND si.active = true
            ORDER BY CASE WHEN si.unitPrice IS NULL THEN 1 ELSE 0 END ASC,
                     si.unitPrice ASC,
                     CASE WHEN si.leadTimeDays IS NULL THEN 1 ELSE 0 END ASC,
                     si.leadTimeDays ASC
            """)
    List<SupplierItem> findActiveByItemOrderByPriceAndLeadTime(@Param("item") InventoryItem item);
}
