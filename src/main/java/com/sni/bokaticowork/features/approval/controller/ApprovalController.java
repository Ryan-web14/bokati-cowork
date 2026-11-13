package com.sni.bokaticowork.features.approval.controller;

import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.features.approval.enums.ApprovalStatus;
import com.sni.bokaticowork.features.approval.service.ApprovalEngine;
import com.sni.bokaticowork.features.approval.service.ApprovalHierarchy;
import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.admin.user.dto.request.UpdateStaffProfileRequest;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Centres d'approbation (lot B15). Routes communes (hierarchie, circuits, delegations, decisions)
 * et files par centre ({@code center} = BILLING, CASH...).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/approvals")
public class ApprovalController {

    private final ApprovalEngine engine;
    private final ApprovalHierarchy hierarchy;
    private final StaffProfileService staffProfileService;

    @GetMapping("/hierarchy")
    public ResponseEntity<List<ApprovalDtos.HierarchyNode>> hierarchy() {
        return ResponseEntity.ok(hierarchy.nodes());
    }

    @Audited(module = "APPROVAL", action = "HIERARCHY_UPDATE", ressource = "user")
    @PutMapping("/hierarchy")
    @PreAuthorize("hasAuthority('APPROVAL:MANAGE')")
    public ResponseEntity<List<ApprovalDtos.HierarchyNode>> updateHierarchy(@Valid @RequestBody ApprovalDtos.HierarchyUpdate update) {
        staffProfileService.update(update.userCode(), new UpdateStaffProfileRequest(null,
                update.managerUserCode() == null ? "" : update.managerUserCode(), null, null));
        return ResponseEntity.ok(hierarchy.nodes());
    }

    @GetMapping("/circuits")
    public ResponseEntity<List<ApprovalDtos.CircuitView>> circuits(@RequestParam(required = false) String center) {
        return ResponseEntity.ok(engine.circuits(center));
    }

    @Audited(module = "APPROVAL", action = "CIRCUIT_SAVE", ressource = "approval_circuit")
    @PostMapping("/circuits")
    @PreAuthorize("hasAuthority('APPROVAL:MANAGE')")
    public ResponseEntity<ApprovalDtos.CircuitView> saveCircuit(@Valid @RequestBody ApprovalDtos.CircuitInput input) {
        return ResponseEntity.ok(engine.saveCircuit(input));
    }

    @GetMapping("/delegations")
    public ResponseEntity<List<ApprovalDtos.DelegationView>> delegations(@RequestParam(required = false) String center) {
        return ResponseEntity.ok(engine.delegations(center));
    }

    @Audited(module = "APPROVAL", action = "DELEGATION_CREATE", ressource = "approval_delegation")
    @PostMapping("/delegations")
    public ResponseEntity<ApprovalDtos.DelegationView> delegate(@Valid @RequestBody ApprovalDtos.DelegationInput input) {
        return ResponseEntity.ok(engine.delegate(input));
    }

    @Audited(module = "APPROVAL", action = "DELEGATION_CANCEL", ressource = "approval_delegation")
    @DeleteMapping("/delegations/{id}")
    public ResponseEntity<Void> cancelDelegation(@PathVariable Long id) {
        engine.cancelDelegation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{center}/mine")
    public ResponseEntity<List<ApprovalDtos.RequestView>> mine(@PathVariable String center) {
        return ResponseEntity.ok(engine.mine(center));
    }

    @GetMapping("/{center}/to-approve")
    public ResponseEntity<List<ApprovalDtos.RequestView>> toApprove(@PathVariable String center) {
        return ResponseEntity.ok(engine.toApprove(center));
    }

    @GetMapping("/{center}/history")
    public ResponseEntity<PaginatedResponse<ApprovalDtos.RequestView>> history(
            @PathVariable String center,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String requester,
            @RequestParam(required = false) String approver,
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new PaginatedResponse<>(engine.history(center, type, requester, approver, status, from, to, page, size)));
    }

    @GetMapping("/requests/{number}")
    public ResponseEntity<ApprovalDtos.RequestView> get(@PathVariable String number) {
        return ResponseEntity.ok(engine.get(number));
    }

    @Audited(module = "APPROVAL", action = "APPROVE", ressource = "approval_request")
    @PatchMapping("/{number}/approve")
    public ResponseEntity<ApprovalDtos.RequestView> approve(@PathVariable String number,
                                                           @Valid @RequestBody(required = false) ApprovalDtos.Decision decision) {
        engine.approve(number, decision);
        return ResponseEntity.ok(engine.get(number));
    }

    @Audited(module = "APPROVAL", action = "REJECT", ressource = "approval_request")
    @PatchMapping("/{number}/reject")
    public ResponseEntity<ApprovalDtos.RequestView> reject(@PathVariable String number,
                                                          @Valid @RequestBody ApprovalDtos.Decision decision) {
        engine.reject(number, decision);
        return ResponseEntity.ok(engine.get(number));
    }

    @Audited(module = "APPROVAL", action = "SEND_BACK", ressource = "approval_request")
    @PatchMapping("/{number}/send-back")
    public ResponseEntity<ApprovalDtos.RequestView> sendBack(@PathVariable String number,
                                                            @Valid @RequestBody ApprovalDtos.Decision decision) {
        engine.sendBack(number, decision);
        return ResponseEntity.ok(engine.get(number));
    }
}
