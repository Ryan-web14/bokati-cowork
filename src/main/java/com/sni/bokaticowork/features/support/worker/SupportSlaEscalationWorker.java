package com.sni.bokaticowork.features.support.worker;

import com.sni.bokaticowork.features.support.enums.TicketEventType;
import com.sni.bokaticowork.features.support.enums.TicketPriority;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
import com.sni.bokaticowork.features.support.service.implementation.SupportTicketEventWriter;
import com.sni.bokaticowork.features.support.service.interfaces.SupportEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupportSlaEscalationWorker {

    private static final int MAX_ESCALATION_LEVEL = 3;

    private final SupportTicketRepository ticketRepository;
    private final SupportEmailService emailService;
    private final SupportTicketEventWriter eventWriter;

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

        if (!firstResponseBreached && !resolutionBreached) return;

        if (firstResponseBreached) {
            emailService.sendSlaBreachAlert(ticket, "FIRST_RESPONSE");
            log.warn("SLA première réponse dépassé · {}", ticket.getTicketNumber());
        }
        if (resolutionBreached) {
            emailService.sendSlaBreachAlert(ticket, "RESOLUTION");
            log.warn("SLA résolution dépassé · {}", ticket.getTicketNumber());
            bumpPriority(ticket);
        }

        String reason = resolutionBreached
                ? "Délai de résolution dépassé pour le ticket " + ticket.getTicketNumber()
                : "Délai de première réponse dépassé pour le ticket " + ticket.getTicketNumber();

        if (canEscalateAgain(ticket, now)) {
            int currentLevel = ticket.getEscalationLevel() == null ? 0 : ticket.getEscalationLevel();
            int nextLevel = Math.min(currentLevel + 1, MAX_ESCALATION_LEVEL);
            ticket.setEscalationLevel(nextLevel);
            ticket.setEscalatedAt(now);
            ticket.setEscalationReason(reason);
            ticket.setLastSlaAlertSentAt(now);
            ticketRepository.save(ticket);
            eventWriter.writeSystem(ticket, TicketEventType.SLA_ESCALATED,
                    "Escalade niveau " + nextLevel + " · " + reason);
            emailService.sendEscalationAlert(ticket, nextLevel, reason);
            log.warn("Escalade niveau {} pour ticket {} ({})", nextLevel, ticket.getTicketNumber(), reason);
        }
    }

    /**
     * Niveau 1 : agent assigné · niveau 2 : manager support · niveau 3 : admin/direction.
     * Le délai entre deux escalades croît avec le niveau pour éviter le spam de notifications.
     */
    private boolean canEscalateAgain(SupportTicket ticket, Instant now) {
        int currentLevel = ticket.getEscalationLevel() == null ? 0 : ticket.getEscalationLevel();
        if (currentLevel >= MAX_ESCALATION_LEVEL) return false;
        if (ticket.getLastSlaAlertSentAt() == null) return true;
        long cooldownHours = switch (currentLevel) {
            case 0 -> 0L;
            case 1 -> 4L;
            default -> 8L;
        };
        return ticket.getLastSlaAlertSentAt().isBefore(now.minus(cooldownHours, ChronoUnit.HOURS));
    }

    private void bumpPriority(SupportTicket ticket) {
        if (ticket.getPriority() == TicketPriority.URGENT) return;
        TicketPriority previous = ticket.getPriority();
        TicketPriority bumped = switch (previous) {
            case LOW    -> TicketPriority.MEDIUM;
            case MEDIUM -> TicketPriority.HIGH;
            case HIGH   -> TicketPriority.URGENT;
            default     -> previous;
        };
        ticket.setPriority(bumped);
        ticketRepository.save(ticket);
        eventWriter.writeSystem(ticket, TicketEventType.PRIORITY_CHANGED,
                "Priorité élevée de " + previous + " à " + bumped + " suite à un dépassement du délai de résolution SLA");
        log.info("Priorité élevée à {} pour ticket {} (SLA résolution dépassé)",
                bumped, ticket.getTicketNumber());
    }
}
