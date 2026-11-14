package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentIntentExpiryAlertWorker implements SchedulingConfigurer {

    private static final String EXPIRY_ALERT_DELAY_MS_KEY = "bokati.payment.workers.expiry-alert-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<PaymentIntentExpiryAlertWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    private org.thymeleaf.spring6.SpringTemplateEngine templateEngine;

    /** Gabarit email/payment-expiry-reminder (modifiable en modele de courriel) ; absent, le texte de repli sert. */
    @Autowired(required = false)
    void setTemplateEngine(org.thymeleaf.spring6.SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Autowired
    void setSelf(ObjectProvider<PaymentIntentExpiryAlertWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().alertExpiringIntents(), triggers.fixedDelay(EXPIRY_ALERT_DELAY_MS_KEY, 1800000L));
    }

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Africa/Brazzaville"));

    private final PaymentIntentRepository intentRepository;
    private final BillingDocumentRepository billingDocumentRepository;
    private final DefaultEmailSender emailSender;

    public void alertExpiringIntents() {
        Instant now = Instant.now();
        Instant threshold = now.plus(2, ChronoUnit.HOURS);
        List<PaymentIntent> expiring = intentRepository.findExpiringSoon(now, threshold);
        for (PaymentIntent intent : expiring) {
            try {
                sendAlert(intent);
                // The 2 h alert window spans four runs of this worker. Marking the intent here · 
                // even when sendAlert had nothing to send · is what keeps it to one reminder.
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
        String amount = intent.getAmount().stripTrailingZeros().toPlainString();
        String body;
        if (templateEngine != null) {
            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context(java.util.Locale.FRENCH);
            context.setVariable("customerName", document.getCustomerName());
            context.setVariable("documentNumber", document.getDocumentNumber());
            context.setVariable("amount", amount);
            context.setVariable("currency", intent.getCurrency());
            context.setVariable("expiresAt", expiresAt);
            body = templateEngine.process("email/payment-expiry-reminder", context);
        } else body = "<p>Bonjour " + org.springframework.web.util.HtmlUtils.htmlEscape(String.valueOf(document.getCustomerName())) + ",</p>"
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