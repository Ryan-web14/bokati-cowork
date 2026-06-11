package com.sni.bokaticowork.features.document.documentMaster.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentCorrectionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentReviewDecisionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentAccessLogResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentAnalyticsResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentDashboardResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFileResult;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFolderResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentVersionResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentAccessLogServiceImpl;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentSearchService;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/documents")
public class DocumentController {

    private final DocumentService service;
    private final DocumentSearchService searchService;
    private final DocumentAccessLogServiceImpl accessLogService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Audited(module = "DOCUMENT", action = "UPLOAD", ressource = "document")
    @Idempotent(operation = "DOCUMENT_UPLOAD", requestBodyArgIndex = -1)
    public ResponseEntity<DocumentResponse> upload(
            @Valid @ModelAttribute DocumentUploadMetadataRequest metadata,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(metadata, file));
    }

    @PostMapping(value = "/{code}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Audited(module = "DOCUMENT", action = "REPLACE", ressource = "document")
    @Idempotent(operation = "DOCUMENT_REPLACE", requestBodyArgIndex = -1)
    public ResponseEntity<DocumentResponse> replace(
            @PathVariable String code,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(service.replace(code, file));
    }

    @GetMapping("/{code}")
    public ResponseEntity<DocumentResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.getByCode(code));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<DocumentResponse>> list(
            @RequestParam(required = false) DocumentOwnerType ownerType,
            @RequestParam(required = false) String ownerCode,
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.list(ownerType, ownerCode, pageable));
    }

    @GetMapping("/{code}/versions")
    public ResponseEntity<List<DocumentVersionResponse>> listVersions(@PathVariable String code) {
        return ResponseEntity.ok(service.listVersions(code));
    }

    @GetMapping("/{code}/download")
    public ResponseEntity<byte[]> download(@PathVariable String code, HttpServletRequest request) {
        DocumentFileResult result = service.getFileWithMeta(code);
        accessLogService.log(code, "DOWNLOAD", clientIp(request), request.getHeader("User-Agent"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.mimeType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(result.fileName()).build());
        headers.setContentLength(result.content().length);
        return ResponseEntity.ok().headers(headers).body(result.content());
    }

    @GetMapping("/{code}/preview")
    public ResponseEntity<byte[]> preview(@PathVariable String code, HttpServletRequest request) {
        DocumentFileResult result = service.getFileWithMeta(code);
        accessLogService.log(code, "PREVIEW", clientIp(request), request.getHeader("User-Agent"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.mimeType()));
        headers.setContentDisposition(ContentDisposition.inline().filename(result.fileName()).build());
        headers.setContentLength(result.content().length);
        return ResponseEntity.ok().headers(headers).body(result.content());
    }

    @PostMapping("/{code}/approve")
    @PreAuthorize("hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')")
    @Audited(module = "DOCUMENT", action = "APPROVE", ressource = "document")
    @Idempotent(operation = "DOCUMENT_APPROVE")
    public ResponseEntity<DocumentResponse> approve(@PathVariable String code, @Valid @RequestBody DocumentReviewDecisionRequest request) {
        return ResponseEntity.ok(service.approve(code, request));
    }

    @PostMapping("/{code}/reject")
    @PreAuthorize("hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')")
    @Audited(module = "DOCUMENT", action = "REJECT", ressource = "document")
    @Idempotent(operation = "DOCUMENT_REJECT")
    public ResponseEntity<DocumentResponse> reject(@PathVariable String code, @Valid @RequestBody DocumentReviewDecisionRequest request) {
        return ResponseEntity.ok(service.reject(code, request));
    }

    @PostMapping("/{code}/request-correction")
    @PreAuthorize("hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')")
    @Audited(module = "DOCUMENT", action = "REQUEST_CORRECTION", ressource = "document")
    public ResponseEntity<DocumentResponse> requestCorrection(
            @PathVariable String code,
            @Valid @RequestBody DocumentCorrectionRequest request) {
        return ResponseEntity.ok(service.requestCorrection(code, request));
    }

    @PostMapping("/{code}/versions/{versionNumber}/restore")
    @PreAuthorize("hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')")
    @Audited(module = "DOCUMENT", action = "RESTORE_VERSION", ressource = "document")
    public ResponseEntity<DocumentResponse> restoreVersion(
            @PathVariable String code,
            @PathVariable Integer versionNumber) {
        return ResponseEntity.ok(service.restoreVersion(code, versionNumber));
    }

    @PostMapping("/{code}/archive")
    @Audited(module = "DOCUMENT", action = "ARCHIVE", ressource = "document")
    public ResponseEntity<DocumentResponse> archive(
            @PathVariable String code,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(service.archive(code, reason));
    }

    // ── Search, dashboard, folder ────────────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<DocumentResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) DocumentSpace space,
            @RequestParam(required = false) DocumentCategory category,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) DocumentOwnerType ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) List<DocumentStatus> statuses,
            @RequestParam(required = false) Integer expiresInDays,
            @RequestParam(required = false) Boolean isExpired,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate uploadedAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate uploadedBefore,
            @RequestParam(required = false) String metaKey,
            @RequestParam(required = false) String metaValue,
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(searchService.search(
                q, space, category, tags, ownerType, ownerCode,
                statuses, expiresInDays, isExpired,
                uploadedAfter, uploadedBefore, metaKey, metaValue, pageable));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DocumentDashboardResponse> dashboard(
            @RequestParam(required = false) DocumentSpace space) {
        return ResponseEntity.ok(searchService.dashboard(space));
    }

    @GetMapping("/folder")
    public ResponseEntity<DocumentFolderResponse> folder(
            @RequestParam DocumentOwnerType ownerType,
            @RequestParam String ownerCode) {
        return ResponseEntity.ok(searchService.folder(ownerType, ownerCode));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')")
    public ResponseEntity<DocumentAnalyticsResponse> analytics(
            @RequestParam(required = false) DocumentSpace space,
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(searchService.analytics(space, months));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) DocumentSpace space,
            @RequestParam(required = false) DocumentCategory category,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) DocumentOwnerType ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) List<DocumentStatus> statuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate uploadedAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate uploadedBefore) {
        byte[] csv = searchService.exportCsv(space, category, tags, ownerType, ownerCode,
                statuses, uploadedAfter, uploadedBefore);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documents-export.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csv);
    }

    @GetMapping("/{code}/access-logs")
    @PreAuthorize("hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')")
    public ResponseEntity<PaginatedResponse<DocumentAccessLogResponse>> accessLogs(
            @PathVariable String code,
            @PageableDefault(size = 20, sort = "accessedAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(accessLogService.getByDocument(code, pageable));
    }

    @GetMapping("/access-logs/by-user")
    @PreAuthorize("hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')")
    public ResponseEntity<PaginatedResponse<DocumentAccessLogResponse>> accessLogsByUser(
            @RequestParam Long userId,
            @PageableDefault(size = 20, sort = "accessedAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(accessLogService.getByUser(userId, pageable));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
