package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.service.support.BillingTaxRuleResolver;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.payment.dto.request.CreateWalletHoldRequest;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
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
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.repository.PassEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.repository.PassTransactionRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class PassCreationOperator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PassRepository passRepository;
    private final PassEntitlementRepository passEntitlementRepository;
    private final PassTransactionRepository passTransactionRepository;
    private final EntitlementDefinitionRepository entitlementDefinitionRepository;
    private final PassPlanEntitlementRepository planEntitlementRepository;
    private final PlanVersionRepository planVersionRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final SubscriptionOwnerResolver ownerResolver;
    private final OutboxService outboxService;
    private final @Lazy EntitlementService entitlementService;
    private final @Lazy SubscriptionService subscriptionService;
    private final PassCodeFactory codeFactory;
    private final PassPlanResolver planResolver;
    private final PassPeriodCalculator periodCalculator;
    private final PassBillingSupport billingSupport;
    private final @Lazy PassLifecycleOperator lifecycleOperator;
    private final PassEventWriter eventWriter;
    private final PassEmailNotifier emailNotifier;
    private final BillingTaxRuleResolver taxRuleResolver;
    private final WalletService walletService;
    private final WalletHoldService walletHoldService;

    public Pass create(CreatePassRequest request) {
        if (!StringUtils.hasText(request.ownerCode())) {
            throw new BadRequestException("Owner code is required");
        }
        SubscriptionOwnerResolver.Owner owner = ownerResolver.resolve(request.ownerType(), request.ownerCode().trim());
        Subscription subscription = StringUtils.hasText(request.subscriptionNumber())
                ? subscriptionService.getForService(request.subscriptionNumber())
                : null;
        PlanVersion planVersion = request.planVersionId() == null
                ? null
                : planVersionRepository.findById(request.planVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan version not found"));

        Pass pass = passRepository.save(Pass.builder()
                .passNumber(sequenceGenerator.next("pass"))
                .passType(request.passType())
                .ownerType(request.ownerType())
                .ownerCode(owner.code())
                .subscription(subscription)
                .planVersion(planVersion)
                .status(PassStatus.ACTIVE)
                .name(request.name().trim())
                .description(trim(request.description()))
                .validFrom(parseInstant(request.validFrom()))
                .validUntil(parseInstant(request.validUntil()))
                .transferable(Boolean.TRUE.equals(request.transferable()))
                .shareable(Boolean.TRUE.equals(request.shareable()))
                .maxUses(request.maxUses())
                .metadataJson(trim(request.metadataJson()))
                .build());

        if (request.entitlements() == null || request.entitlements().isEmpty()) {
            throw new BadRequestException("At least one pass entitlement is required");
        }
        request.entitlements().forEach(entitlement -> passEntitlementRepository.save(toPassEntitlement(pass, entitlement)));
        passTransactionRepository.save(PassTransaction.builder()
                .pass(pass)
                .transactionType("ISSUED")
                .referenceType("PASS")
                .referenceId(pass.getPassNumber())
                .build());
        entitlementService.grantForPass(pass);
        passRepository.save(pass);
        outboxService.publish(
                "CONTRACT_GENERATION_REQUESTED",
                "PASS",
                pass.getPassNumber(),
                java.util.Map.of("sourceType", "PASS", "sourceId", pass.getId())
        );
        return pass;
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

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static final DateTimeFormatter LOCAL_FLEXIBLE = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart().appendPattern(":ss").optionalEnd()
            .toFormatter();

    private static final ZoneId APP_ZONE = ZoneId.of("Africa/Lagos");

    private Instant parseInstant(String value) {
        if (!StringUtils.hasText(value)) return null;
        String v = value.trim();
        try { return Instant.parse(v); } catch (Exception ignored) {}
        try { return OffsetDateTime.parse(v).toInstant(); } catch (Exception ignored) {}
        try { return LocalDateTime.parse(v, LOCAL_FLEXIBLE).atZone(APP_ZONE).toInstant(); } catch (Exception ignored) {}
        throw new BadRequestException("Invalid date-time format: " + v);
    }

    // ── Plan-based purchase ───────────────────────────────────────

    public Pass createFromPlan(String planCode, CreatePassPurchaseRequest request) {
        PassPlanVersion version = planResolver.resolveVersion(planCode, request.planVersionId());
        PassPlanPrice price = planResolver.resolvePrice(version, request.currency());

        SubscriptionOwnerResolver.Owner owner = ownerResolver.resolve(
                request.ownerType(), request.ownerCode());

        PriceAmounts amounts = calculateAmounts(price);

        Instant validFrom = request.validFrom() != null
                ? parseInstant(request.validFrom())
                : Instant.now();
        Instant validUntil = periodCalculator.periodEnd(
                validFrom, version.getDuration(), version.getDurationUnit());

        PassStatus initialStatus = amounts.total().signum() > 0
                ? PassStatus.PENDING_ACTIVATION
                : PassStatus.ACTIVE;

        Pass pass = passRepository.save(Pass.builder()
                .passNumber(codeFactory.nextPassNumber(request.ownerType(), version))
                .passType(version.getPlan().getPassType())
                .ownerType(request.ownerType())
                .ownerCode(owner.code())
                .passVersion(version)
                .status(initialStatus)
                .name(version.getName())
                .description(version.getDescription())
                .validFrom(validFrom)
                .validUntil(validUntil)
                .transferable(Boolean.FALSE)
                .shareable(Boolean.FALSE)
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
        createDepositHold(pass, price, owner);

        if (amounts.total().signum() > 0) {
            billingSupport.createAndInvoice(pass);
        } else {
            lifecycleOperator.activate(pass, "Auto-activation gratuite", "SYSTEM");
            return pass;
        }

        eventWriter.writeHistory(pass, null, initialStatus, "Création", "SYSTEM");
        eventWriter.writeEvent(pass, PassEventType.PASS_CREATED, null);
        emailNotifier.notifyCreated(pass);

        passRepository.save(pass);

        return pass;
    }

    private void copyPlanEntitlements(Pass pass, PassPlanVersion version,
                                       Instant validFrom, Instant validUntil) {
        List<PassPlanEntitlement> planEntitlements =
                planEntitlementRepository.findAllByPassVersion(version);
        planEntitlements.forEach(pe -> passEntitlementRepository.save(
                PassEntitlement.builder()
                        .pass(pass)
                        .entitlementDefinition(pe.getEntitlementDefinition())
                        .quantity(pe.getQuantity())
                        .unlimited(pe.getUnlimited())
                        .validFrom(validFrom)
                        .validUntil(pe.getValidForDays() != null
                                ? validFrom.plus(pe.getValidForDays(), ChronoUnit.DAYS)
                                : validUntil)
                        .build()
        ));
    }

    private PriceAmounts calculateAmounts(PassPlanPrice price) {
        BigDecimal base = nonNegative(price.getAmount());
        BigDecimal setup = nonNegative(price.getSetupFee());
        BigDecimal deposit = nonNegative(price.getDepositAmount());
        BigDecimal taxable = money(base.add(setup));
        BillingTaxRuleResolver.TaxProfile tax = taxRuleResolver.defaultTaxProfile();

        if (Boolean.TRUE.equals(price.getTaxIncluded())) {
            BigDecimal factor = taxFactor(tax.vatRate(), tax.additionalCentRate());
            BigDecimal subtotal = money(taxable.divide(factor, 8, RoundingMode.HALF_UP));
            BigDecimal taxAmt = money(taxable.subtract(subtotal));
            return new PriceAmounts(subtotal, taxAmt, money(taxable.add(deposit)));
        }
        BigDecimal vatAmt = percentage(taxable, tax.vatRate());
        BigDecimal centAmt = percentage(vatAmt, tax.additionalCentRate());
        BigDecimal taxAmt = money(vatAmt.add(centAmt));
        return new PriceAmounts(taxable, taxAmt, money(taxable.add(taxAmt).add(deposit)));
    }

    private void createDepositHold(Pass pass, PassPlanPrice price,
                                    SubscriptionOwnerResolver.Owner owner) {
        if (price.getDepositAmount() == null || price.getDepositAmount().signum() <= 0) return;
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

    private BigDecimal taxFactor(BigDecimal vatRate, BigDecimal additionalCentRate) {
        BigDecimal vat = rateFactor(vatRate);
        BigDecimal cent = rateFactor(additionalCentRate);
        return BigDecimal.ONE.add(vat).add(vat.multiply(cent));
    }

    private BigDecimal rateFactor(BigDecimal rate) {
        if (rate == null || rate.signum() == 0) return BigDecimal.ZERO;
        return rate.divide(HUNDRED, 8, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal rate) {
        if (rate == null || rate.signum() == 0) return BigDecimal.ZERO;
        return money(amount.multiply(rate).divide(HUNDRED, 4, RoundingMode.HALF_UP));
    }

    private BigDecimal nonNegative(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        if (v.signum() < 0) throw new BadRequestException("Pass price amounts cannot be negative");
        return v;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    private record PriceAmounts(BigDecimal subtotal, BigDecimal tax, BigDecimal total) {}
}
