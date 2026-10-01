package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Dit au client ce qui est arrive a son paiement · en francais, et sans le laisser deviner.
 *
 * <p>Un echec mobile money survient souvent plusieurs minutes apres que le client a ferme sa page :
 * il a saisi un mauvais code, son solde ne suffisait pas, la demande a expire pendant qu'il etait
 * en reunion. Jusqu'ici il ne l'apprenait nulle part · sa facture restait ouverte sans explication,
 * et il rappelait l'accueil pour comprendre.</p>
 *
 * <p>Deux messages, jamais plus : l'echec avec sa raison et la conduite a tenir, et le cas ou
 * l'operateur n'a jamais tranche · celui-la ne demande rien au client, il lui dit surtout de ne
 * pas payer deux fois.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MobileMoneyClientNotifier {

    private static final String AGGREGATE = "PAYMENT";

    private final OutboxService outboxService;
    private final TransactionContextResolver contextResolver;

    /** Le paiement a ete refuse par l'operateur · on dit pourquoi et si reessayer a un sens. */
    public void depositFailed(PawapayDeposit deposit) {
        Map<String, Object> payload = base(deposit);
        if (payload == null) {
            return;
        }
        payload.put("failureCode", deposit.getFailureCode());
        payload.put("userMessage", StringUtils.hasText(deposit.getUserMessage())
                ? deposit.getUserMessage()
                : "Le paiement a échoué chez l'opérateur. Vous pouvez réessayer ou payer autrement.");
        payload.put("retryable", Boolean.TRUE.equals(deposit.getRetryable()));
        payload.put("subject", "Votre paiement mobile money n'a pas abouti");
        publish("MOBILE_MONEY_DEPOSIT_FAILED", deposit, payload);
    }

    /**
     * L'operateur n'a jamais tranche · le client doit surtout ne pas payer une seconde fois.
     *
     * <p>C'est le message le plus important du module : un client sans nouvelles qui voit sa
     * facture ouverte repaye, et se retrouve debite deux fois pour une seule prestation.</p>
     */
    public void depositPendingReview(PawapayDeposit deposit) {
        Map<String, Object> payload = base(deposit);
        if (payload == null) {
            return;
        }
        payload.put("userMessage", StringUtils.hasText(deposit.getUserMessage())
                ? deposit.getUserMessage()
                : "Nous n'avons pas reçu de réponse définitive de l'opérateur.");
        payload.put("subject", "Nous vérifions votre paiement mobile money");
        publish("MOBILE_MONEY_DEPOSIT_PENDING_REVIEW", deposit, payload);
    }

    // -----------------------------------------------------------------------------------------

    private Map<String, Object> base(PawapayDeposit deposit) {
        if (deposit == null) {
            return null;
        }
        TransactionContextResolver.PartyView party =
                contextResolver.resolveParty(deposit.getCustomerType(), deposit.getCustomerCode());
        if (!StringUtils.hasText(party.email())) {
            // Sans adresse joignable, rien ne part · on le note, on ne bloque aucun encaissement.
            log.info("Depot {} · aucun destinataire joignable pour {} {}", deposit.getDepositId(),
                    deposit.getCustomerType(), deposit.getCustomerCode());
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientEmail", party.email());
        payload.put("recipientName", StringUtils.hasText(party.name()) ? party.name() : "client");
        payload.put("recipientType", recipientType(deposit.getCustomerType()));
        payload.put("recipientCode", deposit.getCustomerCode());
        payload.put("depositId", deposit.getDepositId());
        payload.put("intentNumber", deposit.getIntentNumber());
        payload.put("transactionNumber", deposit.getTransactionNumber());
        payload.put("amount", plain(deposit.getAmount()));
        payload.put("currency", deposit.getCurrency());
        payload.put("provider", operatorName(deposit.getProvider()));
        payload.put("phoneNumber", deposit.getPhoneNumber());
        return payload;
    }

    private void publish(String eventType, PawapayDeposit deposit, Map<String, Object> payload) {
        payload.put("templateCode", eventType);
        outboxService.publish(eventType, AGGREGATE, deposit.getDepositId(), payload);
    }

    /**
     * Le nom de l'operateur, pas son code technique.
     *
     * <p>{@code MTN_MOMO_COG} ne veut rien dire pour la personne qui lit son courriel · elle
     * reconnait « MTN Mobile Money Congo ».</p>
     */
    private String operatorName(String providerCode) {
        if (!StringUtils.hasText(providerCode)) {
            return null;
        }
        try {
            return CongoCorrespondent.valueOf(providerCode.trim().toUpperCase(Locale.ROOT)).getDisplayName();
        } catch (IllegalArgumentException ex) {
            return providerCode;
        }
    }

    /** Le destinataire tel que le module de notification le nomme · un type inconnu ne le fait pas echouer. */
    private String recipientType(String customerType) {
        String normalized = customerType == null ? "" : customerType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MEMBER" -> "MEMBER";
            case "CUSTOMER" -> "CUSTOMER";
            case "BUSINESS_ENTITY", "BUSINESS", "COMPANY" -> "BUSINESS";
            default -> "SYSTEM";
        };
    }

    private String plain(BigDecimal amount) {
        return amount == null ? null : amount.stripTrailingZeros().toPlainString();
    }
}
