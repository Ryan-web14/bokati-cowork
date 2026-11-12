package com.sni.bokaticowork.features.billing.worker;

import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingPaymentReminderWorker implements SchedulingConfigurer {

    private static final String CRON_KEY = "bokati.billing.workers.payment-reminder-cron";
    private static final String DEFAULT_CRON = "0 0 8 * * *";
    private static final String COOLDOWN_DAYS_KEY = "bokati.billing.workers.payment-reminder-cooldown-days";
    private static final String MAX_REMINDERS_KEY = "bokati.billing.workers.payment-reminder-max";

    private SettingTriggers triggers;
    private ObjectProvider<BillingPaymentReminderWorker> self;
    private PlatformSettings platformSettings;

    private final BillingDocumentRepository documentRepository;
    private final BillingEmailService billingEmailService;
    private final com.sni.bokaticowork.features.billing.dunning.repository.DunningPolicyRepository dunningPolicyRepository;

    /**
     * An invoice stays OVERDUE until it is paid, so the candidate query matches it on every run.
     * These two bounds turn that into a dunning sequence instead of a daily re-send of the
     * document · attached PDF and all · for as long as the balance is outstanding.
     */
    @Value("${bokati.billing.workers.payment-reminder-cooldown-days:7}")
    private int cooldownDays;

    @Value("${bokati.billing.workers.payment-reminder-max:3}")
    private int maxReminders;

    public void sendOverdueReminders() {
        try {
            // Des qu'une politique de relance parametree est active, c'est elle qui relance · pas ce renvoi periodique
            if (dunningPolicyRepository.existsByActiveTrue()) {
                return;
            }
            Instant now = Instant.now();
            Instant notRemindedSince = now.minus(cooldownDays(), ChronoUnit.DAYS);
            int sent = 0;
            int failed = 0;
            for (var document : documentRepository.findOverdueReminderCandidates(100, maxReminders(), notRemindedSince)) {
                try {
                    if (billingEmailService.sendDocument(document.getDocumentNumber())) {
                        int previous = document.getPaymentReminderCount() == null
                                ? 0 : document.getPaymentReminderCount();
                        document.setPaymentReminderCount(previous + 1);
                        document.setPaymentReminderSentAt(now);
                        documentRepository.save(document);
                        sent++;
                    }
                } catch (Exception ex) {
                    failed++;
                    log.warn("Failed to send overdue reminder for document {}: {}",
                            document.getDocumentNumber(), ex.getMessage());
                }
            }
            if (sent > 0 || failed > 0) {
                log.info("Overdue billing reminders: {} sent, {} failed", sent, failed);
            }
        } catch (Exception ex) {
            log.error("BillingPaymentReminderWorker failed: {}", ex.getMessage(), ex);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<BillingPaymentReminderWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().sendOverdueReminders(), triggers.cron(CRON_KEY, DEFAULT_CRON));
    }

    private int cooldownDays() {
        return platformSettings == null ? cooldownDays : platformSettings.integer(COOLDOWN_DAYS_KEY, cooldownDays);
    }

    private int maxReminders() {
        return platformSettings == null ? maxReminders : platformSettings.integer(MAX_REMINDERS_KEY, maxReminders);
    }
}
