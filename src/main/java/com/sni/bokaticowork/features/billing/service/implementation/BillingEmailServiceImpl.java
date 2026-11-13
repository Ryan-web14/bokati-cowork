package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentPdfService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.billing.service.support.MemberBillingContact;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingEmailServiceImpl implements BillingEmailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BillingDocumentService billingDocumentService;
    private final BillingDocumentPdfService billingDocumentPdfService;
    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine templateEngine;
    private final Locale appLocale;
    private final MemberBillingContact memberContact;

    /**
     * Envoi demande explicitement (bouton « renvoyer ») ou relance d'impaye · part toujours, quelles
     * que soient les preferences du membre. Seule l'adresse suit sa preference de facturation.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean sendDocument(String documentNumber) {
        return send(billingDocumentService.get(documentNumber));
    }

    /**
     * Envoi automatique · ne part pas si le membre destinataire a coupe cet evenement par courriel.
     * Le document reste consultable dans le portail ; un envoi coupe n'est pas un echec.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean sendDocumentIfAllowed(String documentNumber, EventType event) {
        BillingDocumentResponse document = billingDocumentService.get(documentNumber);
        if (!memberContact.allowsEmail(document.customerType(), document.customerCode(), event)) {
            log.info("Envoi de {} non fait ({}) : preference du membre {}", documentNumber, event, document.customerCode());
            return false;
        }
        return send(document);
    }

    private boolean send(BillingDocumentResponse document) {
        String documentNumber = document.documentNumber();
        String to = recipient(document);
        if (!StringUtils.hasText(to)) {
            throw new BadRequestException("Billing document customer has no email");
        }
        byte[] pdf = billingDocumentPdfService.generatePdf(documentNumber);
        try {
            return emailSender.sendHtmlEmailWithPdfAttachment(
                    to,
                    billingSubject(document),
                    renderBillingDocument(document),
                    document.documentNumber() + ".pdf",
                    pdf
            ).join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new BadRequestException("Billing document email send failed: " + cause.getMessage(), cause);
        }
    }

    /**
     * Ou en est le document pour celui qui le recoit.
     *
     * <p>Le meme courriel sert a annoncer une facture et a la renvoyer une fois reglee · sans cet
     * etat, un client qui venait de payer recevait « Votre facture est disponible » suivi d'un
     * « Solde du : 0 XAF ». Le document etait juste, le message disait le contraire.</p>
     */
    enum NoticeState {
        /** Rien n'a encore ete regle. */
        ISSUED,
        /** Un premier reglement est arrive, un solde reste. */
        PARTIALLY_PAID,
        /** Plus rien n'est du. */
        SETTLED,
        /** L'echeance est passee et un solde reste. */
        OVERDUE
    }

    /**
     * L'etat se lit sur les montants, pas sur le statut.
     *
     * <p>Une facture peut etre {@code SENT} et deja soldee : le statut suit le cycle du document,
     * les montants suivent l'argent. C'est l'argent qui interesse le destinataire.</p>
     */
    static NoticeState stateOf(BillingDocumentResponse document) {
        boolean settled = document.balanceDue() == null || document.balanceDue().signum() <= 0;
        boolean anyPaid = document.paidAmount() != null && document.paidAmount().signum() > 0;
        if (settled && anyPaid) {
            return NoticeState.SETTLED;
        }
        if (BillingDocumentStatus.OVERDUE.equals(document.status())) {
            return NoticeState.OVERDUE;
        }
        return anyPaid ? NoticeState.PARTIALLY_PAID : NoticeState.ISSUED;
    }

    private String renderBillingDocument(BillingDocumentResponse document) {
        return renderBillingDocument(document, null);
    }

    private String renderBillingDocument(BillingDocumentResponse document, String personalMessage) {
        NoticeState state = stateOf(document);
        boolean overdue = state == NoticeState.OVERDUE;
        boolean settled = state == NoticeState.SETTLED;
        String documentLabel = billingDocumentLabel(document.documentType());
        String balance = money(document.balanceDue(), document.currency());
        String paid = money(document.paidAmount(), document.currency());

        Context context = new Context(appLocale);
        context.setVariable("customerName", valueOrDefault(document.customerName(), "client"));
        context.setVariable("documentLabel", documentLabel);
        context.setVariable("documentNumber", valueOrDefault(document.documentNumber(), "-"));
        context.setVariable("title", switch (state) {
            case SETTLED -> "Votre " + documentLabel + " est soldee";
            case OVERDUE -> "Rappel de reglement";
            case PARTIALLY_PAID -> "Votre reglement a ete enregistre";
            case ISSUED -> "Votre " + documentLabel + " est disponible";
        });
        context.setVariable("subtitle", switch (state) {
            case SETTLED -> "Plus rien ne reste a regler";
            case OVERDUE -> "Un solde reste a regler sur cette facture";
            case PARTIALLY_PAID -> "Un solde reste a regler";
            case ISSUED -> "Le recapitulatif est disponible en piece jointe";
        });
        String standardMessage = switch (state) {
            case SETTLED -> "Nous accusons reception de votre reglement de " + paid + ". Votre "
                    + documentLabel + " est soldee, plus rien ne reste a regler. Vous en trouverez"
                    + " la version a jour en piece jointe.";
            case OVERDUE -> "Nous vous rappelons qu'un solde reste a regler. Vous trouverez le detail de la facture en piece jointe.";
            case PARTIALLY_PAID -> "Nous accusons reception de votre reglement de " + paid + ". Un solde de "
                    + balance + " reste a regler. Vous trouverez la " + documentLabel
                    + " a jour en piece jointe.";
            case ISSUED -> "Vous trouverez en piece jointe le detail de votre " + documentLabel
                    + ". Ce document reprend les informations utiles pour votre suivi.";
        };
        // Lot B4 : un mot de l'expediteur precede le message type.
        context.setVariable("message", StringUtils.hasText(personalMessage) ? personalMessage.trim() + "\n\n" + standardMessage : standardMessage);
        context.setVariable("totalAmount", money(document.totalAmount(), document.currency()));
        context.setVariable("paidAmount", paid);
        context.setVariable("balanceDue", balance);
        context.setVariable("issueDate", date(document.issueDate()));
        context.setVariable("dueDate", date(document.dueDate()));
        context.setVariable("overdue", overdue);
        context.setVariable("settled", settled);
        return templateEngine.process("billing-document", context);
    }

    /**
     * Lot B4 : envoi aux destinataires choisis (principaux, copie, copie cachee), avec un message de
     * l'expediteur ; rend l'objet du courriel.
     */
    @Override
    public String sendDocumentTo(String documentNumber, java.util.List<String> to, java.util.List<String> cc, java.util.List<String> bcc,
                                 String personalMessage) {
        BillingDocumentResponse document = billingDocumentService.get(documentNumber);
        if (to == null || to.stream().noneMatch(StringUtils::hasText)) {
            throw new BadRequestException("Indiquez au moins un destinataire");
        }
        byte[] pdf = billingDocumentPdfService.generatePdf(documentNumber);
        String subject = billingSubject(document);
        try {
            emailSender.sendHtmlEmailWithPdfAttachment(com.sni.bokaticowork.core.utils.validation.EmailRecipients.encode(to, cc, bcc),
                    subject, renderBillingDocument(document, personalMessage), document.documentNumber() + ".pdf", pdf).join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new BadRequestException("Envoi du courriel impossible : " + cause.getMessage(), cause);
        }
        return subject;
    }

    /** L'adresse du document, ou l'adresse de facturation choisie par le membre destinataire. */
    private String recipient(BillingDocumentResponse document) {
        return memberContact.recipient(document.customerType(), document.customerCode(), document.customerEmail());
    }

    /** Envoi automatique d'une facture a son emission · se coupe par INVOICE_ISSUED. */
    @Async
    @Override
    public void sendDocumentAsync(String documentNumber) {
        try {
            sendDocumentIfAllowed(documentNumber, EventType.INVOICE_ISSUED);
        } catch (Exception e) {
            log.warn("Envoi async facture {} échoué : {}", documentNumber, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendPaymentConfirmation(BillingDocumentResponse document, PaymentTransactionResponse transaction) {
        String to = recipient(document);
        if (!StringUtils.hasText(to)) {
            log.debug("No email for customer {}, skipping payment confirmation", document.customerCode());
            return;
        }
        if (!memberContact.allowsEmail(document.customerType(), document.customerCode(), EventType.PAYMENT_RECEIVED)) {
            log.info("Confirmation de reglement de {} non envoyee : preference du membre {}",
                    document.documentNumber(), document.customerCode());
            return;
        }
        try {
            String method = transaction != null && transaction.paymentMethod() != null
                    ? paymentMethodLabel(transaction.paymentMethod().name()) : "Paiement";
            String amount = transaction != null && transaction.amount() != null
                    ? money(transaction.amount(), document.currency()) : "";
            String body = renderPaymentConfirmation(document, method, amount);
            emailSender.sendHtmlEmail(to,
                    "Confirmation de reglement - " + document.documentNumber(), body);
        } catch (MessagingException ex) {
            log.warn("Unable to send payment confirmation email for {}: {}", document.documentNumber(), ex.getMessage());
        }
    }

    private String renderPaymentConfirmation(BillingDocumentResponse document, String method, String amount) {
        Context context = new Context(appLocale);
        context.setVariable("customerName", valueOrDefault(document.customerName(), "client"));
        context.setVariable("documentNumber", valueOrDefault(document.documentNumber(), "-"));
        context.setVariable("paidAmount", valueOrDefault(amount, ""));
        context.setVariable("paymentMethod", valueOrDefault(method, "paiement"));
        context.setVariable("balanceDue", money(document.balanceDue(), document.currency()));
        return templateEngine.process("payment-confirmation", context);
    }

    private String billingSubject(BillingDocumentResponse document) {
        String label = billingDocumentLabel(document.documentType());
        String reference = valueOrDefault(document.documentNumber(), label);
        return switch (stateOf(document)) {
            // Le sujet doit se lire sans ouvrir · « Votre facture INV-1 » sur une facture qu'on
            // vient de payer se lit comme une relance.
            case SETTLED -> "Votre " + label + " " + reference + " est soldee";
            case OVERDUE -> "Rappel de reglement - " + reference;
            case PARTIALLY_PAID -> "Reglement enregistre - " + reference;
            case ISSUED -> "Votre " + label + " " + valueOrDefault(document.documentNumber(), "");
        };
    }

    private String billingDocumentLabel(BillingDocumentType type) {
        if (type == null) {
            return "facture";
        }
        return switch (type) {
            case QUOTE               -> "devis";
            case PROFORMA_INVOICE    -> "facture pro forma";
            case INVOICE             -> "facture";
            case CREDIT_NOTE         -> "avoir";
            case CORRECTIVE_INVOICE  -> "facture rectificative";
            case DEBIT_NOTE          -> "note de debit";
        };
    }

    private String paymentMethodLabel(String method) {
        return switch (method) {
            case "CASH" -> "Especes";
            case "WALLET" -> "Portefeuille client";
            case "MOBILE_MONEY" -> "Mobile money";
            case "BANK_TRANSFER" -> "Virement bancaire";
            case "CARD" -> "Carte bancaire";
            case "CHEQUE" -> "Cheque";
            case "MANUAL_ADJUSTMENT" -> "Ajustement";
            case "CREDIT_NOTE" -> "Avoir";
            default -> method.replace('_', ' ').toLowerCase(Locale.ROOT);
        };
    }

    private String date(LocalDate date) {
        return date == null ? "" : DATE_FORMATTER.format(date);
    }

    private String money(BigDecimal amount, String currency) {
        if (amount == null) {
            return "";
        }
        String value = amount.stripTrailingZeros().toPlainString();
        return StringUtils.hasText(currency) ? value + " " + currency : value;
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
