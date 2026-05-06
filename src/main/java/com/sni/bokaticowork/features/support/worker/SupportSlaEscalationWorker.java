package com.sni.bokaticowork.features.support.worker;

import com.sni.bokaticowork.features.support.enums.TicketPriority;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
import com.sni.bokaticowork.features.support.service.interfaces.SupportEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupportSlaEscalationWorker {

    private final SupportTicketRepository ticketRepository;
    private final SupportEmailService emailService;

    @Scheduled(fixedDelayString = "${bokati.support.sla-check-delay-ms:3600000}")
    @Transactional
    public void checkSlaBreaches() {
        Instant now = Instant.now();
        List<SupportTicket> candidates = ticketRepository.findSlaBreachCandidates(now);
        if (candidates.isEmpty()) return;

        log.info("SLA check: {} ticket(s) en dépassement", candidates.size());
        for (SupportTicket ticket : candidates) {
            try {
                escalate(ticket, now);
            } catch (Exception ex) {
                log.error("Erreur escalade SLA pour ticket {}", ticket.getTicketNumber(), ex);
            }
        }
    }

    private void escalate(SupportTicket ticket, Instant now) {
        boolean firstResponseBreached = ticket.getFirstRespondedAt() == null
                && ticket.getFirstResponseDueAt() != null
                && ticket.getFirstResponseDueAt().isBefore(now);

        boolean resolutionBreached = ticket.getResolvedAt() == null
                && ticket.getResolutionDueAt() != null
                && ticket.getResolutionDueAt().isBefore(now);

        if (firstResponseBreached) {
            emailService.sendSlaBreachAlert(ticket, "FIRST_RESPONSE");
            log.warn("SLA première réponse dépassé — {}", ticket.getTicketNumber());
        }
        if (resolutionBreached) {
            emailService.sendSlaBreachAlert(ticket, "RESOLUTION");
            log.warn("SLA résolution dépassé — {}", ticket.getTicketNumber());
            bumpPriority(ticket);
        }
    }

    private void bumpPriority(SupportTicket ticket) {
        if (ticket.getPriority() == TicketPriority.URGENT) return;
        TicketPriority bumped = switch (ticket.getPriority()) {
            case LOW    -> TicketPriority.MEDIUM;
            case MEDIUM -> TicketPriority.HIGH;
            case HIGH   -> TicketPriority.URGENT;
            default     -> ticket.getPriority();
        };
        ticket.setPriority(bumped);
        ticketRepository.save(ticket);
        log.info("Priorité élevée à {} pour ticket {} (SLA résolution dépassé)",
                bumped, ticket.getTicketNumber());
    }
}
