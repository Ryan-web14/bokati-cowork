package com.sni.bokaticowork.features.ressource.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CreateResourceAvailabilityRequest {

    @NotBlank
    private String resourceCode;

    @NotNull
    private LocalDateTime startedAt;

    @NotNull
    private LocalDateTime endedAt;

    /**
     * Capacite de chaque creneau. {@code totalCapacity} est accepte comme alias : c'est le nom que
     * portent l'entite et la reponse, et celui qu'emploie l'interface. Sans cet alias le champ
     * etait simplement jete et <b>toute</b> capacite demandee retombait sur celle de la ressource.
     *
     * <p>Vide, la capacite de la ressource s'applique.
     *
     * <p>A savoir : la valeur est ensuite <b>plafonnee</b> par
     * {@code ResourceAvailabilityServiceImpl.resolveCapacity} a la limite de reservations
     * simultanees de la ressource. Demander 16 sur une ressource qui en admet 15 donne donc 15,
     * et ce plafonnement est silencieux — ce n'est pas l'alias qui manque alors, c'est la regle
     * qui s'applique.
     */
    @JsonAlias("totalCapacity")
    private Integer capacity;

    /** Creneaux ouverts a la reservation. Vide, ils le sont. */
    private Boolean active;

    // La duree de creneau n'est volontairement pas exposee. Le moteur raisonne partout en creneaux
    // de 30 minutes (ResourceAvailabilityServiceImpl.SLOT_MINUTES), y compris pour composer une
    // fenetre a partir de plusieurs creneaux contigus : une duree variable par disponibilite
    // fausserait ce decompte. Accepter slotDurationMinutes pour l'ignorer redonnerait exactement
    // le silence que l'on cherche a supprimer · le champ est donc refuse.
}
