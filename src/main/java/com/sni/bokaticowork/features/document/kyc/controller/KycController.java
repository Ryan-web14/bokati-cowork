package com.sni.bokaticowork.features.document.kyc.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycRiskLevel;
import com.sni.bokaticowork.features.document.kyc.dto.request.CreateKycCaseRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycAssignRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycCaseBulkApproveRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycCaseBulkRejectRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycCaseCorrectionRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycCaseNoteRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycCaseRequirementRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycDecisionRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycRiskLevelRequest;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycBulkActionResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseRequirementResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseNoteResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDashboardResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycExpiryDocumentStatus;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycRequirementStatus;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycTimelineEntryResponse;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/kyc/cases")
public class KycController {

    private final KycService service;

    @PostMapping
    @Audited(module = "KYC", action = "CREATE_CASE", ressource = "kyc_case")
    @Idempotent(operation = "KYC_CASE_CREATE")
    public ResponseEntity<KycCaseResponse> create(@Valid @RequestBody CreateKycCaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCase(request));
    }

    @GetMapping("/{code}")
    public ResponseEntity<KycCaseResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.getByCode(code));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<KycCaseResponse>> list(
            @RequestParam(required = false) KycCaseStatus status,
            @RequestParam(required = false) DocumentOwnerType ownerType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant submittedAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant submittedBefore,
            @RequestParam(required = false) Long reviewedBy,
            @RequestParam(required = false) Boolean pendingReviewOnly,
            @RequestParam(required = false) Integer expiringWithinDays,
            @RequestParam(required = false) KycRiskLevel riskLevel,
            @PageableDefault(size = 20, sort = "startedAt") Pageable pageable
    ) {
        return ResponseEntity.ok(service.search(status, ownerType, submittedAfter, submittedBefore, reviewedBy,
                pendingReviewOnly, expiringWithinDays, riskLevel, pageable));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<KycDashboardResponse> dashboard() {
        return ResponseEntity.ok(service.dashboard());
    }

    @GetMapping("/expiring-soon")
    public ResponseEntity<PaginatedResponse<KycCaseResponse>> expiringSoon(
            @RequestParam(defaultValue = "30") Integer days,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.expiringSoon(days, pageable));
    }

    @GetMapping("/my-queue")
    public ResponseEntity<PaginatedResponse<KycCaseResponse>> myQueue(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.myQueue(userId, pageable));
    }

    @PostMapping("/{code}/submit")
    @Audited(module = "KYC", action = "SUBMIT_CASE", ressource = "kyc_case")
    @Idempotent(operation = "KYC_CASE_SUBMIT", requestBodyArgIndex = -1)
    public ResponseEntity<KycCaseResponse> submit(@PathVariable String code) {
        return ResponseEntity.ok(service.submit(code));
    }

    @PostMapping("/{code}/approve")
    @Audited(module = "KYC", action = "APPROVE_CASE", ressource = "kyc_case")
    @Idempotent(operation = "KYC_CASE_APPROVE")
    public ResponseEntity<KycCaseResponse> approve(@PathVariable String code, @Valid @RequestBody KycDecisionRequest request) {
        return ResponseEntity.ok(service.approve(code, request));
    }

    @PostMapping("/{code}/reject")
    @Audited(module = "KYC", action = "REJECT_CASE", ressource = "kyc_case")
    @Idempotent(operation = "KYC_CASE_REJECT")
    public ResponseEntity<KycCaseResponse> reject(@PathVariable String code, @Valid @RequestBody KycDecisionRequest request) {
        return ResponseEntity.ok(service.reject(code, request));
    }

    @GetMapping("/{code}/missing-requirements")
    public ResponseEntity<List<KycRequirementStatus>> missingRequirements(@PathVariable String code) {
        return ResponseEntity.ok(service.getMissingRequirements(code));
    }

    @PatchMapping("/{code}/assign")
    @Audited(module = "KYC", action = "ASSIGN_CASE", ressource = "kyc_case")
    public ResponseEntity<KycCaseResponse> assign(@PathVariable String code, @Valid @RequestBody KycAssignRequest request) {
        return ResponseEntity.ok(service.assign(code, request));
    }

    @PostMapping("/{code}/notes")
    @Audited(module = "KYC", action = "ADD_CASE_NOTE", ressource = "kyc_case")
    public ResponseEntity<KycCaseNoteResponse> addNote(@PathVariable String code, @Valid @RequestBody KycCaseNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addNote(code, request));
    }

    @GetMapping("/{code}/notes")
    public ResponseEntity<List<KycCaseNoteResponse>> notes(@PathVariable String code) {
        return ResponseEntity.ok(service.listNotes(code));
    }

    @GetMapping("/{code}/timeline")
    public ResponseEntity<List<KycTimelineEntryResponse>> timeline(@PathVariable String code) {
        return ResponseEntity.ok(service.timeline(code));
    }

    @GetMapping("/{code}/expiry-status")
    public ResponseEntity<List<KycExpiryDocumentStatus>> expiryStatus(@PathVariable String code) {
        return ResponseEntity.ok(service.expiryStatus(code));
    }

    @PatchMapping("/{code}/risk-level")
    @Audited(module = "KYC", action = "UPDATE_RISK_LEVEL", ressource = "kyc_case")
    public ResponseEntity<KycCaseResponse> riskLevel(@PathVariable String code, @Valid @RequestBody KycRiskLevelRequest request) {
        return ResponseEntity.ok(service.updateRiskLevel(code, request));
    }

    @GetMapping("/{code}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable String code,
            @RequestParam(defaultValue = "true") boolean includeInternalNotes
    ) {
        byte[] pdf = service.exportPdf(code, includeInternalNotes);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"kyc-" + code + ".pdf\"")
                .body(pdf);
    }

    // ── Case Requirements ───────────────────────────────────────────────────

    @GetMapping("/{code}/requirements")
    public ResponseEntity<List<KycCaseRequirementResponse>> listCaseRequirements(@PathVariable String code) {
        return ResponseEntity.ok(service.listCaseRequirements(code));
    }

    @PostMapping("/{code}/requirements")
    @Audited(module = "KYC", action = "ADD_CASE_REQUIREMENT", ressource = "kyc_case")
    public ResponseEntity<KycCaseRequirementResponse> addCaseRequirement(
            @PathVariable String code,
            @Valid @RequestBody KycCaseRequirementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addCaseRequirement(code, request));
    }

    @DeleteMapping("/{code}/requirements/{documentTypeCode}")
    @Audited(module = "KYC", action = "REMOVE_CASE_REQUIREMENT", ressource = "kyc_case")
    public ResponseEntity<Void> removeCaseRequirement(
            @PathVariable String code,
            @PathVariable String documentTypeCode) {
        service.removeCaseRequirement(code, documentTypeCode);
        return ResponseEntity.noContent().build();
    }

    // ── Case-scoped Document Review ─────────────────────────────────────────

    @GetMapping("/{code}/review-queue")
    public ResponseEntity<List<KycDocumentResponse>> caseReviewQueue(@PathVariable String code) {
        return ResponseEntity.ok(service.caseReviewQueue(code));
    }

    @PostMapping("/{code}/documents/bulk-approve")
    @Audited(module = "KYC", action = "CASE_BULK_APPROVE", ressource = "kyc_case")
    @Idempotent(operation = "KYC_CASE_BULK_APPROVE")
    public ResponseEntity<KycBulkActionResponse> caseBulkApprove(
            @PathVariable String code,
            @Valid @RequestBody KycCaseBulkApproveRequest request) {
        return ResponseEntity.ok(service.caseBulkApprove(code, request));
    }

    @PostMapping("/{code}/documents/bulk-reject")
    @Audited(module = "KYC", action = "CASE_BULK_REJECT", ressource = "kyc_case")
    @Idempotent(operation = "KYC_CASE_BULK_REJECT")
    public ResponseEntity<KycBulkActionResponse> caseBulkReject(
            @PathVariable String code,
            @Valid @RequestBody KycCaseBulkRejectRequest request) {
        return ResponseEntity.ok(service.caseBulkReject(code, request));
    }

    @PostMapping("/{code}/documents/request-correction")
    @Audited(module = "KYC", action = "CASE_CORRECTION_REQUEST", ressource = "kyc_case")
    public ResponseEntity<KycDocumentResponse> caseCorrectionRequest(
            @PathVariable String code,
            @Valid @RequestBody KycCaseCorrectionRequest request) {
        return ResponseEntity.ok(service.caseCorrectionRequest(code, request));
    }
}
