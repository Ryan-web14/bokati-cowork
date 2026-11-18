package com.sni.bokaticowork.features.ressource.service.pricing;

import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.enums.ResourcePriceAdjustmentType;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lot R1 : moteur de prix unique des ressources. */
class ResourcePriceEngineTest {

    private static final LocalDateTime MONDAY = LocalDateTime.of(2026, 12, 7, 0, 0);
    private static final LocalDateTime SATURDAY = LocalDateTime.of(2026, 12, 12, 0, 0);
    private final Resource desk = Resource.builder().id(1L).code("RES-1").build();
    private final List<ResourcePricingRule> rules = new ArrayList<>();
    private long ids = 1;

    private ResourcePriceEngine engine() {
        ResourcePricingRuleRepository repository = mock(ResourcePricingRuleRepository.class);
        when(repository.findAllActiveByResourceId(1L)).thenReturn(rules);
        return new ResourcePriceEngine(repository);
    }

    private ResourcePricingRule base(ResourceBookingUnit unit, int price) {
        ResourcePricingRule rule = ResourcePricingRule.builder().id(ids).shortId(ids++).resourceBookingUnit(unit).price(price)
                .label(unit + " " + price).active(true).build();
        rules.add(rule);
        return rule;
    }

    private ResourcePricingRule adjust(ResourcePriceAdjustmentType type, int value) {
        ResourcePricingRule rule = ResourcePricingRule.builder().id(ids).shortId(ids++).resourceBookingUnit(ResourceBookingUnit.HOUR)
                .adjustmentType(type).adjustmentValue(value).label(type + " " + value).active(true).build();
        rules.add(rule);
        return rule;
    }

    private static PriceContext ctx(String... segments) {
        return new PriceContext(Set.of(segments), null, "TEAM", MONDAY.minusDays(1));
    }

    private PriceResult price(LocalDateTime start, LocalDateTime end, int quantity, PriceContext context) {
        return engine().price(desk, start, end, quantity, null, context);
    }

