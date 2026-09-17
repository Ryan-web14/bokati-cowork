package com.sni.bokaticowork.features.inventory.catalog.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItemTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryItemTranslationRepository extends JpaRepository<InventoryItemTranslation, Long> {

    List<InventoryItemTranslation> findAllByItemOrderByLanguageCodeAsc(InventoryItem item);

    Optional<InventoryItemTranslation> findByItemAndLanguageCode(InventoryItem item, String languageCode);
}
