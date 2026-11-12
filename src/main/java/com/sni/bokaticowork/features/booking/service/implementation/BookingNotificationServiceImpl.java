package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.booking.enums.BookingNotificationStatus;
import com.sni.bokaticowork.features.booking.enums.BookingNotificationType;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingNotificationOutbox;
import com.sni.bokaticowork.features.booking.repository.BookingNotificationOutboxRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingNotificationService;
import com.sni.bokaticowork.features.booking.service.support.BookingMemberPreferences;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BookingNotificationServiceImpl implements BookingNotificationService {

    private final BookingRepository bookingRepository;
    private final BookingNotificationOutboxRepository notificationRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final DefaultEmailSender emailSender;
    private final BookingMemberPreferences memberPreferences;

    private static final DateTimeFormatter REMINDER_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", Locale.FRENCH);

    @Override
    public int queueReminder(String bookingNumber, int minutesBefore) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + bookingNumber + " not found"));
        if (!StringUtils.hasText(booking.getContactEmail())) {
            return 0;
        }
        notificationRepository.save(BookingNotificationOutbox.builder()
                .notificationNumber(sequenceGenerator.next("booking_notification"))
                .booking(booking)
                .notificationType(BookingNotificationType.REMINDER)
                .recipient(booking.getContactEmail())
                .subject("Booking reminder " + booking.getBookingNumber())
                .body("Reminder for booking " + booking.getBookingNumber() + " at " + reminderTime(booking))
                .scheduledAt(booking.getStartedAt().minusMinutes(minutesBefore).atZone(ZoneId.systemDefault()).toInstant())
                .status(BookingNotificationStatus.PENDING)
                .build());
        return 1;
    }

    @Override
    public int dispatchDue(int limit) {
        var due = notificationRepository.findDue(Instant.now(), limit);
        due.forEach(notification -> {
            if (!memberPreferences.allows(notification.getBooking(), EventType.BOOKING_REMINDER, NotificationChannel.EMAIL)) {
                // Coupe par le membre : le rappel ne part pas, sans compter comme un echec.
                log.info("Rappel {} non envoye : preference du membre", notification.getNotificationNumber());
                notification.setStatus(BookingNotificationStatus.CANCELLED);
                notification.setErrorMessage("Coupe par la preference du membre");
                notification.setDispatchedAt(Instant.now());
                notificationRepository.save(notification);
                return;
            }
            notification.setAttemptCount(notification.getAttemptCount() + 1);
            boolean sent = emailSender.sendEmail(notification.getRecipient(), notification.getSubject(), notification.getBody()).join();
            notification.setStatus(sent ? BookingNotificationStatus.SENT : BookingNotificationStatus.FAILED);
            notification.setDispatchedAt(Instant.now());
            if (!sent) {
                notification.setErrorMessage("Email provider returned failure");
            }
            notificationRepository.save(notification);
        });
        return due.size();
    }

    /** L'heure du rappel dans le fuseau du membre, sinon l'heure de l'espace. */
    private String reminderTime(Booking booking) {
        LocalDateTime start = BookingMemberPreferences.inZone(booking.getStartedAt(), memberPreferences.zone(booking));
        return start == null ? "" : REMINDER_FMT.format(start);
    }
}
