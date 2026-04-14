package com.sni.bokaticowork.features.inventory.procurement.repository;

import com.sni.bokaticowork.features.inventory.procurement.model.PurchaseApprovalStep;
import com.sni.bokaticowork.features.inventory.procurement.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseApprovalStepRepository extends JpaRepository<PurchaseApprovalStep, Long> {
    long countByPurchaseOrder(PurchaseOrder purchaseOrder);

    boolean existsByPurchaseOrderAndApprovedByIgnoreCase(PurchaseOrder purchaseOrder, String approvedBy);

    List<PurchaseApprovalStep> findAllByPurchaseOrderOrderByApprovedAtAsc(PurchaseOrder purchaseOrder);
}
