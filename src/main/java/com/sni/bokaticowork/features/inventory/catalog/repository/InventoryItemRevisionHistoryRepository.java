package com.sni.bokaticowork.features.inventory.catalog.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItemRevisionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryItemRevisionHistoryRepository extends JpaRepository<InventoryItemRevisionHistory, Long> {

    List<InventoryItemRevisionHistory> findAllByItemOrderByChangedAtDesc(InventoryItem item);
}
