package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.request.DealRequest;
import com.sni.bokaticowork.features.billing.dto.request.DealStageRequest;
import com.sni.bokaticowork.features.billing.dto.response.DealDashboardResponse;
import com.sni.bokaticowork.features.billing.dto.response.DealResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDealStage;
import com.sni.bokaticowork.features.billing.service.implementation.DealService;
import com.sni.bokaticowork.features.crm.repository.OpportunityRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Affaires (lot B11). */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/billing/deals")
public class DealController {

    private final DealService deals;
    private final OpportunityRepository opportunities;

    @GetMapping
    public ResponseEntity<PaginatedResponse<DealResponse>> list(
            @RequestParam(required = false) BillingDealStage stage,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new PaginatedResponse<>(deals.list(stage, owner, customerCode, search, from, to, page, size)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) BillingDealStage stage,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] body = ("\uFEFF" + deals.exportCsv(stage, owner, customerCode, search, from, to)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=affaires.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)).body(body);
    }

    @Audited(module = "BILLING", action = "DEAL_CREATE", ressource = "billing_deal")
    @PostMapping
    public ResponseEntity<DealResponse> create(@Valid @RequestBody DealRequest request) {
        return ResponseEntity.ok(deals.create(request));
    }

    /** Affaire creee depuis une opportunite du CRM. */
    @Audited(module = "BILLING", action = "DEAL_FROM_OPPORTUNITY", ressource = "billing_deal")
    @PostMapping("/from-opportunity/{opportunityNumber}")
    public ResponseEntity<DealResponse> fromOpportunity(@PathVariable String opportunityNumber) {
        return ResponseEntity.ok(deals.fromOpportunity(opportunities.findByOpportunityNumber(opportunityNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunité introuvable : " + opportunityNumber))));
    }

    @GetMapping("/{code}")
    public ResponseEntity<DealDashboardResponse> dashboard(@PathVariable String code) {
        return ResponseEntity.ok(deals.dashboard(code));
    }

    @Audited(module = "BILLING", action = "DEAL_UPDATE", ressource = "billing_deal")
    @PutMapping("/{code}")
    public ResponseEntity<DealResponse> update(@PathVariable String code, @Valid @RequestBody DealRequest request) {
        return ResponseEntity.ok(deals.update(code, request));
    }

    @Audited(module = "BILLING", action = "DEAL_STAGE", ressource = "billing_deal")
    @PatchMapping("/{code}/stage")
    public ResponseEntity<DealResponse> stage(@PathVariable String code, @Valid @RequestBody DealStageRequest request) {
        return ResponseEntity.ok(deals.stage(code, request));
    }

    /** Rattache une piece a l'affaire. */
    @PatchMapping("/{code}/documents/{documentNumber}")
    public ResponseEntity<DealResponse> attach(@PathVariable String code, @PathVariable String documentNumber) {
        return ResponseEntity.ok(deals.attach(documentNumber, code));
    }

    /** Detache une piece de son affaire. */
    @DeleteMapping("/{code}/documents/{documentNumber}")
    public ResponseEntity<Void> detach(@PathVariable String code, @PathVariable String documentNumber) {
        deals.attach(documentNumber, null);
        return ResponseEntity.noContent().build();
    }
}
