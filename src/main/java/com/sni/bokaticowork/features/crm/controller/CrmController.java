package com.sni.bokaticowork.features.crm.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.crm.dto.CrmDtos.*;
import com.sni.bokaticowork.features.crm.enums.LeadStage;
import com.sni.bokaticowork.features.crm.service.interfaces.CrmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/crm")
public class CrmController {
    private final CrmService service;

    @PostMapping("/leads")
    public ResponseEntity<LeadResponse> create(@Valid @RequestBody CreateLeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/leads")
    @PreAuthorize("hasAnyAuthority('CRM:READ','CRM_READ')")
    public ResponseEntity<PaginatedResponse<LeadResponse>> list(@RequestParam(required = false) LeadStage stage,
                                                                @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.list(stage, pageable));
    }

    @PatchMapping("/leads/{id}/qualify")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<LeadResponse> qualify(@PathVariable Long id) {
        return ResponseEntity.ok(service.qualify(id));
    }

    @PatchMapping("/leads/{id}/stage")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<LeadResponse> stage(@PathVariable Long id, @RequestBody UpdateLeadStageRequest request) {
        return ResponseEntity.ok(service.updateStage(id, request));
    }

    @PatchMapping("/leads/{id}/convert")
    @PreAuthorize("hasAnyAuthority('CRM:CONVERT','CRM_CONVERT')")
    public ResponseEntity<LeadResponse> convert(@PathVariable Long id, @RequestBody ConvertLeadRequest request) {
        return ResponseEntity.ok(service.convert(id, request));
    }

    @PostMapping("/leads/{id}/activities")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<LeadResponse> activity(@PathVariable Long id, @RequestBody AddLeadActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addActivity(id, request));
    }

    @GetMapping("/pipeline")
    @PreAuthorize("hasAnyAuthority('CRM:READ','CRM_READ')")
    public ResponseEntity<PipelineResponse> pipeline() {
        return ResponseEntity.ok(service.pipeline());
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyAuthority('CRM:READ','CRM_READ')")
    public ResponseEntity<CrmMetricsResponse> metrics() {
        return ResponseEntity.ok(service.metrics());
    }
}
