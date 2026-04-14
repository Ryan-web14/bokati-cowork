package com.sni.bokaticowork.features.contract.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.contract.dto.request.AttachContractDocumentRequest;
import com.sni.bokaticowork.features.contract.dto.request.CreateContractRequest;
import com.sni.bokaticowork.features.contract.dto.request.GenerateContractRequest;
import com.sni.bokaticowork.features.contract.dto.request.GenerateStoredContractDraftRequest;
import com.sni.bokaticowork.features.contract.dto.request.UpdateContractRequest;
import com.sni.bokaticowork.features.contract.dto.request.UpdateContractStatusRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractPreviewResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractTemplateResponse;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractGenerationService;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractService;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/contracts")
public class ContractController {

    private final ContractGenerationService generationService;
    private final ContractService contractService;

    @GetMapping("/templates")
    public ResponseEntity<List<ContractTemplateResponse>> listTemplates() {
        return ResponseEntity.ok(generationService.listTemplates());
    }

    @Audited(module = "CONTRACT", action = "CREATE", ressource = "contract")
    @Idempotent(operation = "CONTRACT_CREATE", required = false)
    @PostMapping
    public ResponseEntity<ContractResponse> create(@Valid @RequestBody CreateContractRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.create(request));
    }

    @GetMapping("/{contractCode}")
    public ResponseEntity<ContractResponse> getByCode(@PathVariable String contractCode) {
        return ResponseEntity.ok(contractService.getByCode(contractCode));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ContractResponse>> list(@RequestParam(required = false) DocumentOwnerType ownerType,
                                                                   @RequestParam(required = false) String ownerCode,
                                                                   @RequestParam(required = false) String status,
                                                                   @RequestParam(required = false) String businessCode,
                                                                   @RequestParam(required = false) String templateCode,
                                                                   Pageable pageable) {
        ContractStatus parsedStatus = parseStatus(status);
        return ResponseEntity.ok(contractService.list(ownerType, ownerCode, parsedStatus, businessCode, templateCode, pageable));
    }

    @Audited(module = "CONTRACT", action = "UPDATE", ressource = "contract")
    @Idempotent(operation = "CONTRACT_UPDATE", required = false)
    @PutMapping("/{contractCode}")
    public ResponseEntity<ContractResponse> update(@PathVariable String contractCode,
                                                   @Valid @RequestBody UpdateContractRequest request) {
        return ResponseEntity.ok(contractService.update(contractCode, request));
    }

    @Audited(module = "CONTRACT", action = "UPDATE_STATUS", ressource = "contract")
    @Idempotent(operation = "CONTRACT_STATUS_UPDATE", required = false)
    @PatchMapping("/{contractCode}/status")
    public ResponseEntity<ContractResponse> updateStatus(@PathVariable String contractCode,
                                                         @Valid @RequestBody UpdateContractStatusRequest request) {
        return ResponseEntity.ok(contractService.updateStatus(contractCode, request));
    }

    @Audited(module = "CONTRACT", action = "ACTIVATE", ressource = "contract")
    @Idempotent(operation = "CONTRACT_ACTIVATE", required = false)
    @PatchMapping("/{contractCode}/activate")
    public ResponseEntity<ContractResponse> activate(@PathVariable String contractCode) {
        return ResponseEntity.ok(contractService.activate(contractCode));
    }

    @Audited(module = "CONTRACT", action = "SUSPEND", ressource = "contract")
    @Idempotent(operation = "CONTRACT_SUSPEND", required = false)
    @PatchMapping("/{contractCode}/suspend")
    public ResponseEntity<ContractResponse> suspend(@PathVariable String contractCode,
                                                    @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(contractService.suspend(contractCode, reason));
    }

    @Audited(module = "CONTRACT", action = "TERMINATE", ressource = "contract")
    @Idempotent(operation = "CONTRACT_TERMINATE", required = false)
    @PatchMapping("/{contractCode}/terminate")
    public ResponseEntity<ContractResponse> terminate(@PathVariable String contractCode,
                                                      @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(contractService.terminate(contractCode, reason));
    }

    @Audited(module = "CONTRACT", action = "CANCEL", ressource = "contract")
    @Idempotent(operation = "CONTRACT_CANCEL", required = false)
    @PatchMapping("/{contractCode}/cancel")
    public ResponseEntity<ContractResponse> cancel(@PathVariable String contractCode,
                                                   @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(contractService.cancel(contractCode, reason));
    }

    @PostMapping("/preview")
    @Audited(module = "CONTRACT", action = "PREVIEW", ressource = "contract")
    public ResponseEntity<ContractPreviewResponse> preview(@Valid @RequestBody GenerateContractRequest request) {
        return ResponseEntity.ok(generationService.preview(request));
    }

    @PostMapping("/generate")
    @Audited(module = "CONTRACT", action = "GENERATE_DRAFT", ressource = "contract")
    @Idempotent(operation = "CONTRACT_GENERATE_DRAFT", required = false)
    public ResponseEntity<DocumentResponse> generate(@Valid @RequestBody GenerateContractRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(generationService.generatePdf(request));
    }

    @Audited(module = "CONTRACT", action = "GENERATE_DRAFT_FROM_RECORD", ressource = "contract")
    @Idempotent(operation = "CONTRACT_GENERATE_DRAFT_FROM_RECORD", required = false)
    @PostMapping("/{contractCode}/generate-draft")
    public ResponseEntity<ContractResponse> generateFromContract(@PathVariable String contractCode,
                                                                 @Valid @RequestBody GenerateStoredContractDraftRequest request) {
        ContractResponse contract = contractService.getByCode(contractCode);
        GenerateContractRequest generateRequest = new GenerateContractRequest();
        generateRequest.setTemplateCode(contract.getTemplateCode());
        generateRequest.setOwnerType(contract.getOwnerType());
        generateRequest.setOwnerCode(contract.getOwnerCode());
        generateRequest.setBusinessCode(contract.getBusinessCode());
        generateRequest.setTitle(contract.getTitle());
        generateRequest.setDescription(contract.getDescription());
        generateRequest.setUploadedBy(request.getUploadedBy());
        generateRequest.setEffectiveDate(contract.getEffectiveDate());
        generateRequest.setStartDate(contract.getStartDate());
        generateRequest.setEndDate(contract.getEndDate());
        DocumentResponse document = generationService.generatePdf(generateRequest);
        return ResponseEntity.ok(contractService.markGenerated(contractCode, document.getCode()));
    }

    @Audited(module = "CONTRACT", action = "MARK_SIGNED", ressource = "contract")
    @Idempotent(operation = "CONTRACT_MARK_SIGNED", required = false)
    @PatchMapping("/{contractCode}/signed-document")
    public ResponseEntity<ContractResponse> markSigned(@PathVariable String contractCode,
                                                       @Valid @RequestBody AttachContractDocumentRequest request) {
        return ResponseEntity.ok(contractService.markSigned(contractCode, request.getDocumentCode()));
    }

    @Audited(module = "CONTRACT", action = "DELETE", ressource = "contract")
    @DeleteMapping("/{contractCode}")
    public ResponseEntity<Void> delete(@PathVariable String contractCode) {
        contractService.delete(contractCode);
        return ResponseEntity.noContent().build();
    }

    private ContractStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ContractStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Statut de contrat invalide: " + status, ex);
        }
    }
}
