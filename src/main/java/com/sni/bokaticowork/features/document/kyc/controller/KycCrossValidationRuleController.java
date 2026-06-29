package com.sni.bokaticowork.features.document.kyc.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycCrossValidationRuleRequest;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCrossValidationRuleResponse;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycCrossValidationRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/kyc/cross-validation-rules")
public class KycCrossValidationRuleController {

    private final KycCrossValidationRuleService service;

    @PostMapping
    @Audited(module = "KYC", action = "CREATE_CROSS_VALIDATION_RULE", ressource = "kyc_cross_validation_rule")
    public ResponseEntity<KycCrossValidationRuleResponse> create(@Valid @RequestBody KycCrossValidationRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @Audited(module = "KYC", action = "UPDATE_CROSS_VALIDATION_RULE", ressource = "kyc_cross_validation_rule")
    public ResponseEntity<KycCrossValidationRuleResponse> update(@PathVariable Long id, @Valid @RequestBody KycCrossValidationRuleRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KycCrossValidationRuleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<KycCrossValidationRuleResponse>> list(@RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(service.list(activeOnly));
    }

    @DeleteMapping("/{id}")
    @Audited(module = "KYC", action = "DELETE_CROSS_VALIDATION_RULE", ressource = "kyc_cross_validation_rule")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
