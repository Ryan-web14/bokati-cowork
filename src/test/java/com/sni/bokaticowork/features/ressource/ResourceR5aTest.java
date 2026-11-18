package com.sni.bokaticowork.features.ressource;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceCalendarDay;
import com.sni.bokaticowork.features.ressource.model.ResourceCalendarDayKind;
import com.sni.bokaticowork.features.ressource.model.ResourceClosure;
import com.sni.bokaticowork.features.ressource.model.ResourceClosureRecurrence;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceCalendarDayRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.service.pricing.PriceContext;
import com.sni.bokaticowork.features.ressource.service.pricing.ResourcePriceEngine;
import com.sni.bokaticowork.features.ressource.service.support.CongoHolidays;
import com.sni.bokaticowork.features.ressource.service.support.MonthlyAvailabilityGenerator.OpeningHours;
import com.sni.bokaticowork.features.ressource.service.support.ResourceCalendarDays;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lot R5a des ressources : calendrier et regles de reservation. */
class ResourceR5aTest {

    @Test
    void congoHolidaysAreComputed() {
        // Paques 2026 : 5 avril.
        assertThat(CongoHolidays.label(LocalDate.of(2026, 4, 6))).isEqualTo("Lundi de Pâques");
        assertThat(CongoHolidays.label(LocalDate.of(2026, 5, 14))).isEqualTo("Ascension");
        assertThat(CongoHolidays.label(LocalDate.of(2026, 5, 25))).isEqualTo("Lundi de Pentecôte");
        assertThat(CongoHolidays.isHoliday(LocalDate.of(2026, 8, 15))).isTrue();
        assertThat(CongoHolidays.isHoliday(LocalDate.of(2026, 11, 28))).isTrue();
        assertThat(CongoHolidays.isHoliday(LocalDate.of(2026, 11, 18))).isFalse();
        assertThat(CongoHolidays.of(2027)).hasSize(10);
    }

    @Test
    void aWeeklyClosureRepeatsUntilItsEnd() {
        LocalDateTime monday = LocalDateTime.of(2026, 11, 23, 8, 0);
        ResourceClosure closure = ResourceClosure.builder().startedAt(monday).endedAt(monday.plusHours(2))
                .recurrence(ResourceClosureRecurrence.WEEKLY).recurrenceUntil(LocalDate.of(2026, 12, 14)).build();

        List<LocalDateTime[]> occurrences = closure.occurrences(monday, monday.plusMonths(2));

        assertThat(occurrences).hasSize(4);
        assertThat(occurrences.get(3)[0]).isEqualTo(LocalDateTime.of(2026, 12, 14, 8, 0));
        assertThat(closure.occursWithin(LocalDateTime.of(2026, 11, 30, 9, 0), LocalDateTime.of(2026, 11, 30, 9, 30))).isTrue();
        assertThat(closure.occursWithin(LocalDateTime.of(2026, 11, 30, 10, 0), LocalDateTime.of(2026, 11, 30, 11, 0))).isFalse();
        assertThat(closure.occursWithin(LocalDateTime.of(2026, 12, 21, 8, 0), LocalDateTime.of(2026, 12, 21, 9, 0))).isFalse();
    }

