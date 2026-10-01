package com.sni.bokaticowork.features.subscription.lifecycle.controller;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.lifecycle.model.*;
import com.sni.bokaticowork.features.subscription.lifecycle.service.*;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Le cycle de vie · la politique, l'engagement, le prelevement, le gel, l'alignement, le prorata.
 *
 * <p>Tout ce qui ne meritait pas un controleur a part. Les resiliations et les devis ont le leur.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/subscriptions/lifecycle")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')")
public class SubscriptionLifecycleController {

    private final SubscriptionPolicyService policyService;
    private final SubscriptionCommitmentService commitmentService;
    private final SubscriptionDirectDebitService directDebitService;
    private final SubscriptionFreezeService freezeService;
    private final SubscriptionAlignmentService alignmentService;
    private final PlanChangeProrationService prorationService;
    private final SubscriptionGraceService graceService;
    private final SubscriptionService subscriptionService;
    private final PlanVersionRepository planVersionRepository;

    // ---- Politique ------------------------------------------------------------------------------

    public record PolicyRequest(ProrationPolicy prorationPolicy, Integer defaultNoticeDays, EarlyTerminationFormula earlyTerminationFormula,
                                BigDecimal earlyTerminationPercent, BigDecimal earlyTerminationFixedFee, Integer freezeMaxPerYear,
                                Integer freezeMaxDays, Integer freezeNoticeDays, BigDecimal freezeFeePercent, Integer gracePeriodDays,
                                Integer suspensionAfterGraceDays, Integer quoteValidityDays) {
    }

    @GetMapping("/policy")
    public ResponseEntity<SubscriptionPolicy> policy() {
        return ResponseEntity.ok(policyService.current());
    }

    @PutMapping("/policy")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<SubscriptionPolicy> updatePolicy(@RequestBody PolicyRequest r) {
        return ResponseEntity.ok(policyService.update(new SubscriptionPolicyService.Update(r.prorationPolicy(), r.defaultNoticeDays(),
                r.earlyTerminationFormula(), r.earlyTerminationPercent(), r.earlyTerminationFixedFee(), r.freezeMaxPerYear(), r.freezeMaxDays(),
                r.freezeNoticeDays(), r.freezeFeePercent(), r.gracePeriodDays(), r.suspensionAfterGraceDays(), r.quoteValidityDays())));
    }

    // ---- Engagement -----------------------------------------------------------------------------

    public record CommitmentRequest(@NotNull Integer commitmentMonths, LocalDate commitmentStart, EarlyTerminationFormula formula,
                                    BigDecimal percent, BigDecimal fixedFee, Boolean autoRenewCommitment) {
    }

    public record CommitmentView(String subscriptionNumber, Integer commitmentMonths, LocalDate commitmentStart, LocalDate commitmentEnd,
                                 EarlyTerminationFormula earlyTerminationFormula, BigDecimal earlyTerminationPercent, BigDecimal earlyTerminationFixedFee,
                                 boolean autoRenewCommitment, SubscriptionCommitment.Source source, String createdBy, Instant createdAt) {
        static CommitmentView of(SubscriptionCommitment c) {
            return new CommitmentView(c.getSubscription().getSubscriptionNumber(), c.getCommitmentMonths(), c.getCommitmentStart(), c.getCommitmentEnd(),
                    c.getEarlyTerminationFormula(), c.getEarlyTerminationPercent(), c.getEarlyTerminationFixedFee(),
                    Boolean.TRUE.equals(c.getAutoRenewCommitment()), c.getSource(), c.getCreatedBy(), c.getCreatedAt());
        }
    }

    @GetMapping("/{subscriptionNumber}/commitment")
    public ResponseEntity<CommitmentView> commitment(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(CommitmentView.of(commitmentService.get(subscriptionNumber)));
    }

    @PutMapping("/{subscriptionNumber}/commitment")
    public ResponseEntity<CommitmentView> setCommitment(@PathVariable String subscriptionNumber, @Valid @RequestBody CommitmentRequest r,
                                                        Authentication authentication) {
        Subscription subscription = subscriptionService.getForService(subscriptionNumber);
        return ResponseEntity.ok(CommitmentView.of(commitmentService.set(subscription,
                new SubscriptionCommitmentService.Spec(r.commitmentMonths(), r.commitmentStart(), r.formula(), r.percent(), r.fixedFee(), r.autoRenewCommitment()),
                SubscriptionCommitment.Source.MANUAL, actor(authentication))));
    }

