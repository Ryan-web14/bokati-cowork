package com.sni.bokaticowork.features.support.repository;

import com.sni.bokaticowork.features.support.model.SupportRoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportRoutingRuleRepository extends JpaRepository<SupportRoutingRule, Long> {
    List<SupportRoutingRule> findAllByOrderBySortOrderAscIdAsc();
    List<SupportRoutingRule> findAllByActiveTrueOrderBySortOrderAscIdAsc();
}
