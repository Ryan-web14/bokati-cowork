package com.sni.bokaticowork.core.communication.whatsapp.infobip;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Acces a Infobip pour WhatsApp. La cle d'API vient de l'environnement, jamais du depot. */
@Data
@ConfigurationProperties(prefix = "bokati.whatsapp.infobip")
public class InfobipWhatsAppProperties {

    /** Active l'envoi ; faux : WhatsApp est signale indisponible. */
    private boolean enabled = false;

    /** Adresse de base propre au compte, par exemple https://xxxxx.api.infobip.com. */
    private String baseUrl;

    /** Cle d'API (droit whatsapp:message:send et gestion des modeles). */
    private String apiKey;

    /** Numero WhatsApp expediteur, au format international sans +. */
    private String sender;

    /** Langue des modeles par defaut. */
    private String language = "fr";

    /** Pays des numeros saisis sans indicatif. */
    private String defaultCountry = "CG";

    /**
     * Texte libre quand aucun modele approuve n'existe pour l'objet. Meta ne le delivre que dans les
     * 24 heures qui suivent un message du destinataire.
     */
    private boolean freeTextAllowed = false;

    private int connectTimeoutMs = 5000;

    private int readTimeoutMs = 15000;
}
