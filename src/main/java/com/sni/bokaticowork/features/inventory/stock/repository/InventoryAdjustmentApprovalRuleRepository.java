package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.stock.model.InventoryAdjustmentApprovalRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryAdjustmentApprovalRuleRepository
        extends JpaRepository<InventoryAdjustmentApprovalRule, Long> {

    List<InventoryAdjustmentApprovalRule> findAllByActiveTrueOrderByMinAmountAsc();

    List<InventoryAdjustmentApprovalRule> findAllByOrderByMinAmountAsc();
}
