package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentPdfService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
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

    @Override
    @Transactional(readOnly = true)
    public boolean sendDocument(String documentNumber) {
        BillingDocumentResponse document = billingDocumentService.get(documentNumber);
        if (!StringUtils.hasText(document.customerEmail())) {
            throw new BadRequestException("Billing document customer has no email");
        }
        byte[] pdf = billingDocumentPdfService.generatePdf(documentNumber);
        try {
            return emailSender.sendHtmlEmailWithPdfAttachment(
                    document.customerEmail(),
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

    private String renderBillingDocument(BillingDocumentResponse document) {
        boolean overdue = BillingDocumentStatus.OVERDUE.equals(document.status());
        String documentLabel = billingDocumentLabel(document.documentType());
        Context context = new Context(appLocale);
        context.setVariable("customerName", valueOrDefault(document.customerName(), "client"));
        context.setVariable("documentLabel", documentLabel);
        context.setVariable("documentNumber", valueOrDefault(document.documentNumber(), "-"));
        context.setVariable("title", overdue ? "Rappel de reglement" : "Votre " + documentLabel + " est disponible");
        context.setVariable("subtitle", overdue
                ? "Un solde reste a regler sur cette facture"
                : "Le recapitulatif est disponible en piece jointe");
        context.setVariable("message", overdue
                ? "Nous vous rappelons qu'un solde reste a regler. Vous trouverez le detail de la facture en piece jointe."
                : "Vous trouverez en piece jointe le detail de votre " + documentLabel + ". Ce document reprend les informations utiles pour votre suivi.");
        context.setVariable("totalAmount", money(document.totalAmount(), document.currency()));
        context.setVariable("paidAmount", money(document.paidAmount(), document.currency()));
        context.setVariable("balanceDue", money(document.balanceDue(), document.currency()));
        context.setVariable("issueDate", date(document.issueDate()));
        context.setVariable("dueDate", date(document.dueDate()));
        context.setVariable("overdue", overdue);
        return templateEngine.process("billing-document", context);
    }

    @Async
    @Override
    public void sendDocumentAsync(String documentNumber) {
        try {
            sendDocument(documentNumber);
        } catch (Exception e) {
            log.warn("Envoi async facture {} échoué : {}", documentNumber, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendPaymentConfirmation(BillingDocumentResponse document, PaymentTransactionResponse transaction) {
        if (!StringUtils.hasText(document.customerEmail())) {
            log.debug("No email for customer {}, skipping payment confirmation", document.customerCode());
            return;
        }
        try {
            String method = transaction != null && transaction.paymentMethod() != null
                    ? paymentMethodLabel(transaction.paymentMethod().name()) : "Paiement";
            String amount = transaction != null && transaction.amount() != null
                    ? money(transaction.amount(), document.currency()) : "";
            String body = renderPaymentConfirmation(document, method, amount);
            emailSender.sendHtmlEmail(document.customerEmail(),
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
        if (BillingDocumentStatus.OVERDUE.equals(document.status())) {
            return "Rappel de reglement - " + valueOrDefault(document.documentNumber(), label);
        }
        return "Votre " + label + " " + valueOrDefault(document.documentNumber(), "");
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
