package com.sni.bokaticowork.features.support.worker;

import com.sni.bokaticowork.features.support.enums.TicketEventType;
import com.sni.bokaticowork.features.support.enums.TicketStatus;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
import com.sni.bokaticowork.features.support.service.implementation.SupportTicketEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class SupportAutoCloseWorker implements SchedulingConfigurer {

    private static final String AUTO_CLOSE_CHECK_DELAY_MS_KEY = "bokati.support.auto-close-check-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<SupportAutoCloseWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<SupportAutoCloseWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().closeStaleResolvedTickets(), triggers.fixedDelay(AUTO_CLOSE_CHECK_DELAY_MS_KEY, 3600000L));
    }

    private final SupportTicketRepository ticketRepository;
    private static final String DELAY_KEY = "bokati.support.auto-close-delay-days";

    private final SupportTicketEventWriter eventWriter;
    private final com.sni.bokaticowork.core.settings.platform.PlatformSettings settings;

    @Value("${" + DELAY_KEY + ":5}")
    private int autoCloseDays;

    @Transactional
    public void closeStaleResolvedTickets() {
        long delayDays = settings.number(DELAY_KEY, autoCloseDays);
        Instant cutoff = Instant.now().minus(delayDays, ChronoUnit.DAYS);
        List<SupportTicket> candidates = ticketRepository.findAutoCloseCandidates(cutoff);
        if (candidates.isEmpty()) return;

        log.info("Auto-close worker: {} ticket(s) RESOLVED sans réponse client depuis {} jours", candidates.size(), delayDays);
        for (SupportTicket ticket : candidates) {
            try {
                ticket.setStatus(TicketStatus.CLOSED);
                ticket.setClosedAt(Instant.now());
                ticketRepository.save(ticket);
                eventWriter.writeSystem(ticket, TicketEventType.AUTO_CLOSED,
                        "Ticket clôturé automatiquement après " + delayDays + " jour(s) sans réponse client suite à la résolution");
                log.info("Ticket {} auto-fermé (résolu le {})", ticket.getTicketNumber(), ticket.getResolvedAt());
            } catch (Exception ex) {
                log.error("Erreur auto-fermeture ticket {}", ticket.getTicketNumber(), ex);
            }
        }
    }
}
