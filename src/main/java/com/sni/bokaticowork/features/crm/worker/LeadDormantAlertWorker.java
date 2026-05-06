package com.sni.bokaticowork.features.crm.worker;

import com.sni.bokaticowork.features.crm.model.Lead;
import com.sni.bokaticowork.features.crm.repository.LeadRepository;
import com.sni.bokaticowork.features.crm.service.interfaces.CrmEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeadDormantAlertWorker {

    private final LeadRepository leadRepository;
    private final CrmEmailService emailService;

    @Value("${bokati.crm.dormant-threshold-days:7}")
    private int dormantThresholdDays;

    @Scheduled(cron = "${bokati.crm.dormant-cron:0 0 8 * * MON-FRI}")
    @Transactional
    public void checkDormantLeads() {
        Instant before = Instant.now().minus(dormantThresholdDays, ChronoUnit.DAYS);
        List<Lead> candidates = leadRepository.findDormantCandidates(before);
        if (candidates.isEmpty()) return;

        log.info("Dormant leads: {} lead(s) sans activité depuis {} jours", candidates.size(), dormantThresholdDays);
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
}
