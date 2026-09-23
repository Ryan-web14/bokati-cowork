package com.sni.bokaticowork.features.payment.provider.pawaypay;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "bokati.payment.pawaypay")
public class PawapayProperties {
    private boolean enabled = false;
    private String apiKey;
    private String baseUrl = "https://api.pawapay.io/";
    private String callbackBaseUrl = "http://localhost:8080";
    /** HMAC-SHA256 shared secret from PawaPay dashboard · leave empty to skip verification */
    private String callbackSecret;
    /** Delai de connexion a l'API, en ms · au-dela l'appel est « injoignable », jamais « refuse ». */
    private long connectTimeoutMs = 5_000;
    /** Delai de reponse de l'API, en ms. */
    private long readTimeoutMs = 20_000;
    /** Duree totale pendant laquelle on attend une reponse definitive de l'operateur · apres, le depot est a rapprocher. */
    private int pollingMaxHours = 24;
    /** Fenetre pendant laquelle une seconde demande sur la meme intention et le meme numero rend le depot en cours. */
    private int inFlightWindowMinutes = 20;
    /** Qui est prevenu d'un depot sans reponse definitive · une ou plusieurs adresses, separees par des virgules. */
    private String alertEmail;
    /** Intervalle entre deux passes du worker, en ms · chaque depot porte sa propre echeance. */
    private long pollingDelayMs = 60_000;
    /**
     * L'essai qui envoie une vraie demande sur un vrai telephone est-il ouvert ?
     *
     * <p>Faux par defaut · la route depense de l'argent reel et fait sonner un numero.</p>
     */
    private boolean testEndpointEnabled = false;
    /** Country code sent to the hosted Payment Page (required alongside a fixed amount) */
    private String paymentPageCountry = "COG";
    /** UI language for the hosted Payment Page */
    private String paymentPageLanguage = "fr";
    /** Frontend page the customer is redirected to after the hosted Payment Page (depositId + status appended) */
    private String paymentPageResultUrl = "";
}