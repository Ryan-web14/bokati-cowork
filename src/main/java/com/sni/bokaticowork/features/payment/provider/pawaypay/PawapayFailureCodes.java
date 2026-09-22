package com.sni.bokaticowork.features.payment.provider.pawaypay;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;

/**
 * Ce que l'operateur dit quand ca echoue, traduit en ce qu'on dit au client.
 *
 * <p>Un code technique n'aide personne a la caisse. Chaque code connu a une phrase en francais et
 * dit si retenter avec le meme numero a un sens · un plafond atteint ou un numero inconnu, non ;
 * un refus ou un delai, oui.</p>
 */
public final class PawapayFailureCodes {

    public record Explanation(String code, String userMessage, boolean retryable) {
    }

    private static final Map<String, Explanation> KNOWN = Map.ofEntries(
            entry("PAYER_NOT_FOUND", "Ce numéro n'est pas un compte mobile money actif chez cet opérateur. Vérifiez le numéro et l'opérateur.", false),
            entry("INVALID_PHONE_NUMBER", "Le numéro de téléphone n'est pas valide pour cet opérateur.", false),
            entry("INVALID_PAYER_FORMAT", "Le numéro de téléphone n'est pas au bon format.", false),
            entry("PAYER_LIMIT_REACHED", "Le plafond de votre compte mobile money est atteint. Contactez votre opérateur ou payez autrement.", false),
            entry("WALLET_LIMIT_REACHED", "Le plafond de votre compte mobile money est atteint. Contactez votre opérateur ou payez autrement.", false),
            entry("PAYMENT_NOT_APPROVED", "Le paiement n'a pas été confirmé sur votre téléphone. Vous pouvez réessayer et valider la demande avec votre code secret.", true),
            entry("PAYER_NOT_ALLOWED_TO_PAY", "Votre compte mobile money n'autorise pas ce paiement. Contactez votre opérateur.", false),
            entry("INSUFFICIENT_BALANCE", "Le solde de votre compte mobile money est insuffisant. Rechargez-le puis réessayez.", true),
            entry("AMOUNT_TOO_SMALL", "Le montant est inférieur au minimum accepté par l'opérateur.", false),
            entry("AMOUNT_TOO_LARGE", "Le montant dépasse le maximum accepté par l'opérateur pour une opération.", false),
            entry("TRANSACTION_ALREADY_IN_PROCESS", "Une demande de paiement est déjà en attente sur votre téléphone. Validez-la ou attendez son expiration avant de réessayer.", true),
            entry("PAYMENT_IN_PROGRESS", "Une demande de paiement est déjà en attente sur votre téléphone. Validez-la ou attendez son expiration avant de réessayer.", true),
            entry("PROVIDER_TEMPORARILY_UNAVAILABLE", "L'opérateur est momentanément indisponible. Réessayez dans quelques minutes.", true),
            entry("OTHER_ERROR", "Le paiement a échoué chez l'opérateur. Vous pouvez réessayer ou payer autrement.", true),
            entry("UNSPECIFIED_FAILURE", "Le paiement a échoué chez l'opérateur. Vous pouvez réessayer ou payer autrement.", true),
            entry("UNKNOWN_ERROR", "Le paiement a échoué chez l'opérateur. Vous pouvez réessayer ou payer autrement.", true),
            entry("NOT_FOUND_AT_PROVIDER", "La demande n'a pas atteint l'opérateur. Aucun montant n'a été prélevé · vous pouvez réessayer.", true),
            entry("UNRESOLVED", "Nous n'avons pas reçu de réponse définitive de l'opérateur. Si un montant a été prélevé, il sera rapproché par notre équipe.", false),
            entry("CORRESPONDENT_TEMPORARILY_UNAVAILABLE", "L'opérateur est momentanément indisponible. Réessayez dans quelques minutes.", true),
            entry("DEPOSIT_NOT_ALLOWED", "Cet opérateur n'accepte pas ce paiement pour le moment.", false),
            entry("MANUALLY_CANCELLED", "La demande a été annulée.", true),
            entry("EXPIRED", "La demande de paiement a expiré sans être validée sur votre téléphone. Vous pouvez réessayer.", true)
    );

    private static final Explanation DEFAULT = new Explanation("UNKNOWN_ERROR",
            "Le paiement a échoué chez l'opérateur. Vous pouvez réessayer ou payer autrement.", true);

    private PawapayFailureCodes() {
    }

    /** Lit {@code failureReason} tel que l'operateur l'envoie · {@code {failureCode, failureMessage}} ou un simple texte. */
    public static Explanation explain(JsonNode failureReason) {
        if (failureReason == null || failureReason.isNull()) {
            return DEFAULT;
        }
        String code = failureReason.isTextual() ? failureReason.asText()
                : failureReason.hasNonNull("failureCode") ? failureReason.get("failureCode").asText()
                : failureReason.hasNonNull("rejectionCode") ? failureReason.get("rejectionCode").asText()
                : null;
        return explain(code);
    }

    public static Explanation explain(String code) {
        if (!StringUtils.hasText(code)) {
            return DEFAULT;
        }
        String key = code.trim().toUpperCase(Locale.ROOT);
        Explanation known = KNOWN.get(key);
        return known != null ? known : new Explanation(key, DEFAULT.userMessage(), true);
    }

    private static Map.Entry<String, Explanation> entry(String code, String message, boolean retryable) {
        return Map.entry(code, new Explanation(code, message, retryable));
    }
}
