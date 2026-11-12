package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.FixedAssetInventoryScan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedAssetInventoryScanRepository extends JpaRepository<FixedAssetInventoryScan, Long> {

    List<FixedAssetInventoryScan> findAllByCampaignIdOrderByScannedAtAsc(Long campaignId);
}
