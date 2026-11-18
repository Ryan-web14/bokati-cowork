package com.sni.bokaticowork.features.ressource.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Lot R5b : reponses des sites, etages, plan en direct et recherche d'une salle. */
public final class ResourceLocationResponses {

    private ResourceLocationResponses() {
    }

    public record Site(Long id, String code, String name, String address, String city, String phone, String timeZone,
                       String openingHoursNote, Boolean active, long floorCount, long resourceCount) {
    }

    public record Floor(Long id, String code, String siteCode, String name, Integer level, String planDocumentCode,
                        Boolean active, long resourceCount) {
    }

    /**
     * Plan d'un etage a un instant : chaque ressource placee avec son etat.
     * Etats : {@code FREE}, {@code PARTIAL} (poste partage entame), {@code OCCUPIED}, {@code CLOSED}
     * (fermeture ou creneau desactive), {@code NO_SLOT} (pas de creneau ouvert), {@code OUT_OF_SERVICE}.
     */
    public record FloorPlan(String floorCode, String floorName, Integer level, String siteCode, String siteName,
                            String planDocumentCode, LocalDateTime at, List<PlacedResource> resources) {
    }

    public record PlacedResource(String code, String name, String typeCode, Integer capacity, BigDecimal planX,
                                 BigDecimal planY, String state, Integer remainingCapacity, LocalDateTime freeAt) {
    }

    /** Resultat de « Trouver une salle » : ce qui convient, puis des alternatives expliquees. */
    public record FindResult(List<Option> matches, List<Option> alternatives) {
    }

    /**
     * Une proposition ; {@code notes} explique l'ecart d'une alternative (autre horaire, autre site,
     * au-dessus du budget, equipement manquant).
     */
    public record Option(String resourceCode, String name, String typeCode, String siteCode, String floorCode,
                         Integer capacity, LocalDateTime startedAt, LocalDateTime endedAt, BigDecimal price,
                         String currency, List<String> notes) {
    }
}
