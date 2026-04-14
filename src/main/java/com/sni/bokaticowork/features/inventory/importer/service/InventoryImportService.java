package com.sni.bokaticowork.features.inventory.importer.service;

import com.sni.bokaticowork.features.inventory.importer.dto.InventoryImportResponse;
import com.sni.bokaticowork.features.inventory.importer.enums.InventoryImportType;
import org.springframework.web.multipart.MultipartFile;

public interface InventoryImportService {
    InventoryImportResponse importFile(InventoryImportType type, MultipartFile file, boolean dryRun);
}