    @DeleteMapping("/{subscriptionNumber}/commitment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> releaseCommitment(@PathVariable String subscriptionNumber, Authentication authentication) {
        commitmentService.release(subscriptionService.getForService(subscriptionNumber), actor(authentication));
        return ResponseEntity.noContent().build();
    }

    /** Ce que couterait de sortir a une date · la question a poser avant tout preavis. */
    @GetMapping("/{subscriptionNumber}/commitment/early-termination")
    public ResponseEntity<SubscriptionCommitmentService.EarlyTermination> earlyTermination(@PathVariable String subscriptionNumber,
                                                                                            @RequestParam(required = false) LocalDate on) {
        return ResponseEntity.ok(commitmentService.earlyTerminationOn(subscriptionService.getForService(subscriptionNumber),
                on == null ? LocalDate.now() : on));
    }

    // ---- Prelevement ----------------------------------------------------------------------------

    public record MandateRequest(@NotBlank String walletNumber, @NotNull SubscriptionDebitMandate.Channel channel, String reference,
                                 BigDecimal maxAmountPerDebit) {
    }

    public record ReasonRequest(String reason) {
    }

    public record MandateView(String mandateCode, String subscriptionNumber, String walletNumber, SubscriptionDebitMandate.Status status,
                              Instant consentGivenAt, String consentGivenBy, SubscriptionDebitMandate.Channel consentChannel, String consentReference,
                              BigDecimal maxAmountPerDebit, Integer consecutiveFailures, Instant lastDebitAt, Instant revokedAt, String revokedBy,
                              String revocationReason) {
        static MandateView of(SubscriptionDebitMandate m) {
            return new MandateView(m.getMandateCode(), m.getSubscription().getSubscriptionNumber(), m.getWalletNumber(), m.getStatus(),
                    m.getConsentGivenAt(), m.getConsentGivenBy(), m.getConsentChannel(), m.getConsentReference(), m.getMaxAmountPerDebit(),
                    m.getConsecutiveFailures(), m.getLastDebitAt(), m.getRevokedAt(), m.getRevokedBy(), m.getRevocationReason());
        }
    }

    public record AttemptView(String invoiceNumber, BigDecimal amount, String currency, SubscriptionDebitAttempt.Status status, String message,
                              String transactionNumber, Instant executedAt) {
        static AttemptView of(SubscriptionDebitAttempt a) {
            return new AttemptView(a.getInvoiceNumber(), a.getAmount(), a.getCurrency(), a.getStatus(), a.getMessage(), a.getTransactionNumber(), a.getExecutedAt());
        }
    }

    @PostMapping("/{subscriptionNumber}/debit-mandate")
    public ResponseEntity<MandateView> giveMandate(@PathVariable String subscriptionNumber, @Valid @RequestBody MandateRequest r,
                                                   Authentication authentication) {
        return ResponseEntity.ok(MandateView.of(directDebitService.give(subscriptionNumber,
                new SubscriptionDirectDebitService.Consent(r.walletNumber(), r.channel(), r.reference(), r.maxAmountPerDebit()), actor(authentication))));
    }

    @GetMapping("/{subscriptionNumber}/debit-mandate")
    public ResponseEntity<List<MandateView>> mandates(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(directDebitService.ofSubscription(subscriptionNumber).stream().map(MandateView::of).toList());
    }

    @GetMapping("/{subscriptionNumber}/debit-attempts")
    public ResponseEntity<List<AttemptView>> attempts(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(directDebitService.attempts(subscriptionNumber).stream().map(AttemptView::of).toList());
    }

    @PostMapping("/debit-mandates/{mandateCode}/revoke")
    public ResponseEntity<MandateView> revokeMandate(@PathVariable String mandateCode, @RequestBody(required = false) ReasonRequest r,
                                                     Authentication authentication) {
        return ResponseEntity.ok(MandateView.of(directDebitService.revoke(mandateCode, r == null ? null : r.reason(), actor(authentication))));
    }

