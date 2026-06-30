package com.sni.bokaticowork.features.contract.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.contract.dto.request.CancelAmendmentRequest;
import com.sni.bokaticowork.features.contract.dto.request.ContractAmendmentSectionRequest;
import com.sni.bokaticowork.features.contract.dto.request.ContractAmendmentVariableRequest;
import com.sni.bokaticowork.features.contract.dto.request.ProposeAmendmentRequest;
import com.sni.bokaticowork.features.contract.dto.request.ReviewAmendmentRequest;
import com.sni.bokaticowork.features.contract.dto.request.SignAmendmentRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractAmendmentResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractAmendmentSectionResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractAmendmentVariableResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractAuditEventResponse;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractAmendmentService;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractAuditEventService;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ContractAmendmentController {

    private final ContractAmendmentService amendmentService;
    private final ContractAuditEventService auditEventService;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Audited(module = "CONTRACT_AMENDMENT", action = "PROPOSE_AMENDMENT")
    @PostMapping(ApiPath.V1 + "/contracts/{contractCode}/amendments")
    public ResponseEntity<ContractAmendmentResponse> propose(
            @PathVariable String contractCode,
            @RequestParam Long proposedBy,
            @Valid @RequestBody ProposeAmendmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(amendmentService.propose(contractCode, proposedBy, request));
    }

    @GetMapping(ApiPath.V1 + "/contracts/{contractCode}/amendments")
    public ResponseEntity<List<ContractAmendmentResponse>> listByContract(
            @PathVariable String contractCode) {
        return ResponseEntity.ok(amendmentService.listByContract(contractCode));
    }

    @GetMapping(ApiPath.V1 + "/contracts/{contractCode}/audit-events")
    public ResponseEntity<List<ContractAuditEventResponse>> getAuditTrail(
            @PathVariable String contractCode) {
        return ResponseEntity.ok(auditEventService.getAuditTrail(contractCode));
    }

    @GetMapping(ApiPath.V1 + "/amendments/{amendmentCode}")
    public ResponseEntity<ContractAmendmentResponse> getByCode(
            @PathVariable String amendmentCode) {
        return ResponseEntity.ok(amendmentService.getByCode(amendmentCode));
    }

    @Audited(module = "CONTRACT_AMENDMENT", action = "SUBMIT_AMENDMENT_FOR_REVIEW")
    @PostMapping(ApiPath.V1 + "/amendments/{amendmentCode}/submit")
    public ResponseEntity<ContractAmendmentResponse> submitForReview(
            @PathVariable String amendmentCode) {
        return ResponseEntity.ok(amendmentService.submitForReview(amendmentCode));
    }

    @Audited(module = "CONTRACT_AMENDMENT", action = "APPROVE_AMENDMENT_REVIEW")
    @PostMapping(ApiPath.V1 + "/amendments/{amendmentCode}/approve")
    public ResponseEntity<ContractAmendmentResponse> approveReview(
            @PathVariable String amendmentCode,
            @RequestParam Long reviewedBy,
            @RequestBody(required = false) ReviewAmendmentRequest request) {
        return ResponseEntity.ok(amendmentService.approveReview(amendmentCode, reviewedBy, request));
    }

    @Audited(module = "CONTRACT_AMENDMENT", action = "REJECT_AMENDMENT_REVIEW")
    @PostMapping(ApiPath.V1 + "/amendments/{amendmentCode}/reject")
    public ResponseEntity<ContractAmendmentResponse> rejectReview(
            @PathVariable String amendmentCode,
            @RequestParam Long reviewedBy,
            @RequestBody(required = false) ReviewAmendmentRequest request) {
        return ResponseEntity.ok(amendmentService.rejectReview(amendmentCode, reviewedBy, request));
    }

    @Audited(module = "CONTRACT_AMENDMENT", action = "SIGN_AMENDMENT")
    @PostMapping(ApiPath.V1 + "/amendments/{amendmentCode}/sign")
    public ResponseEntity<ContractAmendmentResponse> sign(
            @PathVariable String amendmentCode,
            @RequestParam Long actorId,
            @RequestBody(required = false) SignAmendmentRequest request) {
        return ResponseEntity.ok(amendmentService.sign(amendmentCode, actorId, request));
    }

    @Audited(module = "CONTRACT_AMENDMENT", action = "CANCEL_AMENDMENT")
    @PostMapping(ApiPath.V1 + "/amendments/{amendmentCode}/cancel")
    public ResponseEntity<ContractAmendmentResponse> cancel(
            @PathVariable String amendmentCode,
            @RequestParam Long actorId,
            @RequestBody(required = false) CancelAmendmentRequest request) {
        return ResponseEntity.ok(amendmentService.cancel(amendmentCode, actorId,
                request != null ? request.getReason() : null));
    }

    // ── Section management ─────────────────────────────────────────────────────

    @Audited(module = "CONTRACT_AMENDMENT", action = "ADD_AMENDMENT_SECTION")
    @PostMapping(ApiPath.V1 + "/amendments/{amendmentCode}/sections")
    public ResponseEntity<ContractAmendmentSectionResponse> addSection(
            @PathVariable String amendmentCode,
            @Valid @RequestBody ContractAmendmentSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(amendmentService.addSection(amendmentCode, request));
    }

    @Audited(module = "CONTRACT_AMENDMENT", action = "UPDATE_AMENDMENT_SECTION")
    @PutMapping(ApiPath.V1 + "/amendments/{amendmentCode}/sections/{sectionId}")
    public ResponseEntity<ContractAmendmentSectionResponse> updateSection(
            @PathVariable String amendmentCode,
            @PathVariable Long sectionId,
            @RequestBody ContractAmendmentSectionRequest request) {
        return ResponseEntity.ok(amendmentService.updateSection(amendmentCode, sectionId, request));
    }

    @Audited(module = "CONTRACT_AMENDMENT", action = "REMOVE_AMENDMENT_SECTION")
    @DeleteMapping(ApiPath.V1 + "/amendments/{amendmentCode}/sections/{sectionId}")
    public ResponseEntity<Void> removeSection(
            @PathVariable String amendmentCode,
            @PathVariable Long sectionId) {
        amendmentService.removeSection(amendmentCode, sectionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(ApiPath.V1 + "/amendments/{amendmentCode}/sections")
    public ResponseEntity<List<ContractAmendmentSectionResponse>> listSections(
            @PathVariable String amendmentCode) {
        return ResponseEntity.ok(amendmentService.listSections(amendmentCode));
    }

    // ── Variable management ────────────────────────────────────────────────────

    @Audited(module = "CONTRACT_AMENDMENT", action = "SET_AMENDMENT_VARIABLES")
    @PutMapping(ApiPath.V1 + "/amendments/{amendmentCode}/variables")
    public ResponseEntity<List<ContractAmendmentVariableResponse>> setVariables(
            @PathVariable String amendmentCode,
            @Valid @RequestBody List<ContractAmendmentVariableRequest> variables) {
        return ResponseEntity.ok(amendmentService.setVariables(amendmentCode, variables));
    }

    @GetMapping(ApiPath.V1 + "/amendments/{amendmentCode}/variables")
    public ResponseEntity<List<ContractAmendmentVariableResponse>> listVariables(
            @PathVariable String amendmentCode) {
        return ResponseEntity.ok(amendmentService.listVariables(amendmentCode));
    }

    // ── Preview & PDF ──────────────────────────────────────────────────────────

    @GetMapping(value = ApiPath.V1 + "/amendments/{amendmentCode}/preview",
            produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewHtml(@PathVariable String amendmentCode) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(amendmentService.previewHtml(amendmentCode));
    }

    @Audited(module = "CONTRACT_AMENDMENT", action = "GENERATE_AMENDMENT_PDF")
    @PostMapping(ApiPath.V1 + "/amendments/{amendmentCode}/generate-pdf")
    public ResponseEntity<DocumentResponse> generatePdf(
            @PathVariable String amendmentCode,
            @RequestParam Long uploadedBy) {
        return ResponseEntity.ok(amendmentService.generatePdf(amendmentCode, uploadedBy));
    }
}
