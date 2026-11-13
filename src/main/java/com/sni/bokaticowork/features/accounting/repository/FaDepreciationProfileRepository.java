package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.FaDepreciationProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaDepreciationProfileRepository extends JpaRepository<FaDepreciationProfile, Long> {
    List<FaDepreciationProfile> findAllByOrderByBrandAscPriorityDescIdAsc();

    List<FaDepreciationProfile> findAllByActiveTrueAndBrandIgnoreCase(String brand);
}
