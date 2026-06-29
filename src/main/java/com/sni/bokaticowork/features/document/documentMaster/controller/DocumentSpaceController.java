package com.sni.bokaticowork.features.document.documentMaster.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.SpaceDocumentUploadRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentDashboardResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentSpaceService;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/spaces")
public class DocumentSpaceController {

    private final DocumentSpaceService spaceService;
    private final DocumentSearchService searchService;

    // ── CONTRACT SPACE ──────────────────────────────────────────────────────

    @PostMapping(value = "/contracts/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Audited(module = "DOCUMENT", action = "UPLOAD_CONTRACT", ressource = "document")
    @Idempotent(operation = "SPACE_CONTRACT_UPLOAD", requestBodyArgIndex = -1)
    public ResponseEntity<DocumentResponse> uploadContract(
            @Valid @ModelAttribute SpaceDocumentUploadRequest request,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spaceService.uploadToSpace(DocumentSpace.CONTRACT_SPACE, request, file));
    }

    @GetMapping("/contracts/documents")
    public ResponseEntity<PaginatedResponse<DocumentResponse>> listContracts(
            @RequestParam(required = false) DocumentOwnerType ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) List<DocumentStatus> statuses,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(searchService.search(
                q, DocumentSpace.CONTRACT_SPACE, null, null, ownerType, ownerCode,
                statuses, null, null, null, null, null, null, pageable));
    }

    @GetMapping("/contracts/dashboard")
    public ResponseEntity<DocumentDashboardResponse> contractDashboard() {
        return ResponseEntity.ok(searchService.dashboard(DocumentSpace.CONTRACT_SPACE));
    }

    // ── FINANCIAL SPACE ─────────────────────────────────────────────────────

    @PostMapping(value = "/financial/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Audited(module = "DOCUMENT", action = "UPLOAD_FINANCIAL", ressource = "document")
    @Idempotent(operation = "SPACE_FINANCIAL_UPLOAD", requestBodyArgIndex = -1)
    public ResponseEntity<DocumentResponse> uploadFinancial(
            @Valid @ModelAttribute SpaceDocumentUploadRequest request,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spaceService.uploadToSpace(DocumentSpace.FINANCIAL_SPACE, request, file));
    }

    @GetMapping("/financial/documents")
    public ResponseEntity<PaginatedResponse<DocumentResponse>> listFinancial(
            @RequestParam(required = false) DocumentOwnerType ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) List<DocumentStatus> statuses,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate uploadedAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate uploadedBefore,
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(searchService.search(
                q, DocumentSpace.FINANCIAL_SPACE, null, null, ownerType, ownerCode,
                statuses, null, null, uploadedAfter, uploadedBefore, null, null, pageable));
    }

    @GetMapping("/financial/dashboard")
    public ResponseEntity<DocumentDashboardResponse> financialDashboard() {
        return ResponseEntity.ok(searchService.dashboard(DocumentSpace.FINANCIAL_SPACE));
    }

    // ── ASSET SPACE ─────────────────────────────────────────────────────────

    @PostMapping(value = "/assets/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Audited(module = "DOCUMENT", action = "UPLOAD_ASSET", ressource = "document")
    @Idempotent(operation = "SPACE_ASSET_UPLOAD", requestBodyArgIndex = -1)
    public ResponseEntity<DocumentResponse> uploadAsset(
            @Valid @ModelAttribute SpaceDocumentUploadRequest request,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spaceService.uploadToSpace(DocumentSpace.ASSET_SPACE, request, file));
    }

    @GetMapping("/assets/documents")
    public ResponseEntity<PaginatedResponse<DocumentResponse>> listAssets(
            @RequestParam(required = false) DocumentOwnerType ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) List<DocumentStatus> statuses,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(searchService.search(
                q, DocumentSpace.ASSET_SPACE, null, null, ownerType, ownerCode,
                statuses, null, null, null, null, null, null, pageable));
    }

    @GetMapping("/assets/dashboard")
    public ResponseEntity<DocumentDashboardResponse> assetDashboard() {
        return ResponseEntity.ok(searchService.dashboard(DocumentSpace.ASSET_SPACE));
    }

    // ── ADMINISTRATIVE SPACE ────────────────────────────────────────────────

    @PostMapping(value = "/administrative/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Audited(module = "DOCUMENT", action = "UPLOAD_ADMINISTRATIVE", ressource = "document")
    @Idempotent(operation = "SPACE_ADMIN_UPLOAD", requestBodyArgIndex = -1)
    public ResponseEntity<DocumentResponse> uploadAdministrative(
            @Valid @ModelAttribute SpaceDocumentUploadRequest request,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spaceService.uploadToSpace(DocumentSpace.ADMINISTRATIVE, request, file));
    }

    @GetMapping("/administrative/documents")
    public ResponseEntity<PaginatedResponse<DocumentResponse>> listAdministrative(
            @RequestParam(required = false) DocumentOwnerType ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) List<DocumentStatus> statuses,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(searchService.search(
                q, DocumentSpace.ADMINISTRATIVE, null, null, ownerType, ownerCode,
                statuses, null, null, null, null, null, null, pageable));
    }

    @GetMapping("/administrative/dashboard")
    public ResponseEntity<DocumentDashboardResponse> adminDashboard() {
        return ResponseEntity.ok(searchService.dashboard(DocumentSpace.ADMINISTRATIVE));
    }

    // ── GENERIC SPACE ───────────────────────────────────────────────────────

    @PostMapping(value = "/general/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Audited(module = "DOCUMENT", action = "UPLOAD_GENERAL", ressource = "document")
    @Idempotent(operation = "SPACE_GENERAL_UPLOAD", requestBodyArgIndex = -1)
    public ResponseEntity<DocumentResponse> uploadGeneral(
            @Valid @ModelAttribute SpaceDocumentUploadRequest request,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spaceService.uploadToSpace(DocumentSpace.GENERIC, request, file));
    }

    @GetMapping("/general/documents")
    public ResponseEntity<PaginatedResponse<DocumentResponse>> listGeneral(
            @RequestParam(required = false) DocumentOwnerType ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) List<DocumentStatus> statuses,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(searchService.search(
                q, DocumentSpace.GENERIC, null, null, ownerType, ownerCode,
                statuses, null, null, null, null, null, null, pageable));
    }

    @GetMapping("/general/dashboard")
    public ResponseEntity<DocumentDashboardResponse> generalDashboard() {
        return ResponseEntity.ok(searchService.dashboard(DocumentSpace.GENERIC));
    }
}
