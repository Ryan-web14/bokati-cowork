package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.core.utils.time.AppTime;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

/**
 * Lot B9g : agenda personnel synchronise. Un membre active un lien d'abonnement d'agenda (ICS) qui
 * liste ses reservations a venir ; il le colle dans Google Agenda, Outlook ou son telephone. Le
 * lien porte un jeton secret, renouvelable et revocable. Coupe par defaut
 * ({@code bookings.calendar-feed.enabled}).
 */
@Slf4j
@Component
public class BookingCalendarFeed {

    static final String ENABLED_KEY = "bookings.calendar-feed.enabled";
    private static final DateTimeFormatter ICS = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Feed(String memberId, String url, Instant createdAt) {
    }

    private final JdbcTemplate jdbc;
    private final BookingRepository bookings;

    @Value("${app.api-base-url:https://api.elleaose.com}")
    private String apiBaseUrl = "https://api.elleaose.com";

    private PlatformSettings settings;

    public BookingCalendarFeed(JdbcTemplate jdbc, BookingRepository bookings) {
        this.jdbc = jdbc;
        this.bookings = bookings;
    }

    @Autowired(required = false)
    void setSettings(PlatformSettings settings) {
        this.settings = settings;
    }

    public boolean enabled() {
        return settings == null || settings.bool(ENABLED_KEY, true);
    }

    /** Cree ou renouvelle le lien du membre (l'ancien cesse de marcher). */
    @Transactional
    public Feed rotate(String memberId) {
        requireEnabled();
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now();
        jdbc.update("DELETE FROM booking_calendar_feed WHERE member_id = ?", memberId);
        jdbc.update("INSERT INTO booking_calendar_feed (member_id, token, created_at) VALUES (?, ?, ?)", memberId, token, Timestamp.from(now));
        return new Feed(memberId, url(token), now);
    }

    @Transactional(readOnly = true)
    public Feed current(String memberId) {
        List<String> tokens = jdbc.queryForList("SELECT token FROM booking_calendar_feed WHERE member_id = ?", String.class, memberId);
        return tokens.isEmpty() ? null : new Feed(memberId, url(tokens.getFirst()), null);
    }

    @Transactional
    public void revoke(String memberId) {
        jdbc.update("DELETE FROM booking_calendar_feed WHERE member_id = ?", memberId);
    }

    /** Le calendrier ICS du jeton : reservations a venir (et du dernier mois) non annulees du membre. */
    @Transactional(readOnly = true)
    public String calendar(String token) {
        requireEnabled();
        List<String> members = jdbc.queryForList("SELECT member_id FROM booking_calendar_feed WHERE token = ?", String.class, token == null ? "" : token);
        if (members.isEmpty()) {
            throw new ResourceNotFoundException("Agenda introuvable");
        }
        List<String> numbers = jdbc.queryForList("""
                SELECT booking_number FROM booking
                WHERE deleted = false AND owner_type = 'MEMBER' AND owner_code = ?
                  AND status IN ('CONFIRMED', 'IN_PROGRESS', 'PENDING_PAYMENT', 'PENDING_APPROVAL', 'COMPLETED')
                  AND started_at >= ?
                ORDER BY started_at ASC LIMIT 500
                """, String.class, members.getFirst(), Timestamp.valueOf(AppTime.now().minusMonths(1)));
        StringBuilder ics = new StringBuilder("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Elle A Ose//Reservations//FR\r\n")
                .append("CALSCALE:GREGORIAN\r\nMETHOD:PUBLISH\r\nX-WR-CALNAME:Mes réservations Elle A Osé\r\n");
        for (String number : numbers) {
            Booking booking = bookings.findByBookingNumber(number).orElse(null);
            if (booking == null) {
                continue;
            }
            String start = ICS.format(booking.getStartedAt().atZone(AppTime.zone()).withZoneSameInstant(ZoneOffset.UTC));
            String end = ICS.format(booking.getEndedAt().atZone(AppTime.zone()).withZoneSameInstant(ZoneOffset.UTC));
            ics.append("BEGIN:VEVENT\r\n")
                    .append("UID:").append(booking.getBookingNumber()).append("@elleaose\r\n")
                    .append("DTSTAMP:").append(ICS.format(Instant.now().atZone(ZoneOffset.UTC))).append("\r\n")
                    .append("DTSTART:").append(start).append("\r\n")
                    .append("DTEND:").append(end).append("\r\n")
                    .append("SUMMARY:").append(escape(booking.getResource() == null ? "Réservation" : booking.getResource().getName())).append("\r\n")
                    .append("DESCRIPTION:").append(escape("Réservation " + booking.getBookingNumber() + " (" + booking.getStatus().label() + ")")).append("\r\n")
                    .append("STATUS:").append(booking.getStatus().name().startsWith("PENDING") ? "TENTATIVE" : "CONFIRMED").append("\r\n")
                    .append("END:VEVENT\r\n");
        }
        return ics.append("END:VCALENDAR\r\n").toString();
    }

    private String url(String token) {
        String base = settings == null ? apiBaseUrl : settings.text("app.api-base-url", apiBaseUrl);
        return base.replaceAll("/+$", "") + ApiPath.V1 + "/public/bookings/calendar/" + token + ".ics";
    }

    private void requireEnabled() {
        if (!enabled()) {
            throw ConflictException.coded("CALENDAR_FEED_DISABLED", "L'agenda synchronisé n'est pas activé sur cet espace");
        }
    }

    private static String escape(String text) {
        return StringUtils.hasText(text) ? text.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n") : "";
    }
}
