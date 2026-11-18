package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourcePolicyResponse {

    private String code;
    private String name;
    private String description;
    private Integer minBookingDurationMinutes;
    private Integer maxBookingDurationMinutes;
    private Integer minBookingNoticeMinutes;
    private Integer cancellationNoticeMinutes;

    /** Lot B2 : grille de remboursement « heures:pourcentage », par exemple « 72:100,24:50,0:0 ». */
    private String refundRules;
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
