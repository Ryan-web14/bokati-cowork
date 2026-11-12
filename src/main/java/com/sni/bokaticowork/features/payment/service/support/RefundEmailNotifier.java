package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sni.bokaticowork.features.billing.service.support.MemberBillingContact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundEmailNotifier {

    private final DefaultEmailSender emailSender;
    private final TemplateEngine templateEngine;
    private final TransactionContextResolver contextResolver;

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private MemberBillingContact memberContact;

    @Autowired(required = false)
    void setMemberContact(MemberBillingContact memberContact) {
        this.memberContact = memberContact;
    }

    public void notify(PaymentTransaction transaction) {
        if (transaction == null || transaction.getPaymentIntent() == null) {
            return;
        }
        PaymentIntent intent = transaction.getPaymentIntent();
        // Message de facturation · l'adresse de facturation du membre passe avant celle du compte.
        TransactionContextResolver.PartyView party = contextResolver.resolveBillingParty(intent.getCustomerType(), intent.getCustomerCode());
        if (!StringUtils.hasText(party.email())) {
            return;
        }
        Context context = context(transaction, intent, party);
        try {
            emailSender.sendHtmlEmail(
                    party.email(),
                    "Confirmation remboursement " + transaction.getTransactionNumber(),
                    templateEngine.process("refund-confirmation", context)
            );
        } catch (MessagingException ex) {
            log.warn("Failed to queue refund confirmation email for {}", transaction.getTransactionNumber(), ex);
        }
    }

    private Context context(PaymentTransaction transaction,
                            PaymentIntent intent,
                            TransactionContextResolver.PartyView party) {
        Context context = new Context(Locale.FRENCH);
        context.setVariable("recipientName", valueOrDefault(party.name(), "client"));
        context.setVariable("transactionNumber", transaction.getTransactionNumber());
        context.setVariable("intentNumber", intent.getIntentNumber());
        context.setVariable("sourceType", intent.getSourceType());
        context.setVariable("sourceCode", intent.getSourceCode());
        context.setVariable("customerCode", intent.getCustomerCode());
        context.setVariable("amount", money(transaction.getAmount(), transaction.getCurrency()));
        context.setVariable("paymentMethod", transaction.getPaymentMethod() == null ? "-" : transaction.getPaymentMethod().name());
        context.setVariable("provider", valueOrDefault(transaction.getProvider(), "-"));
        context.setVariable("providerReference", valueOrDefault(transaction.getProviderReference(), "-"));
        context.setVariable("status", transaction.getStatus() == null ? PaymentTransactionStatus.REFUNDED.name() : transaction.getStatus().name());
        // L'heure se lit dans le fuseau du membre destinataire, a defaut celui de l'exploitant.
        Instant processedAt = transaction.getPaidAt() == null ? transaction.getUpdatedAt() : transaction.getPaidAt();
        ZoneId zone = memberContact == null ? MemberBillingContact.defaultZone()
                : memberContact.zone(intent.getCustomerType(), intent.getCustomerCode());
        context.setVariable("processedAt", processedAt == null ? null : DATE_TIME.format(processedAt.atZone(zone)));
        context.setVariable("reason", transaction.getFailureReason());
        return context;
    }

    private String money(BigDecimal amount, String currency) {
        String value = amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
        return value + " " + valueOrDefault(currency, "");
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
