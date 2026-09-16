package com.sni.bokaticowork.features.inventory.stock.dto.response;

import com.sni.bokaticowork.features.inventory.procurement.enums.PurchaseApprovalLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryAdjustmentApprovalRuleResponse {

    private Long id;

    private PurchaseApprovalLevel approvalLevel;

    private Long minAmount;

    private Long maxAmount;

    private Boolean active;
}
