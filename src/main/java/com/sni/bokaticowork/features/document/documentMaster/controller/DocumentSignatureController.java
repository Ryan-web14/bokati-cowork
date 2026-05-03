package com.sni.bokaticowork.features.document.documentMaster.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.CreateDocumentSignatureRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.SignDocumentRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentSignatureResponse;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentSignatureService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/documents/{documentCode}/signatures")
public class DocumentSignatureController {

    private final DocumentSignatureService signatureService;

    @PostMapping
    public ResponseEntity<DocumentSignatureResponse> requestSignature(
            @PathVariable String documentCode,
            @Valid @RequestBody CreateDocumentSignatureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(signatureService.requestSignature(documentCode, request));
    }

    @GetMapping
    public ResponseEntity<List<DocumentSignatureResponse>> list(@PathVariable String documentCode) {
        return ResponseEntity.ok(signatureService.list(documentCode));
    }

    @PatchMapping("/{signatureId}/sign")
    public ResponseEntity<DocumentSignatureResponse> sign(
            @PathVariable String documentCode,
            @PathVariable Long signatureId,
            @Valid @RequestBody SignDocumentRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(signatureService.sign(
                documentCode,
                signatureId,
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        ));
    }
}
