package com.sni.bokaticowork.core.audit.dto.request;

import com.sni.bokaticowork.core.audit.enums.AuditStatus;

import java.time.Instant;

/**
 * Filtres du journal des actions (lot A1). Tous facultatifs ; textes sans casse.
 *
 * @param actorEmail recherche partielle
 * @param entityType type d'element exact, par exemple {@code BillingDocument}
 * @param entityCode code exact de l'element, par exemple un numero de facture
 * @param requestId  identifiant de requete exact ({@code X-Request-Id})
 * @param ressource  recherche partielle dans le libelle de ressource de l'action
 */
public record AuditLogFilter(
        String module,
        String action,
        String actorEmail,
        AuditStatus status,
        Instant createdFrom,
        Instant createdTo,
        String entityType,
        String entityCode,
        String requestId,
        String ressource
) {
}
