package com.sni.bokaticowork.features.document.kyc.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.dto.request.CreateKycCaseRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycDecisionRequest;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycRequirementStatus;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<KycCaseResponse>> list(@RequestParam(required = false) DocumentOwnerType ownerType) {
        return ResponseEntity.ok(service.list(ownerType));
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
}
