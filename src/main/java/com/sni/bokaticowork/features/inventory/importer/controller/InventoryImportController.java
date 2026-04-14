package com.sni.bokaticowork.features.inventory.importer.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.importer.dto.InventoryImportResponse;
import com.sni.bokaticowork.features.inventory.importer.enums.InventoryImportType;
import com.sni.bokaticowork.features.inventory.importer.service.InventoryImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/import")
public class InventoryImportController {

    private final InventoryImportService service;

    @PostMapping(value = "/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Audited(module = "INVENTORY", action = "IMPORT", ressource = "inventory_import")
    @Idempotent(operation = "INVENTORY_IMPORT", required = false)
    public ResponseEntity<InventoryImportResponse> importFile(@PathVariable InventoryImportType type,
                                                              @RequestPart("file") MultipartFile file,
                                                              @RequestParam(defaultValue = "false") boolean dryRun) {
        return ResponseEntity.ok(service.importFile(type, file, dryRun));
    }
}
