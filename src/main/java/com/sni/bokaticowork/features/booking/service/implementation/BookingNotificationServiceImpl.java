package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.booking.enums.BookingNotificationStatus;
import com.sni.bokaticowork.features.booking.enums.BookingNotificationType;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingNotificationOutbox;
import com.sni.bokaticowork.features.booking.repository.BookingNotificationOutboxRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingNotificationServiceImpl implements BookingNotificationService {

    private final BookingRepository bookingRepository;
    private final BookingNotificationOutboxRepository notificationRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final DefaultEmailSender emailSender;

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
                .body("Reminder for booking " + booking.getBookingNumber() + " at " + booking.getStartedAt())
                .scheduledAt(booking.getStartedAt().minusMinutes(minutesBefore).atZone(ZoneId.systemDefault()).toInstant())
                .status(BookingNotificationStatus.PENDING)
                .build());
        return 1;
    }

    @Override
    public int dispatchDue(int limit) {
        var due = notificationRepository.findDue(Instant.now(), limit);
        due.forEach(notification -> {
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
}
