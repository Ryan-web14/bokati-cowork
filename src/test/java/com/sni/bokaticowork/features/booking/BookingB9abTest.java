package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.booking.dto.request.BookingStatusChangeRequest;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.mapper.interfaces.BookingMapper;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.booking.service.support.BookingAttendance;
import com.sni.bokaticowork.features.booking.service.support.BookingDeskService;
import com.sni.bokaticowork.features.booking.service.support.BookingEventWriter;
import com.sni.bokaticowork.features.ressource.model.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lots B9a et B9b : reponse au rappel, planning du jour, series. */
class BookingB9abTest {

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingEventWriter events = mock(BookingEventWriter.class);
    private final BookingService bookingService = mock(BookingService.class);

    @SuppressWarnings("unchecked")
    private ObjectProvider<BookingService> provider() {
        ObjectProvider<BookingService> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(bookingService);
        return provider;
    }

    private static Booking booking(String number, LocalDateTime start, BookingStatus status) {
        Resource resource = Resource.builder().code("SAL-1").name("Salle").build();
        return Booking.builder().bookingNumber(number).resource(resource).status(status).checkInToken("ABCD-EFGH")
                .paymentMode(BookingPaymentMode.DIRECT).startedAt(start).endedAt(start.plusHours(2)).quantity(1).build();
    }

    @Test
    void theReminderLinksRecordTheVisitOrCancelWithTheBookingToken() {
        Booking booking = booking("BKG-1", LocalDateTime.now().plusDays(1), BookingStatus.CONFIRMED);
        when(bookings.findByBookingNumber("BKG-1")).thenReturn(Optional.of(booking));
        when(bookings.save(any())).thenAnswer(call -> call.getArgument(0));
        BookingAttendance attendance = new BookingAttendance(bookings, events, provider());

        assertThatThrownBy(() -> attendance.coming("BKG-1", "MAUVAIS")).isInstanceOf(ResourceNotFoundException.class);
        attendance.coming("BKG-1", "ABCD-EFGH");
        assertThat(booking.getAttendanceAnswer()).isEqualTo("COMING");
        verify(events).write(eq(booking), eq(BookingEventType.BOOKING_ATTENDANCE_CONFIRMED), anyString(), anyString(), any());

        attendance.cancel("BKG-1", "ABCD-EFGH");
        verify(bookingService).cancel(eq("BKG-1"), any(BookingStatusChangeRequest.class));
        assertThat(booking.getAttendanceAnswer()).isEqualTo("CANCELLED");
    }

    @Test
    void thePlanningGroupsByResourceAndFlagsLateArrivals() {
        LocalDate today = LocalDate.now();
        Booking late = booking("BKG-2", LocalDateTime.now().minusMinutes(20), BookingStatus.CONFIRMED);
        Booking arrived = booking("BKG-3", LocalDateTime.now().minusMinutes(30), BookingStatus.IN_PROGRESS);
        arrived.setCheckedInAt(Instant.now());
        Booking cancelled = booking("BKG-4", LocalDateTime.now().plusHours(1), BookingStatus.CANCELLED);
        when(bookings.findForDay(any(), any())).thenReturn(List.of(late, arrived, cancelled));
        BookingDeskService desk = new BookingDeskService(bookings, provider(), mock(BookingMapper.class));

        BookingDeskService.Planning planning = desk.planning(today, null);

        assertThat(planning.bookings()).isEqualTo(2);
        assertThat(planning.arrived()).isEqualTo(1);
        assertThat(planning.late()).isEqualTo(1);
        assertThat(planning.resources()).singleElement().satisfies(resource -> assertThat(resource.bookings())
                .extracting(BookingDeskService.PlanningItem::bookingNumber).containsExactly("BKG-2", "BKG-3"));
        assertThat(planning.resources().getFirst().bookings().getFirst().lateMinutes()).isGreaterThanOrEqualTo(19);
    }

    @Test
    void aSeriesIsCancelledFromADateAndRefusalsAreReported() {
        LocalDateTime base = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        Booking first = booking("BKG-5", base, BookingStatus.CONFIRMED);
        Booking second = booking("BKG-6", base.plusWeeks(1), BookingStatus.CONFIRMED);
        Booking done = booking("BKG-7", base.plusWeeks(2), BookingStatus.CANCELLED);
        when(bookings.findByRecurrenceGroupNumberOrderByStartedAtAsc("SER-1")).thenReturn(List.of(first, second, done));
        when(bookingService.cancel(eq("BKG-6"), any())).thenThrow(new IllegalStateException("Délai dépassé"));
        BookingDeskService desk = new BookingDeskService(bookings, provider(), mock(BookingMapper.class));

        BookingDeskService.SeriesOutcome outcome = desk.cancelSeries("SER-1", null, "Fin du projet");

        assertThat(outcome.changedBookings()).containsExactly("BKG-5");
        assertThat(outcome.refused()).containsEntry("BKG-6", "Délai dépassé");
        assertThat(outcome.skipped()).isEqualTo(1);
    }
}
