package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.FixedAssetInventoryCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedAssetInventoryCampaignRepository extends JpaRepository<FixedAssetInventoryCampaign, Long> {

    Optional<FixedAssetInventoryCampaign> findByCode(String code);

    List<FixedAssetInventoryCampaign> findAllByOrderByCreatedAtDesc();
}
