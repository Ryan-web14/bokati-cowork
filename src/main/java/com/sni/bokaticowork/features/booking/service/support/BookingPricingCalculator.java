package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BookingPricingCalculator {

    /**
     * Une demi-journee vaut 5 heures, une journee 10 · de 08h00 a 18h00.
     *
     * <p>La journee valait 480 minutes, soit 8 heures : une reservation de 8h etait facturee au
     * tarif journalier alors qu'il reste deux heures ouvrables devant elle.
     */
    private static final long HALF_DAY_MINUTES = 300;
    public static final long DAY_MINUTES = 600;
    private static final long WEEK_MINUTES = 10080;
    private static final long MONTH_MINUTES = 43200;

    /** Ordre de repli quand la ressource ne porte aucune grille pour l'unite appelee. */
    private static final List<ResourceBookingUnit> FALLBACK_ORDER = List.of(
            ResourceBookingUnit.HOUR, ResourceBookingUnit.HALF_DAY, ResourceBookingUnit.DAY,
            ResourceBookingUnit.WEEK, ResourceBookingUnit.MONTH);

    private final com.sni.bokaticowork.features.ressource.service.pricing.ResourcePriceEngine priceEngine;

    public Price calculate(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt, int quantity) {
        return calculate(resource, startedAt, endedAt, quantity, null, null);
    }

    public Price calculate(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt,
                           int quantity, ResourceBookingUnit forcedUnit) {
        return calculate(resource, startedAt, endedAt, quantity, forcedUnit, null);
    }

    /**
     * Lot R1 : le prix vient du moteur unique des ressources ({@code ResourcePriceEngine}), le meme que
     * le devis et le catalogue. {@code forcedUnit} reste reserve a l'equipe : un forfait impose vaut
     * un forfait.
     */
    public Price calculate(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt,
                           int quantity, ResourceBookingUnit forcedUnit,
                           com.sni.bokaticowork.features.ressource.service.pricing.PriceContext context) {
        com.sni.bokaticowork.features.ressource.service.pricing.PriceResult result =
                priceEngine.price(resource, startedAt, endedAt, quantity, forcedUnit, context);
        BigDecimal quantityBilled = result.units().multiply(BigDecimal.valueOf(Math.max(1, result.multiplier())))
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal unitPrice = quantityBilled.signum() == 0 ? result.unitPrice()
                : result.amount().divide(quantityBilled, 4, RoundingMode.HALF_UP);
        // Prix unitaire entier sans decimales (comme avant) : 5000 et non 5000.0000.
        if (unitPrice.signum() == 0 || unitPrice.remainder(BigDecimal.ONE).signum() == 0) {
            unitPrice = unitPrice.setScale(0, RoundingMode.HALF_UP);
        }
        return new Price(result.unit(), unitPrice, quantityBilled, result.amount(), result.currency(), result);
    }

    /** Calculateur sur un jeu de regles (tests) : le moteur unique, sans parametres de la plateforme. */
    public static BookingPricingCalculator forRules(com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository rules) {
        return new BookingPricingCalculator(new com.sni.bokaticowork.features.ressource.service.pricing.ResourcePriceEngine(rules));
    }

    /** Trace du calcul en JSON, enregistree avec la reservation. */
    public static String trace(Price price) {
        if (price == null || price.result() == null) {
            return null;
        }
        try {
            return TRACE_MAPPER.writeValueAsString(price.result());
        } catch (Exception ex) {
            return null;
        }
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper TRACE_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    public record Price(ResourceBookingUnit unit, BigDecimal unitPrice, BigDecimal quantity, BigDecimal amount, String currency,
                        com.sni.bokaticowork.features.ressource.service.pricing.PriceResult result) {
    }
}
