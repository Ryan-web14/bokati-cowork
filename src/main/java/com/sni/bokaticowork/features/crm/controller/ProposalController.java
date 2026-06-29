package com.sni.bokaticowork.features.crm.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.crm.dto.ProposalDtos.*;
import com.sni.bokaticowork.features.crm.enums.ProposalStatus;
import com.sni.bokaticowork.features.crm.service.interfaces.ProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/crm/proposals")
public class ProposalController {

    private final ProposalService service;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<ProposalResponse> create(@Valid @RequestBody CreateProposalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('CRM:READ','CRM_READ')")
    public ResponseEntity<PaginatedResponse<ProposalResponse>> list(
            @RequestParam(required = false) ProposalStatus status,
            @RequestParam(required = false) Long opportunityId,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.list(status, opportunityId, searchText, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CRM:READ','CRM_READ')")
    public ResponseEntity<ProposalResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/number/{proposalNumber}")
    @PreAuthorize("hasAnyAuthority('CRM:READ','CRM_READ')")
    public ResponseEntity<ProposalResponse> getByNumber(@PathVariable String proposalNumber) {
        return ResponseEntity.ok(service.getByNumber(proposalNumber));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<ProposalResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateProposalRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<ProposalResponse> addLine(@PathVariable Long id,
                                                    @Valid @RequestBody AddProposalLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addLine(id, request));
    }

    @PutMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<ProposalResponse> updateLine(@PathVariable Long id,
                                                       @PathVariable Long lineId,
                                                       @Valid @RequestBody UpdateProposalLineRequest request) {
        return ResponseEntity.ok(service.updateLine(id, lineId, request));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<ProposalResponse> removeLine(@PathVariable Long id,
                                                       @PathVariable Long lineId) {
        return ResponseEntity.ok(service.removeLine(id, lineId));
    }

    @PatchMapping("/{id}/send")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<ProposalResponse> send(@PathVariable Long id) {
        return ResponseEntity.ok(service.send(id));
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<ProposalResponse> accept(@PathVariable Long id) {
        return ResponseEntity.ok(service.accept(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('CRM:WRITE','CRM_WRITE')")
    public ResponseEntity<ProposalResponse> reject(@PathVariable Long id,
                                                   @RequestBody RejectProposalRequest request) {
        return ResponseEntity.ok(service.reject(id, request));
    }
}
