package com.sni.bokaticowork.features.inventory.importer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryImportRowResult {
    private int rowNumber;
    private boolean success;
    private String referenceCode;
    private String message;
}
