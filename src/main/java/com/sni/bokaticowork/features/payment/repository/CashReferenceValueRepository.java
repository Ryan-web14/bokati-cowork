package com.sni.bokaticowork.features.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashReferenceValueRepository extends JpaRepository<com.sni.bokaticowork.features.payment.model.CashReferenceValue, Long> {

    List<com.sni.bokaticowork.features.payment.model.CashReferenceValue> findByListCodeOrderBySortOrderAscLabelAsc(String listCode);

    List<com.sni.bokaticowork.features.payment.model.CashReferenceValue> findAllByOrderByListCodeAscSortOrderAscLabelAsc();

    Optional<com.sni.bokaticowork.features.payment.model.CashReferenceValue> findByListCodeAndCode(String listCode, String code);
}
