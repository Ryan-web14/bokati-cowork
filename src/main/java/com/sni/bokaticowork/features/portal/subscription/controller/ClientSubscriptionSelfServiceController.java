package com.sni.bokaticowork.features.portal.subscription.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.subscription.service.ClientHistoryService;
import com.sni.bokaticowork.features.portal.subscription.service.ClientSubscriptionSelfService;
import com.sni.bokaticowork.features.subscription.change.dto.SubscriptionChangeResponse;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionDebitMandate;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionExitItem;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionQuote;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionTermination;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionCommitmentService;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionTerminationService;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * L'espace client en ecriture · changer, geler, resilier, prelever, accepter un devis, relire.
 *
 * <p>Le membre n'agit que sur ce qui est a lui, et chaque action passe par les memes regles que
 * le guichet. Ce qui demande une decision humaine (dispense de frais, plancher, visa) reste au
 * guichet.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/client")
@RequiredArgsConstructor
public class ClientSubscriptionSelfServiceController {

    private final ClientContextService context;
    private final ClientSubscriptionSelfService selfService;
    private final ClientHistoryService historyService;

    // ---- Contrats ------------------------------------------------------------------------------

    public record PlanChangeRequest(@NotBlank String targetPlanCode, Boolean atNextPeriod, String reason) {
    }

    public record FreezeRequest(@NotNull LocalDate until, String reason) {
    }

    public record TerminationRequest(SubscriptionTermination.ReasonCategory reasonCategory, String reason, LocalDate requestedEffectiveDate) {
    }

    public record ReasonRequest(String reason) {
    }

    public record MandateRequest(@NotBlank String walletNumber, BigDecimal maxAmountPerDebit) {
    }

    public record TerminationView(String terminationCode, SubscriptionTermination.Status status, Instant requestedAt, LocalDate effectiveDate,
                                  Integer noticePeriodDays, SubscriptionTermination.ReasonCategory reasonCategory, boolean earlyTermination,
                                  BigDecimal feeDue, List<String> exitItemsPending) {
        static TerminationView of(SubscriptionTermination t) {
            return new TerminationView(t.getTerminationCode(), t.getStatus(), t.getRequestedAt(), t.getEffectiveDate(), t.getNoticePeriodDays(),
                    t.getReasonCategory(), Boolean.TRUE.equals(t.getEarlyTermination()), t.feeDue(),
                    t.getExitItems().stream().filter(i -> i.getStatus() == SubscriptionExitItem.Status.PENDING).map(SubscriptionExitItem::getLabel).toList());
        }
    }

    public record MandateView(String mandateCode, String walletNumber, SubscriptionDebitMandate.Status status, Instant consentGivenAt,
                              BigDecimal maxAmountPerDebit, Instant lastDebitAt) {
        static MandateView of(SubscriptionDebitMandate m) {
            return new MandateView(m.getMandateCode(), m.getWalletNumber(), m.getStatus(), m.getConsentGivenAt(), m.getMaxAmountPerDebit(), m.getLastDebitAt());
        }
    }

    public record QuoteView(String quoteNumber, SubscriptionQuote.Status status, String planCode, String planName, String billingCycle,
                            String currency, BigDecimal cataloguePrice, BigDecimal quotedPrice, BigDecimal setupFee, Integer commitmentMonths,
                            Integer trialDays, LocalDate startDate, LocalDate validUntil, String notes, String convertedSubscriptionNumber) {
        static QuoteView of(SubscriptionQuote q) {
            return new QuoteView(q.getQuoteNumber(), q.getStatus(), q.getPlanVersion().getPlan().getCode(), q.getPlanVersion().getName(),
                    q.getBillingCycle().name(), q.getCurrency(), q.getCataloguePrice(), q.getQuotedPrice(), q.getSetupFee(), q.getCommitmentMonths(),
                    q.getTrialDays(), q.getStartDate(), q.getValidUntil(), q.getNotes(), q.getConvertedSubscriptionNumber());
        }
    }

    // ---- Changer de plan -----------------------------------------------------------------------

    @GetMapping("/subscriptions/{subscriptionNumber}/plan-change/preview")
    public ResponseEntity<ClientSubscriptionSelfService.PlanChangePreview> previewPlanChange(@PathVariable String subscriptionNumber,
                                                                                            @RequestParam String targetPlanCode,
                                                                                            @RequestParam(defaultValue = "false") boolean atNextPeriod) {
        return ResponseEntity.ok(selfService.previewPlanChange(member(), subscriptionNumber, targetPlanCode, atNextPeriod));
    }

    @PostMapping("/subscriptions/{subscriptionNumber}/plan-change")
    public ResponseEntity<SubscriptionChangeResponse> changePlan(@PathVariable String subscriptionNumber, @Valid @RequestBody PlanChangeRequest r) {
        return ResponseEntity.ok(selfService.changePlan(member(), subscriptionNumber, r.targetPlanCode(), Boolean.TRUE.equals(r.atNextPeriod()), r.reason()));
    }

    // ---- Geler ---------------------------------------------------------------------------------

