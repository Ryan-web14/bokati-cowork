package com.sni.bokaticowork.features.ressource.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ouverture d'une meme plage de disponibilite sur plusieurs ressources.
 *
 * <p>Chaque ressource est traitee independamment : celles qui echouent — chevauchement avec des
 * creneaux existants, plafond d'un mois d'avance, fermeture active — figurent au compte rendu avec
 * leur motif, sans empecher les autres d'aboutir.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BulkCreateResourceAvailabilityRequest {

    @NotEmpty(message = "Au moins un code ressource est requis")
    private List<String> resourceCodes;

    @NotNull
    private LocalDateTime startedAt;

    @NotNull
    private LocalDateTime endedAt;

    /** Voir {@link CreateResourceAvailabilityRequest#getCapacity()} · meme alias, meme plafonnement. */
    @JsonAlias("totalCapacity")
    private Integer capacity;

    private Boolean active;

    /**
     * Voir {@link CreateResourceAvailabilityRequest#getSlotDurationMinutes()}.
     *
     * <p>A savoir sur un appel groupe : la duree porte sur chaque ressource prise separement. Une
     * ressource ayant deja des creneaux futurs d'une autre duree sera ecartee du lot, les autres
     * adoptant la valeur. Le compte rendu le dit ressource par ressource.
     */
    private Integer slotDurationMinutes;

    /** Requete unitaire equivalente pour une ressource · l'appel groupe delegue au meme chemin. */
    public CreateResourceAvailabilityRequest forResource(String resourceCode) {
        return CreateResourceAvailabilityRequest.builder()
                .resourceCode(resourceCode)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .capacity(capacity)
                .active(active)
                .slotDurationMinutes(slotDurationMinutes)
                .build();
    }
}
