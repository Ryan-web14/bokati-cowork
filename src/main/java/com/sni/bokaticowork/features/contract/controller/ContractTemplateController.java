package com.sni.bokaticowork.features.contract.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.contract.dto.request.CreateContractTemplateRequest;
import com.sni.bokaticowork.features.contract.dto.request.UpdateContractTemplateRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractPreviewResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractTemplateResponse;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/contract-templates")
public class ContractTemplateController {

    private final ContractTemplateService templateService;

    @Audited(module = "CONTRACT_TEMPLATE", action = "CREATE", ressource = "contract_template")
    @Idempotent(operation = "CONTRACT_TEMPLATE_CREATE", required = false)
    @PostMapping
    public ResponseEntity<ContractTemplateResponse> create(@Valid @RequestBody CreateContractTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(request));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ContractTemplateResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(templateService.getByCode(code));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ContractTemplateResponse>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(templateService.list(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<ContractTemplateResponse>> search(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(templateService.search(query, pageable));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<PaginatedResponse<ContractTemplateResponse>> listByCategory(
            @PathVariable String category,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(templateService.listByCategory(category, pageable));
    }

    @Audited(module = "CONTRACT_TEMPLATE", action = "UPDATE", ressource = "contract_template")
    @Idempotent(operation = "CONTRACT_TEMPLATE_UPDATE", required = false)
    @PutMapping("/{code}")
    public ResponseEntity<ContractTemplateResponse> update(@PathVariable String code,
                                                           @Valid @RequestBody UpdateContractTemplateRequest request) {
        return ResponseEntity.ok(templateService.update(code, request));
    }

    @PostMapping("/{code}/preview")
    public ResponseEntity<ContractPreviewResponse> preview(@PathVariable String code,
                                                           @RequestBody(required = false) Map<String, String> sampleVariables) {
        return ResponseEntity.ok(templateService.previewTemplate(code, sampleVariables));
    }

    @Audited(module = "CONTRACT_TEMPLATE", action = "DUPLICATE", ressource = "contract_template")
    @PostMapping("/{code}/duplicate")
    public ResponseEntity<ContractTemplateResponse> duplicate(@PathVariable String code) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.duplicate(code));
    }

    @Audited(module = "CONTRACT_TEMPLATE", action = "DELETE", ressource = "contract_template")
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        templateService.delete(code);
        return ResponseEntity.noContent().build();
    }
}
