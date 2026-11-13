package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.FixedAssetSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedAssetScheduleRepository extends JpaRepository<FixedAssetSchedule, Long> {

    List<FixedAssetSchedule> findAllByAssetIdAndKindAndVersionOrderByPeriodCodeAsc(Long assetId, String kind, Integer version);

    List<FixedAssetSchedule> findAllByAssetIdOrderByKindAscVersionAscPeriodCodeAsc(Long assetId);

    List<FixedAssetSchedule> findAllByStatusInAndPeriodCodeLessThanEqualOrderByPeriodCodeAsc(java.util.Collection<String> statuses, String periodCode);
}
