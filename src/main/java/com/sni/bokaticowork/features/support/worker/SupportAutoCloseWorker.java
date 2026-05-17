package com.sni.bokaticowork.features.support.worker;

import com.sni.bokaticowork.features.support.enums.TicketStatus;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
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
public class SupportAutoCloseWorker {

    private final SupportTicketRepository ticketRepository;

    @Value("${bokati.support.auto-close-delay-days:5}")
    private int autoCloseDays;

    @Scheduled(fixedDelayString = "${bokati.support.auto-close-check-delay-ms:3600000}")
    @Transactional
    public void closeStaleResolvedTickets() {
        Instant cutoff = Instant.now().minus(autoCloseDays, ChronoUnit.DAYS);
        List<SupportTicket> candidates = ticketRepository.findAutoCloseCandidates(cutoff);
        if (candidates.isEmpty()) return;

        log.info("Auto-close worker: {} ticket(s) RESOLVED sans réponse client depuis {} jours", candidates.size(), autoCloseDays);
        for (SupportTicket ticket : candidates) {
            try {
                ticket.setStatus(TicketStatus.CLOSED);
                ticket.setClosedAt(Instant.now());
                ticketRepository.save(ticket);
                log.info("Ticket {} auto-fermé (résolu le {})", ticket.getTicketNumber(), ticket.getResolvedAt());
            } catch (Exception ex) {
                log.error("Erreur auto-fermeture ticket {}", ticket.getTicketNumber(), ex);
            }
        }
    }
}
