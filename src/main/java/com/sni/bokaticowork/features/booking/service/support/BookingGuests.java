package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.reference.service.ReferenceLists;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.enums.BookingParticipantStatus;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingParticipant;
import com.sni.bokaticowork.features.booking.repository.BookingParticipantRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lot B9d : invites d'une reunion et details de la reservation.
 *
 * <p>Invitation par courriel avec deux liens a jeton propre a chaque invite (« je participe »,
 * « je ne participe pas ») ; reponse notee sur l'invite ; pointage de chaque invite a l'accueil.
 * Details : disposition de la salle (liste souple {@code BOOKING_ROOM_LAYOUT}), demandes speciales,
 * notes internes de l'equipe et etiquettes (liste souple {@code BOOKING_TAG}).</p>
 */
@Slf4j
@Component
public class BookingGuests {

    public static final String ROOM_LAYOUT_LIST = "BOOKING_ROOM_LAYOUT";
    public static final String TAG_LIST = "BOOKING_TAG";
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH:mm", Locale.FRENCH);
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Details(String roomLayout, String specialRequests, String internalNotes, List<String> tags) {
    }

    private final BookingRepository bookings;
    private final BookingParticipantRepository participants;
    private final BookingEventWriter events;

    @Value("${app.api-base-url:https://api.elleaose.com}")
    private String apiBaseUrl = "https://api.elleaose.com";

    private DefaultEmailSender email;
    private ReferenceLists lists;
    private PlatformSettings settings;

    public BookingGuests(BookingRepository bookings, BookingParticipantRepository participants, BookingEventWriter events) {
        this.bookings = bookings;
        this.participants = participants;
        this.events = events;
    }

    @Autowired(required = false)
    void setOptional(DefaultEmailSender email, ReferenceLists lists, PlatformSettings settings) {
        this.email = email;
        this.lists = lists;
        this.settings = settings;
    }

    // ------------------------------------------------------------------ invitations

