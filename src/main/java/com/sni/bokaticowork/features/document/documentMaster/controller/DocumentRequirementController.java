package com.sni.bokaticowork.features.document.documentMaster.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentRequirementRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentRequirementResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentRequirementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/document-requirements")
public class DocumentRequirementController {

    private final DocumentRequirementService service;

    @PostMapping
    @Audited(module = "DOCUMENT", action = "CREATE_REQUIREMENT", ressource = "document_requirement")
    @Idempotent(operation = "DOCUMENT_REQUIREMENT_CREATE")
    public ResponseEntity<DocumentRequirementResponse> create(@Valid @RequestBody DocumentRequirementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PatchMapping("/{id}")
    @Audited(module = "DOCUMENT", action = "UPDATE_REQUIREMENT", ressource = "document_requirement")
    @Idempotent(operation = "DOCUMENT_REQUIREMENT_UPDATE")
    public ResponseEntity<DocumentRequirementResponse> update(@PathVariable Long id, @Valid @RequestBody DocumentRequirementRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentRequirementResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    public ResponseEntity<List<DocumentRequirementResponse>> list(@RequestParam(required = false) DocumentOwnerType ownerType,
                                                                  @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(service.list(ownerType, active));
    }

    @PatchMapping("/{id}/activate")
    @Audited(module = "DOCUMENT", action = "ACTIVATE_REQUIREMENT", ressource = "document_requirement")
    @Idempotent(operation = "DOCUMENT_REQUIREMENT_ACTIVATE", required = false)
    public ResponseEntity<DocumentRequirementResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(service.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @Audited(module = "DOCUMENT", action = "DEACTIVATE_REQUIREMENT", ressource = "document_requirement")
    @Idempotent(operation = "DOCUMENT_REQUIREMENT_DEACTIVATE", required = false)
    public ResponseEntity<DocumentRequirementResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(service.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @Audited(module = "DOCUMENT", action = "DELETE_REQUIREMENT", ressource = "document_requirement")
    @Idempotent(operation = "DOCUMENT_REQUIREMENT_DELETE", required = false)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
