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
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + bookingNumber));
        if (!StringUtils.hasText(booking.getContactEmail())) {
            // Lot B6 (ecart Bn-16) : dire pourquoi rien n'est prevu, au lieu d'un 200 avec 0.
            throw com.sni.bokaticowork.core.exception.customs.BadRequestException.coded("RAPPEL_SANS_COURRIEL",
                    "Aucun courriel de contact sur la réservation " + bookingNumber + " : rappel impossible");
        }
        if (booking.getStatus() != com.sni.bokaticowork.features.booking.enums.BookingStatus.CONFIRMED) {
            throw new com.sni.bokaticowork.core.exception.customs.ConflictException("Un rappel se prévoit pour une réservation confirmée");
        }
        return queue(booking, minutesBefore);
    }

    private int queue(Booking booking, int minutesBefore) {
        Instant at = booking.getStartedAt().minusMinutes(minutesBefore).atZone(com.sni.bokaticowork.core.utils.time.AppTime.zone()).toInstant();
        if (notificationRepository.existsReminder(booking.getId(), at)) {
            return 0;
        }
        notificationRepository.save(BookingNotificationOutbox.builder()
                .notificationNumber(sequenceGenerator.next("booking_notification"))
                .booking(booking)
                .notificationType(BookingNotificationType.REMINDER)
                .recipient(booking.getContactEmail())
                .subject("Rappel de réservation " + booking.getBookingNumber())
                .body(reminderBody(booking))
                .scheduledAt(at)
                .status(BookingNotificationStatus.PENDING)
                .build());
        return 1;
    }

    static final String OFFSETS_KEY = "bookings.reminders.offsets-minutes";
    static final String ENABLED_KEY = "bookings.reminders.enabled";

    private com.sni.bokaticowork.core.settings.platform.PlatformSettings settings;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setSettings(com.sni.bokaticowork.core.settings.platform.PlatformSettings settings) {
        this.settings = settings;
    }

    /**
     * Lot B6 : rappels automatiques. Pour chaque reservation confirmee a venir avec un courriel,
     * un rappel par delai de {@code bookings.reminders.offsets-minutes} (« 1440,120 » : la veille et
     * 2 h avant), sans doublon ; un delai deja passe est saute.
     */
    @Override
    public int scheduleUpcoming(int limit) {
        if (settings != null && !settings.bool(ENABLED_KEY, true)) {
            return 0;
        }
        java.util.List<Integer> offsets = offsets(settings == null ? "1440,120" : settings.text(OFFSETS_KEY, "1440,120"));
        if (offsets.isEmpty()) {
            return 0;
        }
        LocalDateTime now = com.sni.bokaticowork.core.utils.time.AppTime.now();
        int max = offsets.stream().mapToInt(Integer::intValue).max().orElse(0);
        int queued = 0;
        for (String number : notificationRepository.findBookingsToRemind(now, now.plusMinutes(max + 10L), Math.max(1, limit))) {
            Booking booking = bookingRepository.findByBookingNumber(number).orElse(null);
            if (booking == null) {
                continue;
            }
            for (int offset : offsets) {
                if (booking.getStartedAt().minusMinutes(offset).isAfter(now)) {
                    queued += queue(booking, offset);
                }
            }
        }
        return queued;
    }

    static java.util.List<Integer> offsets(String raw) {
        java.util.List<Integer> values = new java.util.ArrayList<>();
        for (String part : (raw == null ? "" : raw).split(",")) {
            try {
                int value = Integer.parseInt(part.trim());
                if (value > 0 && !values.contains(value)) {
                    values.add(value);
                }
            } catch (NumberFormatException ignored) {
                // Valeur mal saisie : ignoree.
            }
        }
        return values;
    }

    @Override
    public int dispatchDue(int limit) {
        var due = notificationRepository.findDue(Instant.now(), limit);
        due.forEach(notification -> {
            var status = notification.getBooking().getStatus();
            if (status != com.sni.bokaticowork.features.booking.enums.BookingStatus.CONFIRMED
                    && status != com.sni.bokaticowork.features.booking.enums.BookingStatus.IN_PROGRESS) {
                // Lot B6 (ecart Bn-16) : plus de rappel pour une reservation annulee, expiree ou absente.
                notification.setStatus(BookingNotificationStatus.CANCELLED);
                notification.setErrorMessage("Réservation " + status.label() + " : rappel annulé");
                notification.setDispatchedAt(Instant.now());
                notificationRepository.save(notification);
                return;
            }
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
                notification.setErrorMessage("Le service d'envoi a refusé le courriel");
            }
            notificationRepository.save(notification);
        });
        return due.size();
    }

    private String reminderBody(Booking booking) {
        String name = StringUtils.hasText(booking.getContactName()) ? booking.getContactName().trim() : "";
        return "Bonjour " + name + ",\n\nRappel : votre réservation " + booking.getBookingNumber() + " ("
                + booking.getResource().getName() + ") commence le " + reminderTime(booking) + ".\n"
                + "Présentez votre QR à l'accueil. Pour annuler ou déplacer, passez par votre espace client ou contactez l'accueil.\n";
    }

    /** L'heure du rappel dans le fuseau du membre, sinon l'heure de l'espace. */
    private String reminderTime(Booking booking) {
        LocalDateTime start = BookingMemberPreferences.inZone(booking.getStartedAt(), memberPreferences.zone(booking));
        return start == null ? "" : REMINDER_FMT.format(start);
    }
}
