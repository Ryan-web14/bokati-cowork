package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * Previent les super-administrateurs de chaque paiement tranche a la main.
 *
 * <p>Forcer un paiement en « reussi » credite une facture sans confirmation de l'operateur · c'est
 * une decision qui engage de l'argent et que d'autres doivent voir. Envoye apres validation : une
 * resolution annulee ne previent personne.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MobileMoneyResolutionNotifier {

    private final MobileMoneyAlertRecipients alertRecipients;
    private final DefaultEmailSender emailSender;

    public void resolved(PawapayDeposit deposit, String action, String reason, String providerTransactionId, String actor) {
        List<String> recipients = alertRecipients.recipientsWithSuperAdmins();
        if (recipients.isEmpty()) {
            return;
        }
        String subject = "Bokati · paiement mobile money tranché à la main : " + deposit.getDepositId();
        String html = html(deposit, action, reason, providerTransactionId, actor);
        Runnable send = () -> recipients.forEach(recipient -> {
            try {
                emailSender.sendHtmlEmail(recipient, subject, html);
            } catch (Exception ex) {
                log.warn("Alerte de resolution mobile money non envoyee a {} : {}", recipient, ex.getMessage());
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
            return;
        }
        send.run();
    }

    static String html(PawapayDeposit deposit, String action, String reason, String providerTransactionId, String actor) {
        String amount = deposit.getAmount() == null ? "" : deposit.getAmount().stripTrailingZeros().toPlainString();
        return "<p>Un paiement mobile money a été tranché à la main.</p>"
                + "<table style=\"border-collapse:collapse\">"
                + row("Décision", action)
                + row("Par", actor)
                + row("Motif", reason)
                + row("Référence opérateur", providerTransactionId)
                + row("Paiement", deposit.getDepositId())
                + row("Intention", deposit.getIntentNumber())
                + row("Client", deposit.getCustomerCode())
                + row("Montant", amount + " " + (deposit.getCurrency() == null ? "" : deposit.getCurrency()))
                + row("Opérateur", deposit.getProvider())
                + "</table>"
                + "<p>Si vous ne reconnaissez pas cette décision, vérifiez le paiement chez PawaPay avant toute autre action.</p>";
    }

    private static String row(String label, String value) {
        return "<tr><td style=\"padding:4px 12px 4px 0;color:#6b7280\">" + HtmlUtils.htmlEscape(label) + "</td><td style=\"padding:4px 0\">"
                + HtmlUtils.htmlEscape(value == null ? "" : value) + "</td></tr>";
    }
}
