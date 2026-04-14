package com.sni.bokaticowork.features.inventory.intelligence.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventoryReorderRuleResponse {
    private Long id;
    private String itemCode;
    private String itemName;
    private String locationCode;
    private String locationName;
    private BigDecimal minQuantity;
    private BigDecimal maxQuantity;
    private BigDecimal reorderQuantity;
    private String preferredSupplierCode;
    private Boolean active;
}
