package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.features.billing.service.support.MemberBillingContact;
import com.sni.bokaticowork.features.booking.dto.request.BookingAvailabilityRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingAvailabilityResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingSuggestionResponse;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.service.support.BookingOccupancyFinder;
import com.sni.bokaticowork.features.booking.service.support.BookingPricingCalculator;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.payment.service.implementation.PaymentServiceImpl;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityWindowResponse;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Salle occupee de 08:00 a 16:00 : les suggestions trouvent l'apres-midi au lieu de rien, la
 * verification rend la plage occupee en entier ; un membre paie avec le portefeuille de son client
 * personne.
 */
class SlotSuggestionsAndOccupancyTest {

    private static final LocalDateTime DAY = LocalDateTime.of(2026, 11, 17, 0, 0);
    private final Resource room = Resource.builder().id(1L).code("RES-SAL-1").name("Salle de réunion").capacity(15)
            .resourceType(ResourceType.builder().code("RTY-1").name("Salle").wholeResourceBooking(true).build()).build();

    @Test
    void suggestionsWidenToTheRestOfTheDayAndAreSortedByProximity() {
        ResourceService resources = mock(ResourceService.class);
        ResourceAvailabilityService availability = mock(ResourceAvailabilityService.class);
        BookingPricingCalculator pricing = mock(BookingPricingCalculator.class);
        when(resources.getResourceForService("RES-SAL-1")).thenReturn(room);
        when(pricing.calculate(any(Resource.class), any(), any(), anyInt(), any(), any())).thenReturn(new BookingPricingCalculator.Price(
                ResourceBookingUnit.HOUR, new BigDecimal("10000"), BigDecimal.ONE, new BigDecimal("10000"), "XAF", null));
        // Rien a quatre heures pres ; l'apres-midi libre sur la journee.
        when(availability.findRemainingWindows(eq("RES-SAL-1"), eq(DAY.withHour(4)), eq(DAY.withHour(13)), eq(60), eq(1))).thenReturn(List.of());
        when(availability.findRemainingWindows(eq("RES-SAL-1"), eq(DAY), eq(DAY.plusDays(1)), eq(60), eq(1))).thenReturn(List.of(
                window(17), window(16), window(18), window(19), window(20)));
        BookingSuggestionServiceImpl service = new BookingSuggestionServiceImpl(resources, availability, pricing);

        List<BookingSuggestionResponse> suggestions = service.suggest(request());
        assertThat(suggestions).extracting(suggestion -> suggestion.startedAt().getHour()).containsExactly(16, 17, 18, 19, 20);
        assertThat(suggestions).allMatch(suggestion -> "same_day_slot".equals(suggestion.reason()));
    }

    @Test
    void theOccupiedRangeIsReturnedWhole() {
        ResourceAvailabilityRepository slots = mock(ResourceAvailabilityRepository.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        List<ResourceAvailability> day = new ArrayList<>();
        for (int hour = 8; hour < 18; hour++) {
            boolean taken = hour < 16;
            day.add(ResourceAvailability.builder().startedAt(DAY.withHour(hour)).endedAt(DAY.withHour(hour + 1)).active(true).available(!taken)
                    .remainingCapacity(taken ? 0 : 15).totalCapacity(15).build());
        }
        when(slots.findAllSlotsInRange(eq(room), any(), any())).thenReturn(day);
        when(jdbc.queryForList(Mockito.anyString(), eq(String.class), any(), any(), any())).thenReturn(List.of("BKG-SAL-20261114-00000007"));

        List<BookingAvailabilityResponse.OccupiedRange> ranges = new BookingOccupancyFinder(slots, jdbc)
                .occupiedAround(room, DAY.withHour(8), DAY.withHour(9), 1);
        assertThat(ranges).singleElement().satisfies(range -> {
            assertThat(range.startedAt()).isEqualTo(DAY.withHour(8));
            assertThat(range.endedAt()).isEqualTo(DAY.withHour(16));
            assertThat(range.reason()).isEqualTo("BOOKED");
            assertThat(range.bookingNumbers()).containsExactly("BKG-SAL-20261114-00000007");
        });
        BookingAvailabilityResponse response = new BookingAvailabilityResponse("RES-SAL-1", "Salle", DAY.withHour(8), DAY.withHour(9), 60, 1, false,
                null, null, null, null, "XAF", "Occupé", ranges);
        assertThat(response.withoutBookingNumbers().occupiedRanges()).singleElement().satisfies(range -> assertThat(range.bookingNumbers()).isNull());
    }

    @Test
    void aMemberPaysWithTheWalletOfHisPersonCustomer() {
        PaymentServiceImpl payments = mock(PaymentServiceImpl.class, Mockito.CALLS_REAL_METHODS);
        MemberBillingContact contact = mock(MemberBillingContact.class);
        Member member = Member.builder().id(5L).memberId("MBR-202606-00000000").build();
        when(contact.memberOf("CUSTOMER", "CUS-PER-202606-00000000")).thenReturn(Optional.of(member));
        when(contact.memberOf("MEMBER", "MBR-202606-00000000")).thenReturn(Optional.of(member));
        when(contact.memberOf("CUSTOMER", "CUS-AUTRE")).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(payments, "partyContact", contact);

        assertThat(ReflectionTestUtils.<Boolean>invokeMethod(payments, "sameParty", "CUSTOMER", "CUS-PER-202606-00000000", "MEMBER",
                "MBR-202606-00000000")).isTrue();
        assertThat(ReflectionTestUtils.<Boolean>invokeMethod(payments, "sameParty", "CUSTOMER", "CUS-AUTRE", "MEMBER", "MBR-202606-00000000")).isFalse();
        assertThat(ReflectionTestUtils.<Boolean>invokeMethod(payments, "sameParty", "MEMBER", "MBR-1", "member", "mbr-1")).isTrue();
    }

    private static ResourceAvailabilityWindowResponse window(int hour) {
        return ResourceAvailabilityWindowResponse.builder().resourceCode("RES-SAL-1").startedAt(DAY.withHour(hour)).endedAt(DAY.withHour(hour + 1))
                .durationMinutes(60).remainingCapacity(15).build();
    }

    private static BookingAvailabilityRequest request() {
        return new BookingAvailabilityRequest("RES-SAL-1", "MBR-202606-00000000", null, null, null, null, false, null, null, null,
                BookingPaymentMode.DIRECT, DAY.withHour(8), DAY.withHour(9), 1, null);
    }
}
