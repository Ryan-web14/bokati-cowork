package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.booking.enums.BookingNotificationStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingNotificationOutbox;
import com.sni.bokaticowork.features.booking.repository.BookingNotificationOutboxRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingMemberPreferences;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingReminderPreferenceTest {

    private final BookingNotificationOutboxRepository outboxRepository = mock(BookingNotificationOutboxRepository.class);
    private final DefaultEmailSender emailSender = mock(DefaultEmailSender.class);
    private final BookingMemberPreferences preferences = mock(BookingMemberPreferences.class);
    private final BookingNotificationServiceImpl service = new BookingNotificationServiceImpl(
            mock(BookingRepository.class), outboxRepository, mock(SequenceGeneratorFacade.class), emailSender, preferences);

    private BookingNotificationOutbox reminder(Booking booking) {
        return BookingNotificationOutbox.builder()
                .notificationNumber("BN-1")
                .booking(booking)
                .recipient("awa@example.com")
                .subject("Booking reminder BKG-1")
                .body("Reminder")
                .status(BookingNotificationStatus.PENDING)
                .attemptCount(0)
                .build();
    }

    @Test
    @DisplayName("Rappel coupe par le membre : non envoye, annule, sans compter comme un echec")
    void reminderCutByMember() {
        Booking booking = Booking.builder().bookingNumber("BKG-1").build();
        BookingNotificationOutbox notification = reminder(booking);
        when(outboxRepository.findDue(any(), anyInt())).thenReturn(List.of(notification));
        when(preferences.allows(booking, EventType.BOOKING_REMINDER, NotificationChannel.EMAIL)).thenReturn(false);

        service.dispatchDue(10);

        verify(emailSender, never()).sendEmail(anyString(), anyString(), anyString());
        assertThat(notification.getStatus()).isEqualTo(BookingNotificationStatus.CANCELLED);
        assertThat(notification.getAttemptCount()).isZero();
    }

    @Test
    @DisplayName("Sans preference contraire, le rappel part")
    void reminderSentByDefault() {
        Booking booking = Booking.builder().bookingNumber("BKG-1").build();
        BookingNotificationOutbox notification = reminder(booking);
        when(outboxRepository.findDue(any(), anyInt())).thenReturn(List.of(notification));
        when(preferences.allows(booking, EventType.BOOKING_REMINDER, NotificationChannel.EMAIL)).thenReturn(true);
        when(emailSender.sendEmail(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(true));

        service.dispatchDue(10);

        verify(emailSender).sendEmail("awa@example.com", "Booking reminder BKG-1", "Reminder");
        assertThat(notification.getStatus()).isEqualTo(BookingNotificationStatus.SENT);
    }
}
