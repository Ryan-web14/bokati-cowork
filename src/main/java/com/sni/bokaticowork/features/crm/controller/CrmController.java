package com.sni.bokaticowork.features.crm.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.crm.dto.CrmDtos.*;
import com.sni.bokaticowork.features.crm.enums.LeadInterest;
import com.sni.bokaticowork.features.crm.enums.LeadSource;
import com.sni.bokaticowork.features.crm.enums.LeadStage;
import com.sni.bokaticowork.features.crm.enums.OpportunityStage;
import com.sni.bokaticowork.features.crm.service.interfaces.CrmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/crm")
public class CrmController {

    private final CrmService service;

    // ── Leads ─────────────────────────────────────────────────────

    @PostMapping("/leads")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<LeadResponse> create(@Valid @RequestBody CreateLeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/leads")
    @PreAuthorize("hasAnyAuthority('CRM:READ','CRM_READ')")
    public ResponseEntity<PaginatedResponse<LeadResponse>> search(
            @RequestParam(required = false) LeadStage stage,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) LeadSource source,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.search(stage, assignedTo, source, searchText, pageable));
    }

    @GetMapping("/leads/{id}")
    @PreAuthorize("hasAnyAuthority('CRM:READ','CRM_READ')")
    public ResponseEntity<LeadResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/leads/number/{leadNumber}")
    @PreAuthorize("hasAnyAuthority('CRM:READ','CRM_READ')")
    public ResponseEntity<LeadResponse> getByNumber(@PathVariable String leadNumber) {
        return ResponseEntity.ok(service.getByNumber(leadNumber));
    }

    @PutMapping("/leads/{id}")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<LeadResponse> update(@PathVariable Long id,
                                               @RequestBody UpdateLeadRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/leads/{id}/qualify")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<LeadResponse> qualify(@PathVariable Long id) {
        return ResponseEntity.ok(service.qualify(id));
    }

    @PatchMapping("/leads/{id}/stage")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<LeadResponse> stage(@PathVariable Long id,
                                              @RequestBody UpdateLeadStageRequest request) {
        return ResponseEntity.ok(service.updateStage(id, request));
    }

    @PatchMapping("/leads/{id}/convert")
    @PreAuthorize("hasAnyAuthority('CRM:CONVERT','CRM_CONVERT')")
    public ResponseEntity<LeadResponse> convert(@PathVariable Long id,
                                                @RequestBody ConvertLeadRequest request) {
        return ResponseEntity.ok(service.convert(id, request));
    }

    @PostMapping("/leads/{id}/activities")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<LeadResponse> activity(@PathVariable Long id,
                                                 @Valid @RequestBody AddLeadActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addActivity(id, request));
    }

    @PostMapping("/leads/{id}/generate-quote")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<Map<String, String>> generateQuote(@PathVariable Long id,
                                                             @Valid @RequestBody GenerateQuoteRequest request) {
        String quoteNumber = service.generateQuote(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("quoteNumber", quoteNumber));
    }

    // ── Opportunités ──────────────────────────────────────────────

    @PostMapping("/leads/{id}/opportunities")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<OpportunityResponse> createOpportunity(
            @PathVariable Long id,
            @Valid @RequestBody CreateOpportunityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createOpportunity(id, request));
    }

    @GetMapping("/opportunities")
    @PreAuthorize("hasAnyAuthority('CRM:READ','CRM_READ')")
    public ResponseEntity<PaginatedResponse<OpportunityResponse>> opportunities(
            @RequestParam(required = false) OpportunityStage stage,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.listOpportunities(stage, pageable));
    }

    @PatchMapping("/opportunities/{id}/stage")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<OpportunityResponse> opportunityStage(
            @PathVariable Long id,
            @RequestBody UpdateOpportunityStageRequest request) {
        return ResponseEntity.ok(service.updateOpportunityStage(id, request));
    }

    // ── Kanban Board ──────────────────────────────────────────────

    @GetMapping("/board")
    @PreAuthorize("hasAnyAuthority('CRM:READ','CRM_READ')")
    public ResponseEntity<KanbanBoardResponse> board(
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) LeadSource source,
            @RequestParam(required = false) LeadInterest interest,
            @RequestParam(required = false) String searchText) {
        return ResponseEntity.ok(service.getBoard(assignedTo, source, interest, searchText));
    }

    // ── Vue pipeline + métriques ──────────────────────────────────

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

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyAuthority('CRM:READ','CRM_READ')")
    public ResponseEntity<CrmAnalyticsResponse> analytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(service.analytics(from, to));
    }
}
