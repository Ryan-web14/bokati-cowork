package com.sni.bokaticowork.features.billing.dto.request;

/**
 * Correction des informations du destinataire imprimees sur un devis ou une facture.
 * <p>
 * Ce DTO ne porte volontairement <b>ni</b> {@code customerType} <b>ni</b> {@code customerCode} :
 * ces deux champs designent le proprietaire du document (le client a qui il est rattache) et
 * changer de proprietaire n'est pas une correction d'adresse, c'est un autre document. Les
 * separer au niveau du DTO rend l'erreur impossible plutot que de compter sur une validation.
 * <p>
 * Chaque champ est optionnel : {@code null} laisse la valeur en place. Pour vider un champ,
 * envoyer une chaine vide.
 */
public record UpdateBillingRecipientRequest(
        String customerName,
        String customerEmail,
        String customerPhone,
        String customerNiu,
        String customerCategory,
        String customerReference,
        /** Adresse de facturation, en JSON, meme forme que celle posee a la creation. */
        String billingAddressJson,
        String deliveryAddressJson,
        /** Motif de la correction, conserve dans l'historique d'edition du document. */
        String reason,
        String changedBy
) {
}
