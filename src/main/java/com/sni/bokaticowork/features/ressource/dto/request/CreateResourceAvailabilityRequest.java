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

    /**
     * Duree d'un creneau, en minutes. Facultatif · absent, celle de la ressource s'applique, et
     * une ressource qui n'en porte aucune reste a 30. Une interface qui envoie 30 sur un parc a
     * 30 ne change donc rien.
     *
     * <p>Le champ ne definit pas la duree de CETTE disponibilite : il porte la valeur jusqu'a la
     * ressource, ou elle appartient. Le moteur compose une fenetre a partir de creneaux contigus,
     * et des durees melangees sur une meme ressource fausseraient ce decompte en silence.
     *
     * <p>Une valeur differente de celle de la ressource n'est donc acceptee que si celle-ci n'a
     * aucun creneau futur · sinon la creation est refusee, avec le compte des creneaux a
     * supprimer d'abord. Seuls les diviseurs de 60 sont admis.
     */
    private Integer slotDurationMinutes;
}
