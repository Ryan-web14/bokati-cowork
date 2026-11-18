package com.sni.bokaticowork.features.ressource.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UpdateResourcePolicyRequest {

    private String name;
    private String description;
    private Integer minBookingDurationMinutes;
    private Integer maxBookingDurationMinutes;
    private Integer minBookingNoticeMinutes;
    private Integer cancellationNoticeMinutes;
    private Boolean allowCancellation;
    /** L'equipe peut-elle reserver hors preavis et sur un debut passe ? Vrai par defaut. */
    private Boolean staffOverrideAllowed;

    /** Lot R5a : battement entre deux reservations d'une ressource louee entiere (menage, installation), en minutes. */
    private Integer bufferMinutes;

    /** Lot R5a : reservation au plus tant de jours a l'avance (vide : l'horizon des creneaux). */
    private Integer maxAdvanceDays;

    /** Lot R5c : tolerance de depassement au depart, en minutes ; au-dela, le depassement est facture (vide : jamais). */
    private Integer overrunGraceMinutes;

    /** Lot R5c : frais de non-presentation (vide : aucun). */
    private Integer noShowFee;
    private Boolean active;
}
