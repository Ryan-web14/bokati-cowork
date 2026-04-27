package com.sni.bokaticowork.features.inventory.intelligence.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.intelligence.model.InventoryReorderRule;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryReorderRuleRepository extends JpaRepository<InventoryReorderRule, Long> {
    Optional<InventoryReorderRule> findByItemAndLocation(InventoryItem item, InventoryLocation location);

    Optional<InventoryReorderRule> findFirstByItemAndLocationIsNullAndActiveTrue(InventoryItem item);

    Optional<InventoryReorderRule> findFirstByItemAndLocationAndActiveTrue(InventoryItem item, InventoryLocation location);

    boolean existsByItemAndLocation(InventoryItem item, InventoryLocation location);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM inventory_reorder_rule
            WHERE active = TRUE
            ORDER BY id ASC
            """)
    List<InventoryReorderRule> findAllActiveRules();
}
