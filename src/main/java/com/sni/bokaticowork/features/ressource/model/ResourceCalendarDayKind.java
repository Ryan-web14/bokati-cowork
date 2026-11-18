package com.sni.bokaticowork.features.ressource.model;

/**
 * Lot R5a : jour particulier du calendrier.
 *
 * <ul>
 *   <li>{@code HOLIDAY} : jour ferie ajoute par l'equipe (en plus des feries nationaux calcules) ;
 *   horaires du jour ferie (parametre) sauf horaires donnes ;</li>
 *   <li>{@code SPECIAL_HOURS} : horaires exceptionnels a cette date ;</li>
 *   <li>{@code CLOSED} : ferme toute la journee.</li>
 * </ul>
 */
public enum ResourceCalendarDayKind {
    HOLIDAY,
    SPECIAL_HOURS,
    CLOSED
}
