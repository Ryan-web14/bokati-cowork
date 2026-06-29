package com.sni.bokaticowork.features.contract.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.contract.dto.request.ContractDraftSectionRequest;
import com.sni.bokaticowork.features.contract.dto.request.CreateContractDraftRequest;
import com.sni.bokaticowork.features.contract.dto.request.PreviewContractDraftRequest;
import com.sni.bokaticowork.features.contract.dto.request.UpdateContractDraftRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractDraftResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractDraftSectionResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractPreviewResponse;
import com.sni.bokaticowork.features.contract.enums.ContractDraftStatus;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractDraftService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/contract-drafts")
public class ContractDraftController {

    private final ContractDraftService draftService;

    @Audited(module = "CONTRACT_DRAFT", action = "CREATE", ressource = "contract_draft")
    @Idempotent(operation = "CONTRACT_DRAFT_CREATE", required = false)
    @PostMapping
    public ResponseEntity<ContractDraftResponse> create(@Valid @RequestBody CreateContractDraftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(draftService.create(request));
    }

    @Audited(module = "CONTRACT_DRAFT", action = "CREATE_FROM_TEMPLATE", ressource = "contract_draft")
    @Idempotent(operation = "CONTRACT_DRAFT_CREATE_FROM_TEMPLATE", required = false)
    @PostMapping("/from-template/{templateCode}")
    public ResponseEntity<ContractDraftResponse> createFromTemplate(@PathVariable String templateCode,
                                                                    @Valid @RequestBody CreateContractDraftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(draftService.createFromTemplate(templateCode, request));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ContractDraftResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(draftService.getByCode(code));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ContractDraftResponse>> list(
            @RequestParam(required = false) ContractDraftStatus status,
            @RequestParam(required = false) DocumentOwnerType ownerType,
            @RequestParam(required = false) String ownerCode,
            @PageableDefault(size = 20) Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(draftService.listByStatus(status, pageable));
        }
        if (ownerType != null && ownerCode != null) {
            return ResponseEntity.ok(draftService.listByOwner(ownerType, ownerCode, pageable));
        }
        return ResponseEntity.ok(draftService.list(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<ContractDraftResponse>> search(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(draftService.search(query, pageable));
    }

    @Audited(module = "CONTRACT_DRAFT", action = "UPDATE", ressource = "contract_draft")
    @Idempotent(operation = "CONTRACT_DRAFT_UPDATE", required = false)
    @PutMapping("/{code}")
    public ResponseEntity<ContractDraftResponse> update(@PathVariable String code,
                                                        @Valid @RequestBody UpdateContractDraftRequest request) {
        return ResponseEntity.ok(draftService.update(code, request));
    }

    @Audited(module = "CONTRACT_DRAFT", action = "ADD_SECTION", ressource = "contract_draft")
    @PostMapping("/{code}/sections")
    public ResponseEntity<ContractDraftSectionResponse> addSection(@PathVariable String code,
                                                                   @Valid @RequestBody ContractDraftSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(draftService.addSection(code, request));
    }

    @Audited(module = "CONTRACT_DRAFT", action = "UPDATE_SECTION", ressource = "contract_draft")
    @PutMapping("/{code}/sections/{sectionId}")
    public ResponseEntity<ContractDraftSectionResponse> updateSection(@PathVariable String code,
                                                                     @PathVariable Long sectionId,
                                                                     @Valid @RequestBody ContractDraftSectionRequest request) {
        return ResponseEntity.ok(draftService.updateSection(code, sectionId, request));
    }

    @Audited(module = "CONTRACT_DRAFT", action = "REMOVE_SECTION", ressource = "contract_draft")
    @DeleteMapping("/{code}/sections/{sectionId}")
    public ResponseEntity<Void> removeSection(@PathVariable String code,
                                              @PathVariable Long sectionId) {
        draftService.removeSection(code, sectionId);
        return ResponseEntity.noContent().build();
    }

    @Audited(module = "CONTRACT_DRAFT", action = "UPDATE_STATUS", ressource = "contract_draft")
    @PatchMapping("/{code}/status")
    public ResponseEntity<ContractDraftResponse> updateStatus(@PathVariable String code,
                                                              @RequestParam ContractDraftStatus status) {
        return ResponseEntity.ok(draftService.updateStatus(code, status));
    }

    @PostMapping("/{code}/preview")
    public ResponseEntity<ContractPreviewResponse> preview(@PathVariable String code,
                                                           @RequestBody(required = false) PreviewContractDraftRequest request) {
        Map<String, String> vars = request != null ? request.getVariables() : null;
        return ResponseEntity.ok(draftService.preview(code, vars));
    }

    @Audited(module = "CONTRACT_DRAFT", action = "GENERATE_PDF", ressource = "contract_draft")
    @Idempotent(operation = "CONTRACT_DRAFT_GENERATE_PDF", required = false)
    @PostMapping("/{code}/generate")
    public ResponseEntity<DocumentResponse> generatePdf(@PathVariable String code,
                                                        @RequestParam(required = false) Long uploadedBy,
                                                        @RequestBody(required = false) PreviewContractDraftRequest request) {
        Map<String, String> vars = request != null ? request.getVariables() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(draftService.generatePdf(code, vars, uploadedBy));
    }

    @Audited(module = "CONTRACT_DRAFT", action = "DELETE", ressource = "contract_draft")
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        draftService.delete(code);
        return ResponseEntity.noContent().build();
    }
}
