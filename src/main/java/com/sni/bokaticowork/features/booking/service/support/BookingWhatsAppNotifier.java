package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppNotifier;
import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import com.sni.bokaticowork.features.billing.service.support.MemberBillingContact;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.notification.service.support.MemberNotificationEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * WhatsApp d'une reservation, en plus du courriel : {@code reservation_confirmee} a la
 * confirmation, {@code reservation_modifiee} quand l'horaire, l'espace ou le titulaire change.
 * Numero : celui du contact de la reservation, sinon celui du membre. Un membre qui a coupe la
 * confirmation de reservation sur WhatsApp ne recoit rien.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingWhatsAppNotifier {

    static final String CONFIRMED = "BOOKING_CONFIRMED";
    static final String UPDATED = "BOOKING_UPDATED";
    static final String PORTAL_PATH = com.sni.bokaticowork.core.communication.whatsapp.WhatsAppPortalPaths.BOOKINGS;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final WhatsAppNotifier whatsApp;
    private final BookingMemberPreferences memberPreferences;

    /** Objet du modele pour cet evenement, ou vide si l'evenement ne donne pas lieu a un WhatsApp. */
    static Optional<String> purpose(BookingEventType eventType) {
        if (eventType == null) {
            return Optional.empty();
        }
        return switch (eventType) {
            case BOOKING_CONFIRMED -> Optional.of(CONFIRMED);
            case BOOKING_RESCHEDULED, BOOKING_RESOURCE_CHANGED, BOOKING_TRANSFERRED -> Optional.of(UPDATED);
            default -> Optional.empty();
        };
    }

    public void notify(Booking booking, BookingEventType eventType) {
        Optional<String> purpose = purpose(eventType);
        if (booking == null || purpose.isEmpty() || !whatsApp.active()) {
            return;
        }
        Optional<Member> member = memberPreferences.member(booking);
        EventType preference = MemberNotificationEvents.of(eventType.name()).orElse(null);
        if (!memberPreferences.allows(member, preference, NotificationChannel.WHATSAPP)) {
            log.info("WhatsApp {} de la reservation {} non envoye : preference du membre", eventType, booking.getBookingNumber());
            return;
        }
        Optional<String> phone = MemberBillingContact.firstPhone(booking.getContactPhone())
                .or(() -> member.flatMap(MemberBillingContact::whatsAppPhone));
        if (phone.isEmpty()) {
            return;
        }
        ZoneId zone = memberPreferences.zone(member);
        List<String> values = values(booking, purpose.get(), firstName(booking, member), zone);
        whatsApp.notify(phone.get(), purpose.get(), values, booking.getBookingNumber(), UPDATED.equals(purpose.get()) ? PORTAL_PATH : null);
    }

    static final String REMINDER = "BOOKING_REMINDER";
    static final String LAST_MINUTE = "BOOKING_LAST_MINUTE";

    /** Lot B9a : rappel WhatsApp (preference BOOKING_REMINDER du membre, ou contact non membre). */
    public boolean remind(Booking booking) {
        if (booking == null || !whatsApp.active()) {
            return false;
        }
        Optional<Member> member = memberPreferences.member(booking);
        if (!memberPreferences.allows(member, EventType.BOOKING_REMINDER, NotificationChannel.WHATSAPP)) {
            return false;
        }
        Optional<String> phone = MemberBillingContact.firstPhone(booking.getContactPhone()).or(() -> member.flatMap(MemberBillingContact::whatsAppPhone));
        if (phone.isEmpty()) {
            return false;
        }
        ZoneId zone = memberPreferences.zone(member);
        whatsApp.notify(phone.get(), REMINDER, values(booking, CONFIRMED, firstName(booking, member), zone), booking.getBookingNumber(), PORTAL_PATH);
        return true;
    }

    /**
     * Lot B9f : offre de derniere minute a un membre volontaire (preference LAST_MINUTE_OFFERS,
     * coupee par defaut). Valeurs : prenom, espace, date, horaire, prix remise, prix normal.
     */
    public boolean lastMinute(Member member, String space, LocalDateTime start, LocalDateTime end, String price, String normalPrice) {
        if (member == null || !whatsApp.active()
                || !memberPreferences.allows(Optional.of(member), EventType.LAST_MINUTE_OFFERS, NotificationChannel.WHATSAPP)) {
            return false;
        }
        Optional<String> phone = MemberBillingContact.whatsAppPhone(member);
        if (phone.isEmpty()) {
            return false;
        }
        String name = StringUtils.hasText(member.getFirstname()) ? member.getFirstname().trim().split("\\s+")[0] : "client";
        whatsApp.notify(phone.get(), LAST_MINUTE, List.of(name, space, DATE.format(start), hours(start, end), price, normalPrice),
                member.getMemberId(), PORTAL_PATH);
        return true;
    }

    /**
     * Confirmee : prenom, espace, date, horaire, reference. Modifiee : prenom, reference, espace,
     * date, horaire (ordre des variables des modeles approuves).
     */
    static List<String> values(Booking booking, String purpose, String name, ZoneId zone) {
        LocalDateTime start = BookingMemberPreferences.inZone(booking.getStartedAt(), zone);
        LocalDateTime end = BookingMemberPreferences.inZone(booking.getEndedAt(), zone);
        String space = booking.getResource() == null || !StringUtils.hasText(booking.getResource().getName()) ? "Espace ELLE A OSE"
                : booking.getResource().getName();
        String date = start == null ? "-" : DATE.format(start);
        String hours = hours(start, end);
        return CONFIRMED.equals(purpose)
                ? List.of(name, space, date, hours, booking.getBookingNumber())
                : List.of(name, booking.getBookingNumber(), space, date, hours);
    }

    static String hours(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            return "-";
        }
        if (end == null) {
            return TIME.format(start);
        }
        if (end.toLocalDate().equals(start.toLocalDate())) {
            return TIME.format(start) + " - " + TIME.format(end);
        }
        return TIME.format(start) + " - " + DATE.format(end) + " " + TIME.format(end);
    }

    private static String firstName(Booking booking, Optional<Member> member) {
        String name = StringUtils.hasText(booking.getContactName()) ? booking.getContactName()
                : member.map(Member::getDisplayName).orElse(null);
        return StringUtils.hasText(name) ? name.trim().split("\\s+")[0] : "client";
    }
}
