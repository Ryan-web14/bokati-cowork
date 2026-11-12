package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.request.CashAbsenceRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashDelegationRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashSealRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashAbsenceResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashDelegationResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashReplacementSuggestionResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSealResponse;
import com.sni.bokaticowork.features.payment.enums.CashAbsenceStatus;
import com.sni.bokaticowork.features.payment.service.interfaces.CashStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Personnel de caisse : absences, remplacements, scelles, delegations (lot C2). */
@RestController
@RequestMapping(ApiPath.V1 + "/cash-registers")
@RequiredArgsConstructor
public class CashStaffController {

    private final CashStaffService service;

    @PostMapping("/absences")
    public ResponseEntity<CashAbsenceResponse> declare(@Valid @RequestBody CashAbsenceRequest request) {
        return ResponseEntity.ok(service.declareAbsence(request));
    }

    @GetMapping("/absences")
    public ResponseEntity<List<CashAbsenceResponse>> absences(@RequestParam(required = false) String userEmail,
                                                              @RequestParam(required = false) CashAbsenceStatus status) {
        return ResponseEntity.ok(service.absences(userEmail, status));
    }

    @GetMapping("/absences/replacement-suggestions")
    public ResponseEntity<List<CashReplacementSuggestionResponse>> suggestions(
            @RequestParam String registerCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.replacementSuggestions(registerCode, from, to));
    }

    @PatchMapping("/absences/{absenceNumber}/approve")
    public ResponseEntity<CashAbsenceResponse> approve(@PathVariable String absenceNumber) {
        return ResponseEntity.ok(service.approveAbsence(absenceNumber));
    }

    @PatchMapping("/absences/{absenceNumber}/cancel")
    public ResponseEntity<CashAbsenceResponse> cancel(@PathVariable String absenceNumber) {
        return ResponseEntity.ok(service.cancelAbsence(absenceNumber));
    }

    @PatchMapping("/absences/{absenceNumber}/end")
    public ResponseEntity<CashAbsenceResponse> end(@PathVariable String absenceNumber) {
        return ResponseEntity.ok(service.endAbsence(absenceNumber));
    }

    @PostMapping("/{registerCode}/seal")
    public ResponseEntity<CashSealResponse> seal(@PathVariable String registerCode, @Valid @RequestBody CashSealRequest request) {
        return ResponseEntity.ok(service.seal(registerCode, request));
    }

    @PostMapping("/{registerCode}/unseal")
    public ResponseEntity<CashSealResponse> unseal(@PathVariable String registerCode, @Valid @RequestBody CashSealRequest request) {
        return ResponseEntity.ok(service.unseal(registerCode, request));
    }

    @GetMapping("/{registerCode}/seals")
    public ResponseEntity<List<CashSealResponse>> seals(@PathVariable String registerCode) {
        return ResponseEntity.ok(service.seals(registerCode));
    }

    @PostMapping("/delegations")
    public ResponseEntity<CashDelegationResponse> delegate(@Valid @RequestBody CashDelegationRequest request) {
        return ResponseEntity.ok(service.delegate(request));
    }

    @GetMapping("/delegations")
    public ResponseEntity<List<CashDelegationResponse>> delegations() {
        return ResponseEntity.ok(service.delegations());
    }

    @PatchMapping("/delegations/{id}/cancel")
    public ResponseEntity<CashDelegationResponse> cancelDelegation(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancelDelegation(id));
    }
}
