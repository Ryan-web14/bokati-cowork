package com.sni.bokaticowork.features.ressource.service.pricing;

import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.enums.ResourcePriceAdjustmentType;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import org.junit.jupiter.api.DisplayName;
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

    // ------------------------------------------------ 23/11/2026 : prix manquant et superpositions

    /** Configuration relevee sur la salle de l'exploitant : prix horaire le lundi 8 h-18 h, forfait journee, +20 % le samedi. */
    private void operatorRoom() {
        ResourcePricingRule weekday = base(ResourceBookingUnit.HOUR, 15000);
        weekday.setLabel("Tarif heures ouvrables");
        weekday.setDaysOfWeek("1");
        weekday.setStartsAt(LocalTime.of(8, 0));
        weekday.setEndsAt(LocalTime.of(18, 0));
        weekday.setPriority(10);
        base(ResourceBookingUnit.DAY, 105014);
        ResourcePricingRule saturday = adjust(ResourcePriceAdjustmentType.PERCENT_DELTA, 20);
        saturday.setDaysOfWeek("6");
        saturday.setStartsAt(LocalTime.of(11, 0));
        saturday.setEndsAt(LocalTime.of(23, 0));
    }

    @Test
    @DisplayName("Samedi sans prix horaire : prix deduit de la journee, puis majoration du samedi (avant : journee entiere facturee)")
    void aMissingHourlyPriceIsDerivedAndTheSurchargeApplies() {
        operatorRoom();

        PriceResult result = price(SATURDAY.withHour(12), SATURDAY.withHour(14), 1, ctx("PUBLIC"));

        // 105014 / 10 h = 10501,4 ; +20 % = 12601,68 ; 2 h = 25203,36.
        assertThat(result.unit()).isEqualTo(ResourceBookingUnit.HOUR);
        assertThat(result.amount()).isEqualByComparingTo("25203.36");
        assertThat(result.lines()).singleElement().satisfies(line -> {
            assertThat(line.baseRuleLabel()).startsWith("Déduit du prix à la journée");
            assertThat(line.adjustments()).singleElement().extracting(PriceResult.Adjustment::type).isEqualTo("PERCENT_DELTA");
        });
    }

    @Test
    @DisplayName("Heure imposee un samedi : calculee au lieu d'etre refusee")
    void anImposedHourIsPricedOnSaturday() {
        operatorRoom();

        PriceResult result = engine().price(desk, SATURDAY.withHour(12), SATURDAY.withHour(14), 1, ResourceBookingUnit.HOUR, ctx("PUBLIC"));

        assertThat(result.amount()).isEqualByComparingTo("25203.36");
    }

    @Test
    @DisplayName("Lundi 17 h-19 h : heure ouvrable au tarif, heure suivante deduite de la journee")
    void aBookingAcrossTheEndOfBusinessHoursIsSplit() {
        operatorRoom();

        PriceResult result = price(MONDAY.withHour(17), MONDAY.withHour(19), 1, ctx("PUBLIC"));

        assertThat(result.amount()).isEqualByComparingTo("25501.4");
        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines().get(0).baseRuleLabel()).isEqualTo("Tarif heures ouvrables");
    }

    @Test
    @DisplayName("Majoration du samedi et remise membre se superposent ; avant, la plus prioritaire excluait l'autre")
    void aSurchargeAndADiscountBothApply() {
        base(ResourceBookingUnit.HOUR, 10000);
        adjust(ResourcePriceAdjustmentType.PERCENT_DELTA, 20).setDaysOfWeek("6");
        ResourcePricingRule member = adjust(ResourcePriceAdjustmentType.PERCENT_DELTA, -10);
        member.setCustomerSegment("MEMBER");

        // 10000 + 20 % = 12000, puis - 10 % = 10800.
        assertThat(price(SATURDAY.withHour(9), SATURDAY.withHour(10), 1, ctx("MEMBER")).amount()).isEqualByComparingTo("10800");
        assertThat(price(SATURDAY.withHour(9), SATURDAY.withHour(10), 1, ctx("PUBLIC")).amount()).isEqualByComparingTo("12000");
    }

    @Test
    @DisplayName("Deux remises non cumulables de meme priorite : la plus avantageuse pour le client")
    void theBestDiscountForTheClientWins() {
        base(ResourceBookingUnit.HOUR, 10000);
        adjust(ResourcePriceAdjustmentType.PERCENT_DELTA, -10);
        adjust(ResourcePriceAdjustmentType.AMOUNT_DELTA, -1500);

        assertThat(price(MONDAY.withHour(9), MONDAY.withHour(10), 1, ctx("PUBLIC")).amount()).isEqualByComparingTo("8500");
    }

    @Test
    @DisplayName("Creneau hors de tout prix, sans unite voisine : prix de reference de l'unite (avant : gratuit)")
    void anUncoveredSlotUsesTheReferencePrice() {
        ResourcePricingRule weekday = base(ResourceBookingUnit.HOUR, 15000);
        weekday.setDaysOfWeek("1");
        weekday.setStartsAt(LocalTime.of(8, 0));
        weekday.setEndsAt(LocalTime.of(18, 0));

        PriceResult result = price(SATURDAY.withHour(10), SATURDAY.withHour(11), 1, ctx("PUBLIC"));

        assertThat(result.amount()).isEqualByComparingTo("15000");
        assertThat(result.lines().get(0).baseRuleLabel()).startsWith("Prix de référence");
    }

    @Test
    @DisplayName("Prix reserves aux membres : un client public n'est pas servi gratuitement, il est refuse avec la marche a suivre")
    void pricesForOthersOnlyAreRefusedNotFree() {
        base(ResourceBookingUnit.HOUR, 8000).setCustomerSegment("MEMBER");

        assertThat(price(MONDAY.withHour(9), MONDAY.withHour(10), 1, ctx("MEMBER")).amount()).isEqualByComparingTo("8000");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> price(MONDAY.withHour(9), MONDAY.withHour(10), 1, ctx("PUBLIC")))
                .isInstanceOf(com.sni.bokaticowork.core.exception.customs.BadRequestException.class)
                .hasMessageContaining("ajoutez un prix de base général");
    }

    @Test
    @DisplayName("Une ressource sans aucun prix reste gratuite")
    void aResourceWithoutPricesIsFree() {
        assertThat(price(MONDAY.withHour(9), MONDAY.withHour(10), 1, ctx("PUBLIC")).amount()).isEqualByComparingTo("0");
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
