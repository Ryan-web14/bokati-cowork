package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingHoldRequest;
import com.sni.bokaticowork.features.booking.model.BookingHold;
import com.sni.bokaticowork.features.booking.repository.BookingHoldRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingIdentityResolver;
import com.sni.bokaticowork.features.booking.service.support.BookingResourceGuard;
import com.sni.bokaticowork.features.ressource.dto.request.ReserveResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import com.sni.bokaticowork.features.ressource.service.support.ResourceSlotPolicy;
import com.sni.bokaticowork.features.ressource.service.support.StaffBookingOverride;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'option suit la reservation : l'equipe pose une option sur un debut passe, le client non. Avant,
 * l'option refusait toujours (« the requested start time is in the past »).
 */
class StaffBookingHoldTest {

    private final ResourceService resources = mock(ResourceService.class);
    private final ResourceAvailabilityService availabilities = mock(ResourceAvailabilityService.class);
    private final BookingHoldRepository holds = mock(BookingHoldRepository.class);
    private final BookingIdentityResolver identities = mock(BookingIdentityResolver.class);
    private final StaffBookingOverride staffOverride = new StaffBookingOverride(31);
    private final BookingHoldServiceImpl service = new BookingHoldServiceImpl(holds, resources, availabilities,
            new BookingResourceGuard(mock(BookingRepository.class), new ResourceSlotPolicy(), staffOverride),
            mock(SequenceGeneratorFacade.class), identities);

    private final LocalDateTime start = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).minusHours(2);
    private CreateBookingHoldRequest request;

    @BeforeEach
    void setUp() {
        service.setStaffOverride(staffOverride);
        Resource resource = Resource.builder().id(1L).code("RES-SAL-1").name("Salle de réunion").capacity(8).bookingEnabled(true)
                .active(true).status(ResourceStatus.ACTIVE)
                .resourceType(ResourceType.builder().code("RTY-1").name("Salle").bookableSlots(1).wholeResourceBooking(true).build())
                .resourcePolicy(ResourcePolicy.builder().minBookingDurationMinutes(30).maxBookingDurationMinutes(480)
                        .minBookingNoticeMinutes(60).cancellationNoticeMinutes(60).build())
                .build();
        when(resources.getResourceForService("RES-SAL-1")).thenReturn(resource);
        when(identities.resolve(any())).thenReturn(new BookingIdentityResolver.ResolvedBookingIdentity(
                SubscriberType.MEMBER, "MBR-1", "Awa", null, null, false));
        when(holds.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        request = new CreateBookingHoldRequest("RES-SAL-1", "MBR-1", null, null, null, null, false, null, null, null,
                start, start.plusHours(1), 1, 10, null);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void clientCannotHoldAPastStart() {
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(ConflictException.class)
                .hasMessageContaining("in the past");
    }

    @Test
    void staffHoldsAPastStartAndReservesItsSlots() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin@bokati.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        assertThat(service.create(request)).isNotNull();
        ArgumentCaptor<ReserveResourceAvailabilityRequest> reserved = ArgumentCaptor.forClass(ReserveResourceAvailabilityRequest.class);
        verify(availabilities).reserve(reserved.capture());
        assertThat(reserved.getValue().isStaffOverride()).isTrue();
        verify(holds).save(any(BookingHold.class));
    }
}
