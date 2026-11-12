package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccBudget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccBudgetRepository extends JpaRepository<AccBudget, Long> {

    java.util.List<AccBudget> findByFiscalYearCodeOrderByVersionDesc(String fiscalYearCode);

    java.util.Optional<AccBudget> findByFiscalYearCodeAndStatus(String fiscalYearCode, String status);
}
