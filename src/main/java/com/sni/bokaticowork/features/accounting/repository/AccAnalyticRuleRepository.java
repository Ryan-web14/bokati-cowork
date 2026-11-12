package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccAnalyticRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccAnalyticRuleRepository extends JpaRepository<AccAnalyticRule, Long> {

    java.util.List<AccAnalyticRule> findByActiveTrueOrderByPriorityAscIdAsc();

    java.util.List<AccAnalyticRule> findAllByOrderByPriorityAscIdAsc();
}
