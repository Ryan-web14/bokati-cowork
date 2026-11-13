package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccTaxResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccTaxResultRepository extends JpaRepository<AccTaxResult, Long> {
    java.util.Optional<AccTaxResult> findByFiscalYearCode(String fiscalYearCode);
}
