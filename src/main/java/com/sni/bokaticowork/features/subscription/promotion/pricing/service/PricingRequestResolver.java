package com.sni.bokaticowork.features.subscription.promotion.pricing.service;

import com.sni.bokaticowork.features.subscription.promotion.pricing.dto.PricingSimulationRequest;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import com.sni.bokaticowork.features.subscription.repository.PassPlanPriceRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanVersionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlan;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPlanResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Complete une demande de tarification avec ce que le serveur sait deja.
 *
 * <p>Un client qui demande le prix d'un plan ne devrait pas avoir a dire dans quelle devise ce
 * plan est vendu : le catalogue le sait. La devise est donc deduite de la premiere ligne qui
 * designe un objet tarife, et seulement a defaut de la devise par defaut de l'etablissement.
 * L'envoyer explicitement reste possible et prime · rien ne change pour les appels existants.</p>
 *
 * <p>Sur les routes de l'espace client, l'abonne est celui qui est authentifie, jamais un champ du
 * corps · sans cela un membre pourrait demander les promotions nominatives d'un autre en changeant
 * un code dans sa requete.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PricingRequestResolver {

    private final SubscriptionPlanResolver planResolver;
    private final PassPlanRepository passPlanRepository;
    private final PassPlanVersionRepository passPlanVersionRepository;
    private final PassPlanPriceRepository passPlanPriceRepository;

    @Value("${bokati.billing.default-currency:XAF}")
    private String defaultCurrency;

    /** La demande, completee de sa devise quand elle n'en portait pas. */
    @Transactional(readOnly = true)
    public PricingSimulationRequest resolve(PricingSimulationRequest request) {
        return resolveFor(request, null, null);
    }

    /** La meme, avec l'abonne impose par la session · ce que le corps disait de lui est ignore. */
    @Transactional(readOnly = true)
    public PricingSimulationRequest resolveFor(PricingSimulationRequest request, String subscriberType, String subscriberCode) {
        String currency = StringUtils.hasText(request.currency())
                ? request.currency().trim().toUpperCase()
                : currencyOf(request).orElse(defaultCurrency);
        String type = StringUtils.hasText(subscriberType) ? subscriberType : request.subscriberType();
        String code = StringUtils.hasText(subscriberCode) ? subscriberCode : request.subscriberCode();
        return new PricingSimulationRequest(
                type, code, request.subscriberSegment(),
                request.kycLevel(), request.tenureMonths(), request.firstPurchase(), request.lines(),
                request.billingCycle(), request.channel(), request.paymentMethod(), request.locationCode(),
                currency, request.couponCodes(), request.evaluationDate(), request.promotionsAllowed());
    }

    /**
     * La devise du premier objet du catalogue reconnu dans les lignes.
     *
     * <p>Au pire on ne trouve rien · c'est une commodite, pas une regle : une reference inconnue
     * ne doit pas faire echouer une simulation, elle doit juste ne rien apprendre.</p>
     */
    Optional<String> currencyOf(PricingSimulationRequest request) {
        if (request.lines() == null) {
            return Optional.empty();
        }
        for (PricingSimulationRequest.Line line : request.lines()) {
            if (!StringUtils.hasText(line.code())) {
                continue;
            }
            Optional<String> found = switch (line.scope() == null ? TargetScope.LINE : line.scope()) {
                case PLAN -> planCurrency(line.code(), request.billingCycle());
                case PASS -> passCurrency(line.code());
                default -> Optional.empty();
            };
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private Optional<String> planCurrency(String planCode, String billingCycle) {
        try {
            PlanVersion version = planResolver.resolvePlanVersion(planCode, null);
            PlanPrice price = planResolver.resolvePrice(version, cycle(billingCycle));
            return Optional.ofNullable(price.getCurrency());
        } catch (RuntimeException ex) {
            log.debug("Devise indéterminée pour le plan {} · {}", planCode, ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> passCurrency(String passPlanCode) {
        try {
            PassPlan plan = passPlanRepository.findByCode(passPlanCode).orElse(null);
            if (plan == null) {
                return Optional.empty();
            }
            PassPlanVersion version = passPlanVersionRepository
                    .findFirstByPlanAndStatusOrderByVersionNumberDesc(plan, PlanStatus.ACTIVE).orElse(null);
            if (version == null) {
                return Optional.empty();
            }
            return passPlanPriceRepository.findAllByPassVersion(version).stream()
                    .map(PassPlanPrice::getCurrency)
                    .filter(StringUtils::hasText)
                    .findFirst();
        } catch (RuntimeException ex) {
            log.debug("Devise indéterminée pour le pass {} · {}", passPlanCode, ex.getMessage());
            return Optional.empty();
        }
    }

    /** Un rythme illisible ne fait pas echouer la deduction · on prend alors le premier prix du plan. */
    private BillingCycle cycle(String billingCycle) {
        if (!StringUtils.hasText(billingCycle)) {
            return null;
        }
        try {
            return BillingCycle.valueOf(billingCycle.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
