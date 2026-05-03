package com.sni.bokaticowork.features.document.kyc.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycBulkApproveRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycBulkRejectRequest;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycBulkActionResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentOcrResultResponse;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/kyc/documents")
public class KycDocumentReviewController {

    private final KycService service;

    @PostMapping("/bulk-approve")
    @Audited(module = "KYC", action = "BULK_APPROVE_DOCUMENTS", ressource = "kyc_document")
    @Idempotent(operation = "KYC_DOCUMENT_BULK_APPROVE")
    public ResponseEntity<KycBulkActionResponse> bulkApprove(@Valid @RequestBody KycBulkApproveRequest request) {
        return ResponseEntity.ok(service.bulkApprove(request));
    }

    @PostMapping("/bulk-reject")
    @Audited(module = "KYC", action = "BULK_REJECT_DOCUMENTS", ressource = "kyc_document")
    @Idempotent(operation = "KYC_DOCUMENT_BULK_REJECT")
    public ResponseEntity<KycBulkActionResponse> bulkReject(@Valid @RequestBody KycBulkRejectRequest request) {
        return ResponseEntity.ok(service.bulkReject(request));
    }

    @GetMapping("/{documentCode}/ocr-result")
    public ResponseEntity<KycDocumentOcrResultResponse> ocrResult(@PathVariable String documentCode) {
        return ResponseEntity.ok(service.getOcrResult(documentCode));
    }
}
