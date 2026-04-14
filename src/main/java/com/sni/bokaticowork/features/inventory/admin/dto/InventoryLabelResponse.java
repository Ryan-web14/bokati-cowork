package com.sni.bokaticowork.features.inventory.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryLabelResponse {
    private String labelType;
    private String code;
    private String displayText;
    private String barcodeValue;
    private String qrValue;
}
