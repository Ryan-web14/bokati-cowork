package com.sni.bokaticowork.features.ressource.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UpdateActiveRequest {

    @NotNull
    private Boolean active;

    /** Lot R2 : activation d'une fermeture, vrai pour annuler les reservations de la plage et prevenir les clients. */
    private Boolean cancelBookings;
}
