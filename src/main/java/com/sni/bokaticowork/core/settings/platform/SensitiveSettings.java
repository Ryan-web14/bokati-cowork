package com.sni.bokaticowork.core.settings.platform;

import java.util.Map;
import java.util.Set;

/**
 * Parametres dont un changement a chaud peut nuire : ils prennent effet sans redemarrage (decision
 * de l'exploitant), mais chaque changement est journalise et signale aux super-administrateurs avec
 * le risque ci-dessous.
 */
public final class SensitiveSettings {

    private static final Map<String, String> RISKS = Map.ofEntries(
            Map.entry("app.company.machine-code", "entre dans le numero fiscal : formats melanges sur un meme exercice"),
            Map.entry("bokati.billing.default-currency", "factures dans des devises differentes, devis et factures discordants"),
            Map.entry("bokati.company.wallet.currency", "nouveaux portefeuilles d'entreprise dans une autre devise que les existants"),
            Map.entry("app.phone.default-country", "normalisation des numeros : les numeros deja enregistres ne se retrouvent plus"),
            Map.entry("app.phone.default-dial-code", "normalisation des numeros : les numeros deja enregistres ne se retrouvent plus"),
            Map.entry("app.phone.keep-trunk-zero", "normalisation des numeros : les numeros deja enregistres ne se retrouvent plus"),
            Map.entry("app.dev-mode", "affiche les traces techniques des erreurs en production"),
            Map.entry("app.security.failed-login.max-attempts", "politique de verrouillage des comptes, y compris pendant une attaque"),
            Map.entry("app.security.jwt.refresh-reuse-grace-seconds", "elargit la fenetre de detection d'un jeton vole"),
            Map.entry("app.security.rate-limit.enabled", "coupe la protection contre les attaques par force brute"),
            Map.entry("app.security.rate-limit.trusted-proxy-count", "une valeur fausse permet d'usurper l'adresse IP et de contourner les limites"),
            Map.entry("app.security.jwt.access-token-expiration-ms", "duree de vie des sessions"),
            Map.entry("app.security.jwt.refresh-token-expiration-ms", "duree de vie des sessions"),
            Map.entry("app.security.jwt.verification-token-expiration-ms", "duree de vie des jetons de verification"),
            Map.entry("app.security.one-time-token.expiration-ms", "duree de vie des codes a usage unique"),
            Map.entry("app.security.password-reset.expiration-ms", "duree de vie des liens de reinitialisation"),
            Map.entry("app.documents.preview-token.ttl-seconds", "duree de vie des liens d'apercu signes"),
            Map.entry("app.contract.signing-token-hours", "duree de vie des liens de signature de contrat"),
            Map.entry("bokati.pass.credential.rotation-minutes", "rotation des identifiants d'acces des pass"),
            Map.entry("bokati.wallet.confirmation.validity-minutes", "duree de la confirmation renforcee du portefeuille"),
            Map.entry("bokati.wallet.transfer.confirmation-validity-minutes", "duree de la confirmation des transferts"),
            Map.entry("bokati.booking.wallet.settlement-delay-minutes", "retenues en cours : fenetre de retrait raccourcie ou retenues echues avant le reglement"),
            Map.entry("bokati.booking.wallet.hold-grace-minutes", "lie au delai de reglement : a changer avec lui"),
            Map.entry("bokati.wallet.transfer.fee-rate", "le client peut payer d'autres frais que ceux affiches"),
            Map.entry("bokati.wallet.transfer.fee-min", "le client peut payer d'autres frais que ceux affiches"),
            Map.entry("bokati.wallet.transfer.fee-max", "le client peut payer d'autres frais que ceux affiches"),
            Map.entry("bokati.wallet.transfer.fee-wallet-number", "redirige l'argent des frais : risque de fraude"),
            Map.entry("bokati.wallet.admin.four-eyes-threshold", "permet d'agir sans second validateur"),
            Map.entry("bokati.wallet.risk.large-transfer-amount", "controle anti-fraude desactivable"),
            Map.entry("bokati.payment.cash-session.auto-limit", "plafond de controle des caisses"),
            Map.entry("bokati.domiciliation.kyc-level-required", "niveau de conformite exige pour domicilier")
    );

    private SensitiveSettings() {
    }

    public static boolean isSensitive(String key) {
        return key != null && RISKS.containsKey(key);
    }

    public static String risk(String key) {
        return RISKS.get(key);
    }

    public static Set<String> keys() {
        return RISKS.keySet();
    }
}
