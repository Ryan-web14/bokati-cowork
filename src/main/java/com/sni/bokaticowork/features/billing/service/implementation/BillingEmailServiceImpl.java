package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingEmailServiceImpl implements BillingEmailService {

    private final BillingDocumentService billingDocumentService;
    private final DefaultEmailSender emailSender;

    @Override
    @Transactional(readOnly = true)
    public boolean sendDocument(String documentNumber) {
        BillingDocumentResponse document = billingDocumentService.get(documentNumber);
        if (!StringUtils.hasText(document.customerEmail())) {
            throw new BadRequestException("Billing document customer has no email");
        }
        try {
            return emailSender.sendHtmlEmail(
                    document.customerEmail(),
                    "Document " + document.documentNumber(),
                    "<p>Bonjour " + document.customerName() + ",</p><p>Votre document " + document.documentNumber() + " est disponible.</p>"
            ).join();
        } catch (MessagingException ex) {
            throw new BadRequestException("Unable to send billing document email", ex);
        }
    }

    @Override
    public void sendPaymentConfirmation(BillingDocumentResponse document, PaymentTransactionResponse transaction) {
        if (!StringUtils.hasText(document.customerEmail())) {
            log.debug("No email for customer {}, skipping payment confirmation", document.customerCode());
            return;
        }
        try {
            String method = transaction != null && transaction.paymentMethod() != null
                    ? transaction.paymentMethod().name() : "paiement";
            String amount = transaction != null && transaction.amount() != null
                    ? transaction.amount().stripTrailingZeros().toPlainString() + " " + document.currency() : "";
            String body = "<p>Bonjour " + document.customerName() + ",</p>"
                    + "<p>Nous confirmons la réception de votre paiement de <strong>" + amount + "</strong>"
                    + " par <strong>" + method + "</strong> sur la facture <strong>" + document.documentNumber() + "</strong>.</p>"
                    + "<p>Solde restant : <strong>" + document.balanceDue().stripTrailingZeros().toPlainString()
                    + " " + document.currency() + "</strong></p>"
                    + "<p>Merci pour votre règlement.</p>";
            emailSender.sendHtmlEmail(document.customerEmail(),
                    "Confirmation de paiement - " + document.documentNumber(), body);
        } catch (MessagingException ex) {
            log.warn("Unable to send payment confirmation email for {}: {}", document.documentNumber(), ex.getMessage());
        }
    }
}
