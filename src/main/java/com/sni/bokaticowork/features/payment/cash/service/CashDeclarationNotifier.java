package com.sni.bokaticowork.features.payment.cash.service;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.core.utils.mail.Recipients;
import com.sni.bokaticowork.features.payment.cash.model.CashPaymentDeclaration;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * « A encaisser » n est pas « encaisse » · les deux messages ne disent pas la meme chose.
 *
 * <p>Un reglement mobile money ou portefeuille se signale au back-office comme un fait accompli :
 * l argent est la, il n y a rien a faire. Une annonce d especes est l inverse · c est une demande
 * d action, adressee a quelqu un qui doit compter des billets et confirmer. Elle porte donc une
 * severite differente, et reste dans la file tant que personne ne s en occupe.</p>
 *
 * <p>Le client, lui, est prevenu quand la caisse a confirme · sans cela il repart sans trace
 * ecrite de ce qu il vient de payer, et rappelle pour en obtenir une.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashDeclarationNotifier {

    private static final String AGGREGATE = "PAYMENT";

    private final OutboxService outboxService;
    private final AdminInAppNotifier adminInAppNotifier;
    private final TransactionContextResolver contextResolver;

    /** Qui tient la caisse · une ou plusieurs adresses, a defaut celles des paiements libre-service. */
    @Value("${bokati.payment.cash-declaration.desk-email:${bokati.payment.self-service.confirmation-email:${bokati.support.manager-email:}}}")
    private String deskEmail;

    /** Un client a annonce qu il passerait payer · la caisse a quelque chose a faire. */
    public void declared(CashPaymentDeclaration declaration) {
        try {
            Map<String, Object> facts = facts(declaration);
            String subject = "Espèces annoncées · " + facts.get("amount") + " " + facts.get("currency")
                    + " par " + facts.get("customerName");

            for (String recipient : Recipients.split(deskEmail)) {
                Map<String, Object> payload = new LinkedHashMap<>(facts);
                payload.put("adminEmail", recipient);
                payload.put("recipientName", "l'équipe");
                payload.put("recipientType", "ADMIN");
                payload.put("subject", subject);
                payload.put("templateCode", "CASH_PAYMENT_DECLARED");
                outboxService.publish("CASH_PAYMENT_DECLARED", AGGREGATE,
                        declaration.getDeclarationNumber() + ":" + recipient, payload);
                adminInAppNotifier.notify("CASH_PAYMENT_DECLARED", AGGREGATE,
                        declaration.getDeclarationNumber(), recipient, subject, facts);
            }

            // WARNING et non INFO · ce message demande une action, il ne la constate pas.
            adminInAppNotifier.broadcastAlert("CASH_PAYMENT_DECLARED", "PAYMENT", subject,
                    facts.get("customerName") + " passera régler " + facts.get("amount") + " "
                            + facts.get("currency") + " en espèces. À confirmer à l'encaissement.",
                    "WARNING", null);
        } catch (Exception ex) {
            log.warn("Annonce {} · notification de la caisse impossible",
                    declaration.getDeclarationNumber(), ex);
        }
    }

    /** L argent a ete compte · le client en garde une trace ecrite. */
    public void confirmed(CashPaymentDeclaration declaration) {
        publishToCustomer(declaration, "CASH_PAYMENT_CONFIRMED",
                "Votre paiement en espèces a bien été reçu",
                payload -> payload.put("confirmedAmount", plain(declaration.getConfirmedAmount())));
    }

    /** L annonce tombe · le client doit savoir que sa facture est de nouveau a regler. */
    public void expired(CashPaymentDeclaration declaration) {
        publishToCustomer(declaration, "CASH_PAYMENT_DECLARATION_EXPIRED",
                "Votre paiement en espèces n'a pas été reçu",
                payload -> payload.put("bookingReleased", StringUtils.hasText(declaration.getBookingNumber())));
    }

    /** Annulee · rien a dire au client quand c est lui qui a annule, on previent la caisse. */
    public void cancelled(CashPaymentDeclaration declaration) {
        log.info("Annonce {} close · {}", declaration.getDeclarationNumber(), declaration.getCloseReason());
    }

    // -----------------------------------------------------------------------------------------

    private void publishToCustomer(CashPaymentDeclaration declaration, String eventType, String subject,
                                   java.util.function.Consumer<Map<String, Object>> extra) {
        try {
            TransactionContextResolver.PartyView party = contextResolver.resolveParty(
                    declaration.getCustomerType(), declaration.getCustomerCode());
            if (!StringUtils.hasText(party.email())) {
                log.info("Annonce {} · aucun destinataire joignable pour {} {}",
                        declaration.getDeclarationNumber(), declaration.getCustomerType(),
                        declaration.getCustomerCode());
                return;
            }
            Map<String, Object> payload = facts(declaration);
            payload.put("recipientEmail", party.email());
            payload.put("recipientName", StringUtils.hasText(party.name()) ? party.name() : "client");
            payload.put("recipientType", recipientType(declaration.getCustomerType()));
            payload.put("recipientCode", declaration.getCustomerCode());
            payload.put("subject", subject);
            payload.put("templateCode", eventType);
            extra.accept(payload);
            outboxService.publish(eventType, AGGREGATE, declaration.getDeclarationNumber(), payload);
        } catch (Exception ex) {
            log.warn("Annonce {} · notification du client impossible ({})",
                    declaration.getDeclarationNumber(), eventType, ex);
        }
    }

    private Map<String, Object> facts(CashPaymentDeclaration declaration) {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("declarationNumber", declaration.getDeclarationNumber());
        facts.put("documentNumber", declaration.getDocumentNumber());
        facts.put("bookingNumber", declaration.getBookingNumber());
        facts.put("amount", plain(declaration.getAmount()));
        facts.put("currency", declaration.getCurrency());
        facts.put("customerName", StringUtils.hasText(declaration.getCustomerName())
                ? declaration.getCustomerName() : declaration.getCustomerCode());
        facts.put("customerCode", declaration.getCustomerCode());
        facts.put("note", declaration.getNote());
        facts.put("expiresAt", declaration.getExpiresAt() == null ? null : declaration.getExpiresAt().toString());
        facts.put("transactionNumber", declaration.getTransactionNumber());
        facts.put("cashSessionNumber", declaration.getCashSessionNumber());
        return facts;
    }

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
        return amount == null ? "" : amount.stripTrailingZeros().toPlainString();
    }

    /** Utilise par les tests pour verifier le comportement sans destinataire. */
    List<String> recipients() {
        return Recipients.split(deskEmail);
    }
}
