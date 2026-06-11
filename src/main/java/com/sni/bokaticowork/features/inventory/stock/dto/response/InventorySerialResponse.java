package com.sni.bokaticowork.features.inventory.stock.dto.response;

import com.sni.bokaticowork.features.inventory.stock.enums.InventorySerialStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class InventorySerialResponse {

    private String itemCode;
    private String locationCode;
    private String serialNumber;
    private InventorySerialStatus status;
    private String lotNumber;
    private Instant receivedAt;
    private Instant issuedAt;
}
