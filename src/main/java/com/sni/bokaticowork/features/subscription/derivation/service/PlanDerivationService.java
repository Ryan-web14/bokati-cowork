package com.sni.bokaticowork.features.subscription.derivation.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivation;
import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivationApprovalRule;
import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivationDelta;
import com.sni.bokaticowork.features.subscription.derivation.repository.PlanDerivationApprovalRuleRepository;
import com.sni.bokaticowork.features.subscription.derivation.repository.PlanDerivationDeltaRepository;
import com.sni.bokaticowork.features.subscription.derivation.repository.PlanDerivationRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanBenefitRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanPriceRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanBenefit;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanEntitlement;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.PlanPriceAmountCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Deriver la version de plan, pas le plan.
 *
 * <p>Une derivation clone la version de catalogue de l'abonnement en une version privee, y applique
 * ce qui a ete negocie, chiffre l'ecart ligne par ligne, et bascule l'abonnement dessus. Les
 * moteurs existants · facturation, droits, periodes · lisent une version de plan et ne savent pas
 * qu'elle est privee. C'est tout l'interet : rien d'autre ne change.</p>
 *
 * <p>Ce qui evite la derive tient en cinq regles, et elles sont ici. Le lien vers la source est
 * garde. La derivation est versionnee, jamais modifiee. Elle est datee et peut expirer. Son
 * comportement au renouvellement est explicite. Et au-dela d'un seuil, elle attend un visa · en
 * dessous d'un plancher, elle est refusee meme visee.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanDerivationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PlanDerivationRepository derivationRepository;
    private final PlanDerivationDeltaRepository deltaRepository;
    private final PlanDerivationApprovalRuleRepository approvalRuleRepository;
    private final PlanVersionRepository planVersionRepository;
    private final PlanPriceRepository planPriceRepository;
    private final PlanEntitlementRepository planEntitlementRepository;
    private final PlanBenefitRepository planBenefitRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanPriceAmountCalculator amountCalculator;
    private final SequenceGeneratorFacade sequenceGenerator;

    // -----------------------------------------------------------------------------------------
    // Ce qu'on negocie
    // -----------------------------------------------------------------------------------------

    /**
     * Ce qui change par rapport au catalogue · tout est facultatif, ce qui n'est pas dit reste.
     *
     * @param price               prix de la periode, sur le cycle de l'abonnement
     * @param setupFee            frais d'entree
     * @param trialDays           jours d'essai
     * @param commitmentMonths    engagement
     * @param entitlementQuantities quantites par code de droit · nul pour illimite
     * @param includedBenefits    titres d'avantages a inclure en plus
     */
    public record Spec(
            BigDecimal price,
            BigDecimal setupFee,
            Integer trialDays,
            Integer commitmentMonths,
            Map<String, BigDecimal> entitlementQuantities,
            List<String> unlimitedEntitlements,
            List<String> includedBenefits,
            PlanDerivation.Reason reason,
            String reasonDetails,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            PlanDerivation.RenewalBehaviour renewalBehaviour,
            Integer revertAfterPeriods,
            Boolean promotionsAllowed
    ) {
    }

    /** Ce que la simulation dit · avant d'ecrire quoi que ce soit. */
    public record Preview(
            String subscriptionNumber,
            String catalogueVersion,
            BigDecimal cataloguePrice,
            BigDecimal derivedPrice,
            BigDecimal discountPercent,
            BigDecimal totalImpact,
            String currency,
            List<DeltaLine> deltas,
            boolean requiresApproval,
            String blockingReason
    ) {
        public boolean allowed() {
            return blockingReason == null;
        }
    }

    public record DeltaLine(PlanDerivationDelta.Type type, String target, String catalogueValue, String derivedValue, BigDecimal impact) {
    }

    // -----------------------------------------------------------------------------------------
    // Simuler
    // -----------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Preview simulate(String subscriptionNumber, Spec spec) {
        Subscription subscription = subscription(subscriptionNumber);
        PlanVersion source = catalogueSourceOf(subscription);
        return preview(subscription, source, spec);
    }

    /** Simule contre une version de catalogue donnee · pour la protection tarifaire, avant de basculer. */
    @Transactional(readOnly = true)
    public Preview simulateAgainst(Subscription subscription, PlanVersion catalogueVersion, Spec spec) {
        return preview(subscription, catalogueVersion, spec);
    }

    private Preview preview(Subscription subscription, PlanVersion source, Spec spec) {
        PlanPrice cataloguePrice = priceFor(source, subscription.getBillingCycle());
        BigDecimal catalogueAmount = cataloguePrice == null ? BigDecimal.ZERO : cataloguePrice.getAmount();
        BigDecimal derivedAmount = spec.price() == null ? catalogueAmount : spec.price().setScale(4, RoundingMode.HALF_UP);
        List<DeltaLine> deltas = new ArrayList<>();

        if (spec.price() != null && derivedAmount.compareTo(catalogueAmount) != 0) {
            deltas.add(new DeltaLine(PlanDerivationDelta.Type.PRICE, subscription.getBillingCycle().name(),
                    plain(catalogueAmount), plain(derivedAmount), catalogueAmount.subtract(derivedAmount)));
        }
        if (spec.setupFee() != null && cataloguePrice != null && spec.setupFee().compareTo(nonNull(cataloguePrice.getSetupFee())) != 0) {
            deltas.add(new DeltaLine(PlanDerivationDelta.Type.SETUP_FEE, subscription.getBillingCycle().name(),
                    plain(cataloguePrice.getSetupFee()), plain(spec.setupFee()), nonNull(cataloguePrice.getSetupFee()).subtract(spec.setupFee())));
        }
        if (spec.trialDays() != null && cataloguePrice != null && !spec.trialDays().equals(cataloguePrice.getTrialDays())) {
            deltas.add(new DeltaLine(PlanDerivationDelta.Type.TRIAL, subscription.getBillingCycle().name(),
                    String.valueOf(cataloguePrice.getTrialDays()), String.valueOf(spec.trialDays()), BigDecimal.ZERO));
        }
        if (spec.commitmentMonths() != null && cataloguePrice != null && !spec.commitmentMonths().equals(cataloguePrice.getCommitmentMonths())) {
            deltas.add(new DeltaLine(PlanDerivationDelta.Type.COMMITMENT, subscription.getBillingCycle().name(),
                    String.valueOf(cataloguePrice.getCommitmentMonths()), String.valueOf(spec.commitmentMonths()), BigDecimal.ZERO));
        }
        List<PlanEntitlement> entitlements = planEntitlementRepository.findAllByPlanVersionOrderByPriorityAsc(source.getId());
        if (spec.entitlementQuantities() != null) {
            spec.entitlementQuantities().forEach((code, quantity) -> entitlements.stream()
                    .filter(e -> e.getEntitlementDefinition().getCode().equalsIgnoreCase(code)).findFirst()
                    .ifPresentOrElse(e -> {
                        if (e.getQuantity() == null || quantity == null || e.getQuantity().compareTo(quantity) != 0) {
                            deltas.add(new DeltaLine(PlanDerivationDelta.Type.ENTITLEMENT, code,
                                    Boolean.TRUE.equals(e.getUnlimited()) ? "illimité" : plain(e.getQuantity()), plain(quantity), BigDecimal.ZERO));
                        }
                    }, () -> deltas.add(new DeltaLine(PlanDerivationDelta.Type.ENTITLEMENT, code, "absent", plain(quantity), BigDecimal.ZERO))));
        }
        if (spec.unlimitedEntitlements() != null) {
            spec.unlimitedEntitlements().forEach(code -> deltas.add(new DeltaLine(PlanDerivationDelta.Type.ENTITLEMENT, code,
                    entitlements.stream().filter(e -> e.getEntitlementDefinition().getCode().equalsIgnoreCase(code)).findFirst()
                            .map(e -> Boolean.TRUE.equals(e.getUnlimited()) ? "illimité" : plain(e.getQuantity())).orElse("absent"),
                    "illimité", BigDecimal.ZERO)));
        }
        if (spec.includedBenefits() != null) {
            spec.includedBenefits().forEach(title -> deltas.add(new DeltaLine(PlanDerivationDelta.Type.ADDON_INCLUDED, title, "non inclus", "inclus", BigDecimal.ZERO)));
        }

        BigDecimal impact = deltas.stream().map(DeltaLine::impact).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountPercent = catalogueAmount.signum() == 0 ? BigDecimal.ZERO
                : catalogueAmount.subtract(derivedAmount).multiply(HUNDRED).divide(catalogueAmount, 4, RoundingMode.HALF_UP);

        String blocking = null;
        if (source.getFloorPrice() != null && derivedAmount.compareTo(source.getFloorPrice()) < 0) {
            blocking = "Prix plancher de la version de catalogue · " + plain(source.getFloorPrice()) + " " + currency(subscription, cataloguePrice)
                    + " · aucune dérivation en dessous, même approuvée";
        }
        PlanDerivationApprovalRule rule = approvalRuleRepository.findFirstByActiveTrueOrderByIdAsc().orElse(null);
        boolean requiresApproval = false;
        if (rule != null) {
            if (blocking == null && rule.getMaxDiscountPercentAllowed() != null && discountPercent.compareTo(rule.getMaxDiscountPercentAllowed()) > 0) {
                blocking = "Rabais de " + plain(discountPercent) + " % au-delà du maximum admis (" + plain(rule.getMaxDiscountPercentAllowed()) + " %)";
            }
            requiresApproval = discountPercent.compareTo(rule.getMaxDiscountPercentWithoutApproval()) > 0
                    || (rule.getMaxImpactWithoutApproval() != null && impact.compareTo(rule.getMaxImpactWithoutApproval()) > 0);
        }
        if (deltas.isEmpty() && blocking == null) {
            blocking = "Rien ne diffère du catalogue · une dérivation sans écart n'a pas d'objet";
        }
        return new Preview(subscription.getSubscriptionNumber(), source.getName() + " v" + source.getVersionNumber(),
                catalogueAmount, derivedAmount, discountPercent, impact, currency(subscription, cataloguePrice), deltas, requiresApproval, blocking);
    }

    // -----------------------------------------------------------------------------------------
    // Creer
    // -----------------------------------------------------------------------------------------

    /**
     * Cree la derivation · et l'applique tout de suite si aucun visa n'est requis.
     *
     * <p>La version privee est creee des maintenant, en brouillon, pour que le visa porte sur
     * quelque chose de concret et que l'approbation n'ait plus qu'a basculer. Une derivation deja
     * active sur l'abonnement est remplacee, pas modifiee : la nouvelle dit laquelle elle remplace.</p>
     */
    @Transactional
    public PlanDerivation create(String subscriptionNumber, Spec spec, String requestedBy) {
        return create(subscription(subscriptionNumber), spec, requestedBy, null);
    }

    @Transactional
    public PlanDerivation create(Subscription subscription, Spec spec, String requestedBy, String batchCode) {
        if (spec.reason() == null) {
            throw new BadRequestException("Une dérivation a un motif · négociation, geste commercial, partenariat, pilote, protection, fidélité ou correction");
        }
        if (spec.renewalBehaviour() == PlanDerivation.RenewalBehaviour.REVERT_AFTER_PERIODS
                && (spec.revertAfterPeriods() == null || spec.revertAfterPeriods() < 1)) {
            throw new BadRequestException("Retour au catalogue après N périodes · indiquez N");
        }
        PlanVersion source = catalogueSourceOf(subscription);
        Preview preview = preview(subscription, source, spec);
        if (!preview.allowed()) {
            throw new ConflictException("derivation", preview.blockingReason());
        }

        PlanVersion derived = cloneAsPrivate(subscription, source, spec);
        Optional<PlanDerivation> current = derivationRepository.findFirstBySubscription_IdAndStatus(subscription.getId(), PlanDerivation.Status.ACTIVE);

        PlanDerivation derivation = derivationRepository.save(PlanDerivation.builder()
                .derivationCode(sequenceGenerator.next("plan_derivation"))
                .subscription(subscription)
                .sourcePlanVersion(source)
                .derivedPlanVersion(derived)
                .status(preview.requiresApproval() ? PlanDerivation.Status.PENDING_APPROVAL : PlanDerivation.Status.DRAFT)
                .reason(spec.reason())
                .reasonDetails(trim(spec.reasonDetails()))
                .effectiveFrom(spec.effectiveFrom() == null ? LocalDate.now() : spec.effectiveFrom())
                .effectiveTo(spec.effectiveTo())
                .renewalBehaviour(spec.renewalBehaviour() == null ? PlanDerivation.RenewalBehaviour.KEEP : spec.renewalBehaviour())
                .revertAfterPeriods(spec.revertAfterPeriods())
                .promotionsAllowed(Boolean.TRUE.equals(spec.promotionsAllowed()))
                .totalImpactAmount(preview.totalImpact())
                .discountPercent(preview.discountPercent())
                .currency(preview.currency())
                .requestedBy(requestedBy)
                .supersedesDerivationId(current.map(PlanDerivation::getId).orElse(null))
                .batchCode(batchCode)
                .build());
        for (DeltaLine line : preview.deltas()) {
            deltaRepository.save(PlanDerivationDelta.builder().derivation(derivation).deltaType(line.type())
                    .targetCode(line.target()).catalogueValue(line.catalogueValue()).derivedValue(line.derivedValue())
                    .impactAmount(line.impact()).build());
        }
        if (!preview.requiresApproval()) {
            apply(derivation);
        }
        return derivation;
    }

    /** Approuve · par un autre que le demandeur · et applique. */
    @Transactional
    public PlanDerivation approve(String derivationCode, String approvedBy) {
        PlanDerivation derivation = get(derivationCode);
        if (derivation.getStatus() != PlanDerivation.Status.PENDING_APPROVAL) {
            throw new BadRequestException("Cette dérivation n'attend pas de visa · elle est " + derivation.getStatus());
        }
        if (!StringUtils.hasText(approvedBy) || approvedBy.trim().equalsIgnoreCase(derivation.getRequestedBy())) {
            throw new ConflictException("derivation", "le visa doit venir d'une autre personne que le demandeur");
        }
        derivation.setApprovedBy(approvedBy.trim());
        derivation.setApprovedAt(Instant.now());
        apply(derivation);
        return derivationRepository.save(derivation);
    }

    @Transactional
    public PlanDerivation reject(String derivationCode, String reason, String rejectedBy) {
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Un refus se motive");
        }
        PlanDerivation derivation = get(derivationCode);
        if (derivation.getStatus() != PlanDerivation.Status.PENDING_APPROVAL && derivation.getStatus() != PlanDerivation.Status.DRAFT) {
            throw new BadRequestException("Cette dérivation ne peut plus être refusée · elle est " + derivation.getStatus());
        }
        derivation.setStatus(PlanDerivation.Status.REJECTED);
        derivation.setRejectionReason(reason.trim());
        derivation.setEndedAt(Instant.now());
        PlanVersion derived = derivation.getDerivedPlanVersion();
        derived.setStatus(PlanStatus.ARCHIVED);
        planVersionRepository.save(derived);
        return derivationRepository.save(derivation);
    }

    /**
     * Bascule l'abonnement sur sa version privee et recalcule ses montants.
     *
     * <p>La derivation active precedente passe SUPERSEDED · elle reste lisible, avec sa date de
     * fin. « Quel prix appliquions-nous en mars » a toujours une reponse.</p>
     */
    private void apply(PlanDerivation derivation) {
        Subscription subscription = derivation.getSubscription();
        derivationRepository.findFirstBySubscription_IdAndStatus(subscription.getId(), PlanDerivation.Status.ACTIVE)
                .filter(previous -> !previous.getId().equals(derivation.getId()))
                .ifPresent(previous -> {
                    previous.setStatus(PlanDerivation.Status.SUPERSEDED);
                    previous.setEndedAt(Instant.now());
                    derivationRepository.save(previous);
                    PlanVersion old = previous.getDerivedPlanVersion();
                    old.setStatus(PlanStatus.ARCHIVED);
                    old.setEffectiveTo(LocalDate.now());
                    planVersionRepository.save(old);
                });

        PlanVersion derived = derivation.getDerivedPlanVersion();
        derived.setStatus(PlanStatus.ACTIVE);
        derived.setEffectiveFrom(derivation.getEffectiveFrom());
        derived.setEffectiveTo(derivation.getEffectiveTo());
        planVersionRepository.save(derived);

        switchSubscriptionTo(subscription, derived);

        derivation.setStatus(PlanDerivation.Status.ACTIVE);
        derivation.setAppliedAt(Instant.now());
        derivationRepository.save(derivation);
        log.info("Abonnement {} · derivation {} appliquee ({} · impact {} {})", subscription.getSubscriptionNumber(),
                derivation.getDerivationCode(), derivation.getReason(), plain(derivation.getTotalImpactAmount()), derivation.getCurrency());
    }

    // -----------------------------------------------------------------------------------------
    // Revenir au catalogue
    // -----------------------------------------------------------------------------------------

    /** Retour a la version de catalogue active du plan · la derivation expire, elle ne disparait pas. */
    @Transactional
    public PlanDerivation revert(String subscriptionNumber, String because) {
        Subscription subscription = subscription(subscriptionNumber);
        PlanDerivation active = derivationRepository.findFirstBySubscription_IdAndStatus(subscription.getId(), PlanDerivation.Status.ACTIVE)
                .orElseThrow(() -> new ConflictException("derivation", "cet abonnement n'a pas de dérivation active"));
        return revert(active, because);
    }

    @Transactional
    public PlanDerivation revert(PlanDerivation active, String because) {
        Subscription subscription = active.getSubscription();
        PlanVersion catalogue = planVersionRepository
                .findFirstByPlanAndStatusOrderByVersionNumberDesc(active.getSourcePlanVersion().getPlan().getId(), PlanStatus.ACTIVE.name())
                .orElse(active.getSourcePlanVersion());
        switchSubscriptionTo(subscription, catalogue);

        PlanVersion derived = active.getDerivedPlanVersion();
        derived.setStatus(PlanStatus.ARCHIVED);
        derived.setEffectiveTo(LocalDate.now());
        planVersionRepository.save(derived);

        active.setStatus(PlanDerivation.Status.EXPIRED);
        active.setEndedAt(Instant.now());
        active.setReasonDetails(join(active.getReasonDetails(), "Fin · " + because));
        log.info("Abonnement {} · retour au catalogue ({})", subscription.getSubscriptionNumber(), because);
        return derivationRepository.save(active);
    }

    /**
     * Au renouvellement · ce que la derivation a decide d'avance.
     *
     * <p>Appele par le cycle de vie avant de chiffrer la nouvelle periode. Une derivation echue,
     * ou qui a dit « retour au catalogue » ou « retour apres N periodes », rend l'abonnement au
     * catalogue ici · sans que personne n'ait a s'en souvenir.</p>
     */
    @Transactional
    public void beforeRenewal(Subscription subscription) {
        derivationRepository.findFirstBySubscription_IdAndStatus(subscription.getId(), PlanDerivation.Status.ACTIVE)
                .ifPresent(active -> {
                    LocalDate today = LocalDate.now();
                    if (active.expiredOn(today)) {
                        revert(active, "échéance du " + active.getEffectiveTo());
                        return;
                    }
                    switch (active.getRenewalBehaviour()) {
                        case KEEP -> {
                            active.setPeriodsApplied(active.getPeriodsApplied() + 1);
                            derivationRepository.save(active);
                        }
                        case REVERT_TO_CATALOGUE -> revert(active, "retour au catalogue au renouvellement");
                        case REVERT_AFTER_PERIODS -> {
                            active.setPeriodsApplied(active.getPeriodsApplied() + 1);
                            if (active.getPeriodsApplied() >= active.getRevertAfterPeriods()) {
                                revert(active, "retour au catalogue après " + active.getRevertAfterPeriods() + " période(s)");
                            } else {
                                derivationRepository.save(active);
                            }
                        }
                    }
                });
    }

    /** Une promotion par-dessus une negociation cumule deux concessions · sauf si la derivation l'admet. */
    @Transactional(readOnly = true)
    public boolean promotionsAllowed(Subscription subscription) {
        return derivationRepository.findFirstBySubscription_IdAndStatus(subscription.getId(), PlanDerivation.Status.ACTIVE)
                .map(d -> Boolean.TRUE.equals(d.getPromotionsAllowed()))
                .orElse(true);
    }

    // -----------------------------------------------------------------------------------------
    // Lire
    // -----------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PlanDerivation get(String derivationCode) {
        return derivationRepository.findByDerivationCode(derivationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Dérivation introuvable"));
    }

    @Transactional(readOnly = true)
    public List<PlanDerivationDelta> deltas(String derivationCode) {
        return deltaRepository.findByDerivation_IdOrderByIdAsc(get(derivationCode).getId());
    }

    @Transactional(readOnly = true)
    public List<PlanDerivation> ofSubscription(String subscriptionNumber) {
        return derivationRepository.findBySubscription_IdOrderByCreatedAtDesc(subscription(subscriptionNumber).getId());
    }

    @Transactional(readOnly = true)
    public Page<PlanDerivation> list(List<PlanDerivation.Status> statuses, Pageable pageable) {
        return statuses == null || statuses.isEmpty()
                ? derivationRepository.findAllByOrderByCreatedAtDesc(pageable)
                : derivationRepository.findByStatusInOrderByCreatedAtDesc(statuses, pageable);
    }

    public record ConcessionLine(String requestedBy, PlanDerivation.Reason reason, long count, BigDecimal totalImpact) {
    }

    /** Les concessions par commercial et par motif sur une periode · ce qu'on ne pouvait pas dire avant. */
    @Transactional(readOnly = true)
    public List<ConcessionLine> concessions(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        return derivationRepository.concessionsBetween(start, end).stream()
                .map(row -> new ConcessionLine(String.valueOf(row[0]), PlanDerivation.Reason.valueOf(String.valueOf(row[1])),
                        ((Number) row[2]).longValue(), new BigDecimal(String.valueOf(row[3]))))
                .toList();
    }

    // -----------------------------------------------------------------------------------------
    // Le clone
    // -----------------------------------------------------------------------------------------

    /**
     * Copie la version de catalogue en version privee et y applique ce qui est negocie.
     *
     * <p>Tout est copie, pas seulement ce qui change : la version privee doit se suffire, parce
     * que le catalogue peut evoluer et que l'abonne garde ce qu'on lui a promis, pas ce que le
     * catalogue devient.</p>
     */
    private PlanVersion cloneAsPrivate(Subscription subscription, PlanVersion source, Spec spec) {
        PlanVersion derived = planVersionRepository.save(PlanVersion.builder()
                .plan(source.getPlan())
                .versionNumber(planVersionRepository.maxVersionNumber(source.getPlan().getId()) + 1)
                .name(source.getName() + " · " + subscription.getSubscriptionNumber())
                .description(source.getDescription())
                .status(PlanStatus.DRAFT)
                .termsJson(source.getTermsJson())
                .scope(PlanVersion.Scope.SUBSCRIPTION)
                .ownerSubscriptionId(subscription.getId())
                .derivedFromVersionId(source.getId())
                .build());

        for (PlanPrice price : planPriceRepository.findAllByPlanVersion(source.getId())) {
            boolean thisCycle = price.getBillingCycle() == subscription.getBillingCycle();
            planPriceRepository.save(PlanPrice.builder()
                    .planVersion(derived)
                    .billingCycle(price.getBillingCycle())
                    .currency(price.getCurrency())
                    .amount(thisCycle && spec.price() != null ? spec.price() : price.getAmount())
                    .setupFee(thisCycle && spec.setupFee() != null ? spec.setupFee() : price.getSetupFee())
                    .depositAmount(price.getDepositAmount())
                    .taxIncluded(price.getTaxIncluded())
                    .trialDays(thisCycle && spec.trialDays() != null ? spec.trialDays() : price.getTrialDays())
                    .commitmentMonths(thisCycle && spec.commitmentMonths() != null ? spec.commitmentMonths() : price.getCommitmentMonths())
                    .taxCode(price.getTaxCode())
                    .build());
        }
        for (PlanEntitlement entitlement : planEntitlementRepository.findAllByPlanVersionOrderByPriorityAsc(source.getId())) {
            String code = entitlement.getEntitlementDefinition().getCode();
            boolean unlimited = spec.unlimitedEntitlements() != null && spec.unlimitedEntitlements().stream().anyMatch(code::equalsIgnoreCase);
            BigDecimal quantity = spec.entitlementQuantities() == null ? null : spec.entitlementQuantities().entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(code)).map(Map.Entry::getValue).findFirst().orElse(null);
            planEntitlementRepository.save(PlanEntitlement.builder()
                    .planVersion(derived)
                    .entitlementDefinition(entitlement.getEntitlementDefinition())
                    .quantity(unlimited ? null : quantity != null ? quantity : entitlement.getQuantity())
                    .unlimited(unlimited || (quantity == null && Boolean.TRUE.equals(entitlement.getUnlimited())))
                    .rolloverAllowed(entitlement.getRolloverAllowed())
                    .rolloverLimit(entitlement.getRolloverLimit())
                    .validForDays(entitlement.getValidForDays())
                    .priority(entitlement.getPriority())
                    .restrictionsJson(entitlement.getRestrictionsJson())
                    .build());
        }
        for (PlanBenefit benefit : planBenefitRepository.findAllByPlanVersionOrderByDisplayOrderAscCreatedAtAsc(source.getId())) {
            boolean forced = spec.includedBenefits() != null && spec.includedBenefits().stream().anyMatch(t -> t.equalsIgnoreCase(benefit.getTitle()));
            planBenefitRepository.save(PlanBenefit.builder()
                    .planVersion(derived)
                    .title(benefit.getTitle())
                    .description(benefit.getDescription())
                    .icon(benefit.getIcon())
                    .category(benefit.getCategory())
                    .displayOrder(benefit.getDisplayOrder())
                    .highlighted(benefit.getHighlighted())
                    .included(forced || Boolean.TRUE.equals(benefit.getIncluded()))
                    .metadataJson(benefit.getMetadataJson())
                    .build());
        }
        return derived;
    }

    /** Bascule et recalcule · les montants de l'abonnement suivent la version qu'il lit. */
    private void switchSubscriptionTo(Subscription subscription, PlanVersion version) {
        subscription.setPlanVersion(version);
        PlanPrice price = priceFor(version, subscription.getBillingCycle());
        if (price != null) {
            PlanPriceAmountCalculator.Amounts amounts = amountCalculator.compute(price.getAmount(), BigDecimal.ZERO, BigDecimal.ZERO, price.getTaxIncluded());
            subscription.setSubtotalAmount(amounts.subtotal());
            subscription.setTaxAmount(amounts.tax());
            subscription.setTotalAmount(amounts.total());
        }
        subscriptionRepository.save(subscription);
    }

    // -----------------------------------------------------------------------------------------

    /** La version de catalogue dont part la derivation · celle de l'abonne, ou la source de sa version privee. */
    PlanVersion catalogueSourceOf(Subscription subscription) {
        PlanVersion current = subscription.getPlanVersion();
        if (current == null) {
            throw new ConflictException("derivation", "cet abonnement n'a pas de version de plan");
        }
        if (!current.privateToSubscription()) {
            return current;
        }
        return planVersionRepository.findById(current.getDerivedFromVersionId())
                .orElseThrow(() -> new ConflictException("derivation", "la version privée de cet abonnement a perdu sa source"));
    }

    // ---- Politique : seuils et plancher ---------------------------------------------------------

    /** La regle de visa active · celle que la simulation applique. */
    @Transactional(readOnly = true)
    public PlanDerivationApprovalRule approvalRule() {
        return approvalRuleRepository.findFirstByActiveTrueOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("Aucune règle de visa active"));
    }

    /** Les seuils sont des donnees · on les modifie sans livrer. Un seuil nul se lit : pas de borne. */
    @Transactional
    public PlanDerivationApprovalRule updateApprovalRule(BigDecimal maxDiscountPercentWithoutApproval, BigDecimal maxImpactWithoutApproval,
                                                         BigDecimal maxDiscountPercentAllowed) {
        PlanDerivationApprovalRule rule = approvalRule();
        if (maxDiscountPercentWithoutApproval != null) {
            if (maxDiscountPercentWithoutApproval.signum() < 0 || maxDiscountPercentWithoutApproval.compareTo(HUNDRED) > 0) {
                throw new BadRequestException("Le seuil de visa est un pourcentage entre 0 et 100");
            }
            rule.setMaxDiscountPercentWithoutApproval(maxDiscountPercentWithoutApproval);
        }
        if (maxImpactWithoutApproval != null) {
            rule.setMaxImpactWithoutApproval(maxImpactWithoutApproval.signum() < 0 ? null : maxImpactWithoutApproval);
        }
        if (maxDiscountPercentAllowed != null) {
            if (maxDiscountPercentAllowed.signum() < 0 || maxDiscountPercentAllowed.compareTo(HUNDRED) > 0) {
                throw new BadRequestException("Le plafond de remise est un pourcentage entre 0 et 100");
            }
            if (maxDiscountPercentAllowed.compareTo(rule.getMaxDiscountPercentWithoutApproval()) < 0) {
                throw new BadRequestException("Le plafond de remise ne peut pas être sous le seuil de visa");
            }
            rule.setMaxDiscountPercentAllowed(maxDiscountPercentAllowed);
        }
        return approvalRuleRepository.save(rule);
    }

    /** Le plancher d'une version de catalogue · nul pour le retirer. Une version privee n'en a pas. */
    @Transactional
    public PlanVersion setFloorPrice(Long catalogueVersionId, BigDecimal floorPrice) {
        PlanVersion version = planVersionRepository.findById(catalogueVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version de plan introuvable"));
        if (version.privateToSubscription()) {
            throw new BadRequestException("Le plancher se pose sur une version de catalogue");
        }
        if (floorPrice != null && floorPrice.signum() < 0) {
            throw new BadRequestException("Le plancher ne peut pas être négatif");
        }
        version.setFloorPrice(floorPrice);
        return planVersionRepository.save(version);
    }

    private PlanPrice priceFor(PlanVersion version, BillingCycle cycle) {
        return planPriceRepository.findAllByPlanVersion(version.getId()).stream()
                .filter(p -> p.getBillingCycle() == cycle).findFirst().orElse(null);
    }

    private Subscription subscription(String number) {
        return subscriptionRepository.findBySubscriptionNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable"));
    }

    private String currency(Subscription subscription, PlanPrice price) {
        return StringUtils.hasText(subscription.getCurrency()) ? subscription.getCurrency() : price == null ? "XAF" : price.getCurrency();
    }

    private static BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String join(String existing, String more) {
        return StringUtils.hasText(existing) ? existing + " · " + more : more;
    }
}
