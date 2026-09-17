package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.features.booking.config.BookingCheckInProperties;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.ressource.model.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingServiceImplTest {

    private final BookingServiceImpl service = new BookingServiceImpl(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
    );

    @Test
    void shouldMarkSubscriptionResourceLineAsCoveredBySubscription() {
        Booking booking = Booking.builder()
                .resource(Resource.builder().name("Bureau Prive").build())
                .paymentMode(BookingPaymentMode.SUBSCRIPTION)
                .subscriptionNumber("SUB-0001")
                .build();

        String description = ReflectionTestUtils.invokeMethod(service, "resourceLineDescription", booking);

        assertEquals("Booking resource - Bureau Prive (Abonne - couvert par abonnement SUB-0001)", description);
    }

    @Test
    void shouldMarkSubscriptionEntitlementLineAsSubscriberUsage() {
        Booking booking = Booking.builder()
                .paymentMode(BookingPaymentMode.SUBSCRIPTION)
                .entitlementCode("ENT-ACC-HOU")
                .build();

        String description = ReflectionTestUtils.invokeMethod(service, "entitlementLineDescription", booking);

        assertEquals("Abonne - consommation de l'abonnement ENT-ACC-HOU", description);
    }

    // ---------------------------------------------------------------------------------------
    // Pointage autonome · la fenetre etait ouverte jusqu'a la fin de la reservation. Pointer a la
    // derniere minute d'un creneau de neuf heures valait presence pleine, ce qui vidait la notion
    // de presence et rendait le marquage automatique en absence pratiquement inoperant.
    // ---------------------------------------------------------------------------------------

    @Test
    void shouldAcceptSelfCheckInAroundTheStartTime() {
        BookingServiceImpl guarded = withCheckInWindow(15, 15);

        assertDoesNotThrow(() -> activation(guarded, LocalDateTime.now().plusMinutes(10)));
        assertDoesNotThrow(() -> activation(guarded, LocalDateTime.now().minusMinutes(10)));
    }

    @Test
    void shouldRefuseSelfCheckInTooEarly() {
        BookingServiceImpl guarded = withCheckInWindow(15, 15);

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> activation(guarded, LocalDateTime.now().plusMinutes(20)));
        assertEquals(true, ex.getMessage().contains("accueil"));
    }

    @Test
    void shouldRefuseSelfCheckInOnceTheToleranceHasPassed() {
        // Le point du changement : autrefois accepte jusqu'a l'heure de fin.
        BookingServiceImpl guarded = withCheckInWindow(15, 15);

        assertThrows(ForbiddenException.class,
                () -> activation(guarded, LocalDateTime.now().minusMinutes(20)));
    }

    @Test
    void shouldNotStartABookingCheckedInBeforeItsHour() {
        // Pointer quinze minutes avant l'heure n'ouvre pas la salle pour autant.
        Booking booking = Booking.builder()
                .status(BookingStatus.CONFIRMED)
                .startedAt(LocalDateTime.now().plusMinutes(10))
                .build();

        ReflectionTestUtils.invokeMethod(service, "startIfHourHasCome", booking);

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void shouldStartABookingCheckedInAtOrAfterItsHour() {
        Booking booking = Booking.builder()
                .status(BookingStatus.CONFIRMED)
                .startedAt(LocalDateTime.now().minusMinutes(1))
                .build();

        ReflectionTestUtils.invokeMethod(service, "startIfHourHasCome", booking);

        assertEquals(BookingStatus.IN_PROGRESS, booking.getStatus());
    }

    private void activation(BookingServiceImpl target, LocalDateTime startedAt) {
        ReflectionTestUtils.invokeMethod(target, "assertActivationAllowed",
                Booking.builder().startedAt(startedAt).endedAt(startedAt.plusHours(9)).build(),
                "check in");
    }

    private BookingServiceImpl withCheckInWindow(int early, int late) {
        BookingCheckInProperties properties = new BookingCheckInProperties();
        properties.setEarlyWindowMinutes(early);
        properties.setLateWindowMinutes(late);
        ReflectionTestUtils.setField(service, "checkInProperties", properties);
        return service;
    }
}
