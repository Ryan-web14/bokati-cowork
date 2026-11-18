package com.sni.bokaticowork.features.ressource.dto.response;

import java.time.LocalDate;

/**
 * Lot R5a : jour particulier ; {@code national} = jour ferie national calcule (sans identifiant,
 * non supprimable : se neutralise par un jour SPECIAL_HOURS ou par le parametre).
 */
public record ResourceCalendarDayResponse(Long id, LocalDate date, String resourceCode, String kind, String label,
                                          String openingHours, boolean national) {
}
