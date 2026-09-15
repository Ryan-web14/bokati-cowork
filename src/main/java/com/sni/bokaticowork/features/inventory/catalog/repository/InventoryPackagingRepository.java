package com.sni.bokaticowork.features.inventory.catalog.repository;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryPackagingLevel;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryPackaging;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryPackagingRepository extends JpaRepository<InventoryPackaging, Long> {

    List<InventoryPackaging> findAllByItemOrderByQuantityAsc(InventoryItem item);

    Optional<InventoryPackaging> findByItemAndPackagingLevel(InventoryItem item, InventoryPackagingLevel packagingLevel);

    Optional<InventoryPackaging> findFirstByBarcodeValue(String barcodeValue);
}
