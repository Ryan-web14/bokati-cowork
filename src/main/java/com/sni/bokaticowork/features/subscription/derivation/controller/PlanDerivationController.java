package com.sni.bokaticowork.features.subscription.derivation.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivation;
import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivationApprovalRule;
import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivationDelta;
import com.sni.bokaticowork.features.subscription.derivation.service.PlanDerivationBatchService;
import com.sni.bokaticowork.features.subscription.derivation.service.PlanDerivationService;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Les abonnements derives · un prix et des avantages propres, sans plan de catalogue dedie.
 *
 * <p>Toujours simuler avant de creer : la simulation dit l'ecart, le cout, et si un visa sera
 * necessaire. Le demandeur est celui qui est authentifie, jamais un champ du corps · c'est ce qui
 * donne un sens au second visa, et a la liste des concessions par commercial.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/subscriptions/derivations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')")
public class PlanDerivationController {

    private final PlanDerivationService derivationService;
    private final PlanDerivationBatchService batchService;

    // ---- Contrats ---------------------------------------------------------------------------

    public record SpecRequest(
            BigDecimal price, BigDecimal setupFee, Integer trialDays, Integer commitmentMonths,
            Map<String, BigDecimal> entitlementQuantities, List<String> unlimitedEntitlements, List<String> includedBenefits,
            @NotNull(message = "Une dérivation a un motif") PlanDerivation.Reason reason, String reasonDetails,
            LocalDate effectiveFrom, LocalDate effectiveTo,
            PlanDerivation.RenewalBehaviour renewalBehaviour, Integer revertAfterPeriods, Boolean promotionsAllowed
    ) {
        PlanDerivationService.Spec toSpec() {
            return new PlanDerivationService.Spec(price, setupFee, trialDays, commitmentMonths, entitlementQuantities,
                    unlimitedEntitlements, includedBenefits, reason, reasonDetails, effectiveFrom, effectiveTo,
                    renewalBehaviour, revertAfterPeriods, promotionsAllowed);
        }
    }

    public record BulkRequest(@NotNull List<String> subscriptionNumbers, @NotNull SpecRequest spec, Boolean simulateOnly) {
    }

    public record ProtectRequest(@NotNull Long previousVersionId, LocalDate protectedUntil, Boolean simulateOnly) {
    }

    public record ReasonRequest(@NotBlank String reason) {
    }

    public record DerivationView(String derivationCode, String subscriptionNumber, String planCode, String sourceVersion,
                                 String derivedVersion, PlanDerivation.Status status, PlanDerivation.Reason reason,
                                 String reasonDetails, LocalDate effectiveFrom, LocalDate effectiveTo,
                                 PlanDerivation.RenewalBehaviour renewalBehaviour, Integer revertAfterPeriods, Integer periodsApplied,
                                 Boolean promotionsAllowed, BigDecimal totalImpactAmount, BigDecimal discountPercent, String currency,
                                 String requestedBy, String approvedBy, Instant approvedAt, String rejectionReason,
                                 Instant appliedAt, Instant endedAt, Long supersedesDerivationId, String batchCode, Instant createdAt) {
        static DerivationView of(PlanDerivation d) {
            return new DerivationView(d.getDerivationCode(), d.getSubscription().getSubscriptionNumber(),
                    d.getSourcePlanVersion().getPlan().getCode(),
                    d.getSourcePlanVersion().getName() + " v" + d.getSourcePlanVersion().getVersionNumber(),
                    d.getDerivedPlanVersion().getName() + " v" + d.getDerivedPlanVersion().getVersionNumber(),
                    d.getStatus(), d.getReason(), d.getReasonDetails(), d.getEffectiveFrom(), d.getEffectiveTo(),
                    d.getRenewalBehaviour(), d.getRevertAfterPeriods(), d.getPeriodsApplied(), d.getPromotionsAllowed(),
                    d.getTotalImpactAmount(), d.getDiscountPercent(), d.getCurrency(), d.getRequestedBy(), d.getApprovedBy(),
                    d.getApprovedAt(), d.getRejectionReason(), d.getAppliedAt(), d.getEndedAt(), d.getSupersedesDerivationId(),
                    d.getBatchCode(), d.getCreatedAt());
        }
    }

    public record DeltaView(PlanDerivationDelta.Type type, String targetCode, String catalogueValue, String derivedValue, BigDecimal impactAmount) {
        static DeltaView of(PlanDerivationDelta d) {
            return new DeltaView(d.getDeltaType(), d.getTargetCode(), d.getCatalogueValue(), d.getDerivedValue(), d.getImpactAmount());
        }
    }

    // ---- Simuler, creer -----------------------------------------------------------------------

    @PostMapping("/subscriptions/{subscriptionNumber}/simulate")
    public ResponseEntity<PlanDerivationService.Preview> simulate(@PathVariable String subscriptionNumber,
                                                                  @jakarta.validation.Valid @RequestBody SpecRequest request) {
        return ResponseEntity.ok(derivationService.simulate(subscriptionNumber, request.toSpec()));
    }

    @PostMapping("/subscriptions/{subscriptionNumber}")
    public ResponseEntity<DerivationView> create(@PathVariable String subscriptionNumber,
                                                 @jakarta.validation.Valid @RequestBody SpecRequest request, Authentication authentication) {
        return ResponseEntity.ok(DerivationView.of(derivationService.create(subscriptionNumber, request.toSpec(), actor(authentication))));
    }

