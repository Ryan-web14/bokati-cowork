package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.FixedAssetDisposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedAssetDisposalRepository extends JpaRepository<FixedAssetDisposal, Long> {

    List<FixedAssetDisposal> findAllByAssetIdOrderByCreatedAtAsc(Long assetId);

    List<FixedAssetDisposal> findAllByStatusOrderByDisposalDateAsc(String status);

    Optional<FixedAssetDisposal> findByVoucherNumber(String voucherNumber);
}
