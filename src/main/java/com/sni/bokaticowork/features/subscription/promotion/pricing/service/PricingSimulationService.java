package com.sni.bokaticowork.features.subscription.promotion.pricing.service;

import com.sni.bokaticowork.features.subscription.promotion.pricing.dto.PricingSimulationRequest;
import com.sni.bokaticowork.features.subscription.promotion.pricing.dto.PricingSimulationResponse;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingContext;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Traduit une demande de simulation en contexte de moteur, et rien d'autre. */
@Service
@RequiredArgsConstructor
public class PricingSimulationService {

    private final PricingEngine pricingEngine;

    @Transactional(readOnly = true)
    public PricingSimulationResponse simulate(PricingSimulationRequest request) {
        return PricingSimulationResponse.from(pricingEngine.evaluate(toContext(request)));
    }

    public PricingContext toContext(PricingSimulationRequest request) {
        List<PricingContext.PricingLine> lines = request.lines().stream()
                .map(line -> new PricingContext.PricingLine(
                        line.reference(), line.scope(), line.code(), line.categoryCode(),
                        line.label(), line.quantity(), line.unitPrice(), line.setupFee()))
                .toList();
        return new PricingContext(
                request.subscriberType(),
                request.subscriberCode(),
                request.subscriberSegment(),
                request.kycLevel(),
                request.tenureMonths(),
                request.firstPurchase(),
                lines,
                request.billingCycle(),
                request.channel(),
                request.paymentMethod(),
                request.locationCode(),
                request.currency(),
                request.couponCodes(),
                request.evaluationDate(),
                // Par defaut les promotions s'appliquent · seul un prix negocie les ecarte, et cela
                // se declare explicitement.
                request.promotionsAllowed() == null || request.promotionsAllowed());
    }
}
