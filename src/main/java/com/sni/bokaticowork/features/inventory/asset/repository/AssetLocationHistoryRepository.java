package com.sni.bokaticowork.features.inventory.asset.repository;

import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import com.sni.bokaticowork.features.inventory.asset.model.AssetLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetLocationHistoryRepository extends JpaRepository<AssetLocationHistory, Long> {
    List<AssetLocationHistory> findAllByAssetOrderByChangedAtDesc(Asset asset);
}