    @PostMapping("/debit-mandates/{mandateCode}/reactivate")
    public ResponseEntity<MandateView> reactivateMandate(@PathVariable String mandateCode, Authentication authentication) {
        return ResponseEntity.ok(MandateView.of(directDebitService.reactivate(mandateCode, actor(authentication))));
    }

    /** Tente maintenant le prelevement d'une facture · pour une echeance restee impayee. */
    @PostMapping("/{subscriptionNumber}/debit-mandate/collect/{invoiceNumber}")
    public ResponseEntity<AttemptView> collect(@PathVariable String subscriptionNumber, @PathVariable String invoiceNumber) {
        Subscription subscription = subscriptionService.getForService(subscriptionNumber);
        return directDebitService.collect(subscription.getId(), invoiceNumber).map(AttemptView::of).map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun mandat actif, ou rien à prélever sur cette facture"));
    }

    // ---- Gel --------------------------------------------------------------------------------------

    public record FreezeView(LocalDate startedOn, LocalDate plannedUntil, LocalDate resumedOn, Integer daysPlanned, Integer daysEffective,
                             BigDecimal feeAmount, String feeBillableNumber, String reason, String requestedBy) {
        static FreezeView of(SubscriptionFreeze f) {
            return new FreezeView(f.getStartedOn(), f.getPlannedUntil(), f.getResumedOn(), f.getDaysPlanned(), f.getDaysEffective(),
                    f.getFeeAmount(), f.getFeeBillableNumber(), f.getReason(), f.getRequestedBy());
        }
    }

    @GetMapping("/{subscriptionNumber}/freezes")
    public ResponseEntity<List<FreezeView>> freezes(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(freezeService.history(subscriptionService.getForService(subscriptionNumber)).stream().map(FreezeView::of).toList());
    }

    /** Ce que la politique permet encore · avant de proposer un gel. */
    @GetMapping("/{subscriptionNumber}/freezes/allowance")
    public ResponseEntity<SubscriptionFreezeService.Allowance> freezeAllowance(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(freezeService.allowance(subscriptionService.getForService(subscriptionNumber)));
    }

    // ---- Tolerance ------------------------------------------------------------------------------

    /** Sortie manuelle de tolerance · le paiement a ete constate hors systeme. */
    @PostMapping("/{subscriptionNumber}/grace/exit")
    public ResponseEntity<Void> exitGrace(@PathVariable String subscriptionNumber, @RequestBody(required = false) ReasonRequest r,
                                          Authentication authentication) {
        Subscription subscription = subscriptionService.getForService(subscriptionNumber);
        graceService.exit(subscription, (r == null || r.reason() == null ? "Régularisation constatée" : r.reason()) + " · " + actor(authentication));
        return ResponseEntity.noContent().build();
    }

    // ---- Alignement, prorata ------------------------------------------------------------------

    public record AlignRequest(@NotNull SubscriberType subscriberType, @NotBlank String subscriberCode, LocalDate targetPeriodEnd, Boolean simulateOnly) {
    }

    @PostMapping("/align")
    public ResponseEntity<SubscriptionAlignmentService.Outcome> align(@Valid @RequestBody AlignRequest r, Authentication authentication) {
        return ResponseEntity.ok(alignmentService.align(r.subscriberType(), r.subscriberCode(), r.targetPeriodEnd(),
                r.simulateOnly() == null || r.simulateOnly(), actor(authentication)));
    }

    /** Ce qu'un changement de plan couterait ou rendrait a une date · avant de le demander. */
    @GetMapping("/{subscriptionNumber}/plan-change/preview")
    public ResponseEntity<PlanChangeProrationService.Preview> planChangePreview(@PathVariable String subscriptionNumber,
                                                                                @RequestParam Long targetPlanVersionId,
                                                                                @RequestParam(required = false) LocalDate effectiveDate) {
        Subscription subscription = subscriptionService.getForService(subscriptionNumber);
        PlanVersion target = planVersionRepository.findById(targetPlanVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version de plan introuvable"));
        return ResponseEntity.ok(prorationService.preview(subscription, target, effectiveDate));
    }

    private static String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null ? "STAFF" : authentication.getName();
    }
}
