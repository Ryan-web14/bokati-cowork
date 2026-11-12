package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.request.CashCountRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashDenominationRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashHandoverAnswerRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashHandoverRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashReasonRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashVarianceDecisionRequest;
import com.sni.bokaticowork.features.payment.dto.request.ResumeCashSessionRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashCountResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashDenominationResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashExpectedOpeningResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashHandoverResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionStepResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashVarianceReviewResponse;
import com.sni.bokaticowork.features.payment.enums.CashVarianceStatus;
import com.sni.bokaticowork.features.payment.service.interfaces.CashSessionLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Cycle de vie des sessions de caisse et comptages (lot C2 du plan finance). */
@RestController
@RequestMapping(ApiPath.V1 + "/cash-registers")
@RequiredArgsConstructor
public class CashSessionLifecycleController {

    private final CashSessionLifecycleService service;

    /** Corps d'une cloture d'office : le comptage (avec son temoin) et le motif. */
    public record ForceCloseRequest(@Valid CashCountRequest count, CashReasonRequest reason) {
    }

    @GetMapping("/{registerCode}/expected-opening")
    public ResponseEntity<CashExpectedOpeningResponse> expectedOpening(@PathVariable String registerCode) {
        return ResponseEntity.ok(service.expectedOpening(registerCode));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_CONFIRM_OPENING", ressource = "cash_register")
    @PatchMapping("/sessions/{sessionNumber}/confirm-opening")
    public ResponseEntity<CashSessionStepResponse> confirmOpening(@PathVariable String sessionNumber,
                                                                  @RequestBody(required = false) CashReasonRequest request) {
        return ResponseEntity.ok(service.confirmOpening(sessionNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_PAUSE", ressource = "cash_register")
    @PatchMapping("/sessions/{sessionNumber}/pause")
    public ResponseEntity<CashSessionStepResponse> pause(@PathVariable String sessionNumber) {
        return ResponseEntity.ok(service.pause(sessionNumber));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_RESUME", ressource = "cash_register")
    @PatchMapping("/sessions/{sessionNumber}/resume")
    public ResponseEntity<CashSessionStepResponse> resume(@PathVariable String sessionNumber,
                                                          @RequestBody(required = false) ResumeCashSessionRequest request) {
        return ResponseEntity.ok(service.resume(sessionNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_SUSPEND", ressource = "cash_register")
    @PatchMapping("/sessions/{sessionNumber}/suspend")
    public ResponseEntity<CashSessionStepResponse> suspend(@PathVariable String sessionNumber,
                                                           @RequestBody(required = false) CashReasonRequest request) {
        return ResponseEntity.ok(service.suspend(sessionNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_UNSUSPEND", ressource = "cash_register")
    @PatchMapping("/sessions/{sessionNumber}/unsuspend")
    public ResponseEntity<CashSessionStepResponse> unsuspend(@PathVariable String sessionNumber) {
        return ResponseEntity.ok(service.unsuspend(sessionNumber));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_START_CLOSING", ressource = "cash_register")
    @PatchMapping("/sessions/{sessionNumber}/start-closing")
    public ResponseEntity<CashSessionStepResponse> startClosing(@PathVariable String sessionNumber) {
        return ResponseEntity.ok(service.startClosing(sessionNumber));
    }

    /** Comptage de cloture, ou de recomptage quand la session est en recomptage. */
    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_SUBMIT_COUNT", ressource = "cash_register")
    @PostMapping("/sessions/{sessionNumber}/counts")
    public ResponseEntity<CashSessionStepResponse> submitCount(@PathVariable String sessionNumber,
                                                               @Valid @RequestBody CashCountRequest request) {
        return ResponseEntity.ok(service.submitClosingCount(sessionNumber, request));
    }

    @GetMapping("/sessions/{sessionNumber}/counts")
    public ResponseEntity<List<CashCountResponse>> counts(@PathVariable String sessionNumber) {
        return ResponseEntity.ok(service.counts(sessionNumber));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_RECOUNT", ressource = "cash_register")
    @PatchMapping("/sessions/{sessionNumber}/recount")
    public ResponseEntity<CashSessionStepResponse> recount(@PathVariable String sessionNumber,
                                                           @RequestBody(required = false) CashReasonRequest request) {
        return ResponseEntity.ok(service.requestRecount(sessionNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_DECIDE_SESSION_VARIANCE", ressource = "cash_register")
    @PatchMapping("/sessions/{sessionNumber}/variance-decision")
    public ResponseEntity<CashSessionStepResponse> decideSessionVariance(@PathVariable String sessionNumber,
                                                                         @RequestBody(required = false) CashVarianceDecisionRequest request) {
        return ResponseEntity.ok(service.decideSessionVariance(sessionNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_SPOT_CHECK", ressource = "cash_register")
    @PostMapping("/sessions/{sessionNumber}/spot-checks")
    public ResponseEntity<CashSessionStepResponse> spotCheck(@PathVariable String sessionNumber,
                                                             @Valid @RequestBody CashCountRequest request) {
        return ResponseEntity.ok(service.spotCheck(sessionNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_FORCE_CLOSE", ressource = "cash_register")
    @PatchMapping("/sessions/{sessionNumber}/force-close")
    public ResponseEntity<CashSessionStepResponse> forceClose(@PathVariable String sessionNumber,
                                                              @Valid @RequestBody ForceCloseRequest request) {
        return ResponseEntity.ok(service.forceClose(sessionNumber, request.count(), request.reason()));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_CONTEST", ressource = "cash_register")
    @PatchMapping("/sessions/{sessionNumber}/contest")
    public ResponseEntity<CashSessionStepResponse> contest(@PathVariable String sessionNumber,
                                                           @RequestBody CashReasonRequest request) {
        return ResponseEntity.ok(service.contest(sessionNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_CANCEL", ressource = "cash_register")
    @PatchMapping("/sessions/{sessionNumber}/cancel")
    public ResponseEntity<CashSessionStepResponse> cancel(@PathVariable String sessionNumber,
                                                          @RequestBody CashReasonRequest request) {
        return ResponseEntity.ok(service.cancel(sessionNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_HANDOVER", ressource = "cash_register")
    @PostMapping("/sessions/{sessionNumber}/handover")
    public ResponseEntity<CashHandoverResponse> handover(@PathVariable String sessionNumber,
                                                         @Valid @RequestBody CashHandoverRequest request) {
        return ResponseEntity.ok(service.startHandover(sessionNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_ACCEPT_HANDOVER", ressource = "cash_register")
    @PatchMapping("/handovers/{handoverNumber}/accept")
    public ResponseEntity<CashSessionStepResponse> acceptHandover(@PathVariable String handoverNumber,
                                                                  @RequestBody(required = false) CashHandoverAnswerRequest request) {
        return ResponseEntity.ok(service.acceptHandover(handoverNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_DISPUTE_HANDOVER", ressource = "cash_register")
    @PatchMapping("/handovers/{handoverNumber}/dispute")
    public ResponseEntity<CashSessionStepResponse> disputeHandover(@PathVariable String handoverNumber,
                                                                   @Valid @RequestBody CashHandoverAnswerRequest request) {
        return ResponseEntity.ok(service.disputeHandover(handoverNumber, request));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_CANCEL_HANDOVER", ressource = "cash_register")
    @PatchMapping("/handovers/{handoverNumber}/cancel")
    public ResponseEntity<CashHandoverResponse> cancelHandover(@PathVariable String handoverNumber) {
        return ResponseEntity.ok(service.cancelHandover(handoverNumber));
    }

    @GetMapping("/variance-reviews")
    public ResponseEntity<List<CashVarianceReviewResponse>> reviews(@RequestParam(required = false) CashVarianceStatus status,
                                                                    @RequestParam(required = false) String registerCode,
                                                                    @RequestParam(required = false) String sessionNumber) {
        return ResponseEntity.ok(service.reviews(status, registerCode, sessionNumber));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_DECIDE_REVIEW", ressource = "cash_register")
    @PatchMapping("/variance-reviews/{reviewNumber}/decision")
    public ResponseEntity<CashSessionStepResponse> decideReview(@PathVariable String reviewNumber,
                                                                @RequestBody(required = false) CashVarianceDecisionRequest request) {
        return ResponseEntity.ok(service.decideReview(reviewNumber, request));
    }

    @GetMapping("/denominations")
    public ResponseEntity<List<CashDenominationResponse>> denominations(@RequestParam(required = false) String currency,
                                                                        @RequestParam(defaultValue = "false") boolean includeRetired) {
        return ResponseEntity.ok(service.denominations(currency, includeRetired));
    }

    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_REPLACE_DENOMINATIONS", ressource = "cash_register")
    @PutMapping("/denominations/{currency}")
    public ResponseEntity<List<CashDenominationResponse>> replaceDenominations(@PathVariable String currency,
                                                                               @Valid @RequestBody List<CashDenominationRequest> values) {
        return ResponseEntity.ok(service.replaceDenominations(currency, values));
    }
}
