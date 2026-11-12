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

    @PatchMapping("/sessions/{sessionNumber}/confirm-opening")
    public ResponseEntity<CashSessionStepResponse> confirmOpening(@PathVariable String sessionNumber,
                                                                  @RequestBody(required = false) CashReasonRequest request) {
        return ResponseEntity.ok(service.confirmOpening(sessionNumber, request));
    }

    @PatchMapping("/sessions/{sessionNumber}/pause")
    public ResponseEntity<CashSessionStepResponse> pause(@PathVariable String sessionNumber) {
        return ResponseEntity.ok(service.pause(sessionNumber));
    }

    @PatchMapping("/sessions/{sessionNumber}/resume")
    public ResponseEntity<CashSessionStepResponse> resume(@PathVariable String sessionNumber,
                                                          @RequestBody(required = false) ResumeCashSessionRequest request) {
        return ResponseEntity.ok(service.resume(sessionNumber, request));
    }

    @PatchMapping("/sessions/{sessionNumber}/suspend")
    public ResponseEntity<CashSessionStepResponse> suspend(@PathVariable String sessionNumber,
                                                           @RequestBody(required = false) CashReasonRequest request) {
        return ResponseEntity.ok(service.suspend(sessionNumber, request));
    }

    @PatchMapping("/sessions/{sessionNumber}/unsuspend")
    public ResponseEntity<CashSessionStepResponse> unsuspend(@PathVariable String sessionNumber) {
        return ResponseEntity.ok(service.unsuspend(sessionNumber));
    }

    @PatchMapping("/sessions/{sessionNumber}/start-closing")
    public ResponseEntity<CashSessionStepResponse> startClosing(@PathVariable String sessionNumber) {
        return ResponseEntity.ok(service.startClosing(sessionNumber));
    }

    /** Comptage de cloture, ou de recomptage quand la session est en recomptage. */
    @PostMapping("/sessions/{sessionNumber}/counts")
    public ResponseEntity<CashSessionStepResponse> submitCount(@PathVariable String sessionNumber,
                                                               @Valid @RequestBody CashCountRequest request) {
        return ResponseEntity.ok(service.submitClosingCount(sessionNumber, request));
    }

    @GetMapping("/sessions/{sessionNumber}/counts")
    public ResponseEntity<List<CashCountResponse>> counts(@PathVariable String sessionNumber) {
        return ResponseEntity.ok(service.counts(sessionNumber));
    }

    @PatchMapping("/sessions/{sessionNumber}/recount")
    public ResponseEntity<CashSessionStepResponse> recount(@PathVariable String sessionNumber,
                                                           @RequestBody(required = false) CashReasonRequest request) {
        return ResponseEntity.ok(service.requestRecount(sessionNumber, request));
    }

    @PatchMapping("/sessions/{sessionNumber}/variance-decision")
    public ResponseEntity<CashSessionStepResponse> decideSessionVariance(@PathVariable String sessionNumber,
                                                                         @RequestBody(required = false) CashVarianceDecisionRequest request) {
        return ResponseEntity.ok(service.decideSessionVariance(sessionNumber, request));
    }

    @PostMapping("/sessions/{sessionNumber}/spot-checks")
    public ResponseEntity<CashSessionStepResponse> spotCheck(@PathVariable String sessionNumber,
                                                             @Valid @RequestBody CashCountRequest request) {
        return ResponseEntity.ok(service.spotCheck(sessionNumber, request));
    }

    @PatchMapping("/sessions/{sessionNumber}/force-close")
    public ResponseEntity<CashSessionStepResponse> forceClose(@PathVariable String sessionNumber,
                                                              @Valid @RequestBody ForceCloseRequest request) {
        return ResponseEntity.ok(service.forceClose(sessionNumber, request.count(), request.reason()));
    }

    @PatchMapping("/sessions/{sessionNumber}/contest")
    public ResponseEntity<CashSessionStepResponse> contest(@PathVariable String sessionNumber,
                                                           @RequestBody CashReasonRequest request) {
        return ResponseEntity.ok(service.contest(sessionNumber, request));
    }

    @PatchMapping("/sessions/{sessionNumber}/cancel")
    public ResponseEntity<CashSessionStepResponse> cancel(@PathVariable String sessionNumber,
                                                          @RequestBody CashReasonRequest request) {
        return ResponseEntity.ok(service.cancel(sessionNumber, request));
    }

    @PostMapping("/sessions/{sessionNumber}/handover")
    public ResponseEntity<CashHandoverResponse> handover(@PathVariable String sessionNumber,
                                                         @Valid @RequestBody CashHandoverRequest request) {
        return ResponseEntity.ok(service.startHandover(sessionNumber, request));
    }

    @PatchMapping("/handovers/{handoverNumber}/accept")
    public ResponseEntity<CashSessionStepResponse> acceptHandover(@PathVariable String handoverNumber,
                                                                  @RequestBody(required = false) CashHandoverAnswerRequest request) {
        return ResponseEntity.ok(service.acceptHandover(handoverNumber, request));
    }

    @PatchMapping("/handovers/{handoverNumber}/dispute")
    public ResponseEntity<CashSessionStepResponse> disputeHandover(@PathVariable String handoverNumber,
                                                                   @Valid @RequestBody CashHandoverAnswerRequest request) {
        return ResponseEntity.ok(service.disputeHandover(handoverNumber, request));
    }

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

    @PutMapping("/denominations/{currency}")
    public ResponseEntity<List<CashDenominationResponse>> replaceDenominations(@PathVariable String currency,
                                                                               @Valid @RequestBody List<CashDenominationRequest> values) {
        return ResponseEntity.ok(service.replaceDenominations(currency, values));
    }
}
