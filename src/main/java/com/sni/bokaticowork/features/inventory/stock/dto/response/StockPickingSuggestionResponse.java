package com.sni.bokaticowork.features.inventory.stock.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class StockPickingSuggestionResponse {
    private String itemCode;
    private BigDecimal requestedQuantity;
    private BigDecimal suggestedQuantity;
    private Boolean fullyCovered;
    private List<Line> lines;

    @Data
    @Builder
    public static class Line {
        private String locationCode;
        private String locationName;
        private BigDecimal availableQuantity;
        private BigDecimal pickQuantity;
    }
}
