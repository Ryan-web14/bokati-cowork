package com.sni.bokaticowork.features.inventory.stock.dto.request;

import com.sni.bokaticowork.features.inventory.procurement.enums.PurchaseApprovalLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class InventoryAdjustmentApprovalRuleRequest {

    @NotNull
    private PurchaseApprovalLevel approvalLevel;

    @NotNull
    @PositiveOrZero
    private Long minAmount;

    private Long maxAmount;

    private Boolean active;
}
