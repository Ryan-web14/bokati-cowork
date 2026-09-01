package com.sni.bokaticowork.features.ressource.dto.response;

import java.util.List;

/**
 * Compte rendu d'une operation appliquee a plusieurs ressources.
 *
 * <p>Volontairement un compte rendu, et non un tout-ou-rien. Chaque ressource a ses propres
 * contraintes — plafond d'un mois d'avance, chevauchement avec l'existant, capacite, politique de
 * reservation. Sur vingt ressources, il est normal que deux echouent. Refuser l'ensemble pour deux
 * echecs obligerait a retirer les fautives une par une et a relancer, ce qui est exactement le
 * travail que l'appel groupe doit epargner.
 *
 * @param processed ressources traitees avec succes
 * @param skipped   ressources ecartees · la raison figure sur chaque ligne
 */
public record BulkResourceOperationResponse(int processed,
                                            int skipped,
                                            List<Entry> results) {

    /**
     * @param status  {@code PROCESSED} ou {@code SKIPPED}
     * @param reason  motif de l'ecart, destine a etre affiche tel quel · vide en cas de succes
     * @param detail  precision facultative propre a l'operation, par exemple le nombre de creneaux
     */
    public record Entry(String resourceCode, String status, String reason, Integer detail) {

        public static Entry processed(String resourceCode, Integer detail) {
            return new Entry(resourceCode, "PROCESSED", null, detail);
        }

        public static Entry skipped(String resourceCode, String reason) {
            return new Entry(resourceCode, "SKIPPED", reason, null);
        }
    }

    public static BulkResourceOperationResponse of(List<Entry> results) {
        int processed = (int) results.stream().filter(e -> "PROCESSED".equals(e.status())).count();
        return new BulkResourceOperationResponse(processed, results.size() - processed, results);
    }
}
