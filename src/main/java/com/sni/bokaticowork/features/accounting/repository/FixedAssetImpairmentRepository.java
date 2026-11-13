package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.FixedAssetImpairment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedAssetImpairmentRepository extends JpaRepository<FixedAssetImpairment, Long> {

    List<FixedAssetImpairment> findAllByAssetIdOrderByCreatedAtAsc(Long assetId);

    List<FixedAssetImpairment> findAllByOrderByImpairmentDateAsc();
}
