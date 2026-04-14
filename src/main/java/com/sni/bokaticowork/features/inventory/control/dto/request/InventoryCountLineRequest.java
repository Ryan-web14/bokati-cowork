package com.sni.bokaticowork.features.inventory.control.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryCountLineRequest {
    @NotBlank
    private String itemCode;
    @NotNull
    @PositiveOrZero
    private BigDecimal countedQuantity;
    private String notes;
}