    @Test
    void aOneOffClosureOccursOnce() {
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 8, 0);
        ResourceClosure closure = ResourceClosure.builder().startedAt(start).endedAt(start.plusDays(1)).build();
        assertThat(closure.occurrences(null, null)).hasSize(1);
    }

    private ResourceCalendarDays days(ResourceCalendarDayRepository repository) {
        return new ResourceCalendarDays(repository, mock(ResourceRepository.class));
    }

    @Test
    void specialHoursReplaceTheWeekAndAClosedDayClosesIt() {
        ResourceCalendarDayRepository repository = mock(ResourceCalendarDayRepository.class);
        Resource resource = Resource.builder().id(1L).code("SAL-1").build();
        LocalDate special = LocalDate.of(2026, 12, 24);
        LocalDate closed = LocalDate.of(2026, 12, 31);
        when(repository.findFirstByDayDateAndResource(any(), any())).thenReturn(Optional.empty());
        when(repository.findFirstByDayDateAndResourceIsNull(any())).thenReturn(Optional.empty());
        when(repository.findFirstByDayDateAndResourceIsNull(special)).thenReturn(Optional.of(ResourceCalendarDay.builder()
                .dayDate(special).kind(ResourceCalendarDayKind.SPECIAL_HOURS).openingHours("08:00-13:00").build()));
        when(repository.findFirstByDayDateAndResource(closed, resource)).thenReturn(Optional.of(ResourceCalendarDay.builder()
                .dayDate(closed).resource(resource).kind(ResourceCalendarDayKind.CLOSED).build()));

        ResourceCalendarDays calendar = days(repository);

        assertThat(calendar.hoursOn(resource, special)).contains(List.of(new OpeningHours(LocalTime.of(8, 0), LocalTime.of(13, 0))));
        assertThat(calendar.hoursOn(resource, closed)).contains(List.of());
        // Jour ordinaire : la semaine type s'applique ; jour ferie national : horaires normaux par defaut.
        assertThat(calendar.hoursOn(resource, LocalDate.of(2026, 11, 18))).isEmpty();
        assertThat(calendar.hoursOn(resource, LocalDate.of(2026, 12, 25))).isEmpty();
        assertThat(calendar.isHoliday(LocalDate.of(2026, 12, 25))).isTrue();
    }

    @Test
    void aHolidayOnlyRuleAppliesOnHolidaysOnly() throws Exception {
        ResourcePriceEngine engine = new ResourcePriceEngine(null);
        ResourcePricingRule rule = ResourcePricingRule.builder().holidayMode("ONLY").build();
        ResourcePricingRule outside = ResourcePricingRule.builder().holidayMode("EXCLUDED").build();
        Method applies = ResourcePriceEngine.class.getDeclaredMethod("applies", ResourcePricingRule.class, LocalDateTime.class,
                LocalDateTime.class, long.class, int.class, PriceContext.class);
        applies.setAccessible(true);
        LocalDateTime christmas = LocalDateTime.of(2026, 12, 25, 10, 0);
        LocalDateTime ordinary = LocalDateTime.of(2026, 12, 22, 10, 0);
        PriceContext ctx = PriceContext.anonymous();

        assertThat((boolean) applies.invoke(engine, rule, christmas, christmas, 60L, 1, ctx)).isTrue();
        assertThat((boolean) applies.invoke(engine, rule, ordinary, ordinary, 60L, 1, ctx)).isFalse();
        assertThat((boolean) applies.invoke(engine, outside, christmas, christmas, 60L, 1, ctx)).isFalse();
        assertThat((boolean) applies.invoke(engine, outside, ordinary, ordinary, 60L, 1, ctx)).isTrue();
    }

    @Test
    void theBufferProtectsTheTurnaroundOfAWholeRoom() throws Exception {
        var availability = mock(com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository.class);
        var service = new com.sni.bokaticowork.features.ressource.service.implementation.ResourceAvailabilityServiceImpl(
                availability, null, null, null, null, null);
        Resource room = Resource.builder().id(1L).code("SAL-1")
                .resourceType(com.sni.bokaticowork.features.ressource.model.ResourceType.builder().wholeResourceBooking(true).build())
                .resourcePolicy(com.sni.bokaticowork.features.ressource.model.ResourcePolicy.builder().active(true).bufferMinutes(30).build())
                .build();
        LocalDateTime start = LocalDateTime.of(2026, 12, 2, 10, 0);
        var taken = com.sni.bokaticowork.features.ressource.model.ResourceAvailability.builder()
                .startedAt(start.minusMinutes(30)).endedAt(start).totalCapacity(1).remainingCapacity(0).build();
        when(availability.findOverlapping(any(), any(), any())).thenReturn(List.of());
        when(availability.findOverlapping(room, start.minusMinutes(30), start)).thenReturn(List.of(taken));
        Method bufferTaken = service.getClass().getDeclaredMethod("bufferTaken", Resource.class, LocalDateTime.class, LocalDateTime.class);
        bufferTaken.setAccessible(true);

        assertThat((boolean) bufferTaken.invoke(service, room, start, start.plusHours(1))).isTrue();
        assertThat((boolean) bufferTaken.invoke(service, room, start.plusHours(2), start.plusHours(3))).isFalse();
    }
}
