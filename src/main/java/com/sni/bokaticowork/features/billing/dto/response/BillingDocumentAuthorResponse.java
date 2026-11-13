package com.sni.bokaticowork.features.billing.dto.response;

/**
 * Auteur de la piece (lot B12) : celui qui l'a creee et celui qui a etabli la version courante.
 * Le PDF imprime l'auteur de la version courante dans « Etabli par » et dans le visa.
 */
public record BillingDocumentAuthorResponse(
        String createdBy,
        String createdByName,
        String createdByTitle,
        String versionAuthor,
        String versionAuthorName,
        String versionAuthorTitle
) {
}
