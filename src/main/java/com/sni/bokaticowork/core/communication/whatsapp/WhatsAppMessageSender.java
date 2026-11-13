package com.sni.bokaticowork.core.communication.whatsapp;

import java.util.List;

/**
 * Envoi d'un message WhatsApp sortant (texte seul). Implementation : Infobip
 * ({@code bokati.whatsapp.infobip.enabled}) ; sans elle, les fonctions qui proposent WhatsApp le
 * signalent indisponible. Aucun lien ni aucune route ouverte vers l'exterieur : le destinataire
 * repond sur WhatsApp, et l'equipe saisit sa reponse.
 */
public interface WhatsAppMessageSender {

    /** Resultat de l'envoi : identifiant du message et statut rendu par l'operateur. */
    record Result(String messageId, String kind, String templateName, String statusGroup, String statusName) {
    }

    /**
     * Envoie par le modele approuve de cet objet ; a defaut, le texte libre (admis par Meta seulement
     * dans les 24 heures qui suivent un message du destinataire, et si le parametre l'autorise).
     *
     * @param phone        numero du destinataire (local ou international)
     * @param purpose      objet du message, cle du modele (par exemple {@code BALANCE_CONFIRMATION})
     * @param placeholders valeurs des variables du modele, dans l'ordre
     * @param text         texte libre equivalent
     * @param reference    reference metier, pour le journal des envois
     */
    default Result send(String phone, String purpose, List<String> placeholders, String text, String reference) {
        return send(phone, purpose, placeholders, text, reference, null);
    }

    /**
     * Comme {@link #send(String, String, List, String, String)}, avec le suffixe du lien du bouton du
     * modele (par exemple {@code espace-client/factures/FAC-2026-00012}). Pour un code
     * d'authentification, le code est la seule variable ; il sert aussi au bouton de copie.
     */
    Result send(String phone, String purpose, List<String> placeholders, String text, String reference, String buttonParameter);
}
