package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.FixedAssetInventoryLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedAssetInventoryLinkRepository extends JpaRepository<FixedAssetInventoryLink, Long> {

    List<FixedAssetInventoryLink> findAllByFixedAssetId(Long fixedAssetId);

    List<FixedAssetInventoryLink> findAllByInventoryAssetCode(String inventoryAssetCode);
}
