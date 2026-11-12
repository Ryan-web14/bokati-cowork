package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.BankAssignmentRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAssignmentRuleRepository extends JpaRepository<BankAssignmentRule, Long> {

    java.util.List<BankAssignmentRule> findByActiveTrueOrderByPriorityAscIdAsc();

    java.util.List<BankAssignmentRule> findAllByOrderByPriorityAscIdAsc();
}
