package com.sni.bokaticowork.features.inventory.stock.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class StockMovementLotResponse {
    private String lotNumber;
    private LocalDate expiryDate;
    private BigDecimal quantity;
}
