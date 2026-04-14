package com.sni.bokaticowork.features.inventory.control.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventoryCountItemResponse {
    private String itemCode;
    private String itemName;
    private BigDecimal expectedQuantity;
    private BigDecimal countedQuantity;
    private BigDecimal varianceQuantity;
    private String notes;
}
