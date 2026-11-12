package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccBudgetOverride;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccBudgetOverrideRepository extends JpaRepository<AccBudgetOverride, Long> {

    java.util.List<AccBudgetOverride> findByLineId(Long lineId);
}
