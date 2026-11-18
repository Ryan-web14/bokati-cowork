package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Lot R0 : plage d'un calendrier de ressource. Par defaut, d'aujourd'hui a un mois plus tard ;
 * date de fin anterieure au debut refusee ; 92 jours au plus (l'horizon de reservation de 90 jours
 * tient dans une seule demande). Avant : aucune borne, une boucle jour par jour sur une plage
 * quelconque, sur une route publique sans jeton.
 */
public record ResourceCalendarRange(LocalDate start, LocalDate end) {

    public static final int MAX_DAYS = 92;

    public static ResourceCalendarRange of(LocalDate fromDate, LocalDate toDate) {
        LocalDate start = fromDate == null ? LocalDate.now(java.time.ZoneId.of("Africa/Brazzaville")) : fromDate;
        LocalDate end = toDate == null ? start.plusMonths(1) : toDate;
        if (end.isBefore(start)) {
            throw new BadRequestException("La date de fin (" + end + ") précède la date de début (" + start + ")");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
            throw new BadRequestException("Plage trop longue : " + MAX_DAYS + " jours au plus par demande");
        }
        return new ResourceCalendarRange(start, end);
    }
}
