package com.sni.bokaticowork.features.inventory.procurement.repository;

import com.sni.bokaticowork.features.inventory.procurement.enums.PurchaseApprovalLevel;
import com.sni.bokaticowork.features.inventory.procurement.model.PurchaseApprovalRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseApprovalRuleRepository extends JpaRepository<PurchaseApprovalRule, Long> {
    Optional<PurchaseApprovalRule> findByApprovalLevel(PurchaseApprovalLevel approvalLevel);

    List<PurchaseApprovalRule> findAllByActiveTrueOrderByMinAmountAsc();
}
