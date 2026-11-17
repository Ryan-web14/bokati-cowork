package com.sni.bokaticowork.core.communication.whatsapp;

import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Pages de l'espace client ouvertes par les boutons des modeles WhatsApp. Le lien du bouton est
 * {@code https://espace.elleaose.com/{{1}}} : la fin de l'adresse est donnee a l'envoi. Les envois
 * designent une page par sa cle ({@link #INVOICES}...) ; l'adresse se regle a chaud dans les
 * parametres ({@code bokati.whatsapp.portal-path.<cle>}), sans redeploiement ni nouveau modele.
 */
@Component
public class WhatsAppPortalPaths {

    public static final String INVOICES = "invoices";
    public static final String QUOTES = "quotes";
    public static final String BOOKINGS = "bookings";
    public static final String SUBSCRIPTIONS = "subscriptions";
    public static final String WALLET = "wallet";
    /** Lot KG7 : pieces KYC a deposer. */
    public static final String KYC = "kyc";

    static final String PREFIX = "bokati.whatsapp.portal-path.";
    static final Map<String, String> DEFAULTS = Map.of(
            INVOICES, "client/invoices",
            QUOTES, "client/quotes",
            BOOKINGS, "client/bookings",
            SUBSCRIPTIONS, "client/subscriptions",
            WALLET, "client/wallet",
            KYC, "client/kyc");

    private final ObjectProvider<PlatformSettings> settings;

    public WhatsAppPortalPaths(ObjectProvider<PlatformSettings> settings) {
        this.settings = settings;
    }

    /** Adresse de la page pour une cle connue ; tout autre parametre est rendu tel quel. */
    public String resolve(String parameter) {
        if (parameter == null || !DEFAULTS.containsKey(parameter)) {
            return parameter;
        }
        PlatformSettings current = settings.getIfAvailable();
        String path = current == null ? DEFAULTS.get(parameter) : current.text(PREFIX + parameter, DEFAULTS.get(parameter));
        return path == null ? null : path.trim().replaceAll("^/+", "");
    }
}
