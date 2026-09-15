package com.sni.bokaticowork.features.inventory.catalog.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItemSubstitute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryItemSubstituteRepository extends JpaRepository<InventoryItemSubstitute, Long> {

    List<InventoryItemSubstitute> findAllByItemAndActiveTrueOrderByPriorityAsc(InventoryItem item);

    Optional<InventoryItemSubstitute> findByItemAndSubstituteItem(InventoryItem item, InventoryItem substituteItem);

    /**
     * Substituts declares dans l'autre sens et marques reciproques : ils valent aussi pour cet article.
     */
    @Query("""
            SELECT link FROM InventoryItemSubstitute link
            WHERE link.substituteItem = :item
              AND link.bidirectional = TRUE
              AND link.active = TRUE
            ORDER BY link.priority ASC
            """)
    List<InventoryItemSubstitute> findReciprocal(@Param("item") InventoryItem item);
}
