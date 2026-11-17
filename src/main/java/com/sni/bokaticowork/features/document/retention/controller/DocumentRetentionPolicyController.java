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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/document-retention-policies")
// Lot KG6 : un seul droit, SYSTEM:SETTINGS (regles d'acces), comme les autres parametres documentaires.
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

    /** Lot KG4 : une politique desactivee se reactive. */
    @org.springframework.web.bind.annotation.PatchMapping("/{code}/activate")
    @Audited(module = "DOCUMENT", action = "RETENTION_POLICY_ACTIVATE", ressource = "document_retention_policy")
    public ResponseEntity<DocumentRetentionPolicyResponse> activate(@PathVariable String code) {
        retentionService.activate(code);
        return ResponseEntity.ok(retentionService.getByCode(code));
    }

    @DeleteMapping("/{code}")
    @Audited(module = "DOCUMENT", action = "RETENTION_POLICY_DEACTIVATE", ressource = "document_retention_policy")
    public ResponseEntity<Void> deactivate(@PathVariable String code) {
        retentionService.deactivate(code);
        return ResponseEntity.noContent().build();
    }
}
