package com.sni.bokaticowork.features.crm.worker;

import com.sni.bokaticowork.features.crm.model.Lead;
import com.sni.bokaticowork.features.crm.repository.LeadRepository;
import com.sni.bokaticowork.features.crm.service.interfaces.CrmEmailService;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeadDormantAlertWorker implements SchedulingConfigurer {

    private static final String CRON_KEY = "bokati.crm.dormant-cron";
    private static final String DEFAULT_CRON = "0 0 8 * * MON-FRI";
    private static final String THRESHOLD_DAYS_KEY = "bokati.crm.dormant-threshold-days";

    private SettingTriggers triggers;
    private ObjectProvider<LeadDormantAlertWorker> self;
    private PlatformSettings platformSettings;

    private final LeadRepository leadRepository;
    private final CrmEmailService emailService;

    @Value("${bokati.crm.dormant-threshold-days:7}")
    private int dormantThresholdDays;

    @Transactional
    public void checkDormantLeads() {
        int thresholdDays = dormantThresholdDays();
        Instant before = Instant.now().minus(thresholdDays, ChronoUnit.DAYS);
        List<Lead> candidates = leadRepository.findDormantCandidates(before);
        if (candidates.isEmpty()) return;

        log.info("Dormant leads: {} lead(s) sans activité depuis {} jours", candidates.size(), thresholdDays);
        for (Lead lead : candidates) {
            try {
                emailService.sendDormantAlert(lead);
                lead.setDormantAlertSentAt(Instant.now());
                leadRepository.save(lead);
                log.info("Alerte dormant envoyée pour lead {}", lead.getLeadNumber());
            } catch (Exception ex) {
                log.error("Erreur alerte dormant pour lead {}", lead.getLeadNumber(), ex);
            }
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<LeadDormantAlertWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().checkDormantLeads(), triggers.cron(CRON_KEY, DEFAULT_CRON));
    }

    private int dormantThresholdDays() {
        return platformSettings == null ? dormantThresholdDays : platformSettings.integer(THRESHOLD_DAYS_KEY, dormantThresholdDays);
    }
}
