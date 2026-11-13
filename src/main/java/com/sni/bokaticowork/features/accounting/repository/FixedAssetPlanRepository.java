package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.FixedAssetPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedAssetPlanRepository extends JpaRepository<FixedAssetPlan, Long> {

    List<FixedAssetPlan> findAllByAssetIdOrderByVersionAscKindAsc(Long assetId);

    Optional<FixedAssetPlan> findByAssetIdAndVersionAndKind(Long assetId, Integer version, String kind);
}
