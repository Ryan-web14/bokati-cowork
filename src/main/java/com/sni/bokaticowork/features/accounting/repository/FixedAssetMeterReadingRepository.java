package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.FixedAssetMeterReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedAssetMeterReadingRepository extends JpaRepository<FixedAssetMeterReading, Long> {

    Optional<FixedAssetMeterReading> findByAssetIdAndPeriodCode(Long assetId, String periodCode);

    List<FixedAssetMeterReading> findAllByAssetIdOrderByPeriodCodeAsc(Long assetId);
}
