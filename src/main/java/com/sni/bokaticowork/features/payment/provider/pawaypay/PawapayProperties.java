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
    /**
     * Ignore · PawaPay ne signe pas ses rappels par HMAC. Les rappels se verifient avec ses cles
     * publiques (RFC 9421), voir {@link PawapaySignatureVerifier}. Garde pour ne pas casser une
     * configuration existante.
     */
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

    /** Signature des requetes envoyees a PawaPay · voir {@link PawapayRequestSigner}. */
    private Signing signing = new Signing();

    @Getter
    @Setter
    public static class Signing {
        /**
         * La cle privee, au format PEM · celle dont la cle publique est deposee chez PawaPay.
         *
         * <p>Vide, rien n'est signe · PawaPay refuse alors les demandes financieres si les appels
         * signes sont exiges dans son tableau de bord. Jamais dans le depot : l'environnement seulement.</p>
         */
        private String privateKey;
        /** L'identifiant de la cle, tel que PawaPay l'attend dans {@code keyid}. */
        private String keyId;
        /**
         * ecdsa-p256-sha256, ecdsa-p384-sha384, rsa-pss-sha512 ou rsa-v1_5-sha256 · vide, celui qui
         * convient a la cle (rsa-pss-sha512 pour une cle RSA, ecdsa-p256-sha256 pour une cle EC).
         */
        private String algorithm;

        public boolean isConfigured() {
            return privateKey != null && !privateKey.isBlank();
        }
    }
}