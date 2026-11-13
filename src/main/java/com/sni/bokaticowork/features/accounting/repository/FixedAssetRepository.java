package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.FixedAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long> {

    Optional<FixedAsset> findByCode(String code);

    List<FixedAsset> findAllByOrderByCodeAsc();

    List<FixedAsset> findAllByParentIdOrderByCodeAsc(Long parentId);

    List<FixedAsset> findAllBySupplierInvoiceNumber(String supplierInvoiceNumber);

    List<FixedAsset> findAllByAccountingStatusIn(java.util.Collection<String> statuses);
}
