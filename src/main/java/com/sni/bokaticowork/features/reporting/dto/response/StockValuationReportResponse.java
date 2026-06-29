package com.sni.bokaticowork.features.reporting.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StockValuationReportResponse(
        Instant generatedAt,
        BigDecimal totalValue,
        long totalItems,
        long totalLocations,
        List<CategoryValuation> byCategory,
        List<LocationValuation> byLocation,
        List<LowStockItem> lowStockItems,
        List<DeadStockItem> deadStockItems
) {

    public record CategoryValuation(
            String categoryCode,
            String categoryName,
            long itemCount,
            BigDecimal totalQuantity,
            BigDecimal totalValue,
            BigDecimal percentage
    ) {}

    public record LocationValuation(
            String locationCode,
            String locationName,
            String locationType,
            long itemCount,
            BigDecimal totalQuantity,
            BigDecimal totalValue,
            BigDecimal percentage
    ) {}

    public record LowStockItem(
            String itemCode,
            String itemName,
            String categoryName,
            BigDecimal quantityAvailable,
            BigDecimal reorderPoint,
            BigDecimal reorderQuantity
    ) {}

    public record DeadStockItem(
            String itemCode,
            String itemName,
            String categoryName,
            String locationName,
            BigDecimal quantityOnHand,
            BigDecimal stockValue,
            LocalDate lastMovementDate,
            long daysSinceLastMovement
    ) {}
}
