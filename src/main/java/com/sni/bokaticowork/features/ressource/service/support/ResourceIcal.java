package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceClosure;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceClosureRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

/**
 * Lot R5b : calendriers iCalendar (RFC 5545).
 *
 * <ul>
 *   <li>Export d'une ressource pour l'equipe (reservations avec le nom du contact, fermetures) ;</li>
 *   <li>flux d'abonnement par lien a jeton, <b>coupe par defaut</b>
 *   ({@code app.resources.ical.public-feed-enabled}), qui ne montre que « Occupé » et « Fermé », sans
 *   aucune donnee de client ;</li>
 *   <li>invitation d'une reservation, pour le client (lien du courriel de confirmation).</li>
 * </ul>
 */
@Component
public class ResourceIcal {

    public static final String PUBLIC_FEED_KEY = "app.resources.ical.public-feed-enabled";
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ResourceRepository resourceRepository;
    private final ResourceClosureRepository closureRepository;
    private ResourceBookingImpact bookings;
    private PlatformSettings settings;

    public ResourceIcal(ResourceRepository resourceRepository, ResourceClosureRepository closureRepository) {
        this.resourceRepository = resourceRepository;
        this.closureRepository = closureRepository;
    }

    @Autowired(required = false)
    void setOptional(ResourceBookingImpact bookings, PlatformSettings settings) {
        this.bookings = bookings;
        this.settings = settings;
    }

    /** Export de l'equipe : reservations actives (avec le contact) et fermetures, de -7 a +90 jours. */
    @Transactional(readOnly = true)
    public String export(String resourceCode) {
        Resource resource = resource(resourceCode);
        return calendar(resource, true);
    }

    /** Nouveau jeton d'abonnement (l'ancien cesse de fonctionner). */
    @Transactional
    public String rotateToken(String resourceCode) {
        Resource resource = resource(resourceCode);
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        resource.setIcalToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        resourceRepository.save(resource);
        return resource.getIcalToken();
    }

    public boolean publicFeedEnabled() {
        return settings != null && settings.bool(PUBLIC_FEED_KEY, false);
    }

    /** Flux d'abonnement : « Occupé » et « Fermé » seulement ; introuvable si le flux est coupe. */
    @Transactional(readOnly = true)
    public String publicFeed(String token) {
        if (!publicFeedEnabled() || !StringUtils.hasText(token)) {
            throw new ResourceNotFoundException("Calendrier introuvable");
        }
        Resource resource = resourceRepository.findByIcalToken(token.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Calendrier introuvable"));
        return calendar(resource, false);
    }

    private String calendar(Resource resource, boolean withNames) {
        ZoneId zone = ResourceSites.zone(resource.getSite());
        LocalDateTime from = LocalDateTime.now(zone).minusDays(7);
        LocalDateTime to = from.plusDays(97);
        StringBuilder ics = begin(resource.getName());
        if (bookings != null) {
            for (ResourceBookingImpact.ImpactedBooking booking : bookings.activeBookings(resource.getId(), from, to)) {
                String summary = withNames
                        ? "Réservation " + booking.bookingNumber() + (booking.contactName() == null ? "" : " : " + booking.contactName())
                        : "Occupé";
                event(ics, "booking-" + booking.bookingNumber() + "@bokati", booking.startedAt(), booking.endedAt(), zone,
                        summary, withNames ? "Statut : " + booking.status() : null, resource.getName());
            }
        }
        for (ResourceClosure closure : closureRepository.findActiveForCalendar(resource, from, to)) {
            int index = 0;
            for (LocalDateTime[] occurrence : closure.occurrences(from, to)) {
                String summary = withNames && StringUtils.hasText(closure.getReason()) ? "Fermé : " + closure.getReason() : "Fermé";
                event(ics, "closure-" + closure.getId() + "-" + (index++) + "@bokati", occurrence[0], occurrence[1], zone,
                        summary, null, resource.getName());
            }
        }
        return end(ics);
    }

    /** Invitation d'une reservation (fichier .ics joint ou telecharge depuis le courriel). */
    public static String invitation(String bookingNumber, String resourceName, String location, LocalDateTime startedAt,
                                    LocalDateTime endedAt, ZoneId zone, String organizerName) {
        StringBuilder ics = begin(organizerName == null ? "Réservation" : organizerName);
        ics.append("METHOD:PUBLISH\r\n");
        event(ics, "booking-" + bookingNumber + "@bokati", startedAt, endedAt, zone == null ? ZoneId.of("Africa/Brazzaville") : zone,
                "Réservation " + (resourceName == null ? bookingNumber : resourceName),
                "Réservation " + bookingNumber, location == null ? resourceName : location);
        return end(ics);
    }

    private static StringBuilder begin(String name) {
        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n")
                .append("VERSION:2.0\r\n")
                .append("PRODID:-//Bokati Cowork//Ressources//FR\r\n")
                .append("CALSCALE:GREGORIAN\r\n");
        line(ics, "X-WR-CALNAME", name);
        return ics;
    }

    private static String end(StringBuilder ics) {
        return ics.append("END:VCALENDAR\r\n").toString();
    }

    private static void event(StringBuilder ics, String uid, LocalDateTime start, LocalDateTime end, ZoneId zone,
                              String summary, String description, String location) {
        ics.append("BEGIN:VEVENT\r\n");
        line(ics, "UID", uid);
        ics.append("DTSTAMP:").append(UTC.format(Instant.now().atOffset(ZoneOffset.UTC))).append("\r\n");
        ics.append("DTSTART:").append(UTC.format(start.atZone(zone).withZoneSameInstant(ZoneOffset.UTC))).append("\r\n");
        ics.append("DTEND:").append(UTC.format(end.atZone(zone).withZoneSameInstant(ZoneOffset.UTC))).append("\r\n");
        line(ics, "SUMMARY", summary);
        if (description != null) line(ics, "DESCRIPTION", description);
        if (location != null) line(ics, "LOCATION", location);
        ics.append("END:VEVENT\r\n");
    }

    private static void line(StringBuilder ics, String name, String value) {
        if (value == null) {
            return;
        }
        String escaped = value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
        String full = name + ":" + escaped;
        // Lignes pliees a 75 octets (RFC 5545, 3.1), sans couper un caractere.
        int start = 0;
        boolean first = true;
        while (start < full.length()) {
            int end = start;
            int bytes = 0;
            int limit = first ? 75 : 74;
            while (end < full.length()) {
                int size = String.valueOf(full.charAt(end)).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                if (bytes + size > limit) break;
                bytes += size;
                end++;
            }
            ics.append(first ? "" : " ").append(full, start, end).append("\r\n");
            start = end;
            first = false;
        }
    }

    private Resource resource(String code) {
        return resourceRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Ressource introuvable : " + code.trim()));
    }

    /** Pour les tests : lignes du calendrier. */
    static List<String> lines(String ics) {
        return List.of(ics.split("\r\n"));
    }
}
