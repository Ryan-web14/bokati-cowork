package com.sni.bokaticowork.features.inventory.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventoryUnitConversionResponse {
    private String itemCode;
    private String fromUnitCode;
    private String toUnitCode;
    private BigDecimal factor;
}
