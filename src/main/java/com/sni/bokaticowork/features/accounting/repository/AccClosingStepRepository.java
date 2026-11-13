package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccClosingStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccClosingStepRepository extends JpaRepository<AccClosingStep, Long> {
    List<AccClosingStep> findAllByFiscalYearCode(String fiscalYearCode);
    java.util.Optional<AccClosingStep> findByFiscalYearCodeAndStepCode(String fiscalYearCode, String stepCode);
}
