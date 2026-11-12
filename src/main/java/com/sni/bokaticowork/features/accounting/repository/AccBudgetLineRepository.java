package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccBudgetLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccBudgetLineRepository extends JpaRepository<AccBudgetLine, Long> {

    java.util.List<AccBudgetLine> findByBudgetIdOrderByItemCodeAscSectionCodeAsc(Long budgetId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select l from AccBudgetLine l where l.id = :id")
    java.util.Optional<AccBudgetLine> lock(@org.springframework.data.repository.query.Param("id") Long id);
}
