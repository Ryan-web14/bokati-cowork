package com.sni.bokaticowork.features.document.retention.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.retention.dto.request.DocumentRetentionPolicyRequest;
import com.sni.bokaticowork.features.document.retention.dto.response.DocumentRetentionPolicyResponse;
import com.sni.bokaticowork.features.document.retention.service.interfaces.DocumentRetentionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/document-retention-policies")
@PreAuthorize("hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')")
public class DocumentRetentionPolicyController {

    private final DocumentRetentionService retentionService;

    @GetMapping
    public ResponseEntity<List<DocumentRetentionPolicyResponse>> list() {
        return ResponseEntity.ok(retentionService.listAll());
    }

    @GetMapping("/{code}")
    public ResponseEntity<DocumentRetentionPolicyResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(retentionService.getByCode(code));
    }

    @PostMapping
    @Audited(module = "DOCUMENT", action = "RETENTION_POLICY_CREATE", ressource = "document_retention_policy")
    public ResponseEntity<DocumentRetentionPolicyResponse> create(
            @Valid @RequestBody DocumentRetentionPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(retentionService.create(request));
    }

    @PutMapping("/{code}")
    @Audited(module = "DOCUMENT", action = "RETENTION_POLICY_UPDATE", ressource = "document_retention_policy")
    public ResponseEntity<DocumentRetentionPolicyResponse> update(
            @PathVariable String code,
            @Valid @RequestBody DocumentRetentionPolicyRequest request) {
        return ResponseEntity.ok(retentionService.update(code, request));
    }

    @DeleteMapping("/{code}")
    @Audited(module = "DOCUMENT", action = "RETENTION_POLICY_DEACTIVATE", ressource = "document_retention_policy")
    public ResponseEntity<Void> deactivate(@PathVariable String code) {
        retentionService.deactivate(code);
        return ResponseEntity.noContent().build();
    }
}
