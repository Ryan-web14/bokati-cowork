package com.sni.bokaticowork.features.ressource.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Lot R5a : jour particulier (ferie ajoute, horaires exceptionnels, fermeture). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceCalendarDayRequest {
    private LocalDate date;
    /** Vide : toutes les ressources. */
    private String resourceCode;
    /** HOLIDAY, SPECIAL_HOURS ou CLOSED. */
    private String kind;
    private String label;
    /** {@code HH:mm-HH:mm}, separees par des virgules. */
    private String openingHours;
}
