package com.sni.bokaticowork.features.verify;

/**
 * Ce qu'une page de vérification publique a le droit de montrer.
 *
 * <p>La route est ouverte sans jeton et sa clé · le numéro de document · est <b>énumérable</b> :
 * {@code INV-MEM-20260830-00000009} désigne sans ambiguïté ses voisins. Tout champ exposé ici
 * l'est donc à quiconque sait compter, pour l'ensemble des clients.
 *
 * <p>D'où deux niveaux :
 *
 * <ul>
 *   <li><b>Minimal</b>, par défaut · le numéro, la date et le fait que le document est authentique.
 *       Cela confirme l'existence à qui a le document sous les yeux, et n'apprend rien à qui ne
 *       l'a pas.</li>
 *   <li><b>Détaillé</b>, seulement sur présentation du paramètre {@code k} · le préfixe de la
 *       signature fiscale, que seul le porteur du document possède puisqu'il est encodé dans son
 *       QR code. Il ne se devine pas : sa production exige la clé HMAC du serveur.</li>
 * </ul>
 *
 * <p>Ne jamais ajouter ici un champ de contact, d'adresse, de ligne ou de paiement. Le détail
 * sert à confirmer un document que l'on tient déjà, pas à le reconstituer.
 *
 * @param authentic le document existe et correspond
 * @param detailed  le porteur a prouvé qu'il détient le document
 */
public record VerificationView(
        String reference,
        String documentLabel,
        String issueDate,
        String statusLabel,
        boolean authentic,
        boolean detailed,
        String partyName,
        String totalAmount,
        String currency) {

    /** Réponse minimale · rien au-delà de ce qui confirme l'existence. */
    public static VerificationView minimal(String reference, String documentLabel,
                                           String issueDate, String statusLabel) {
        return new VerificationView(reference, documentLabel, issueDate, statusLabel,
                true, false, null, null, null);
    }

    /** Réponse détaillée · réservée au porteur du document. */
    public static VerificationView detailed(String reference, String documentLabel,
                                            String issueDate, String statusLabel,
                                            String partyName, String totalAmount, String currency) {
        return new VerificationView(reference, documentLabel, issueDate, statusLabel,
                true, true, partyName, totalAmount, currency);
    }
}
