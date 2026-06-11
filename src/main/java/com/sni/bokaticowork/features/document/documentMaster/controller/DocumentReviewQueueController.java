package com.sni.bokaticowork.features.document.documentMaster.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentBulkApproveRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentBulkCorrectionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentBulkRejectRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentBulkActionResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentReviewQueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/review")
public class DocumentReviewQueueController {

    private final DocumentReviewQueueService service;

    @GetMapping("/queue")
    @PreAuthorize("hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')")
    public ResponseEntity<PaginatedResponse<DocumentResponse>> globalQueue(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.getGlobalQueue(pageable));
    }

    @GetMapping("/{space}")
    @PreAuthorize("@docSpaceSecurity.canReview(authentication, #space)")
    public ResponseEntity<PaginatedResponse<DocumentResponse>> spaceQueue(
            @PathVariable DocumentSpace space,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.getQueue(space, pageable));
    }

    @PostMapping("/{space}/bulk-approve")
    @PreAuthorize("@docSpaceSecurity.canReview(authentication, #space)")
    @Audited(module = "DOCUMENT", action = "BULK_APPROVE", ressource = "document")
    public ResponseEntity<DocumentBulkActionResponse> bulkApprove(
            @PathVariable DocumentSpace space,
            @Valid @RequestBody DocumentBulkApproveRequest request) {
        return ResponseEntity.ok(service.bulkApprove(space, request));
    }

    @PostMapping("/{space}/bulk-reject")
    @PreAuthorize("@docSpaceSecurity.canReview(authentication, #space)")
    @Audited(module = "DOCUMENT", action = "BULK_REJECT", ressource = "document")
    public ResponseEntity<DocumentBulkActionResponse> bulkReject(
            @PathVariable DocumentSpace space,
            @Valid @RequestBody DocumentBulkRejectRequest request) {
        return ResponseEntity.ok(service.bulkReject(space, request));
    }

    @PostMapping("/{space}/bulk-request-correction")
    @PreAuthorize("@docSpaceSecurity.canReview(authentication, #space)")
    @Audited(module = "DOCUMENT", action = "BULK_REQUEST_CORRECTION", ressource = "document")
    public ResponseEntity<DocumentBulkActionResponse> bulkRequestCorrection(
            @PathVariable DocumentSpace space,
            @Valid @RequestBody DocumentBulkCorrectionRequest request) {
        return ResponseEntity.ok(service.bulkRequestCorrection(space, request));
    }
}
