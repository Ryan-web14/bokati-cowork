package com.sni.bokaticowork.features.inventory.catalog.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryBarcode;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryBarcodeRepository extends JpaRepository<InventoryBarcode, Long> {

    Optional<InventoryBarcode> findByBarcodeValue(String barcodeValue);

    boolean existsByBarcodeValue(String barcodeValue);

    List<InventoryBarcode> findAllByItemOrderByPrimaryCodeDescBarcodeValueAsc(InventoryItem item);

    Optional<InventoryBarcode> findFirstByItemAndPrimaryCodeTrue(InventoryItem item);

    List<InventoryBarcode> findAllByItemAndPrimaryCodeTrue(InventoryItem item);
}
