package com.sni.bokaticowork.features.document.documentMaster.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.CreateShareLinkRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.ShareLinkResponse;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1)
public class DocumentShareController {

    private final DocumentShareService service;

    @PostMapping("/documents/{documentCode}/share")
    @Audited(module = "DOCUMENT", action = "CREATE_SHARE_LINK", ressource = "document")
    public ResponseEntity<ShareLinkResponse> shareDocument(
            @PathVariable String documentCode,
            @Valid @RequestBody CreateShareLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.shareDocument(documentCode, request));
    }

    @PostMapping("/document-folders/{folderCode}/share")
    @Audited(module = "DOCUMENT", action = "CREATE_SHARE_LINK", ressource = "document_folder")
    public ResponseEntity<ShareLinkResponse> shareFolder(
            @PathVariable String folderCode,
            @Valid @RequestBody CreateShareLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.shareFolder(folderCode, request));
    }

    @GetMapping("/shares/{token}")
    public ResponseEntity<ShareLinkResponse> access(@PathVariable String token) {
        ShareLinkResponse response = service.access(token);
        service.recordAccess(token);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/shares/{token}")
    @Audited(module = "DOCUMENT", action = "REVOKE_SHARE_LINK", ressource = "document")
    public ResponseEntity<Void> revoke(@PathVariable String token) {
        service.revoke(token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/documents/{documentCode}/shares")
    public ResponseEntity<List<ShareLinkResponse>> listByDocument(@PathVariable String documentCode) {
        return ResponseEntity.ok(service.listByDocument(documentCode));
    }
}