    @GetMapping("/subscriptions/{subscriptionNumber}/freeze/terms")
    public ResponseEntity<ClientSubscriptionSelfService.FreezeTerms> freezeTerms(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(selfService.freezeTerms(member(), subscriptionNumber));
    }

    @PostMapping("/subscriptions/{subscriptionNumber}/freeze")
    public ResponseEntity<SubscriptionResponse> freeze(@PathVariable String subscriptionNumber, @Valid @RequestBody FreezeRequest r) {
        return ResponseEntity.ok(selfService.freeze(member(), subscriptionNumber, r.until(), r.reason()));
    }

    @PostMapping("/subscriptions/{subscriptionNumber}/resume")
    public ResponseEntity<SubscriptionResponse> resume(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(selfService.resume(member(), subscriptionNumber));
    }

    // ---- Resilier ------------------------------------------------------------------------------

    @GetMapping("/subscriptions/{subscriptionNumber}/commitment/early-termination")
    public ResponseEntity<SubscriptionCommitmentService.EarlyTermination> earlyTermination(
            @PathVariable String subscriptionNumber, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate on) {
        return ResponseEntity.ok(selfService.earlyTermination(member(), subscriptionNumber, on));
    }

    @PostMapping("/subscriptions/{subscriptionNumber}/termination/preview")
    public ResponseEntity<SubscriptionTerminationService.Preview> previewTermination(@PathVariable String subscriptionNumber,
                                                                                     @RequestBody(required = false) TerminationRequest r) {
        return ResponseEntity.ok(selfService.previewTermination(member(), subscriptionNumber,
                r == null ? null : r.reasonCategory(), r == null ? null : r.requestedEffectiveDate()));
    }

    @PostMapping("/subscriptions/{subscriptionNumber}/termination")
    public ResponseEntity<TerminationView> requestTermination(@PathVariable String subscriptionNumber, @RequestBody(required = false) TerminationRequest r) {
        return ResponseEntity.ok(TerminationView.of(selfService.requestTermination(member(), subscriptionNumber,
                r == null ? null : r.reasonCategory(), r == null ? null : r.reason(), r == null ? null : r.requestedEffectiveDate())));
    }

    @GetMapping("/subscriptions/{subscriptionNumber}/terminations")
    public ResponseEntity<List<TerminationView>> terminations(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(selfService.terminations(member(), subscriptionNumber).stream().map(TerminationView::of).toList());
    }

    @PostMapping("/terminations/{terminationCode}/retract")
    public ResponseEntity<TerminationView> retract(@PathVariable String terminationCode, @RequestBody(required = false) ReasonRequest r) {
        return ResponseEntity.ok(TerminationView.of(selfService.retractTermination(member(), terminationCode, r == null ? null : r.reason())));
    }

    // ---- Prelevement ---------------------------------------------------------------------------

    @GetMapping("/subscriptions/{subscriptionNumber}/debit-mandate")
    public ResponseEntity<MandateView> mandate(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(MandateView.of(selfService.mandate(member(), subscriptionNumber)));
    }

    @PostMapping("/subscriptions/{subscriptionNumber}/debit-mandate")
    public ResponseEntity<MandateView> giveMandate(@PathVariable String subscriptionNumber, @Valid @RequestBody MandateRequest r) {
        return ResponseEntity.ok(MandateView.of(selfService.giveMandate(member(), subscriptionNumber, r.walletNumber(), r.maxAmountPerDebit())));
    }

    @DeleteMapping("/subscriptions/{subscriptionNumber}/debit-mandate")
    public ResponseEntity<MandateView> revokeMandate(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(MandateView.of(selfService.revokeMandate(member(), subscriptionNumber)));
    }

    // ---- Devis ---------------------------------------------------------------------------------

    @GetMapping("/quotes")
    public ResponseEntity<List<QuoteView>> quotes() {
        return ResponseEntity.ok(selfService.quotes(member()).stream().map(QuoteView::of).toList());
    }

    @GetMapping("/quotes/{quoteNumber}")
    public ResponseEntity<QuoteView> quote(@PathVariable String quoteNumber) {
        return ResponseEntity.ok(QuoteView.of(selfService.quote(member(), quoteNumber)));
    }

    @PostMapping("/quotes/{quoteNumber}/accept")
    public ResponseEntity<QuoteView> acceptQuote(@PathVariable String quoteNumber) {
        return ResponseEntity.ok(QuoteView.of(selfService.acceptQuote(member(), quoteNumber)));
    }

    @PostMapping("/quotes/{quoteNumber}/reject")
    public ResponseEntity<QuoteView> rejectQuote(@PathVariable String quoteNumber, @RequestBody(required = false) ReasonRequest r) {
        return ResponseEntity.ok(QuoteView.of(selfService.rejectQuote(member(), quoteNumber, r == null ? null : r.reason())));
    }

    // ---- Historique ----------------------------------------------------------------------------

    @GetMapping("/subscriptions/{subscriptionNumber}/timeline")
    public ResponseEntity<List<ClientHistoryService.Entry>> timeline(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(historyService.ofSubscription(member(), subscriptionNumber));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ClientHistoryService.Entry>> history() {
        return ResponseEntity.ok(historyService.ofMember(member()));
    }

    private Member member() {
        return context.getAuthenticatedMember();
    }
}
