package com.sni.bokaticowork.features.document.documentMaster.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.GrantPermissionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentPermissionResponse;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/document-permissions")
public class DocumentPermissionController {

    private final DocumentPermissionService service;

    @PostMapping
    @Audited(module = "DOCUMENT", action = "GRANT_PERMISSION", ressource = "document_permission")
    public ResponseEntity<DocumentPermissionResponse> grant(@Valid @RequestBody GrantPermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.grant(request));
    }

    @DeleteMapping
    @Audited(module = "DOCUMENT", action = "REVOKE_PERMISSION", ressource = "document_permission")
    public ResponseEntity<Void> revoke(
            @RequestParam String targetType, @RequestParam String targetCode,
            @RequestParam String granteeType, @RequestParam Long granteeId,
            @RequestParam String permission) {
        service.revoke(targetType, targetCode, granteeType, granteeId, permission);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<DocumentPermissionResponse>> list(
            @RequestParam String targetType, @RequestParam String targetCode) {
        return ResponseEntity.ok(service.listPermissions(targetType, targetCode));
    }

    @GetMapping("/effective")
    public ResponseEntity<Set<String>> effective(
            @RequestParam Long userId, @RequestParam String documentCode) {
        return ResponseEntity.ok(service.getEffectivePermissions(userId, documentCode));
    }
}
