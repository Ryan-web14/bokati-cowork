package com.sni.bokaticowork.features.booking.service.support;

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

    private static final long HALF_DAY_MINUTES = 300;
    private static final long DAY_MINUTES = 480;
    private static final long WEEK_MINUTES = 10080;
    private static final long MONTH_MINUTES = 43200;

    private final ResourcePricingRuleRepository pricingRuleRepository;

    public Price calculate(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt, int quantity) {
        Map<ResourceBookingUnit, ResourcePricingRule> rulesByUnit = activeRulesByUnit(resource);
        ResourcePricingRule rule = selectRule(rulesByUnit, startedAt, endedAt);
        ResourceBookingUnit unit = rule == null ? ResourceBookingUnit.HOUR : rule.getResourceBookingUnit();
        BigDecimal unitPrice = BigDecimal.valueOf(rule == null || rule.getPrice() == null ? 0 : rule.getPrice());
        BigDecimal units = units(unit, startedAt, endedAt).multiply(BigDecimal.valueOf(quantity));
        BigDecimal amount = unitPrice.multiply(units).setScale(4, RoundingMode.HALF_UP);
        return new Price(unit, unitPrice, units, amount, "XAF");
    }

    public BigDecimal entitlementQuantity(ResourceBookingUnit unit, LocalDateTime startedAt, LocalDateTime endedAt, int quantity) {
        return units(unit, startedAt, endedAt).multiply(BigDecimal.valueOf(quantity)).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal units(ResourceBookingUnit unit, LocalDateTime startedAt, LocalDateTime endedAt) {
        long minutes = Duration.between(startedAt, endedAt).toMinutes();
        return switch (unit) {
            case HOUR -> BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            case HALF_DAY -> ceil(minutes, HALF_DAY_MINUTES);
            case DAY -> ceil(minutes, DAY_MINUTES);
            case WEEK -> ceil(minutes, WEEK_MINUTES);
            case MONTH -> ceil(minutes, MONTH_MINUTES);
        };
    }

    private ResourcePricingRule selectRule(Map<ResourceBookingUnit, ResourcePricingRule> rulesByUnit,
                                           LocalDateTime startedAt,
                                           LocalDateTime endedAt) {
        if (rulesByUnit.isEmpty()) {
            return null;
        }

        long minutes = Duration.between(startedAt, endedAt).toMinutes();
        if (minutes >= MONTH_MINUTES && rulesByUnit.containsKey(ResourceBookingUnit.MONTH)) {
            return rulesByUnit.get(ResourceBookingUnit.MONTH);
        }
        if (minutes >= WEEK_MINUTES && rulesByUnit.containsKey(ResourceBookingUnit.WEEK)) {
            return rulesByUnit.get(ResourceBookingUnit.WEEK);
        }
        if (minutes >= DAY_MINUTES && rulesByUnit.containsKey(ResourceBookingUnit.DAY)) {
            return rulesByUnit.get(ResourceBookingUnit.DAY);
        }
        if (minutes >= HALF_DAY_MINUTES && rulesByUnit.containsKey(ResourceBookingUnit.HALF_DAY)) {
            return rulesByUnit.get(ResourceBookingUnit.HALF_DAY);
        }
        if (rulesByUnit.containsKey(ResourceBookingUnit.HOUR)) {
            return rulesByUnit.get(ResourceBookingUnit.HOUR);
        }
        if (rulesByUnit.containsKey(ResourceBookingUnit.HALF_DAY)) {
            return rulesByUnit.get(ResourceBookingUnit.HALF_DAY);
        }
        if (rulesByUnit.containsKey(ResourceBookingUnit.DAY)) {
            return rulesByUnit.get(ResourceBookingUnit.DAY);
        }
        if (rulesByUnit.containsKey(ResourceBookingUnit.WEEK)) {
            return rulesByUnit.get(ResourceBookingUnit.WEEK);
        }
        return rulesByUnit.get(ResourceBookingUnit.MONTH);
    }

    private Map<ResourceBookingUnit, ResourcePricingRule> activeRulesByUnit(Resource resource) {
        List<ResourcePricingRule> rules = pricingRuleRepository.findAllActiveByResourceId(resource.getId());
        Map<ResourceBookingUnit, ResourcePricingRule> rulesByUnit = new EnumMap<>(ResourceBookingUnit.class);
        for (ResourcePricingRule rule : rules) {
            if (rule.getResourceBookingUnit() != null && !rulesByUnit.containsKey(rule.getResourceBookingUnit())) {
                rulesByUnit.put(rule.getResourceBookingUnit(), rule);
            }
        }
        return rulesByUnit;
    }

    private BigDecimal ceil(long minutes, long denominator) {
        return BigDecimal.valueOf((long) Math.ceil((double) minutes / denominator)).setScale(4, RoundingMode.HALF_UP);
    }

    public record Price(ResourceBookingUnit unit, BigDecimal unitPrice, BigDecimal quantity, BigDecimal amount, String currency) {
    }
}
