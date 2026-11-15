package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.booking.dto.response.BookingAvailabilityResponse.OccupiedRange;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Plages occupees autour d'un creneau demande : on n'a plus a parcourir le calendrier pour savoir
 * pourquoi il est refuse.
 *
 * <p>Un creneau de la journee bloque la demande s'il est ferme, desactive, ou s'il n'a plus la place
 * demandee (une salle louee entiere doit etre entierement libre). Les creneaux bloquants qui se
 * suivent forment une plage ; seules les plages qui chevauchent la demande sont rendues, chacune en
 * entier, avec les reservations qui l'occupent.</p>
 */
@Component
@RequiredArgsConstructor
public class BookingOccupancyFinder {

    static final String BOOKED = "BOOKED";
    static final String UNAVAILABLE = "UNAVAILABLE";

    private final ResourceAvailabilityRepository availabilityRepository;
    private final JdbcTemplate jdbc;

    public List<OccupiedRange> occupiedAround(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt, int quantity) {
        if (resource == null || startedAt == null || endedAt == null) {
            return List.of();
        }
        List<ResourceAvailability> slots = new ArrayList<>(availabilityRepository.findAllSlotsInRange(resource,
                startedAt.toLocalDate().atStartOfDay(), endedAt.toLocalDate().plusDays(1).atStartOfDay()));
        slots.sort(Comparator.comparing(ResourceAvailability::getStartedAt));
        List<LocalDateTime[]> ranges = new ArrayList<>();
        LocalDateTime[] current = null;
        for (ResourceAvailability slot : slots) {
            if (!blocks(resource, slot, quantity)) {
                current = null;
                continue;
            }
            if (current != null && !slot.getStartedAt().isAfter(current[1])) {
                current[1] = slot.getEndedAt().isAfter(current[1]) ? slot.getEndedAt() : current[1];
            } else {
                current = new LocalDateTime[]{slot.getStartedAt(), slot.getEndedAt()};
                ranges.add(current);
            }
        }
        List<OccupiedRange> result = new ArrayList<>();
        for (LocalDateTime[] range : ranges) {
            if (range[0].isBefore(endedAt) && range[1].isAfter(startedAt)) {
                List<String> bookings = bookings(resource, range[0], range[1]);
                result.add(new OccupiedRange(range[0], range[1], bookings.isEmpty() ? UNAVAILABLE : BOOKED, bookings));
            }
        }
        return result;
    }

    static boolean blocks(Resource resource, ResourceAvailability slot, int quantity) {
        if (!Boolean.TRUE.equals(slot.getActive()) || !Boolean.TRUE.equals(slot.getAvailable())) {
            return true;
        }
        int remaining = slot.getRemainingCapacity() == null ? 0 : slot.getRemainingCapacity();
        int total = slot.getTotalCapacity() == null ? remaining : slot.getTotalCapacity();
        return resource.isWholeResourceBooking() ? remaining < total : remaining < Math.max(1, quantity);
    }

    private List<String> bookings(Resource resource, LocalDateTime from, LocalDateTime to) {
        return jdbc.queryForList("SELECT booking_number FROM booking WHERE resource_id = ? AND started_at < ? AND ended_at > ? "
                        + "AND status NOT IN ('CANCELLED', 'REJECTED', 'EXPIRED', 'FAILED') ORDER BY started_at",
                String.class, resource.getId(), Timestamp.valueOf(to), Timestamp.valueOf(from));
    }
}
