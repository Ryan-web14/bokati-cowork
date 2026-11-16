package com.sni.bokaticowork.features.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Occupation d'une periode, avec la regle du rapport d'occupation : minutes reservees (reservations
 * confirmees, en cours ou terminees, par heure de debut) sur 8 heures par jour et par ressource
 * d'un type actif.
 *
 * @param date             premier jour de la periode
 * @param days             jours de la periode compris dans l'intervalle demande
 * @param bookingCount     reservations
 * @param bookedMinutes    minutes reservees
 * @param availableMinutes ressources x jours x 480 minutes
 * @param occupancyRate    pourcentage, deux decimales
 */
public record OccupancyTrendResponse(
        LocalDate date,
        int days,
        long bookingCount,
        long bookedMinutes,
        long availableMinutes,
        BigDecimal occupancyRate
) {
}
