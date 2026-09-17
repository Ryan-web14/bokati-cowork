package com.sni.bokaticowork.features.subscription.promotion.pricing.engine;

import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.ConditionOperator;
import com.sni.bokaticowork.features.subscription.promotion.pricing.model.PromotionCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/**
 * Decide si les conditions d'une promotion sont reunies pour un contexte donne.
 *
 * <p>Toutes les conditions doivent etre vraies. Un « ou » se modelise par deux promotions, ce qui
 * reste plus lisible qu'un arbre booleen range dans une table.</p>
 *
 * <p>Une condition que l'on ne sait pas evaluer <b>ne s'applique pas</b>, et la promotion est donc
 * ecartee. C'est le sens le plus sur : accorder une remise sur une condition incomprise coute de
 * l'argent, la refuser coute une reclamation qui se voit et se corrige.</p>
 */
@Slf4j
@Component
public class PromotionConditionEvaluator {

    private static final ZoneId APP_ZONE = ZoneId.of("Africa/Lagos");

    public boolean matches(List<PromotionCondition> conditions, PricingContext context) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        return conditions.stream().allMatch(condition -> matches(condition, context));
    }

    private boolean matches(PromotionCondition condition, PricingContext context) {
        return switch (condition.getConditionType()) {
            case SUBSCRIBER_TYPE -> compareText(condition, context.subscriberType());
            case SUBSCRIBER_SEGMENT -> compareText(condition, context.subscriberSegment());
            case BILLING_CYCLE -> compareText(condition, context.billingCycle());
            case CHANNEL -> compareText(condition, context.channel());
            case PAYMENT_METHOD -> compareText(condition, context.paymentMethod());
            case LOCATION_CODE -> compareText(condition, context.locationCode());
            case REFERRAL_CODE -> compareText(condition, null);

            case PLAN_CODE, PLAN_TYPE, PASS_TYPE -> matchesAnyLineCode(condition, context);

            case FIRST_PURCHASE -> Boolean.TRUE.equals(context.firstPurchase()) == expectedBoolean(condition);
            case KYC_LEVEL -> compareNumber(condition, toDecimal(context.kycLevel()));
            case TENURE_MONTHS -> compareNumber(condition, toDecimal(context.tenureMonths()));
            case MIN_AMOUNT -> compareNumber(condition, orderAmount(context));
            case MIN_QUANTITY -> compareNumber(condition, orderQuantity(context));

            case DAY_OF_WEEK -> matchesDayOfWeek(condition, context);
            case TIME_RANGE -> matchesTimeRange(condition, context);
        };
    }

    // -----------------------------------------------------------------------------------------

    private boolean matchesAnyLineCode(PromotionCondition condition, PricingContext context) {
        return context.linesOrEmpty().stream().anyMatch(line ->
                compareText(condition, line.code()) || compareText(condition, line.categoryCode()));
    }

    private boolean matchesDayOfWeek(PromotionCondition condition, PricingContext context) {
        DayOfWeek day = context.evaluationDateOrNow().atZone(APP_ZONE).getDayOfWeek();
        return compareText(condition, day.name());
    }

    /**
     * Plage horaire, bornes incluses, exprimee en {@code valueList} sous la forme {@code 08:00,12:00}.
     * Une plage qui passe minuit est acceptee et lue comme telle.
     */
    private boolean matchesTimeRange(PromotionCondition condition, PricingContext context) {
        List<String> bounds = condition.values();
        if (bounds.size() != 2) {
            log.debug("Condition TIME_RANGE {} sans deux bornes · promotion ecartee", condition.getId());
            return false;
        }
        try {
            LocalTime now = context.evaluationDateOrNow().atZone(APP_ZONE).toLocalTime();
            LocalTime from = LocalTime.parse(bounds.get(0));
            LocalTime to = LocalTime.parse(bounds.get(1));
            if (from.isAfter(to)) {
                return !now.isBefore(from) || !now.isAfter(to);
            }
            return !now.isBefore(from) && !now.isAfter(to);
        } catch (Exception ex) {
            log.debug("Condition TIME_RANGE {} illisible · promotion ecartee", condition.getId());
            return false;
        }
    }

    // -----------------------------------------------------------------------------------------

    private boolean compareText(PromotionCondition condition, String observed) {
        String actual = observed == null ? null : observed.trim().toUpperCase(Locale.ROOT);
        return switch (condition.getOperator()) {
            case EQUALS -> actual != null && actual.equalsIgnoreCase(normalized(condition.getValue()));
            case NOT_EQUALS -> actual == null || !actual.equalsIgnoreCase(normalized(condition.getValue()));
            case IN -> actual != null && upperValues(condition).contains(actual);
            case NOT_IN -> actual == null || !upperValues(condition).contains(actual);
            // Un comparateur numerique sur du texte n'a pas de sens : la promotion est ecartee
            // plutot que de produire une comparaison lexicographique dont personne n'a decide.
            case GREATER_THAN, LESS_THAN, BETWEEN -> false;
        };
    }

    private boolean compareNumber(PromotionCondition condition, BigDecimal observed) {
        if (observed == null) {
            return false;
        }
        return switch (condition.getOperator()) {
            case EQUALS -> observed.compareTo(decimal(condition.getValue())) == 0;
            case NOT_EQUALS -> observed.compareTo(decimal(condition.getValue())) != 0;
            case GREATER_THAN -> observed.compareTo(decimal(condition.getValue())) >= 0;
            case LESS_THAN -> observed.compareTo(decimal(condition.getValue())) <= 0;
            case BETWEEN -> between(condition, observed);
            case IN -> condition.values().stream().anyMatch(candidate -> observed.compareTo(decimal(candidate)) == 0);
            case NOT_IN -> condition.values().stream().noneMatch(candidate -> observed.compareTo(decimal(candidate)) == 0);
        };
    }

    private boolean between(PromotionCondition condition, BigDecimal observed) {
        List<String> bounds = condition.values();
        if (bounds.size() != 2) {
            return false;
        }
        return observed.compareTo(decimal(bounds.get(0))) >= 0
                && observed.compareTo(decimal(bounds.get(1))) <= 0;
    }

    /**
     * {@code GREATER_THAN} et {@code LESS_THAN} sont volontairement inclusifs. Un seuil commercial
     * se pense en « a partir de » : « a partir de trois pass » doit accepter trois pass, faute de
     * quoi chaque campagne devrait etre saisie avec un seuil decale d'une unite.
     */
    private BigDecimal orderAmount(PricingContext context) {
        return context.linesOrEmpty().stream()
                .map(PricingContext.PricingLine::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal orderQuantity(PricingContext context) {
        return BigDecimal.valueOf(context.linesOrEmpty().stream()
                .mapToInt(PricingContext.PricingLine::quantity)
                .sum());
    }

    private boolean expectedBoolean(PromotionCondition condition) {
        boolean expected = !"false".equalsIgnoreCase(normalized(condition.getValue()));
        return condition.getOperator() == ConditionOperator.NOT_EQUALS ? !expected : expected;
    }

    private List<String> upperValues(PromotionCondition condition) {
        return condition.values().stream().map(value -> value.toUpperCase(Locale.ROOT)).toList();
    }

    private String normalized(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal decimal(String value) {
        try {
            return value == null ? BigDecimal.ZERO : new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal toDecimal(Integer value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
