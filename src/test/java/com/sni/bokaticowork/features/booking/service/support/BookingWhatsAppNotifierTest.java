package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.ressource.model.Resource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Variables des modeles reservation_confirmee et reservation_modifiee, dans l'ordre des modeles approuves. */
class BookingWhatsAppNotifierTest {

    @Test
    void confirmedAndUpdatedFollowTheApprovedTemplates() {
        Resource room = mock(Resource.class);
        when(room.getName()).thenReturn("Salle Kouilou");
        Booking booking = Booking.builder().bookingNumber("BKG-2026-00123").resource(room)
                .startedAt(LocalDateTime.of(2026, 11, 14, 9, 0)).endedAt(LocalDateTime.of(2026, 11, 14, 11, 0)).build();

        assertThat(BookingWhatsAppNotifier.values(booking, BookingWhatsAppNotifier.CONFIRMED, "Awa", BookingMemberPreferences.SITE_ZONE))
                .isEqualTo(List.of("Awa", "Salle Kouilou", "14/11/2026", "09:00 - 11:00", "BKG-2026-00123"));
        assertThat(BookingWhatsAppNotifier.values(booking, BookingWhatsAppNotifier.UPDATED, "Awa", BookingMemberPreferences.SITE_ZONE))
                .isEqualTo(List.of("Awa", "BKG-2026-00123", "Salle Kouilou", "14/11/2026", "09:00 - 11:00"));
        assertThat(BookingWhatsAppNotifier.hours(LocalDateTime.of(2026, 11, 14, 18, 0), LocalDateTime.of(2026, 11, 15, 8, 0)))
                .isEqualTo("18:00 - 15/11/2026 08:00");
    }

    @Test
    void onlyConfirmationAndChangesSendAWhatsApp() {
        assertThat(BookingWhatsAppNotifier.purpose(BookingEventType.BOOKING_CONFIRMED)).contains("BOOKING_CONFIRMED");
        assertThat(BookingWhatsAppNotifier.purpose(BookingEventType.BOOKING_RESCHEDULED)).contains("BOOKING_UPDATED");
        assertThat(BookingWhatsAppNotifier.purpose(BookingEventType.BOOKING_RESOURCE_CHANGED)).contains("BOOKING_UPDATED");
        assertThat(BookingWhatsAppNotifier.purpose(BookingEventType.BOOKING_CREATED)).isEmpty();
        assertThat(BookingWhatsAppNotifier.purpose(BookingEventType.BOOKING_CANCELLED)).isEmpty();
    }
}
