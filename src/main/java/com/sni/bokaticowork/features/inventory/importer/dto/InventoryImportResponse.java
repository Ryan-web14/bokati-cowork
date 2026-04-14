package com.sni.bokaticowork.features.inventory.importer.dto;

import com.sni.bokaticowork.features.inventory.importer.enums.InventoryImportType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InventoryImportResponse {
    private InventoryImportType importType;
    private String fileName;
    private boolean dryRun;
    private int totalRows;
    private int successCount;
    private int errorCount;
    private List<InventoryImportRowResult> rows;
}
