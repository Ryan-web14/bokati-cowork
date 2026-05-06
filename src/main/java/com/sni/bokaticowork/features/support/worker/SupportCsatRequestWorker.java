package com.sni.bokaticowork.features.support.worker;

import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
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
public class SupportCsatRequestWorker {

    private final SupportTicketRepository ticketRepository;
    private final SupportEmailService emailService;

    @Scheduled(fixedDelayString = "${bokati.support.csat-check-delay-ms:3600000}")
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
