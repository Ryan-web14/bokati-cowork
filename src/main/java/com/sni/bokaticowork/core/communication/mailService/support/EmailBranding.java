package com.sni.bokaticowork.core.communication.mailService.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;

/**
 * Elements de marque communs a tous les courriels.
 *
 * <p><b>Une URL, pas du base64.</b> Les PDF embarquent le logo ; un courriel ne le peut pas :
 * Gmail supprime les images en {@code data:}. Le logo doit donc etre servi par une adresse
 * publique, que le client de messagerie charge sans jeton.
 *
 * <p>L'adresse etait construite dans deux services distincts, et seize gabarits attendaient une
 * variable {@code logo} que personne ne fournissait — ils retombaient tous sur le nom en texte.
 * Ici, une seule source, posee sur chaque contexte.
 */
@Component
public class EmailBranding {

    private static final String LOGO_PATH = "/images/logo.png";

    private final String publicBaseUrl;

    public EmailBranding(@Value("${app.verify-base-url:}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    /** Adresse absolue du logo · relative seulement si aucune base publique n'est configuree. */
    public String logoUrl() {
        if (!StringUtils.hasText(publicBaseUrl)) {
            return LOGO_PATH;
        }
        return publicBaseUrl.replaceAll("/+$", "") + LOGO_PATH;
    }

    /**
     * Pose les variables de marque sur un contexte de courriel.
     *
     * <p>{@code preheader} est le texte d'apercu de la boite de reception. Aucun gabarit n'en
     * portait : la ligne grise affichait le premier texte venu, c'est-a-dire l'en-tete, et les
     * trente-quatre courriels s'y ressemblaient tous. Chaque gabarit en propose un par defaut ;
     * l'appelant peut le remplacer par une phrase portant le fait saillant du message.
     */
    public void apply(Context context) {
        context.setVariable("logoUrl", logoUrl());
        if (!context.containsVariable("preheader")) {
            context.setVariable("preheader", null);
        }
    }
}
