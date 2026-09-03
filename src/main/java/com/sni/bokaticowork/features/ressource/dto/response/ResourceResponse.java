package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourceResponse {

    private String code;
    private String typeCode;
    private String groupCode;
    private String policyCode;
    private String name;
    private String description;
    private Integer capacity;
    private String zone;
    private String locationLabel;
    private String status;
    private Boolean bookingEnabled;
    private Boolean portalVisible;
    private Integer displayOrder;
    private Boolean active;

    /**
     * Duree d'un creneau de cette ressource, en minutes.
     *
     * <p>En lecture seule ici : elle se fixe a l'ouverture des disponibilites, ou l'invariant
     * - pas de durees melangees sur une ressource ayant des creneaux futurs · peut etre verifie.
     * Sans ce champ, une interface ne pouvait ni afficher la duree en vigueur ni pre-remplir le
     * formulaire, et proposait 30 par defaut a une ressource qui travaille en quarts d'heure.
     */
    private Integer slotDurationMinutes;
}
