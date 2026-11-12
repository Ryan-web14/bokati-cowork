package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.request.CashExchangeRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashNatureRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashReasonRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashTransferReceiveRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashTransferRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashNatureResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashPettyRefillResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashTransferResponse;
import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashNatureFamily;
import com.sni.bokaticowork.features.payment.enums.CashTransferStatus;
import com.sni.bokaticowork.features.payment.service.interfaces.CashOperationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Operations de caisse et transferts internes (lot C3 du plan finance). */
@RestController
@RequestMapping(ApiPath.V1 + "/cash-registers")
@RequiredArgsConstructor
public class CashOperationsController {

    private final CashOperationsService service;

    @GetMapping("/natures")
    public ResponseEntity<List<CashNatureResponse>> natures(@RequestParam(required = false) CashFlowDirection direction,
                                                            @RequestParam(required = false) CashNatureFamily family,
                                                            @RequestParam(required = false) String search,
                                                            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(service.natures(direction, family, search, includeInactive));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_REPLACE_NATURES", ressource = "cash_register")
    @PutMapping("/natures")
    public ResponseEntity<List<CashNatureResponse>> replaceNatures(@Valid @RequestBody List<CashNatureRequest> natures) {
        return ResponseEntity.ok(service.replaceNatures(natures));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_APPROVE", ressource = "cash_register")
    @PatchMapping("/movements/{movementNumber}/approve")
    public ResponseEntity<CashMovementResponse> approve(@PathVariable String movementNumber) {
        return ResponseEntity.ok(service.approveMovement(movementNumber));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_CANCEL", ressource = "cash_register")
    @PatchMapping("/movements/{movementNumber}/cancel")
    public ResponseEntity<CashMovementResponse> cancel(@PathVariable String movementNumber,
                                                       @RequestBody CashReasonRequest request) {
        return ResponseEntity.ok(service.cancelMovement(movementNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_REVERSE", ressource = "cash_register")
    @PostMapping("/movements/{movementNumber}/reverse")
    public ResponseEntity<CashMovementResponse> reverse(@PathVariable String movementNumber,
                                                        @RequestBody CashReasonRequest request) {
        return ResponseEntity.ok(service.reverseMovement(movementNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_EXCHANGE", ressource = "cash_register")
    @PostMapping("/sessions/{sessionNumber}/exchanges")
    public ResponseEntity<CashMovementResponse> exchange(@PathVariable String sessionNumber,
                                                         @Valid @RequestBody CashExchangeRequest request) {
        return ResponseEntity.ok(service.exchange(sessionNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_SEND", ressource = "cash_register")
    @PostMapping("/transfers")
    public ResponseEntity<CashTransferResponse> send(@Valid @RequestBody CashTransferRequest request) {
        return ResponseEntity.ok(service.sendTransfer(request));
    }

    @GetMapping("/transfers")
    public ResponseEntity<List<CashTransferResponse>> transfers(@RequestParam(required = false) String registerCode,
                                                                @RequestParam(required = false) CashTransferStatus status) {
        return ResponseEntity.ok(service.transfers(registerCode, status));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_RECEIVE", ressource = "cash_register")
    @PatchMapping("/transfers/{transferNumber}/receive")
    public ResponseEntity<CashTransferResponse> receive(@PathVariable String transferNumber,
                                                        @Valid @RequestBody CashTransferReceiveRequest request) {
        return ResponseEntity.ok(service.receiveTransfer(transferNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_CANCEL_TRANSFER", ressource = "cash_register")
    @PatchMapping("/transfers/{transferNumber}/cancel")
    public ResponseEntity<CashTransferResponse> cancelTransfer(@PathVariable String transferNumber,
                                                               @RequestBody CashReasonRequest request) {
        return ResponseEntity.ok(service.cancelTransfer(transferNumber, request));
    }

    @GetMapping("/{registerCode}/petty-cash/refill")
    public ResponseEntity<CashPettyRefillResponse> pettyRefillProposal(@PathVariable String registerCode) {
        return ResponseEntity.ok(service.pettyRefillProposal(registerCode));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_PETTY_REFILL", ressource = "cash_register")
    @PostMapping("/{registerCode}/petty-cash/refill")
    public ResponseEntity<CashTransferResponse> pettyRefill(@PathVariable String registerCode,
                                                            @RequestParam(required = false) String carrierEmail) {
        return ResponseEntity.ok(service.pettyRefill(registerCode, carrierEmail));
    }
}
