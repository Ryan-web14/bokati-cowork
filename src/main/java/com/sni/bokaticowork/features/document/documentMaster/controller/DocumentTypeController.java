package com.sni.bokaticowork.features.document.documentMaster.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTypeRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentTypeResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentUploadConfigResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/document-types")
public class DocumentTypeController {

    private final DocumentTypeService service;

    @PostMapping
    @Audited(module = "DOCUMENT", action = "CREATE_TYPE", ressource = "document_type")
    @Idempotent(operation = "DOCUMENT_TYPE_CREATE")
    public ResponseEntity<DocumentTypeResponse> create(@Valid @RequestBody DocumentTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PatchMapping("/{code}")
    @Audited(module = "DOCUMENT", action = "UPDATE_TYPE", ressource = "document_type")
    @Idempotent(operation = "DOCUMENT_TYPE_UPDATE")
    public ResponseEntity<DocumentTypeResponse> update(@PathVariable String code, @Valid @RequestBody DocumentTypeRequest request) {
        return ResponseEntity.ok(service.update(code, request));
    }

    @GetMapping("/{code}")
    public ResponseEntity<DocumentTypeResponse> get(@PathVariable String code) {
        return ResponseEntity.ok(service.get(code));
    }

    @GetMapping
    public ResponseEntity<List<DocumentTypeResponse>> list(@RequestParam(required = false) DocumentOwnerType ownerType,
                                                           @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(service.list(ownerType, active));
    }

    @PatchMapping("/{code}/activate")
    @Audited(module = "DOCUMENT", action = "ACTIVATE_TYPE", ressource = "document_type")
    @Idempotent(operation = "DOCUMENT_TYPE_ACTIVATE", required = false)
    public ResponseEntity<DocumentTypeResponse> activate(@PathVariable String code) {
        return ResponseEntity.ok(service.activate(code));
    }

    @PatchMapping("/{code}/deactivate")
    @Audited(module = "DOCUMENT", action = "DEACTIVATE_TYPE", ressource = "document_type")
    @Idempotent(operation = "DOCUMENT_TYPE_DEACTIVATE", required = false)
    public ResponseEntity<DocumentTypeResponse> deactivate(@PathVariable String code) {
        return ResponseEntity.ok(service.deactivate(code));
    }

    @DeleteMapping("/{code}")
    @Audited(module = "DOCUMENT", action = "DELETE_TYPE", ressource = "document_type")
    @Idempotent(operation = "DOCUMENT_TYPE_DELETE", required = false)
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{code}/upload-config")
    public ResponseEntity<DocumentUploadConfigResponse> uploadConfig(@PathVariable String code) {
        return ResponseEntity.ok(service.getUploadConfig(code));
    }
}
