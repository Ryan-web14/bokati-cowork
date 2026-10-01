package com.sni.bokaticowork.features.subscription.derivation.repository;

import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivationApprovalRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanDerivationApprovalRuleRepository extends JpaRepository<PlanDerivationApprovalRule, Long> {

    Optional<PlanDerivationApprovalRule> findFirstByActiveTrueOrderByIdAsc();

    Optional<PlanDerivationApprovalRule> findByRuleCode(String ruleCode);
}
