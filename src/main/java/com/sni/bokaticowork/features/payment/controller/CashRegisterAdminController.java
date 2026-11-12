package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.request.CashDeviceDecisionRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashReferenceValueRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashRegisterAssignmentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashRegisterStatusRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateResponsibilityTransferRequest;
import com.sni.bokaticowork.features.payment.dto.request.EmergencyAssignmentRequest;
import com.sni.bokaticowork.features.payment.dto.request.UpdateCashRegisterRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashFormDefaultsResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashPendingItemResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashReferenceValueResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterAssignmentResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterDetailResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterHistoryResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashResponsibilityTransferResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Referentiel des caisses (lot C1 du plan finance). */
@RestController
@RequestMapping(ApiPath.V1 + "/cash-registers")
@RequiredArgsConstructor
public class CashRegisterAdminController {

    private final CashRegisterAdminService service;

    @GetMapping("/form-defaults")
    public ResponseEntity<CashFormDefaultsResponse> formDefaults(@RequestParam String context,
                                                                 @RequestParam Map<String, String> parameters) {
        return ResponseEntity.ok(service.formDefaults(context, parameters));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<CashRegisterDetailResponse>> myRegisters() {
        return ResponseEntity.ok(service.myRegisters());
    }

    @GetMapping("/reference-lists")
    public ResponseEntity<Map<String, List<CashReferenceValueResponse>>> referenceLists(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(service.referenceLists(includeInactive));
    }

    @GetMapping("/reference-lists/{listCode}")
    public ResponseEntity<List<CashReferenceValueResponse>> referenceList(@PathVariable String listCode,
                                                                          @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(service.referenceList(listCode, includeInactive));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_REPLACE_REFERENCE_LIST", ressource = "cash_register")
    @PutMapping("/reference-lists/{listCode}")
    public ResponseEntity<List<CashReferenceValueResponse>> replaceReferenceList(@PathVariable String listCode,
                                                                                 @Valid @RequestBody List<CashReferenceValueRequest> values) {
        return ResponseEntity.ok(service.replaceReferenceList(listCode, values));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_SIGN_TRANSFER", ressource = "cash_register")
    @PatchMapping("/responsibility-transfers/{transferNumber}/sign")
    public ResponseEntity<CashResponsibilityTransferResponse> signTransfer(@PathVariable String transferNumber) {
        return ResponseEntity.ok(service.signResponsibilityTransfer(transferNumber));
    }

    @GetMapping("/{registerCode}")
    public ResponseEntity<CashRegisterDetailResponse> get(@PathVariable String registerCode) {
        return ResponseEntity.ok(service.get(registerCode));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_UPDATE", ressource = "cash_register")
    @PatchMapping("/{registerCode}")
    public ResponseEntity<CashRegisterDetailResponse> update(@PathVariable String registerCode,
                                                             @Valid @RequestBody UpdateCashRegisterRequest request) {
        return ResponseEntity.ok(service.update(registerCode, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_SUSPEND", ressource = "cash_register")
    @PatchMapping("/{registerCode}/suspend")
    public ResponseEntity<CashRegisterDetailResponse> suspend(@PathVariable String registerCode,
                                                              @RequestBody(required = false) CashRegisterStatusRequest request) {
        return ResponseEntity.ok(service.suspend(registerCode, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_REACTIVATE", ressource = "cash_register")
    @PatchMapping("/{registerCode}/reactivate")
    public ResponseEntity<CashRegisterDetailResponse> reactivate(@PathVariable String registerCode) {
        return ResponseEntity.ok(service.reactivate(registerCode));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_CLOSE", ressource = "cash_register")
    @PatchMapping("/{registerCode}/close")
    public ResponseEntity<CashRegisterDetailResponse> close(@PathVariable String registerCode,
                                                            @RequestBody(required = false) CashRegisterStatusRequest request) {
        return ResponseEntity.ok(service.close(registerCode, request));
    }

    @GetMapping("/{registerCode}/history")
    public ResponseEntity<List<CashRegisterHistoryResponse>> history(@PathVariable String registerCode) {
        return ResponseEntity.ok(service.history(registerCode));
    }

    @GetMapping("/{registerCode}/assignments")
    public ResponseEntity<List<CashRegisterAssignmentResponse>> assignments(@PathVariable String registerCode,
                                                                            @RequestParam(defaultValue = "false") boolean includeEnded) {
        return ResponseEntity.ok(service.assignments(registerCode, includeEnded));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_REPLACE_ASSIGNMENTS", ressource = "cash_register")
    @PutMapping("/{registerCode}/assignments")
    public ResponseEntity<List<CashRegisterAssignmentResponse>> replaceAssignments(@PathVariable String registerCode,
                                                                                   @Valid @RequestBody List<CashRegisterAssignmentRequest> assignments) {
        return ResponseEntity.ok(service.replaceAssignments(registerCode, assignments));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_EMERGENCY_ASSIGNMENT", ressource = "cash_register")
    @PostMapping("/{registerCode}/emergency-assignments")
    public ResponseEntity<CashRegisterAssignmentResponse> emergencyAssignment(@PathVariable String registerCode,
                                                                              @Valid @RequestBody EmergencyAssignmentRequest request) {
        return ResponseEntity.ok(service.emergencyAssignment(registerCode, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_DECIDE_DEVICE", ressource = "cash_register")
    @PatchMapping("/{registerCode}/devices/{deviceCode}")
    public ResponseEntity<CashRegisterDetailResponse> decideDevice(@PathVariable String registerCode,
                                                                   @PathVariable String deviceCode,
                                                                   @Valid @RequestBody CashDeviceDecisionRequest request) {
        return ResponseEntity.ok(service.decideDevice(registerCode, deviceCode, request));
    }

    @GetMapping("/{registerCode}/responsibility-transfers/pending-items")
    public ResponseEntity<List<CashPendingItemResponse>> pendingItems(@PathVariable String registerCode) {
        return ResponseEntity.ok(service.pendingItems(registerCode));
    }

    @GetMapping("/{registerCode}/responsibility-transfers")
    public ResponseEntity<List<CashResponsibilityTransferResponse>> transfers(@PathVariable String registerCode) {
        return ResponseEntity.ok(service.responsibilityTransfers(registerCode));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_CREATE_TRANSFER", ressource = "cash_register")
    @PostMapping("/{registerCode}/responsibility-transfers")
    public ResponseEntity<CashResponsibilityTransferResponse> createTransfer(@PathVariable String registerCode,
                                                                             @Valid @RequestBody CreateResponsibilityTransferRequest request) {
        return ResponseEntity.ok(service.createResponsibilityTransfer(registerCode, request));
    }
}
