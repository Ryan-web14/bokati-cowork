package com.sni.bokaticowork.features.document.documentMaster.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentReviewDecisionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFileResult;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentVersionResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/documents")
public class DocumentController {

    private final DocumentService service;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Audited(module = "DOCUMENT", action = "UPLOAD", ressource = "document")
    @Idempotent(operation = "DOCUMENT_UPLOAD", requestBodyArgIndex = -1)
    public ResponseEntity<DocumentResponse> upload(
            @Valid @ModelAttribute DocumentUploadMetadataRequest metadata,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(metadata, file));
    }

    @PostMapping(value = "/{code}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Audited(module = "DOCUMENT", action = "REPLACE", ressource = "document")
    @Idempotent(operation = "DOCUMENT_REPLACE", requestBodyArgIndex = -1)
    public ResponseEntity<DocumentResponse> replace(
            @PathVariable String code,
            @RequestParam Long uploadedBy,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(service.replace(code, uploadedBy, file));
    }

    @GetMapping("/{code}")
    public ResponseEntity<DocumentResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.getByCode(code));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<DocumentResponse>> list(
            @RequestParam(required = false) DocumentOwnerType ownerType,
            @RequestParam(required = false) String ownerCode,
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.list(ownerType, ownerCode, pageable));
    }

    @GetMapping("/{code}/versions")
    public ResponseEntity<List<DocumentVersionResponse>> listVersions(@PathVariable String code) {
        return ResponseEntity.ok(service.listVersions(code));
    }

    @GetMapping("/{code}/download")
    public ResponseEntity<byte[]> download(@PathVariable String code) {
        DocumentFileResult result = service.getFileWithMeta(code);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.mimeType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(result.fileName()).build());
        headers.setContentLength(result.content().length);
        return ResponseEntity.ok().headers(headers).body(result.content());
    }

    @GetMapping("/{code}/preview")
    public ResponseEntity<byte[]> preview(@PathVariable String code) {
        DocumentFileResult result = service.getFileWithMeta(code);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.mimeType()));
        headers.setContentDisposition(ContentDisposition.inline().filename(result.fileName()).build());
        headers.setContentLength(result.content().length);
        return ResponseEntity.ok().headers(headers).body(result.content());
    }

    @PostMapping("/{code}/approve")
    @Audited(module = "DOCUMENT", action = "APPROVE", ressource = "document")
    @Idempotent(operation = "DOCUMENT_APPROVE")
    public ResponseEntity<DocumentResponse> approve(@PathVariable String code, @Valid @RequestBody DocumentReviewDecisionRequest request) {
        return ResponseEntity.ok(service.approve(code, request));
    }

    @PostMapping("/{code}/reject")
    @Audited(module = "DOCUMENT", action = "REJECT", ressource = "document")
    @Idempotent(operation = "DOCUMENT_REJECT")
    public ResponseEntity<DocumentResponse> reject(@PathVariable String code, @Valid @RequestBody DocumentReviewDecisionRequest request) {
        return ResponseEntity.ok(service.reject(code, request));
    }
}
