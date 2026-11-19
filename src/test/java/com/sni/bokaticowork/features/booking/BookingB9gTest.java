package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingCalendarFeed;
import com.sni.bokaticowork.features.booking.service.support.BookingReviews;
import com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier;
import com.sni.bokaticowork.features.ressource.model.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot B9g : avis apres utilisation et agenda synchronise. */
class BookingB9gTest {

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final PlatformSettings settings = mock(PlatformSettings.class);

    private static Booking booking() {
        return Booking.builder().bookingNumber("BKG-1").checkInToken("ABCD-EFGH").status(BookingStatus.COMPLETED)
                .resource(Resource.builder().code("SAL-1").name("Salle Kouilou").build())
                .startedAt(LocalDateTime.of(2026, 12, 1, 9, 0)).endedAt(LocalDateTime.of(2026, 12, 1, 11, 0)).build();
    }

    @Test
    void aLowReviewAlertsTheTeamAndTheCommentNeedsTheBookingToken() throws Exception {
        AdminInAppNotifier alerts = mock(AdminInAppNotifier.class);
        BookingReviews reviews = new BookingReviews(bookings);
        ReflectionTestUtils.setField(reviews, "settings", settings);
        ReflectionTestUtils.setField(reviews, "adminNotifier", alerts);
        when(settings.integer(anyString(), anyInt())).thenAnswer(call -> call.getArgument(1));
        Booking booking = booking();
        booking.setCsatScore(4);
        when(bookings.findByBookingNumber("BKG-1")).thenReturn(Optional.of(booking));

        reviews.rated(booking);
        verify(alerts, never()).broadcastAlert(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        booking.setCsatScore(1);
        assertThatThrownBy(() -> reviews.comment("BKG-1", "FAUX", "Bruit")).isInstanceOf(ResourceNotFoundException.class);
        assertThat(reviews.comment("BKG-1", "ABCD-EFGH", "Climatisation en panne")).isEqualTo(1);
        assertThat(booking.getCsatComment()).isEqualTo("Climatisation en panne");
        verify(alerts).broadcastAlert(eq("BOOKING_LOW_REVIEW"), eq("BOOKING"), anyString(), contains("Climatisation"), eq("WARNING"), anyString());
    }

    @Test
    void theCalendarFeedIsOffByDefaultAndListsTheMemberBookings() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        BookingCalendarFeed feed = new BookingCalendarFeed(jdbc, bookings);
        ReflectionTestUtils.setField(feed, "settings", settings);
        when(settings.bool("bookings.calendar-feed.enabled", false)).thenReturn(false);
        assertThatThrownBy(() -> feed.rotate("MBR-1")).isInstanceOf(ConflictException.class);

        when(settings.bool("bookings.calendar-feed.enabled", false)).thenReturn(true);
        when(settings.text(anyString(), anyString())).thenAnswer(call -> call.getArgument(1));
        when(jdbc.queryForList(contains("FROM booking_calendar_feed WHERE token"), eq(String.class), any(Object[].class))).thenReturn(List.of("MBR-1"));
        when(jdbc.queryForList(contains("FROM booking_calendar_feed WHERE token"), eq(String.class), anyString())).thenReturn(List.of("MBR-1"));
        when(jdbc.queryForList(contains("SELECT booking_number FROM booking"), eq(String.class), any(), any())).thenReturn(List.of("BKG-1"));
        when(bookings.findByBookingNumber("BKG-1")).thenReturn(Optional.of(booking()));

        String ics = feed.calendar("JETON");

        assertThat(ics).startsWith("BEGIN:VCALENDAR").contains("UID:BKG-1@elleaose", "SUMMARY:Salle Kouilou", "DTSTART:20261201T080000Z")
                .endsWith("END:VCALENDAR\r\n");
        assertThat(feed.rotate("MBR-1").url()).contains("/public/bookings/calendar/").endsWith(".ics");
    }
}
