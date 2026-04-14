package com.sni.bokaticowork.features.inventory.asset.repository;

import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import com.sni.bokaticowork.features.inventory.asset.model.AssetMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AssetMaintenanceRepository extends JpaRepository<AssetMaintenance, Long> {

    boolean existsByMaintenanceCode(String maintenanceCode);

    Optional<AssetMaintenance> findByMaintenanceCode(String maintenanceCode);

    List<AssetMaintenance> findAllByAssetOrderByScheduledAtDesc(Asset asset);

    List<AssetMaintenance> findAllByStatusAndScheduledAtBefore(com.sni.bokaticowork.features.inventory.asset.enums.AssetMaintenanceStatus status,
                                                               Instant scheduledAt);
}
