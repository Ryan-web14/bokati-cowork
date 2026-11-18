package com.sni.bokaticowork.features.ressource.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Lot R5b : demandes des sites, etages, placement et recherche d'une salle. */
public final class ResourceLocationRequests {

    private ResourceLocationRequests() {
    }

    /** Site ; en modification, les champs vides sont laisses tels quels. */
    public record Site(String name, String address, String city, String phone, String timeZone,
                       String openingHoursNote, Boolean active) {
    }

    /** Etage ; {@code planDocumentCode} = image du plan dans la GED. */
    public record Floor(String name, Integer level, String planDocumentCode, Boolean active) {
    }

    /**
     * Placement d'une ressource : site, etage, position sur le plan en pourcentage (0 a 100, depuis
     * le coin haut gauche). Un champ vide retire l'information.
     */
    public record Placement(String siteCode, String floorCode, BigDecimal planX, BigDecimal planY) {
    }

    /**
     * « Trouver une salle » : creneau, participants, equipements exiges, site, type, budget maximal
     * et titulaire (pour le prix qui lui sera fait).
     */
    public record Find(LocalDateTime startedAt, LocalDateTime endedAt, Integer participants, List<String> amenityCodes,
                       String siteCode, String typeCode, BigDecimal maxBudget, String ownerType, String ownerCode) {
    }
}
