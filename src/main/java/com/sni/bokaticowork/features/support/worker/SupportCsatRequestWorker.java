package com.sni.bokaticowork.features.support.worker;

import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
import com.sni.bokaticowork.features.support.service.interfaces.SupportEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
public class SupportCsatRequestWorker implements SchedulingConfigurer {

    private static final String CSAT_CHECK_DELAY_MS_KEY = "bokati.support.csat-check-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<SupportCsatRequestWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<SupportCsatRequestWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().sendPendingCsatRequests(), triggers.fixedDelay(CSAT_CHECK_DELAY_MS_KEY, 3600000L));
    }

    private final SupportTicketRepository ticketRepository;
    private final SupportEmailService emailService;

    @Transactional
    public void sendPendingCsatRequests() {
        Instant closedBefore = Instant.now().minus(24, ChronoUnit.HOURS);
        List<SupportTicket> candidates = ticketRepository.findCsatRequestCandidates(closedBefore);
        if (candidates.isEmpty()) return;

        log.info("CSAT worker: {} ticket(s) éligibles à l'envoi du questionnaire", candidates.size());
        for (SupportTicket ticket : candidates) {
            try {
                emailService.sendCsatRequest(ticket);
                ticket.setCsatEmailSentAt(Instant.now());
                ticketRepository.save(ticket);
                log.info("Email CSAT envoyé pour ticket {}", ticket.getTicketNumber());
            } catch (Exception ex) {
                log.error("Erreur envoi CSAT pour ticket {}", ticket.getTicketNumber(), ex);
            }
        }
    }
}
