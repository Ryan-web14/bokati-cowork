package com.sni.bokaticowork.features.inventory.stock.dto.response;

import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReservationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class StockReservationResponse {
    private String reservationCode;
    private String itemCode;
    private String itemName;
    private String locationCode;
    private String locationName;
    private BigDecimal quantity;
    private StockReservationStatus status;
    private StockReferenceType referenceType;
    private String referenceCode;
    private String reservedBy;
    private Instant reservedAt;
    private Instant expiresAt;
    private Instant closedAt;
}
