package com.sni.bokaticowork.features.inventory.control.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.control.model.InventoryCount;
import com.sni.bokaticowork.features.inventory.control.model.InventoryCountItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface InventoryCountItemRepository extends JpaRepository<InventoryCountItem, Long> {
    Optional<InventoryCountItem> findByInventoryCountAndItem(InventoryCount inventoryCount, InventoryItem item);

    List<InventoryCountItem> findAllByInventoryCount(InventoryCount inventoryCount);

    long countByVarianceQuantityGreaterThanOrVarianceQuantityLessThan(BigDecimal positiveThreshold, BigDecimal negativeThreshold);
}
