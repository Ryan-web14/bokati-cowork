package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentIntentExpiryAlertWorker {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Africa/Brazzaville"));

    private final PaymentIntentRepository intentRepository;
    private final BillingDocumentRepository billingDocumentRepository;
    private final DefaultEmailSender emailSender;

    @Scheduled(fixedDelayString = "${bokati.payment.workers.expiry-alert-delay-ms:1800000}")
    public void alertExpiringIntents() {
        Instant now = Instant.now();
        Instant threshold = now.plus(2, ChronoUnit.HOURS);
        List<PaymentIntent> expiring = intentRepository.findExpiringSoon(now, threshold);
        for (PaymentIntent intent : expiring) {
            try {
                sendAlert(intent);
                // The 2 h alert window spans four runs of this worker. Marking the intent here —
                // even when sendAlert had nothing to send — is what keeps it to one reminder.
                intent.setExpiryAlertSentAt(now);
                intentRepository.save(intent);
            } catch (Exception ex) {
                log.error("Expiry alert failed for intent {}: {}", intent.getIntentNumber(), ex.getMessage());
            }
        }
        if (!expiring.isEmpty()) {
            log.info("Processed expiry alerts for {} payment intents", expiring.size());
        }
    }

    private void sendAlert(PaymentIntent intent) {
        log.warn("Intent {} ({}/{}) expires at {}", intent.getIntentNumber(),
                intent.getCustomerType(), intent.getCustomerCode(), intent.getExpiresAt());

        if (!"BILLING_DOCUMENT".equalsIgnoreCase(intent.getSourceType())) {
            return;
        }
        BillingDocument document = billingDocumentRepository
                .findByDocumentNumber(intent.getSourceCode()).orElse(null);
        if (document == null || !StringUtils.hasText(document.getCustomerEmail())) {
            return;
        }
        String expiresAt = DISPLAY_FORMAT.format(intent.getExpiresAt());
        String body = "<p>Bonjour " + document.getCustomerName() + ",</p>"
                + "<p>Votre intention de paiement pour la facture <strong>" + document.getDocumentNumber()
                + "</strong> (montant : <strong>" + intent.getAmount().stripTrailingZeros().toPlainString()
                + " " + intent.getCurrency() + "</strong>) expire le <strong>" + expiresAt + "</strong>.</p>"
                + "<p>Merci de procéder au règlement avant cette date pour éviter l'annulation.</p>";
        try {
            emailSender.sendHtmlEmail(document.getCustomerEmail(),
                    "Rappel : paiement en attente - " + document.getDocumentNumber(), body);
        } catch (MessagingException ex) {
            log.warn("Could not send expiry alert email for intent {}: {}", intent.getIntentNumber(), ex.getMessage());
        }
    }
}