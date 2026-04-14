package com.sni.bokaticowork.features.inventory.intelligence.repository;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertStatus;
import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertType;
import com.sni.bokaticowork.features.inventory.intelligence.model.InventoryAlert;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryAlertRepository extends JpaRepository<InventoryAlert, Long> {
    boolean existsByAlertCode(String alertCode);

    Optional<InventoryAlert> findFirstByAlertTypeAndItemAndLocationAndStatusOrderByCreatedAtDesc(
            InventoryAlertType alertType,
            InventoryItem item,
            InventoryLocation location,
            InventoryAlertStatus status
    );

    Optional<InventoryAlert> findFirstByAlertTypeAndAssetCodeAndStatusOrderByCreatedAtDesc(
            InventoryAlertType alertType,
            String assetCode,
            InventoryAlertStatus status
    );

    List<InventoryAlert> findAllByItemAndLocationAndStatus(InventoryItem item, InventoryLocation location, InventoryAlertStatus status);

    Page<InventoryAlert> findAllByStatusOrderByCreatedAtDesc(InventoryAlertStatus status, Pageable pageable);

    Page<InventoryAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(InventoryAlertStatus status);

    long countByStatusAndAlertType(InventoryAlertStatus status, InventoryAlertType alertType);
}
