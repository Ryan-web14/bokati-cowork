package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.ResourceAnalytics;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceCost;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceCostRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lot R5e : pilotage des ressources. */
class ResourceR5eTest {

    @Test
    void aMonthlyCostIsSharedDayByDay() throws Exception {
        ResourceCost rent = ResourceCost.builder().kind(ResourceCost.Kind.RENT).basis(ResourceCost.Basis.MONTHLY)
                .amount(BigDecimal.valueOf(300000)).build();
        Method share = ResourceAnalytics.class.getDeclaredMethod("monthlyShare", ResourceCost.class, LocalDate.class, LocalDate.class);
        share.setAccessible(true);

        // Novembre : 30 jours, 10 000 par jour.
        assertThat((BigDecimal) share.invoke(null, rent, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15)))
                .isEqualByComparingTo("150000");
        rent.setValidFrom(LocalDate.of(2026, 11, 11));
        assertThat((BigDecimal) share.invoke(null, rent, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15)))
                .isEqualByComparingTo("50000");
    }

    @Test
    void peakAndOffPeakMinutesAreSplit() throws Exception {
        Method split = ResourceAnalytics.class.getDeclaredMethod("split", LocalDateTime.class, LocalDateTime.class, List.class);
        split.setAccessible(true);
        List<LocalTime[]> peak = List.<LocalTime[]>of(new LocalTime[]{LocalTime.of(9, 0), LocalTime.of(12, 0)});

        // Mercredi 08:00 a 10:00 : une heure creuse, une heure pleine.
        long[] wednesday = (long[]) split.invoke(null, LocalDateTime.of(2026, 12, 2, 8, 0), LocalDateTime.of(2026, 12, 2, 10, 0), peak);
        assertThat(wednesday).containsExactly(60, 60);
        // Samedi : tout en heures creuses.
        long[] saturday = (long[]) split.invoke(null, LocalDateTime.of(2026, 12, 5, 9, 0), LocalDateTime.of(2026, 12, 5, 11, 0), peak);
        assertThat(saturday).containsExactly(0, 120);
    }

    @Test
    void theAppliedRulesAreReadFromTheTrace() throws Exception {
        ResourceAnalytics analytics = new ResourceAnalytics(mock(BookingRepository.class), mock(ResourceRepository.class),
                mock(ResourceCostRepository.class), mock(EntityManager.class));
        Method rules = ResourceAnalytics.class.getDeclaredMethod("rules", String.class);
        rules.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) rules.invoke(analytics,
                "{\"lines\":[{\"label\":\"Tarif horaire\",\"adjustments\":[{\"label\":\"Heures creuses -20 %\"}]}]}");

        assertThat(names).containsExactly("Tarif horaire", "Heures creuses -20 %");
    }

    @Test
    void aBookingMarginSubtractsItsCosts() {
        BookingRepository bookings = mock(BookingRepository.class);
        ResourceCostRepository costs = mock(ResourceCostRepository.class);
        Resource room = Resource.builder().id(1L).code("SAL-1").build();
        LocalDateTime start = LocalDateTime.of(2026, 12, 2, 10, 0);
        Booking booking = Booking.builder().bookingNumber("BKG-1").resource(room).startedAt(start).endedAt(start.plusHours(2))
                .totalAmount(BigDecimal.valueOf(20000)).build();
        when(bookings.findByBookingNumber("BKG-1")).thenReturn(Optional.of(booking));
        when(costs.findAllByResourceOrderByKindAscIdAsc(room)).thenReturn(List.of(
                ResourceCost.builder().kind(ResourceCost.Kind.CLEANING).basis(ResourceCost.Basis.PER_BOOKING).amount(BigDecimal.valueOf(2000)).build(),
                ResourceCost.builder().kind(ResourceCost.Kind.ENERGY).basis(ResourceCost.Basis.PER_HOUR).amount(BigDecimal.valueOf(500)).build()));
        ResourceAnalytics analytics = new ResourceAnalytics(bookings, mock(ResourceRepository.class), costs,
                mock(EntityManager.class, RETURNS_DEEP_STUBS));

        ResourceAnalytics.BookingMargin margin = analytics.bookingMargin("BKG-1");

        assertThat(margin.costs()).isEqualByComparingTo("3000");
        assertThat(margin.margin()).isEqualByComparingTo("17000");
        assertThat(margin.costsByKind()).containsKeys("CLEANING", "ENERGY");
    }
}
