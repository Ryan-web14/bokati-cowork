package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.features.booking.dto.request.BookingAvailabilityRequest;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingIdentityResolver;
import com.sni.bokaticowork.features.booking.service.support.BookingPaymentContextResolver;
import com.sni.bokaticowork.features.booking.service.support.BookingPricingCalculator;
import com.sni.bokaticowork.features.booking.service.support.BookingResourceGuard;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityWindowResponse;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import com.sni.bokaticowork.features.ressource.service.support.ResourceSlotPolicy;
import com.sni.bokaticowork.features.ressource.service.support.StaffBookingOverride;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * La verification d'un creneau passe · refusee au client, acceptee a l'equipe qui reserve pour lui.
 */
class StaffAvailabilityCheckTest {

    private final ResourceService resources = mock(ResourceService.class);
    private final ResourceAvailabilityService availabilities = mock(ResourceAvailabilityService.class);
    private final BookingPricingCalculator pricing = mock(BookingPricingCalculator.class);
    private final StaffBookingOverride staffOverride = new StaffBookingOverride(31);
    private final BookingAvailabilityServiceImpl service = new BookingAvailabilityServiceImpl(resources, availabilities,
            new BookingResourceGuard(mock(BookingRepository.class), new ResourceSlotPolicy(), staffOverride), pricing,
            mock(BookingIdentityResolver.class), mock(BookingPaymentContextResolver.class), staffOverride);

    private final LocalDateTime start = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).minusHours(2);
    private BookingAvailabilityRequest request;

    @BeforeEach
    void setUp() {
        Resource resource = Resource.builder().code("RES-SAL-1").name("Salle de réunion").capacity(8).bookingEnabled(true)
                .active(true).status(ResourceStatus.ACTIVE)
                .resourceType(ResourceType.builder().code("RTY-1").name("Salle").bookableSlots(1).wholeResourceBooking(true).build())
                .resourcePolicy(ResourcePolicy.builder().minBookingDurationMinutes(30).maxBookingDurationMinutes(480)
                        .minBookingNoticeMinutes(60).cancellationNoticeMinutes(60).build())
                .build();
        when(resources.getResourceForService("RES-SAL-1")).thenReturn(resource);
        when(availabilities.findRemainingWindows(eq("RES-SAL-1"), any(), any(), anyInt(), anyInt())).thenReturn(List.of(
                ResourceAvailabilityWindowResponse.builder().resourceCode("RES-SAL-1").startedAt(start)
                        .endedAt(start.plusHours(1)).durationMinutes(60).remainingCapacity(1).slotCount(2).build()));
        when(pricing.calculate(any(), any(), any(), anyInt(), any())).thenReturn(new BookingPricingCalculator.Price(
                ResourceBookingUnit.HOUR, new BigDecimal("10000"), BigDecimal.ONE, new BigDecimal("10000"), "XAF"));
        request = new BookingAvailabilityRequest("RES-SAL-1", "MBR-1", null, null, null, null, false, null, null, null,
                BookingPaymentMode.DIRECT, start, start.plusHours(1), 1, null);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Client : un creneau passe n'est pas disponible")
    void clientPastIsUnavailable() {
        var response = service.check(request);

        assertThat(response.available()).isFalse();
        assertThat(response.message()).contains("past");
    }

    @Test
    @DisplayName("Agent des operations : le creneau passe est disponible, le message dit pourquoi")
    void agentPastIsAvailable() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("support@sni-cg.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        var response = service.check(request);

        assertThat(response.available()).isTrue();
        assertThat(response.estimatedAmount()).isEqualByComparingTo("10000");
        assertThat(response.message()).contains("hors préavis");
    }
}
