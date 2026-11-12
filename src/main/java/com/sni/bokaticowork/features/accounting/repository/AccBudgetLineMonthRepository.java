package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccBudgetLineMonth;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccBudgetLineMonthRepository extends JpaRepository<AccBudgetLineMonth, Long> {

    java.util.List<AccBudgetLineMonth> findByLineIdOrderByMonthAsc(Long lineId);

    void deleteByLineId(Long lineId);
}
