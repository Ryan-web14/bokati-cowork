package com.sni.bokaticowork.features.billing.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Une version archivee d'un document.
 *
 * @param snapshot      etat du document tel qu'il etait · absent de la liste, present au detail
 * @param sentToCustomerAt horodatage de transmission · une version jamais envoyee reste du
 *                         travail en cours, seules les versions transmises ont valeur probante
 */
public record BillingDocumentVersionResponse(
        Integer versionNumber,
        String editType,
        String changedBy,
        Instant changedAt,
        Instant sentToCustomerAt,
        String changeSummary,
        Map<String, Object> snapshot) {

    /**
     * Ecart entre deux versions, champ par champ.
     *
     * @param changes une entree par champ ayant change, valeur avant et valeur apres
     */
    public record Diff(Integer fromVersion,
                       Integer toVersion,
                       List<FieldChange> changes) {

        public record FieldChange(String field, Object before, Object after) {}
    }
}
