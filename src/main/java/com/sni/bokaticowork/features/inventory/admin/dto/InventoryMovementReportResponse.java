package com.sni.bokaticowork.features.inventory.admin.dto;

import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class InventoryMovementReportResponse {
    private String reportTitle;
    private String itemCode;
    private String locationCode;
    private String fromDate;
    private String toDate;
    private String generatedAt;
    private Summary summary;
    private List<Line> lines;
    private List<ItemLine> items;
    private List<LocationLine> locations;
    private List<MovementDetail> recentMovements;

    @Data
    @Builder
    public static class Line {
        private StockMovementType movementType;
        private long movementCount;
        private BigDecimal totalQuantity;
        private Long totalValue;
        private BigDecimal signedQuantity;
        private Long signedValue;
    }

    @Data
    @Builder
    public static class Summary {
        private long movementCount;
        private BigDecimal totalInQuantity;
        private BigDecimal totalOutQuantity;
        private BigDecimal netQuantity;
        private Long totalInValue;
        private Long totalOutValue;
        private Long netValue;
        private Long totalMovementValue;
        private String firstMovementAt;
        private String lastMovementAt;
    }

    @Data
    @Builder
    public static class ItemLine {
        private String itemCode;
        private String itemName;
        private long movementCount;
        private BigDecimal totalInQuantity;
        private BigDecimal totalOutQuantity;
        private BigDecimal netQuantity;
        private Long totalValue;
    }

    @Data
    @Builder
    public static class LocationLine {
        private String locationCode;
        private String locationName;
        private long movementCount;
        private BigDecimal totalInQuantity;
        private BigDecimal totalOutQuantity;
        private BigDecimal netQuantity;
        private Long totalValue;
    }

    @Data
    @Builder
    public static class MovementDetail {
        private String movementCode;
        private StockMovementType movementType;
        private String itemCode;
        private String itemName;
        private String locationFromCode;
        private String locationToCode;
        private BigDecimal quantity;
        private Long unitCost;
        private Long totalCost;
        private String referenceType;
        private String referenceCode;
        private String performedBy;
        private String performedAt;
    }
}
