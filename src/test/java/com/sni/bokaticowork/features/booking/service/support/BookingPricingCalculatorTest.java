package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void shouldUseDayPricingFromTenHours() {
        // La journee normale va de 08h00 a 18h00 · c'est a dix heures que le forfait journalier
        // s'applique, non a huit comme auparavant.
        assertUnit(hours(8, 18), ResourceBookingUnit.DAY, "30000", "1.0000", "30000.0000");
    }

    @Test
    void shouldBillHourlyBetweenHalfDayAndDay() {
        // Le forfait demi-journee ne vaut qu'a sa duree exacte · au-dela, le tarif horaire reprend.
        // Sans cela, six heures seraient facturees cinq, et le systeme rendrait une heure sans
        // que personne ne l'ait decide.
        assertUnit(hours(8, 14), ResourceBookingUnit.HOUR, "5000", "6.0000", "30000.0000");
        assertUnit(hours(8, 16), ResourceBookingUnit.HOUR, "5000", "8.0000", "40000.0000");
    }

    @Test
    void shouldBillHourlyJustAboveFiveHours() {
        // 5h30 · le forfait ne couvre pas la tranche, seulement la duree exacte.
        assertUnit(new LocalDateTime[]{LocalDateTime.of(2026, 4, 27, 8, 0),
                                       LocalDateTime.of(2026, 4, 27, 13, 30)},
                ResourceBookingUnit.HOUR, "5000", "5.5000", "27500.0000");
    }

    @Test
    void shouldBillHourlyBelowFiveHours() {
        assertUnit(hours(8, 12), ResourceBookingUnit.HOUR, "5000", "4.0000", "20000.0000");
    }

    @Test
    void shouldApplyTheUnitImposedByAnAdministrator() {
        // Six heures appellent le tarif horaire · l'administration peut imposer la journee.
        // Le prix n'est pas saisi, il vient de la grille journaliere de la ressource.
        Resource resource = Resource.builder().id(10L).build();
        BookingPricingCalculator.Price price = calculator(resource).calculate(
                resource, LocalDateTime.of(2026, 4, 27, 8, 0), LocalDateTime.of(2026, 4, 27, 14, 0),
                1, ResourceBookingUnit.DAY);

        assertEquals(ResourceBookingUnit.DAY, price.unit());
        assertEquals(new BigDecimal("30000"), price.unitPrice());
        assertEquals(new BigDecimal("1.0000"), price.quantity());
        assertEquals(new BigDecimal("30000.0000"), price.amount());
    }

    @Test
    void shouldBillOneImposedPackageWhateverTheDuration() {
        // Un forfait impose reste un forfait · l'arrondi au superieur en compterait deux sur six
        // heures, et imposer le forfait couterait alors plus cher que de ne rien imposer, ce qui
        // viderait l'override de son objet.
        Resource resource = Resource.builder().id(13L).build();
        BookingPricingCalculator calculator = calculator(resource);

        for (int endHour : new int[]{12, 14, 18}) {
            BookingPricingCalculator.Price price = calculator.calculate(
                    resource, LocalDateTime.of(2026, 4, 27, 8, 0),
                    LocalDateTime.of(2026, 4, 27, endHour, 0), 1, ResourceBookingUnit.HALF_DAY);

            assertEquals(new BigDecimal("1.0000"), price.quantity(), "duree jusqu'a " + endHour + "h");
            assertEquals(new BigDecimal("18000.0000"), price.amount());
        }
    }

    @Test
    void shouldKeepTheCeilingWhenNothingIsImposed() {
        // Le forfait unique ne vaut que sur override · le calcul automatique garde son arrondi.
        // Dix heures sans regle journaliere ni horaire ne laissent que la demi-journee : deux.
        Resource resource = Resource.builder().id(16L).build();
        BookingPricingCalculator calculator = new BookingPricingCalculator(repository(List.of(
                rule(resource, ResourceBookingUnit.HALF_DAY, 18000))));

        BookingPricingCalculator.Price price = calculator.calculate(
                resource, LocalDateTime.of(2026, 4, 27, 8, 0), LocalDateTime.of(2026, 4, 27, 18, 0), 1);

        assertEquals(ResourceBookingUnit.HALF_DAY, price.unit());
        assertEquals(new BigDecimal("2.0000"), price.quantity());
        assertEquals(new BigDecimal("36000.0000"), price.amount());
    }

    @Test
    void shouldKeepImposedHourlyProportionalToTheDuration() {
        // L'heure fait exception : un forfait couvre une plage, une heure se compte.
        Resource resource = Resource.builder().id(14L).build();
        BookingPricingCalculator.Price price = calculator(resource).calculate(
                resource, LocalDateTime.of(2026, 4, 27, 8, 0), LocalDateTime.of(2026, 4, 27, 13, 0),
                1, ResourceBookingUnit.HOUR);

        assertEquals(new BigDecimal("5.0000"), price.quantity());
        assertEquals(new BigDecimal("25000.0000"), price.amount());
    }

    @Test
    void shouldMultiplyAnImposedPackageByTheBookedQuantity() {
        // Deux places pour une demi-journee imposee · le forfait vaut un, la quantite reste.
        Resource resource = Resource.builder().id(15L).build();
        BookingPricingCalculator.Price price = calculator(resource).calculate(
                resource, LocalDateTime.of(2026, 4, 27, 8, 0), LocalDateTime.of(2026, 4, 27, 14, 0),
                2, ResourceBookingUnit.HALF_DAY);

        assertEquals(new BigDecimal("2.0000"), price.quantity());
        assertEquals(new BigDecimal("36000.0000"), price.amount());
    }

    @Test
    void shouldRefuseAnImposedUnitWithoutAPricingRule() {
        // Retomber en silence sur une autre unite facturerait autre chose que ce qui a ete demande.
        Resource resource = Resource.builder().id(11L).code("RES-TEST").build();
        BookingPricingCalculator calculator = new BookingPricingCalculator(repository(List.of(
                rule(resource, ResourceBookingUnit.HOUR, 5000))));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> calculator.calculate(
                resource, LocalDateTime.of(2026, 4, 27, 8, 0), LocalDateTime.of(2026, 4, 27, 14, 0),
                1, ResourceBookingUnit.DAY));
        assertTrue(ex.getMessage().contains("RES-TEST"));
        assertTrue(ex.getMessage().contains("DAY"));
    }

    @Test
    void shouldFallBackToHourlyWhenTheResourceHasNoRuleForTheCalledUnit() {
        // Dix heures appellent la journee · sans grille journaliere, l'heure prend le relais,
        // seule unite dont le montant suit la duree reelle.
        Resource resource = Resource.builder().id(12L).build();
        BookingPricingCalculator calculator = new BookingPricingCalculator(repository(List.of(
                rule(resource, ResourceBookingUnit.HOUR, 5000))));

        BookingPricingCalculator.Price price = calculator.calculate(
                resource, LocalDateTime.of(2026, 4, 27, 8, 0), LocalDateTime.of(2026, 4, 27, 18, 0), 1);

        assertEquals(ResourceBookingUnit.HOUR, price.unit());
        assertEquals(new BigDecimal("50000.0000"), price.amount());
    }

    private void assertUnit(LocalDateTime[] window, ResourceBookingUnit unit,
                            String unitPrice, String quantity, String amount) {
        Resource resource = Resource.builder().id(99L).build();
        BookingPricingCalculator.Price price =
                calculator(resource).calculate(resource, window[0], window[1], 1);

        assertEquals(unit, price.unit());
        assertEquals(new BigDecimal(unitPrice), price.unitPrice());
        assertEquals(new BigDecimal(quantity), price.quantity());
        assertEquals(new BigDecimal(amount), price.amount());
    }

    private LocalDateTime[] hours(int from, int to) {
        return new LocalDateTime[]{LocalDateTime.of(2026, 4, 27, from, 0),
                                   LocalDateTime.of(2026, 4, 27, to, 0)};
    }

    private BookingPricingCalculator calculator(Resource resource) {
        return new BookingPricingCalculator(repository(List.of(
                rule(resource, ResourceBookingUnit.HOUR, 5000),
                rule(resource, ResourceBookingUnit.HALF_DAY, 18000),
                rule(resource, ResourceBookingUnit.DAY, 30000))));
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
