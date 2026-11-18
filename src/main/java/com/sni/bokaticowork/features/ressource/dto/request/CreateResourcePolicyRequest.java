package com.sni.bokaticowork.features.ressource.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CreateResourcePolicyRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Integer minBookingDurationMinutes;

    @NotNull
    private Integer maxBookingDurationMinutes;

    @NotNull
    private Integer minBookingNoticeMinutes;

    private Integer cancellationNoticeMinutes;
    private Boolean allowCancellation;
    /** L'equipe peut-elle reserver hors preavis et sur un debut passe ? Vrai par defaut. */
    private Boolean staffOverrideAllowed;

    /** Lot R5a : battement entre deux reservations d'une ressource louee entiere (menage, installation), en minutes. */
    private Integer bufferMinutes;

    /** Lot R5a : reservation au plus tant de jours a l'avance (vide : l'horizon des creneaux). */
    private Integer maxAdvanceDays;
    private Boolean active;
}
