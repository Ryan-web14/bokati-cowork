package com.sni.bokaticowork.features.inventory.catalog.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryUnitConversion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryUnitConversionRepository extends JpaRepository<InventoryUnitConversion, Long> {
    Optional<InventoryUnitConversion> findByItemAndFromUnitCodeAndToUnitCodeAndActiveTrue(InventoryItem item, String fromUnitCode, String toUnitCode);

    List<InventoryUnitConversion> findAllByItemAndActiveTrue(InventoryItem item);
}
