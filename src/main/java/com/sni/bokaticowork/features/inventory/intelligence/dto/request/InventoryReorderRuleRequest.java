package com.sni.bokaticowork.features.inventory.intelligence.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryReorderRuleRequest {

    @NotBlank
    private String itemCode;

    private String locationCode;

    @Positive
    private BigDecimal minQuantity;

    private BigDecimal maxQuantity;

    @Positive
    private BigDecimal reorderQuantity;

    private String preferredSupplierCode;

    private Boolean active;
}
