package com.sni.bokaticowork.features.payment.compliance.repository;

import com.sni.bokaticowork.features.payment.compliance.model.ComplianceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceRuleRepository extends JpaRepository<ComplianceRule, Long> {

    Optional<ComplianceRule> findByRuleCode(String ruleCode);

    List<ComplianceRule> findByActiveTrueOrderByRuleCodeAsc();

    List<ComplianceRule> findAllByOrderByRuleCodeAsc();
}
