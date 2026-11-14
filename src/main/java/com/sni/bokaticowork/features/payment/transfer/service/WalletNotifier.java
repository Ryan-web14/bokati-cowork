package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.billing.service.support.MemberBillingContact;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Previent le titulaire de ce qui arrive a son portefeuille.
 *
 * <p>Tout passe par la boite de sortie transactionnelle : un courriel qui partirait avant le commit
 * annoncerait un transfert qui n'a peut-etre pas eu lieu, et un courriel perdu apres le commit
 * laisserait un titulaire debite sans savoir pourquoi. La boite de sortie regle les deux.</p>
 *
 * <p>Le titulaire choisit ce qu'il veut recevoir · un debit tres frequent, comme un cafe par jour,
 * peut lasser. Mais une alerte de securite ne se desactive pas : elle est precisement destinee au
 * cas ou ce n'est pas lui qui opere.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletNotifier {

    private static final String AGGREGATE = "WALLET";

    private final OutboxService outboxService;
    private final TransactionContextResolver contextResolver;

    private MemberBillingContact memberContact;
    private com.sni.bokaticowork.core.communication.whatsapp.WhatsAppNotifier whatsApp;

    /** WhatsApp en plus du courriel ; absent, seul le courriel part. */
    @Autowired(required = false)
    void setWhatsApp(com.sni.bokaticowork.core.communication.whatsapp.WhatsAppNotifier whatsApp) {
        this.whatsApp = whatsApp;
    }

    /** Absent : un recu a chaque operation, a l'adresse du compte, comme avant les preferences. */
    @Autowired(required = false)
    void setMemberContact(MemberBillingContact memberContact) {
        this.memberContact = memberContact;
    }

    public void transferSent(WalletAccount source, WalletAccount target, BigDecimal amount, String transferNumber) {
        if (!Boolean.TRUE.equals(source.getNotifyOnDebit()) || !wantsReceipt(source, "WALLET_TRANSFER_SENT")) {
            return;
        }
        publishReceipt("WALLET_TRANSFER_SENT", source, transferNumber, payload(source, amount)
                .with("counterpartyWallet", target.getWalletNumber())
                .with("counterpartyName", partyName(target))
                .with("subject", "Transfert envoyé · " + plain(amount) + " " + source.getCurrency()));
        whatsApp(source, "WALLET_TRANSFER_SENT", transferNumber,
                money(amount), partyName(target), transferNumber, money(source.getAvailableBalance()));
    }

    public void transferReceived(WalletAccount target, WalletAccount source, BigDecimal amount, String transferNumber) {
        if (!Boolean.TRUE.equals(target.getNotifyOnCredit()) || !wantsReceipt(target, "WALLET_TRANSFER_RECEIVED")) {
            return;
        }
        publishReceipt("WALLET_TRANSFER_RECEIVED", target, transferNumber, payload(target, amount)
                .with("counterpartyWallet", source.getWalletNumber())
                .with("counterpartyName", partyName(source))
                .with("subject", "Transfert reçu · " + plain(amount) + " " + target.getCurrency()));
        whatsApp(target, "WALLET_TRANSFER_RECEIVED", transferNumber,
                money(amount), partyName(source), transferNumber, money(target.getAvailableBalance()));
    }

    public void topUpCompleted(WalletAccount wallet, BigDecimal amount, String transactionNumber) {
        if (!Boolean.TRUE.equals(wallet.getNotifyOnCredit()) || !wantsReceipt(wallet, "WALLET_TOPUP_COMPLETED")) {
            return;
        }
        publishReceipt("WALLET_TOPUP_COMPLETED", wallet, transactionNumber, payload(wallet, amount)
                .with("subject", "Rechargement effectué · " + plain(amount) + " " + wallet.getCurrency()));
        whatsApp(wallet, "WALLET_TOPUP_COMPLETED", transactionNumber, money(amount), transactionNumber, money(wallet.getAvailableBalance()));
    }

    public void paymentRequestReceived(WalletAccount payer, WalletAccount requester, BigDecimal amount,
                                       String requestNumber, String reason) {
        publish("WALLET_PAYMENT_REQUEST_RECEIVED", payer, requestNumber, payload(payer, amount)
                .with("counterpartyWallet", requester.getWalletNumber())
                .with("counterpartyName", partyName(requester))
                .with("reason", reason)
                .with("subject", "Demande de paiement · " + plain(amount) + " " + payer.getCurrency()));
        whatsApp(payer, "WALLET_PAYMENT_REQUEST_RECEIVED", requestNumber, partyName(requester), money(amount), reason);
    }

    /** Ne se desactive pas · c'est le titulaire qui a fixe le seuil, l'alerte est ce qu'il a demande. */
    public void lowBalance(WalletAccount wallet) {
        publish("WALLET_LOW_BALANCE", wallet, wallet.getWalletNumber(), payload(wallet, wallet.getAvailableBalance())
                .with("threshold", plain(wallet.getLowBalanceThreshold()))
                .with("subject", "Solde bas · " + plain(wallet.getAvailableBalance()) + " " + wallet.getCurrency()));
        whatsApp(wallet, "WALLET_LOW_BALANCE", wallet.getWalletNumber(), money(wallet.getAvailableBalance()), money(wallet.getLowBalanceThreshold()));
    }

    /** Ne se desactive pas non plus · destinee au cas ou ce n'est pas le titulaire qui opere. */
    public void securityEvent(WalletAccount wallet, String eventType, String subject, Map<String, Object> details) {
        Payload payload = payload(wallet, null).with("subject", subject);
        details.forEach(payload::with);
        publish(eventType, wallet, wallet.getWalletNumber(), payload);
        if (StringUtils.hasText(subject)) {
            String text = subject.trim();
            whatsApp(wallet, "WALLET_SECURITY_ALERT", wallet.getWalletNumber(), Character.toLowerCase(text.charAt(0)) + text.substring(1));
        }
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Le titulaire membre qui a choisi « factures seulement » ne recoit pas de recu par courriel a
     * chaque operation. Les alertes de solde et de securite ne passent pas par ici.
     */
    private boolean wantsReceipt(WalletAccount wallet, String eventType) {
        if (memberContact == null || memberContact.wantsEveryWalletReceipt(wallet.getOwnerType(), wallet.getOwnerCode())) {
            return true;
        }
        log.debug("Portefeuille {} · {} non envoye : preference du membre (factures seulement)",
                wallet.getWalletNumber(), eventType);
        return false;
    }

    /** Un recu d'operation · part a l'adresse de facturation du membre, a defaut a celle du compte. */
    private void publishReceipt(String eventType, WalletAccount wallet, String aggregateId, Payload payload) {
        publish(eventType, wallet, aggregateId, payload,
                contextResolver.resolveBillingParty(wallet.getOwnerType(), wallet.getOwnerCode()));
    }

    private void publish(String eventType, WalletAccount wallet, String aggregateId, Payload payload) {
        publish(eventType, wallet, aggregateId, payload,
                contextResolver.resolveParty(wallet.getOwnerType(), wallet.getOwnerCode()));
    }

    private void publish(String eventType, WalletAccount wallet, String aggregateId, Payload payload,
                         TransactionContextResolver.PartyView party) {
        if (!StringUtils.hasText(party.email())) {
            // Sans adresse, la notification ne peut pas partir · on le note, on ne bloque rien.
            log.info("Portefeuille {} · {} sans destinataire joignable", wallet.getWalletNumber(), eventType);
            return;
        }
        payload.with("recipientEmail", party.email())
                .with("recipientName", party.name())
                .with("recipientType", wallet.getOwnerType())
                .with("recipientCode", wallet.getOwnerCode())
                .with("templateCode", eventType);
        outboxService.publish(eventType, AGGREGATE, aggregateId, payload.values);
    }

    /**
     * WhatsApp au titulaire, apres la validation de la transaction : le prenom (ou la raison sociale)
     * puis les variables donnees. Seulement pour un portefeuille en XAF, la devise des modeles.
     */
    private void whatsApp(WalletAccount wallet, String purpose, String reference, String... values) {
        if (whatsApp == null || memberContact == null || !whatsApp.active()) {
            return;
        }
        if (StringUtils.hasText(wallet.getCurrency()) && !"XAF".equalsIgnoreCase(wallet.getCurrency().trim())) {
            return;
        }
        try {
            memberContact.whatsAppPhone(wallet.getOwnerType(), wallet.getOwnerCode()).ifPresent(phone -> {
                TransactionContextResolver.PartyView party = contextResolver.resolveParty(wallet.getOwnerType(), wallet.getOwnerCode());
                String name = StringUtils.hasText(party.name()) ? party.name().trim().split("\\s+")[0] : "client";
                java.util.List<String> all = new java.util.ArrayList<>();
                all.add(name);
                all.addAll(java.util.Arrays.asList(values));
                whatsApp.notify(phone, purpose, all, reference, "portefeuille");
            });
        } catch (RuntimeException ex) {
            log.warn("WhatsApp {} du portefeuille {} non envoye : {}", purpose, wallet.getWalletNumber(), ex.getMessage());
        }
    }

    /** Montant en XAF avec separateur de milliers, sans decimales. */
    private static String money(BigDecimal amount) {
        return amount == null ? "0" : String.format(java.util.Locale.FRANCE, "%,d", amount.setScale(0, java.math.RoundingMode.HALF_UP).longValue())
                .replace('\u202f', ' ').replace('\u00a0', ' ');
    }

    private Payload payload(WalletAccount wallet, BigDecimal amount) {
        return new Payload()
                .with("walletNumber", wallet.getWalletNumber())
                .with("currency", wallet.getCurrency())
                .with("amount", plain(amount))
                .with("availableBalance", plain(wallet.getAvailableBalance()));
    }

    private String partyName(WalletAccount wallet) {
        TransactionContextResolver.PartyView party = contextResolver.resolveParty(wallet.getOwnerType(), wallet.getOwnerCode());
        return StringUtils.hasText(party.name()) ? party.name() : wallet.getWalletNumber();
    }

    private String plain(BigDecimal amount) {
        return amount == null ? null : amount.stripTrailingZeros().toPlainString();
    }

    /** Petite carte ordonnee · ignore les valeurs nulles pour ne pas polluer le gabarit. */
    private static final class Payload {
        private final Map<String, Object> values = new LinkedHashMap<>();

        Payload with(String key, Object value) {
            if (value != null) {
                values.put(key, value);
            }
            return this;
        }
    }
}
