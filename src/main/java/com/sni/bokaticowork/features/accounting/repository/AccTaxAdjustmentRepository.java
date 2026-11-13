package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccTaxAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccTaxAdjustmentRepository extends JpaRepository<AccTaxAdjustment, Long> {
    List<AccTaxAdjustment> findAllByFiscalYearCodeOrderByCreatedAtAsc(String fiscalYearCode);
}
