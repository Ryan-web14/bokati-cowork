package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingPricingCalculatorTest {

    @Test
    void shouldUseHourlyPricingForShortBooking() {
        Resource resource = Resource.builder().id(1L).build();
        BookingPricingCalculator calculator = new BookingPricingCalculator(repository(List.of(
                rule(resource, ResourceBookingUnit.HOUR, 5000),
                rule(resource, ResourceBookingUnit.HALF_DAY, 18000),
                rule(resource, ResourceBookingUnit.DAY, 30000)
        )));

        BookingPricingCalculator.Price price = calculator.calculate(
                resource,
                LocalDateTime.of(2026, 4, 27, 9, 0),
                LocalDateTime.of(2026, 4, 27, 11, 0),
                1
        );

        assertEquals(ResourceBookingUnit.HOUR, price.unit());
        assertEquals(new BigDecimal("5000"), price.unitPrice());
        assertEquals(new BigDecimal("2.0000"), price.quantity());
        assertEquals(new BigDecimal("10000.0000"), price.amount());
    }

    @Test
    void shouldUseHalfDayPricingForFiveHourBooking() {
        Resource resource = Resource.builder().id(2L).build();
        BookingPricingCalculator calculator = new BookingPricingCalculator(repository(List.of(
                rule(resource, ResourceBookingUnit.HOUR, 5000),
                rule(resource, ResourceBookingUnit.HALF_DAY, 18000),
                rule(resource, ResourceBookingUnit.DAY, 30000)
        )));

        BookingPricingCalculator.Price price = calculator.calculate(
                resource,
                LocalDateTime.of(2026, 4, 27, 8, 0),
                LocalDateTime.of(2026, 4, 27, 13, 0),
                1
        );

        assertEquals(ResourceBookingUnit.HALF_DAY, price.unit());
        assertEquals(new BigDecimal("18000"), price.unitPrice());
        assertEquals(new BigDecimal("1.0000"), price.quantity());
        assertEquals(new BigDecimal("18000.0000"), price.amount());
    }

    @Test
    void shouldUseDayPricingForEightHourBooking() {
        Resource resource = Resource.builder().id(3L).build();
        BookingPricingCalculator calculator = new BookingPricingCalculator(repository(List.of(
                rule(resource, ResourceBookingUnit.HOUR, 5000),
                rule(resource, ResourceBookingUnit.HALF_DAY, 18000),
                rule(resource, ResourceBookingUnit.DAY, 30000)
        )));

        BookingPricingCalculator.Price price = calculator.calculate(
                resource,
                LocalDateTime.of(2026, 4, 27, 8, 0),
                LocalDateTime.of(2026, 4, 27, 16, 0),
                1
        );

        assertEquals(ResourceBookingUnit.DAY, price.unit());
        assertEquals(new BigDecimal("30000"), price.unitPrice());
        assertEquals(new BigDecimal("1.0000"), price.quantity());
        assertEquals(new BigDecimal("30000.0000"), price.amount());
    }

    private ResourcePricingRuleRepository repository(List<ResourcePricingRule> rules) {
        return (ResourcePricingRuleRepository) Proxy.newProxyInstance(
                ResourcePricingRuleRepository.class.getClassLoader(),
                new Class<?>[]{ResourcePricingRuleRepository.class},
                (proxy, method, args) -> {
                    if ("findAllActiveByResourceId".equals(method.getName())) {
                        return rules;
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("toString".equals(method.getName())) {
                        return "ResourcePricingRuleRepositoryProxy";
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private ResourcePricingRule rule(Resource resource, ResourceBookingUnit unit, int price) {
        return ResourcePricingRule.builder()
                .resource(resource)
                .resourceBookingUnit(unit)
                .price(price)
                .active(true)
                .build();
    }
}
