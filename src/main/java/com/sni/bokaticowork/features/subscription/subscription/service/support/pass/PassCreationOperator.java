package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.CreateWalletHoldRequest;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.repository.PassEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.repository.PassTransactionRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePassPurchaseRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePassRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.PassEntitlementRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassEntitlement;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanEntitlement;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.PassTransaction;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.PlanPriceAmountCalculator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriberKycLevelGuard;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Creation d'un pass, par vente depuis un plan ou par emission manuelle.
 *
 * <p>Les deux voies produisent le meme objet et doivent donc raconter la meme histoire. Ce n'etait
 * pas le cas : l'emission manuelle posait le statut {@code ACTIVE} a la main, sans passer par
 * l'activation, donc sans historique, sans evenement d'activation et sans echeancier de
 * renouvellement. La vente, elle, sortait avant d'ecrire sa trace de creation lorsque le pass etait
 * gratuit. Un meme objet, deux naissances, dont l'une laissait un pass actif que rien ne relatait.</p>
 *
 * <p>Les deux voies partagent desormais la meme fin : trace de creation ecrite <b>avant</b> toute
 * branche de prix, puis facturation si quelque chose est du, activation sinon. L'activation est le
 * seul endroit qui pose {@code ACTIVE}, et c'est elle qui accorde les droits, planifie le
 * renouvellement et demande le contrat.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PassCreationOperator {

    private static final ZoneId APP_ZONE = ZoneId.of("Africa/Lagos");

    private static final DateTimeFormatter LOCAL_FLEXIBLE = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart().appendPattern(":ss").optionalEnd()
            .toFormatter();

    private final PassRepository passRepository;
    private final PassEntitlementRepository passEntitlementRepository;
    private final PassTransactionRepository passTransactionRepository;
    private final EntitlementDefinitionRepository entitlementDefinitionRepository;
    private final PassPlanEntitlementRepository planEntitlementRepository;
    private final PlanVersionRepository planVersionRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final SubscriptionOwnerResolver ownerResolver;
    private final @Lazy SubscriptionService subscriptionService;
    private final PassCodeFactory codeFactory;
    private final PassPlanResolver planResolver;
    private final PassPeriodCalculator periodCalculator;
    private final PassBillingSupport billingSupport;
    private final @Lazy PassLifecycleOperator lifecycleOperator;
    private final PassEventWriter eventWriter;
    private final PassEmailNotifier emailNotifier;
    private final PlanPriceAmountCalculator priceCalculator;
    private final SubscriberKycLevelGuard kycGuard;
    private final WalletService walletService;
    private final WalletHoldService walletHoldService;

    // -----------------------------------------------------------------------------------------
    // Emission manuelle
    // -----------------------------------------------------------------------------------------

    public Pass create(CreatePassRequest request) {
        Pass replayed = replay(request.idempotencyKey());
        if (replayed != null) {
            return replayed;
        }
        if (!StringUtils.hasText(request.ownerCode())) {
            throw new BadRequestException("Owner code is required");
        }
        if (request.entitlements() == null || request.entitlements().isEmpty()) {
            throw new BadRequestException("At least one pass entitlement is required");
        }

        SubscriptionOwnerResolver.Owner owner = ownerResolver.resolve(request.ownerType(), request.ownerCode().trim());
        Subscription subscription = StringUtils.hasText(request.subscriptionNumber())
                ? subscriptionService.getForService(request.subscriptionNumber())
                : null;
        PlanVersion planVersion = request.planVersionId() == null
                ? null
                : planVersionRepository.findById(request.planVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan version not found"));
        kycGuard.require(requiredKycLevel(planVersion), owner, "pass");

        Pass pass = passRepository.save(Pass.builder()
                .passNumber(sequenceGenerator.next("pass"))
                .idempotencyKey(trim(request.idempotencyKey()))
                .passType(request.passType())
                .ownerType(request.ownerType())
                .ownerCode(owner.code())
                .subscription(subscription)
                .planVersion(planVersion)
                // Jamais ACTIVE ici : seule l'activation pose ce statut, et elle seule accorde les
                // droits, planifie le renouvellement et demande le contrat.
                .status(PassStatus.PENDING_ACTIVATION)
                .name(request.name().trim())
                .description(trim(request.description()))
                .validFrom(parseInstant(request.validFrom()))
                .validUntil(parseInstant(request.validUntil()))
                .transferable(Boolean.TRUE.equals(request.transferable()))
                .shareable(Boolean.TRUE.equals(request.shareable()))
                .maxUses(request.maxUses())
                .metadataJson(trim(request.metadataJson()))
                .build());

        request.entitlements().forEach(entitlement ->
                passEntitlementRepository.save(toPassEntitlement(pass, entitlement)));
        recordIssued(pass);
        writeCreationTrace(pass);

        return lifecycleOperator.activate(pass, "Emission manuelle", "ADMIN");
    }

    // -----------------------------------------------------------------------------------------
    // Vente depuis un plan
    // -----------------------------------------------------------------------------------------

    public Pass createFromPlan(String planCode, CreatePassPurchaseRequest request) {
        Pass replayed = replay(request.idempotencyKey());
        if (replayed != null) {
            return replayed;
        }

        PassPlanVersion version = planResolver.resolveVersion(planCode, request.planVersionId());
        PassPlanPrice price = planResolver.resolvePrice(version, request.currency());
        SubscriptionOwnerResolver.Owner owner = ownerResolver.resolve(request.ownerType(), request.ownerCode());
        kycGuard.require(version.getRequiredKycLevel(), owner, "pass plan");

        PlanPriceAmountCalculator.Amounts amounts = priceCalculator.compute(
                price.getAmount(), price.getSetupFee(), price.getDepositAmount(), price.getTaxIncluded());

        Instant validFrom = request.validFrom() != null ? parseInstant(request.validFrom()) : Instant.now();
        Instant validUntil = periodCalculator.periodEnd(validFrom, version.getDuration(), version.getDurationUnit());

        Pass pass = passRepository.save(Pass.builder()
                .passNumber(codeFactory.nextPassNumber(request.ownerType(), version))
                .idempotencyKey(trim(request.idempotencyKey()))
                .passType(version.getPlan().getPassType())
                .ownerType(request.ownerType())
                .ownerCode(owner.code())
                .passVersion(version)
                .status(PassStatus.PENDING_ACTIVATION)
                .name(version.getName())
                .description(version.getDescription())
                .validFrom(validFrom)
                .validUntil(validUntil)
                // Lus sur le plan, non forces a faux. Les y forcer rendait sans effet tout travail
                // sur le transfert et le partage, puisque l'attribut etait ecrase a l'achat.
                .transferable(Boolean.TRUE.equals(version.getTransferable()))
                .shareable(Boolean.TRUE.equals(version.getShareable()))
                .maxUses(version.getMaxUses())
                .autoRenew(Boolean.TRUE.equals(request.autoRenew())
                        && Boolean.TRUE.equals(version.getAutoRenewable()))
                .currency(price.getCurrency())
                .subtotalAmount(amounts.subtotal())
                .taxAmount(amounts.tax())
                .totalAmount(amounts.total())
                .metadataJson(trim(request.metadataJson()))
                .build());

        copyPlanEntitlements(pass, version, validFrom, validUntil);
        recordIssued(pass);
        createDepositHold(pass, price, owner);

        // Avant la branche de prix, et non apres : un pass gratuit sortait sans historique, sans
        // evenement de creation et sans courriel · offert, actif, et relate nulle part.
        writeCreationTrace(pass);

        if (amounts.total().signum() > 0) {
            billingSupport.createAndInvoice(pass);
            return passRepository.save(pass);
        }
        return lifecycleOperator.activate(pass, "Auto-activation gratuite", "SYSTEM");
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Rend le pass deja cree sous cette cle, s'il existe. Sans cela un double envoi de la demande
     * produisait deux pass et deux factures, ce que le titulaire ne decouvrait qu'en les recevant.
     */
    private Pass replay(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        return passRepository.findByIdempotencyKey(idempotencyKey.trim()).orElse(null);
    }

    private void writeCreationTrace(Pass pass) {
        eventWriter.writeHistory(pass, null, pass.getStatus(), "Création", "SYSTEM");
        eventWriter.writeEvent(pass, PassEventType.PASS_CREATED, null);
        emailNotifier.notifyCreated(pass);
    }

    private void recordIssued(Pass pass) {
        passTransactionRepository.save(PassTransaction.builder()
                .pass(pass)
                .transactionType("ISSUED")
                .referenceType("PASS")
                .referenceId(pass.getPassNumber())
                .build());
    }

    private Integer requiredKycLevel(PlanVersion planVersion) {
        if (planVersion == null || planVersion.getPlan() == null) {
            return null;
        }
        return planVersion.getPlan().getRequiredKycLevel();
    }

    private PassEntitlement toPassEntitlement(Pass pass, PassEntitlementRequest request) {
        EntitlementDefinition definition = entitlementDefinitionRepository.findByCodeIgnoreCase(request.entitlementCode())
                .orElseThrow(() -> new ResourceNotFoundException("Entitlement definition " + request.entitlementCode() + " not found"));
        boolean unlimited = Boolean.TRUE.equals(request.unlimited());
        if (!unlimited && request.quantity() == null) {
            throw new BadRequestException("Pass entitlement quantity is required when unlimited is false");
        }
        return PassEntitlement.builder()
                .pass(pass)
                .entitlementDefinition(definition)
                .quantity(unlimited ? null : request.quantity())
                .unlimited(unlimited)
                .validFrom(request.validFrom() == null ? pass.getValidFrom() : parseInstant(request.validFrom()))
                .validUntil(request.validUntil() == null ? pass.getValidUntil() : parseInstant(request.validUntil()))
                .build();
    }

    private void copyPlanEntitlements(Pass pass, PassPlanVersion version, Instant validFrom, Instant validUntil) {
        List<PassPlanEntitlement> planEntitlements = planEntitlementRepository.findAllByPassVersion(version);
        planEntitlements.forEach(planEntitlement -> passEntitlementRepository.save(
                PassEntitlement.builder()
                        .pass(pass)
                        .entitlementDefinition(planEntitlement.getEntitlementDefinition())
                        .quantity(planEntitlement.getQuantity())
                        .unlimited(planEntitlement.getUnlimited())
                        .validFrom(validFrom)
                        .validUntil(planEntitlement.getValidForDays() != null
                                ? validFrom.plus(planEntitlement.getValidForDays(), ChronoUnit.DAYS)
                                : validUntil)
                        .build()
        ));
    }

    private void createDepositHold(Pass pass, PassPlanPrice price, SubscriptionOwnerResolver.Owner owner) {
        if (price.getDepositAmount() == null || price.getDepositAmount().signum() <= 0) {
            return;
        }
        try {
            WalletResponse wallet = walletService.getOrCreate(
                    pass.getOwnerType().name(), owner.code(), price.getCurrency());
            walletHoldService.create(new CreateWalletHoldRequest(
                    wallet.walletNumber(), price.getDepositAmount(),
                    "PASS", pass.getPassNumber(), null, "SYSTEM"));
        } catch (Exception ex) {
            log.warn("Failed to place deposit hold for pass {}", pass.getPassNumber(), ex);
        }
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Instant parseInstant(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String candidate = value.trim();
        try {
            return Instant.parse(candidate);
        } catch (Exception ignored) {
            // format suivant
        }
        try {
            return OffsetDateTime.parse(candidate).toInstant();
        } catch (Exception ignored) {
            // format suivant
        }
        try {
            return LocalDateTime.parse(candidate, LOCAL_FLEXIBLE).atZone(APP_ZONE).toInstant();
        } catch (Exception ignored) {
            throw new BadRequestException("Invalid date-time format: " + candidate);
        }
    }
}
