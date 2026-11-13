package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.model.AccArchive;
import com.sni.bokaticowork.features.accounting.model.AccExportBatch;
import com.sni.bokaticowork.features.accounting.model.AccExportMapping;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingArchiveService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingExportService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/** Exports pour l'expert-comptable, outils de reprise et archive annuelle (lot F14). Les droits sont controles par les services. */
@RestController
@RequestMapping(ApiPath.V1)
@RequiredArgsConstructor
public class AccountingExchangeController {

    private final AccountingExportService exports;
    private final AccountingImportService imports;
    private final AccountingArchiveService archives;

    // ------------------------------------------------------------------ exports

    @Audited(module = "ACCOUNTING", action = "EXPORT", ressource = "acc_export_batch")
    @PostMapping("/accounting/exports")
    public ResponseEntity<byte[]> export(@RequestBody AccountingExportService.ExportRequest request) {
        AccountingExportService.ExportFile file = exports.export(request);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .header("X-Export-Batch", file.batch().getNumber())
                .contentType(MediaType.parseMediaType(file.contentType())).body(file.content());
    }

    @GetMapping("/accounting/exports")
    public ResponseEntity<List<AccExportBatch>> batches() {
        return ResponseEntity.ok(exports.batches());
    }

    @GetMapping("/accounting/exports/mappings")
    public ResponseEntity<List<AccExportMapping>> mappings(@RequestParam String format) {
        return ResponseEntity.ok(exports.mappings(format));
    }

    @Audited(module = "ACCOUNTING", action = "EXPORT_MAPPING", ressource = "acc_export_mapping")
    @PostMapping("/accounting/exports/mappings")
    public ResponseEntity<AccExportMapping> saveMapping(@RequestBody AccountingExportService.MappingRequest request) {
        return ResponseEntity.ok(exports.saveMapping(request));
    }

    @Audited(module = "ACCOUNTING", action = "EXPORT_MAPPING_DELETE", ressource = "acc_export_mapping")
    @DeleteMapping("/accounting/exports/mappings/{id}")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long id) {
        exports.deleteMapping(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ reprise

    @Audited(module = "ACCOUNTING", action = "IMPORT_ENTRIES", ressource = "acc_entry")
    @PostMapping(value = "/accounting/imports/entries", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AccountingImportService.ImportResult> importEntries(@RequestParam("file") MultipartFile file,
                                                                             @RequestParam(required = false, defaultValue = "false") boolean validate) {
        return ResponseEntity.ok(imports.importEntries(bytes(file), validate));
    }

    @Audited(module = "ACCOUNTING", action = "IMPORT_OPENING", ressource = "acc_entry")
    @PostMapping(value = "/accounting/imports/opening/{year}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AccountingImportService.ImportResult> importOpening(@PathVariable String year, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imports.importOpening(year, bytes(file)));
    }

    @PostMapping(value = "/accounting/imports/compare", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AccountingImportService.Comparison> compare(@RequestParam("file") MultipartFile file,
                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                                     @RequestParam(required = false, defaultValue = "CSV") String format) {
        return ResponseEntity.ok(imports.compare(date, format, bytes(file)));
    }

    // ------------------------------------------------------------------ archive

    @Audited(module = "ACCOUNTING", action = "ARCHIVE", ressource = "acc_archive")
    @PostMapping("/accounting/archives/{year}")
    public ResponseEntity<byte[]> archive(@PathVariable String year) {
        AccountingArchiveService.Archive archive = archives.archive(year);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archive.record().getFileName() + "\"")
                .header("X-Archive-Sha256", archive.record().getContentHash())
                .contentType(MediaType.parseMediaType("application/zip")).body(archive.content());
    }

    @GetMapping("/accounting/archives/{year}")
    public ResponseEntity<List<AccArchive>> archives(@PathVariable String year) {
        return ResponseEntity.ok(archives.archives(year));
    }

    private static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BadRequestException("Fichier illisible : " + exception.getMessage());
        }
    }
}
