package com.sni.bokaticowork.features.inventory.admin.dto;

import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class InventoryMovementReportResponse {
    private String itemCode;
    private String locationCode;
    private String fromDate;
    private String toDate;
    private List<Line> lines;

    @Data
    @Builder
    public static class Line {
        private StockMovementType movementType;
        private long movementCount;
        private BigDecimal totalQuantity;
        private Long totalValue;
    }
}