    @GetMapping("/subscriptions/{subscriptionNumber}")
    public ResponseEntity<List<DerivationView>> ofSubscription(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(derivationService.ofSubscription(subscriptionNumber).stream().map(DerivationView::of).toList());
    }

    /** Retour au catalogue · la derivation active expire, elle ne disparait pas. */
    @PostMapping("/subscriptions/{subscriptionNumber}/revert")
    public ResponseEntity<DerivationView> revert(@PathVariable String subscriptionNumber, @RequestBody ReasonRequest request) {
        return ResponseEntity.ok(DerivationView.of(derivationService.revert(subscriptionNumber, request.reason())));
    }

    // ---- Visa -----------------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<PaginatedResponse<DerivationView>> list(@RequestParam(required = false) List<PlanDerivation.Status> status,
                                                                  @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new PaginatedResponse<>(derivationService.list(status,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())).map(DerivationView::of)));
    }

    @GetMapping("/{derivationCode}")
    public ResponseEntity<DerivationView> get(@PathVariable String derivationCode) {
        return ResponseEntity.ok(DerivationView.of(derivationService.get(derivationCode)));
    }

    @GetMapping("/{derivationCode}/deltas")
    public ResponseEntity<List<DeltaView>> deltas(@PathVariable String derivationCode) {
        return ResponseEntity.ok(derivationService.deltas(derivationCode).stream().map(DeltaView::of).toList());
    }

    @PostMapping("/{derivationCode}/approve")
    public ResponseEntity<DerivationView> approve(@PathVariable String derivationCode, Authentication authentication) {
        return ResponseEntity.ok(DerivationView.of(derivationService.approve(derivationCode, actor(authentication))));
    }

    @PostMapping("/{derivationCode}/reject")
    public ResponseEntity<DerivationView> reject(@PathVariable String derivationCode, @RequestBody ReasonRequest request,
                                                 Authentication authentication) {
        return ResponseEntity.ok(DerivationView.of(derivationService.reject(derivationCode, request.reason(), actor(authentication))));
    }

    // ---- Lot, protection, concessions -------------------------------------------------------

    @PostMapping("/bulk")
    public ResponseEntity<PlanDerivationBatchService.Outcome> bulk(@jakarta.validation.Valid @RequestBody BulkRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(batchService.apply(request.subscriptionNumbers(), request.spec().toSpec(),
                request.simulateOnly() == null || request.simulateOnly(), actor(authentication)));
    }

    @PostMapping("/protect")
    public ResponseEntity<PlanDerivationBatchService.Outcome> protect(@jakarta.validation.Valid @RequestBody ProtectRequest request,
                                                                      Authentication authentication) {
        return ResponseEntity.ok(batchService.protectSubscribers(request.previousVersionId(), request.protectedUntil(),
                request.simulateOnly() == null || request.simulateOnly(), actor(authentication)));
    }

    @GetMapping("/batches/{batchCode}")
    public ResponseEntity<List<DerivationView>> batch(@PathVariable String batchCode) {
        return ResponseEntity.ok(batchService.ofBatch(batchCode).stream().map(DerivationView::of).toList());
    }

    @GetMapping("/concessions")
    public ResponseEntity<List<PlanDerivationService.ConcessionLine>> concessions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(derivationService.concessions(from, to));
    }

    // ---- Politique ----------------------------------------------------------------------------

    public record ApprovalRuleRequest(BigDecimal maxDiscountPercentWithoutApproval, BigDecimal maxImpactWithoutApproval,
                                      BigDecimal maxDiscountPercentAllowed) {
    }

    public record ApprovalRuleView(String ruleCode, String name, BigDecimal maxDiscountPercentWithoutApproval,
                                   BigDecimal maxImpactWithoutApproval, BigDecimal maxDiscountPercentAllowed, Instant updatedAt) {
        static ApprovalRuleView of(PlanDerivationApprovalRule r) {
            return new ApprovalRuleView(r.getRuleCode(), r.getName(), r.getMaxDiscountPercentWithoutApproval(),
                    r.getMaxImpactWithoutApproval(), r.getMaxDiscountPercentAllowed(), r.getUpdatedAt());
        }
    }

    public record FloorPriceRequest(BigDecimal floorPrice) {
    }

    public record FloorPriceView(Long versionId, String planCode, Integer versionNumber, BigDecimal floorPrice) {
    }

    @GetMapping("/policy/approval-rule")
    public ResponseEntity<ApprovalRuleView> approvalRule() {
        return ResponseEntity.ok(ApprovalRuleView.of(derivationService.approvalRule()));
    }

    @PutMapping("/policy/approval-rule")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApprovalRuleView> updateApprovalRule(@RequestBody ApprovalRuleRequest request) {
        return ResponseEntity.ok(ApprovalRuleView.of(derivationService.updateApprovalRule(request.maxDiscountPercentWithoutApproval(),
                request.maxImpactWithoutApproval(), request.maxDiscountPercentAllowed())));
    }

    /** Le plancher d'une version de catalogue · un corps vide ou un plancher nul le retire. */
    @PutMapping("/policy/plan-versions/{versionId}/floor-price")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<FloorPriceView> setFloorPrice(@PathVariable Long versionId, @RequestBody(required = false) FloorPriceRequest request) {
        PlanVersion version = derivationService.setFloorPrice(versionId, request == null ? null : request.floorPrice());
        return ResponseEntity.ok(new FloorPriceView(version.getId(), version.getPlan().getCode(), version.getVersionNumber(), version.getFloorPrice()));
    }

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null ? "STAFF" : authentication.getName();
    }
}
