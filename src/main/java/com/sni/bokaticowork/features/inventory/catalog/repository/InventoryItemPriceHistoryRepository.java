package com.sni.bokaticowork.features.inventory.catalog.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItemPriceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemPriceHistoryRepository extends JpaRepository<InventoryItemPriceHistory, Long> {

    Page<InventoryItemPriceHistory> findAllByItemOrderByChangedAtDesc(InventoryItem item, Pageable pageable);
}
