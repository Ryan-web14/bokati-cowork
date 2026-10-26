package com.sni.bokaticowork.features.portal.booking;

import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.features.booking.dto.response.BookingAvailabilityResponse;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingAvailabilityService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.booking.dto.request.ClientCheckAvailabilityRequest;
import com.sni.bokaticowork.features.portal.booking.dto.request.ClientCreateBookingRequest;
import com.sni.bokaticowork.features.portal.booking.service.ClientPortalBookingService;
import com.sni.bokaticowork.features.portal.context.SubscriberAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sur le portail, la soiree n'est reservable que par un abonne.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientPortalBookingEveningTest {

    private static final LocalDateTime EVENING = LocalDateTime.of(2026, 11, 3, 19, 0);

    @Mock private BookingService bookingService;
    @Mock private BookingAvailabilityService bookingAvailabilityService;
    @Mock private SubscriberAccess subscriberAccess;
    @InjectMocks private ClientPortalBookingService service;

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setMemberId("MBR-1");
        member.setFirstname("Joel");
        member.setLastname("B");
    }

    @Test
    @DisplayName("Un non-abonne ne cree pas de reservation le soir")
    void nonSubscriberCannotBookTheEvening() {
        ClientCreateBookingRequest request = new ClientCreateBookingRequest();
        request.setResourceCode("RES-1");
        request.setStartedAt(EVENING);
        request.setEndedAt(EVENING.plusHours(1));
        doThrow(new ForbiddenException("Ce créneau est réservé aux abonnés."))
                .when(subscriberAccess).assertMayBook(member, "RES-1", EVENING, EVENING.plusHours(1));

        assertThatThrownBy(() -> service.createBooking(member, request)).isInstanceOf(ForbiddenException.class);
        verify(bookingService, never()).create(any());
    }

    @Test
    @DisplayName("La verification de disponibilite repond indisponible a un non-abonne, sans erreur")
    void availabilityCheckSaysReservedToSubscribers() {
        ClientCheckAvailabilityRequest request = new ClientCheckAvailabilityRequest();
        request.setResourceCode("RES-1");
        request.setStartedAt(EVENING);
        request.setEndedAt(EVENING.plusHours(1));
        when(bookingAvailabilityService.check(any())).thenReturn(new BookingAvailabilityResponse(
                "RES-1", "Open space", EVENING, EVENING.plusHours(1), 60, 1, true, 10,
                null, null, null, "XAF", "Disponible"));
        when(subscriberAccess.isDenied(member, "RES-1", EVENING, EVENING.plusHours(1))).thenReturn(true);

        BookingAvailabilityResponse response = service.checkAvailability(member, request);

        assertThat(response.available()).isFalse();
        assertThat(response.message()).isEqualTo("Créneau réservé aux abonnés");
    }

    @Test
    @DisplayName("Un abonne voit la soiree disponible")
    void subscriberSeesTheEveningAvailable() {
        ClientCheckAvailabilityRequest request = new ClientCheckAvailabilityRequest();
        request.setResourceCode("RES-1");
        request.setStartedAt(EVENING);
        request.setEndedAt(EVENING.plusHours(1));
        when(bookingAvailabilityService.check(any())).thenReturn(new BookingAvailabilityResponse(
                "RES-1", "Open space", EVENING, EVENING.plusHours(1), 60, 1, true, 10,
                null, null, null, "XAF", "Disponible"));
        when(subscriberAccess.isDenied(member, "RES-1", EVENING, EVENING.plusHours(1))).thenReturn(false);

        assertThat(service.checkAvailability(member, request).available()).isTrue();
    }
}
