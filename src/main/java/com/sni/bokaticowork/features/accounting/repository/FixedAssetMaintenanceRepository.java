package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.FixedAssetMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedAssetMaintenanceRepository extends JpaRepository<FixedAssetMaintenance, Long> {

    List<FixedAssetMaintenance> findAllByAssetIdOrderByMaintenanceDateAsc(Long assetId);

    List<FixedAssetMaintenance> findAllByMaintenanceDateBetween(java.time.LocalDate from, java.time.LocalDate to);
}
