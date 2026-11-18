package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.ressource.model.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lot R2, decision D4 : une ressource supprimee, fermee, mise en maintenance, hors service ou retiree
 * alors qu'elle a des reservations a venir. Refus par defaut, avec la liste des reservations ; option
 * explicite « annuler et prevenir » ({@code cancelBookings = true}).
 */
@Slf4j
@Component
public class ResourceImpactGuard {

    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    private ResourceBookingImpact impact;

    @Autowired(required = false)
    void setImpact(ResourceBookingImpact impact) {
        this.impact = impact;
    }

    public List<ResourceBookingImpact.ImpactedBooking> impacted(Resource resource, LocalDateTime from, LocalDateTime to) {
        if (impact == null || resource == null || resource.getId() == null) {
            return List.of();
        }
        LocalDateTime start = from == null ? LocalDateTime.now(ZONE) : from;
        LocalDateTime end = to == null ? start.plusYears(5) : to;
        return impact.activeBookings(resource.getId(), start, end);
    }

    /**
     * @param action          ce qu'on fait (« supprimer la ressource », « fermer la ressource »...)
     * @param cancelBookings  vrai : annuler les reservations touchees et prevenir les clients
     */
    public void check(Resource resource, LocalDateTime from, LocalDateTime to, boolean cancelBookings, String action, String reason) {
        List<ResourceBookingImpact.ImpactedBooking> bookings = impacted(resource, from, to);
        if (bookings.isEmpty()) {
            return;
        }
        if (!cancelBookings) {
            String numbers = bookings.stream().limit(20).map(ResourceBookingImpact.ImpactedBooking::bookingNumber)
                    .collect(Collectors.joining(", "));
            throw ConflictException.coded("RESOURCE_HAS_BOOKINGS", "Impossible de " + action + " " + resource.getCode() + " : "
                    + bookings.size() + " réservation(s) à venir (" + numbers + (bookings.size() > 20 ? "…" : "")
                    + "). Déplacez-les d'abord, ou recommencez avec cancelBookings=true pour les annuler et prévenir les clients.");
        }
        impact.cancelAndNotify(bookings.stream().map(ResourceBookingImpact.ImpactedBooking::bookingNumber).toList(),
                reason == null ? "Ressource " + resource.getCode() + " indisponible" : reason);
        log.info("{} {} : {} reservation(s) annulee(s) et clients prevenus", action, resource.getCode(), bookings.size());
    }
}
