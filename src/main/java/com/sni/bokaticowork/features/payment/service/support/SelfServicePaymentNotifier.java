package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.core.utils.mail.Recipients;
import com.sni.bokaticowork.features.payment.enums.PaymentChannel;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Previent la caisse qu'un client vient de regler tout seul.
 *
 * <p>Un paiement encaisse au guichet est connu de la personne qui l'a encaisse. Un paiement fait
 * depuis l'espace client, lui, n'etait annonce a personne : la caisse decouvrait le reglement en
 * ouvrant la facture, parfois des jours plus tard · entre-temps le client se presentait avec un
 * badge qu'on lui refusait, ou recevait une relance pour une facture deja payee.</p>
 *
 * <p>Le message confirme la transaction au back-office : elle est deja encaissee, il n'y a rien a
 * valider. Ce qui est attendu de la caisse, c'est de la voir · pour servir le client, rapprocher
 * la recette du jour, et ne pas relancer quelqu'un qui a paye.</p>
 *
 * <p>Deux canaux, parce qu'ils ne servent pas au meme moment : la notification dans le portail pour
 * l'agent qui est devant son ecran, le courriel pour celui qui ne l'est pas.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelfServicePaymentNotifier {

    private static final String EVENT = "SELF_SERVICE_PAYMENT_RECEIVED";
    private static final String AGGREGATE = "PAYMENT";

    private final OutboxService outboxService;
    private final AdminInAppNotifier adminInAppNotifier;
    private final TransactionContextResolver contextResolver;

    /** Qui est prevenu · une ou plusieurs adresses, a defaut le responsable support. */
    @Value("${bokati.payment.self-service.confirmation-email:${bokati.support.manager-email:}}")
    private String confirmationEmail;

    /**
     * Un reglement vient d'aboutir · on ne parle que de ceux que personne n'a vus passer.
     *
     * <p>Rien ne part pour un paiement saisi au guichet ni pour une ecriture automatique : la
     * premiere a deja quelqu'un devant elle, la seconde n'a pas de client a servir derriere.</p>
     */
    public void paymentSettled(PaymentTransaction transaction) {
        if (transaction == null || transaction.getChannel() != PaymentChannel.SELF_SERVICE) {
            return;
        }
        try {
            notifyAll(transaction);
        } catch (Exception ex) {
            // Prevenir la caisse ne doit jamais faire echouer un encaissement · l'argent est recu.
            log.warn("Paiement libre-service {} · notification du back-office impossible",
                    transaction.getTransactionNumber(), ex);
        }
    }

    private void notifyAll(PaymentTransaction transaction) {
        Map<String, Object> facts = facts(transaction);
        String subject = "Paiement client · " + facts.get("amount") + " " + facts.get("currency")
                + " par " + facts.get("customerName");

        List<String> recipients = Recipients.split(confirmationEmail);
        if (recipients.isEmpty()) {
            log.info("Paiement libre-service {} · aucune adresse de confirmation configuree",
                    transaction.getTransactionNumber());
        }
        for (String recipient : recipients) {
            Map<String, Object> payload = new LinkedHashMap<>(facts);
            payload.put("adminEmail", recipient);
            payload.put("recipientName", "l'équipe");
            payload.put("recipientType", "ADMIN");
            payload.put("subject", subject);
            payload.put("templateCode", EVENT);
            // Un identifiant par destinataire · sans cela l'outbox dedoublonnerait les envois.
            outboxService.publish(EVENT, AGGREGATE, transaction.getTransactionNumber() + ":" + recipient, payload);
            adminInAppNotifier.notify(EVENT, AGGREGATE, transaction.getTransactionNumber(),
                    recipient, subject, facts);
        }

        // L'ecran de caisse ouvert s'allume tout de suite · c'est la que le client se presente.
        adminInAppNotifier.broadcastAlert(EVENT, "PAYMENT", subject,
                facts.get("customerName") + " a réglé " + facts.get("amount") + " " + facts.get("currency")
                        + " depuis son espace client (" + facts.get("paymentMethod") + ").",
                "INFO", null);

        log.info("Paiement libre-service {} · {} confirme, {} destinataire(s) prevenu(s)",
                transaction.getTransactionNumber(), facts.get("paymentMethod"), recipients.size());
    }

    private Map<String, Object> facts(PaymentTransaction transaction) {
        PaymentIntent intent = transaction.getPaymentIntent();
        TransactionContextResolver.PartyView party = intent == null
                ? TransactionContextResolver.PartyView.empty()
                : contextResolver.resolveParty(intent.getCustomerType(), intent.getCustomerCode());
        TransactionContextResolver.SourceView source = intent == null
                ? TransactionContextResolver.SourceView.empty()
                : contextResolver.resolveSource(intent.getSourceType(), intent.getSourceCode());

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("transactionNumber", transaction.getTransactionNumber());
        facts.put("receiptNumber", transaction.getReceiptNumber());
        facts.put("intentNumber", intent == null ? null : intent.getIntentNumber());
        facts.put("amount", plain(transaction.getAmount()));
        facts.put("currency", transaction.getCurrency());
        facts.put("paymentMethod", method(transaction));
        facts.put("provider", transaction.getProvider());
        facts.put("providerReference", transaction.getProviderReference());
        facts.put("paidAt", transaction.getPaidAt() == null ? null : transaction.getPaidAt().toString());
        facts.put("customerType", intent == null ? null : intent.getCustomerType());
        facts.put("customerCode", intent == null ? null : intent.getCustomerCode());
        facts.put("customerName", StringUtils.hasText(party.name())
                ? party.name()
                : (intent == null ? "Un client" : intent.getCustomerCode()));
        facts.put("customerEmail", party.email());
        facts.put("customerPhone", party.phone());
        facts.put("purpose", StringUtils.hasText(source.label())
                ? source.label()
                : (intent == null ? null : intent.getPurpose()));
        return facts;
    }

    /** Le moyen de paiement en francais · un ecran de caisse ne lit pas des constantes. */
    private String method(PaymentTransaction transaction) {
        if (transaction.getPaymentMethod() == null) {
            return "Paiement";
        }
        return switch (transaction.getPaymentMethod()) {
            case MOBILE_MONEY -> "Mobile money";
            case WALLET -> "Portefeuille";
            case CARD -> "Carte";
            case BANK_TRANSFER -> "Virement";
            case CASH -> "Espèces";
            default -> transaction.getPaymentMethod().name();
        };
    }

    private String plain(BigDecimal amount) {
        return amount == null ? "" : amount.stripTrailingZeros().toPlainString();
    }
}