    @Test
    void peakHoursArePricedProRata() {
        base(ResourceBookingUnit.HOUR, 5000);
        ResourcePricingRule evening = adjust(ResourcePriceAdjustmentType.PERCENT_DELTA, 20);
        evening.setStartsAt(LocalTime.of(18, 0));
        evening.setEndsAt(LocalTime.of(22, 0));

        PriceResult result = price(MONDAY.withHour(17), MONDAY.withHour(19), 1, ctx("PUBLIC"));

        assertThat(result.amount()).isEqualByComparingTo("11000");
        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines().get(1).adjustments()).singleElement().extracting(PriceResult.Adjustment::type).isEqualTo("PERCENT_DELTA");
    }

    @Test
    void theMostPreciseBaseWinsOnItsDays() {
        base(ResourceBookingUnit.HOUR, 5000);
        base(ResourceBookingUnit.HOUR, 7000).setDaysOfWeek("6,7");

        assertThat(price(MONDAY.withHour(9), MONDAY.withHour(10), 1, ctx("PUBLIC")).amount()).isEqualByComparingTo("5000");
        assertThat(price(SATURDAY.withHour(9), SATURDAY.withHour(10), 1, ctx("PUBLIC")).amount()).isEqualByComparingTo("7000");
    }

    @Test
    void segmentsDecideWhoGetsADiscount() {
        base(ResourceBookingUnit.HOUR, 10000);
        adjust(ResourcePriceAdjustmentType.PERCENT_DELTA, -10).setCustomerSegment("MEMBER");

        assertThat(price(MONDAY.withHour(9), MONDAY.withHour(10), 1, ctx("MEMBER")).amount()).isEqualByComparingTo("9000");
        assertThat(price(MONDAY.withHour(9), MONDAY.withHour(10), 1, ctx("PUBLIC")).amount()).isEqualByComparingTo("10000");
    }

    @Test
    void onlyStackableAdjustmentsAddUp() {
        base(ResourceBookingUnit.HOUR, 10000);
        adjust(ResourcePriceAdjustmentType.AMOUNT_DELTA, -1000).setPriority(1);
        adjust(ResourcePriceAdjustmentType.AMOUNT_DELTA, -3000).setPriority(0);
        adjust(ResourcePriceAdjustmentType.PERCENT_DELTA, -10).setStackable(true);

        // Meilleur non cumulable (priorite 1 : -1000) puis cumulable (-10 %) : (10000 - 1000) - 10 % = 8100.
        assertThat(price(MONDAY.withHour(9), MONDAY.withHour(10), 1, ctx("PUBLIC")).amount()).isEqualByComparingTo("8100");
    }

    @Test
    void aFixedPriceReplacesTheBase() {
        base(ResourceBookingUnit.HOUR, 10000);
        adjust(ResourcePriceAdjustmentType.FIXED_PRICE, 4000).setPlanCode("PLAN-PRO");

        PriceContext subscriber = new PriceContext(Set.of("MEMBER", "SUBSCRIBER"), "PLAN-PRO", "PORTAL", MONDAY.minusDays(1));
        assertThat(price(MONDAY.withHour(9), MONDAY.withHour(11), 1, subscriber).amount()).isEqualByComparingTo("8000");
        assertThat(price(MONDAY.withHour(9), MONDAY.withHour(11), 1, ctx("MEMBER")).amount()).isEqualByComparingTo("20000");
    }

    @Test
    void aRoomIsRentedWholeUnlessPricedPerParticipant() {
        Resource room = Resource.builder().id(1L).code("SAL-1")
                .resourceType(ResourceType.builder().wholeResourceBooking(true).build()).build();
        base(ResourceBookingUnit.HOUR, 20000);
        PriceResult whole = engine().price(room, MONDAY.withHour(9), MONDAY.withHour(10), 6, null, ctx("PUBLIC"));
        assertThat(whole.amount()).isEqualByComparingTo("20000");

        rules.clear();
        base(ResourceBookingUnit.HOUR, 3000).setPerParticipant(true);
        PriceResult perHead = engine().price(room, MONDAY.withHour(9), MONDAY.withHour(10), 6, null, ctx("PUBLIC"));
        assertThat(perHead.amount()).isEqualByComparingTo("18000");
    }

    @Test
    void earlyBookingIsRewarded() {
        base(ResourceBookingUnit.HOUR, 10000);
        adjust(ResourcePriceAdjustmentType.PERCENT_DELTA, -15).setMinLeadMinutes(7 * 24 * 60);
        LocalDateTime start = MONDAY.withHour(9);

        PriceContext early = new PriceContext(Set.of("PUBLIC"), null, null, start.minusDays(10));
        PriceContext late = new PriceContext(Set.of("PUBLIC"), null, null, start.minusDays(1));
        assertThat(price(start, start.plusHours(1), 1, early).amount()).isEqualByComparingTo("8500");
        assertThat(price(start, start.plusHours(1), 1, late).amount()).isEqualByComparingTo("10000");
    }

    @Test
    void aDeletedRuleIsIgnored() {
        base(ResourceBookingUnit.HOUR, 5000);
        base(ResourceBookingUnit.HOUR, 1).setDeleted(true);
        rules.get(1).setPriority(99);

        assertThat(price(MONDAY.withHour(9), MONDAY.withHour(10), 1, ctx("PUBLIC")).amount()).isEqualByComparingTo("5000");
    }

    @Test
    void aMultiDayBookingCountsOneDayPerCalendarDay() {
        base(ResourceBookingUnit.DAY, 30000);

        PriceResult result = price(MONDAY.withHour(8), MONDAY.plusDays(1).withHour(18), 1, ctx("PUBLIC"));

        assertThat(result.unit()).isEqualTo(ResourceBookingUnit.DAY);
        assertThat(result.units()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(result.amount()).isEqualByComparingTo("60000");
    }
}