    /** Envoie (ou renvoie) l'invitation d'un invite ; rend le nombre de courriels partis. */
    @Transactional
    public int invite(String bookingNumber, Long participantId) {
        Booking booking = booking(bookingNumber);
        requireLive(booking);
        BookingParticipant participant = participants.findByIdAndBookingId(participantId, booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant introuvable"));
        return send(booking, participant) ? 1 : 0;
    }

    /** Invite tous les participants qui ont un courriel et n'ont pas encore repondu. */
    @Transactional
    public int inviteAll(String bookingNumber) {
        Booking booking = booking(bookingNumber);
        requireLive(booking);
        int sent = 0;
        for (BookingParticipant participant : participants.findByBookingId(booking.getId())) {
            if (participant.getStatus() == BookingParticipantStatus.INVITED && participant.getRespondedAt() == null && send(booking, participant)) {
                sent++;
            }
        }
        return sent;
    }

    /** Reponse de l'invite par son lien ; rend le participant. */
    @Transactional
    public BookingParticipant respond(String token, boolean accepted) {
        BookingParticipant participant = participants.findByInviteToken(token == null ? "" : token.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation introuvable"));
        if (participant.getStatus() == BookingParticipantStatus.REMOVED) {
            throw new ConflictException("Cette invitation a été retirée par l'organisateur");
        }
        Booking booking = participant.getBooking();
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED
                || booking.getStatus() == BookingStatus.EXPIRED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ConflictException("La réunion est " + booking.getStatus().label() + " : la réponse n'est plus nécessaire");
        }
        participant.setStatus(accepted ? BookingParticipantStatus.CONFIRMED : BookingParticipantStatus.DECLINED);
        participant.setRespondedAt(Instant.now());
        participants.save(participant);
        events.write(booking, BookingEventType.BOOKING_PARTICIPANT_ANSWERED, accepted ? "Invité présent" : "Invité absent",
                label(participant) + (accepted ? " participera" : " ne participera pas"), null);
        return participant;
    }

    /** Pointage d'un invite a l'accueil. */
    @Transactional
    public BookingParticipant checkIn(String bookingNumber, Long participantId) {
        Booking booking = booking(bookingNumber);
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("Seule une réservation confirmée ou en cours se pointe");
        }
        BookingParticipant participant = participants.findByIdAndBookingId(participantId, booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant introuvable"));
        if (participant.getStatus() == BookingParticipantStatus.REMOVED) {
            throw new ConflictException("Ce participant a été retiré");
        }
        if (participant.getCheckedInAt() == null) {
            participant.setCheckedInAt(Instant.now());
            participants.save(participant);
            events.write(booking, BookingEventType.BOOKING_PARTICIPANT_CHECKED_IN, "Invité arrivé", label(participant), null);
        }
        return participant;
    }

    private boolean send(Booking booking, BookingParticipant participant) {
        if (email == null || !StringUtils.hasText(participant.getEmail())) {
            return false;
        }
        if (!StringUtils.hasText(participant.getInviteToken())) {
            byte[] bytes = new byte[18];
            RANDOM.nextBytes(bytes);
            participant.setInviteToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        }
        String base = (settings == null ? apiBaseUrl : settings.text("app.api-base-url", apiBaseUrl)).replaceAll("/+$", "")
                + ApiPath.V1 + "/public/bookings/invitations/" + participant.getInviteToken() + "?answer=";
        String host = StringUtils.hasText(booking.getContactName()) ? booking.getContactName() : "L'organisateur";
        String body = "Bonjour " + (StringUtils.hasText(participant.getName()) ? participant.getName() : "") + ",\n\n"
                + host + " vous invite : " + booking.getResource().getName() + ", " + WHEN.format(booking.getStartedAt())
                + " (réservation " + booking.getBookingNumber() + ").\n\n"
                + "Je participe : " + base + "ACCEPT\nJe ne participe pas : " + base + "DECLINE\n";
        try {
            email.sendEmail(participant.getEmail(), "Invitation : " + booking.getResource().getName() + ", " + WHEN.format(booking.getStartedAt()), body);
            participant.setInvitedAt(Instant.now());
            participants.save(participant);
            events.write(booking, BookingEventType.BOOKING_PARTICIPANT_INVITED, "Invitation envoyée", label(participant), null);
            return true;
        } catch (RuntimeException ex) {
            log.warn("Invitation de {} pour {} non envoyee : {}", participant.getEmail(), booking.getBookingNumber(), ex.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------ details

    /** Disposition, demandes speciales, notes internes, etiquettes ; seuls les champs donnes changent. */
    @Transactional
    public Booking updateDetails(String bookingNumber, Details details) {
        Booking booking = booking(bookingNumber);
        if (details.roomLayout() != null) {
            booking.setRoomLayout(details.roomLayout().isBlank() ? null
                    : lists == null ? details.roomLayout().trim() : lists.require(ROOM_LAYOUT_LIST, details.roomLayout()).getCode());
        }
        if (details.specialRequests() != null) {
            booking.setSpecialRequests(limit(details.specialRequests(), 1000, "Demandes spéciales"));
        }
        if (details.internalNotes() != null) {
            booking.setInternalNotes(limit(details.internalNotes(), 2000, "Notes internes"));
        }
        if (details.tags() != null) {
            Set<String> codes = new LinkedHashSet<>();
            for (String tag : details.tags()) {
                if (StringUtils.hasText(tag)) {
                    codes.add(lists == null ? ReferenceLists.normalize(tag) : lists.require(TAG_LIST, tag).getCode());
                }
            }
            if (codes.size() > 10) {
                throw new BadRequestException("Dix étiquettes au plus");
            }
            booking.setTags(codes.isEmpty() ? null : String.join(",", codes));
        }
        return bookings.save(booking);
    }

    public static List<String> tags(Booking booking) {
        return booking.getTags() == null || booking.getTags().isBlank() ? List.of() : new ArrayList<>(List.of(booking.getTags().split(",")));
    }

    private static String limit(String text, int max, String what) {
        String trimmed = text.trim();
        if (trimmed.length() > max) {
            throw new BadRequestException(what + " : " + max + " caractères au plus");
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Booking booking(String bookingNumber) {
        return bookings.findByBookingNumber(bookingNumber).orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + bookingNumber));
    }

    private static void requireLive(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING_PAYMENT
                && booking.getStatus() != BookingStatus.PENDING_APPROVAL && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("Une réservation " + booking.getStatus().label() + " n'envoie plus d'invitation");
        }
    }

    private static String label(BookingParticipant participant) {
        return StringUtils.hasText(participant.getName()) ? participant.getName()
                + (StringUtils.hasText(participant.getEmail()) ? " (" + participant.getEmail() + ")" : "") : participant.getEmail();
    }
}
