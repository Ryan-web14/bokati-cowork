package com.sni.bokaticowork.features.support.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.support.dto.SupportDtos.*;
import com.sni.bokaticowork.features.support.enums.*;
import com.sni.bokaticowork.features.support.mapper.SupportTicketMapper;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.TicketMessage;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
import com.sni.bokaticowork.features.support.repository.TicketMessageRepository;
import com.sni.bokaticowork.features.support.service.interfaces.SupportEmailService;
import com.sni.bokaticowork.features.support.service.interfaces.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final SupportTicketMapper mapper;
    @Lazy private final SupportEmailService emailService;

    // ── Création ─────────────────────────────────────────────────

    @Override
    public SupportTicketResponse create(CreateTicketRequest request) {
        if (request == null || !StringUtils.hasText(request.title())) {
            throw new BadRequestException("Ticket title is required");
        }
        TicketPriority priority = request.priority() == null ? TicketPriority.MEDIUM : request.priority();
        SupportTicket ticket = SupportTicket.builder()
                .ticketNumber("TCK-" + Instant.now().toEpochMilli())
                .title(request.title().trim())
                .description(request.description())
                .status(TicketStatus.OPEN)
                .priority(priority)
                .category(request.category() == null ? TicketCategory.OTHER : request.category())
                .ownerType(request.ownerType())
                .ownerCode(request.ownerCode())
                .contactName(request.contactName())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .relatedType(request.relatedType())
                .relatedCode(request.relatedCode())
                .firstResponseDueAt(Instant.now().plus(firstResponseHours(priority), ChronoUnit.HOURS))
                .resolutionDueAt(Instant.now().plus(resolutionHours(priority), ChronoUnit.HOURS))
                .build();
        SupportTicket saved = ticketRepository.save(ticket);
        if (StringUtils.hasText(request.description())) {
            saveMessage(saved, TicketSenderType.CLIENT, request.ownerCode(),
                    request.contactName(), request.description(), false);
        }
        SupportTicketResponse response = get(saved.getTicketNumber());
        emailService.sendTicketCreated(saved);
        return response;
    }

    // ── Recherche ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<SupportTicketResponse> search(TicketStatus status, Long assignedTo,
                                                           String ownerType, String ownerCode,
                                                           String searchText, Pageable pageable) {
        String text = StringUtils.hasText(searchText) ? searchText.trim() : null;
        String statusStr = status != null ? status.name() : null;
        return new PaginatedResponse<>(
                ticketRepository.search(statusStr, assignedTo, ownerType, ownerCode, text, pageable)
                        .map(t -> mapper.toResponse(t, messageRepository.findAllByTicketOrderByCreatedAtAsc(t))));
    }

    // ── Lecture ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SupportTicketResponse get(String ticketNumber) {
        SupportTicket ticket = getTicket(ticketNumber);
        return mapper.toResponse(ticket, messageRepository.findAllByTicketOrderByCreatedAtAsc(ticket));
    }

    // ── Assignation ───────────────────────────────────────────────

    @Override
    public SupportTicketResponse assign(String ticketNumber, AssignTicketRequest request) {
        SupportTicket ticket = getTicket(ticketNumber);
        Long previousAgent = ticket.getAssignedTo();
        ticket.setAssignedTo(request == null ? null : request.assignedTo());
        if (ticket.getStatus() == TicketStatus.OPEN && ticket.getAssignedTo() != null) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        SupportTicket saved = ticketRepository.save(ticket);
        if (saved.getAssignedTo() != null && !saved.getAssignedTo().equals(previousAgent)) {
            emailService.sendClientMessage(saved, null);
        }
        return mapper.toResponse(saved, messageRepository.findAllByTicketOrderByCreatedAtAsc(saved));
    }

    // ── Changement de statut ──────────────────────────────────────

    @Override
    public SupportTicketResponse updateStatus(String ticketNumber, UpdateTicketStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new BadRequestException("Ticket status is required");
        }
        SupportTicket ticket = getTicket(ticketNumber);
        TicketStatus previous = ticket.getStatus();
        ticket.setStatus(request.status());
        if (request.status() == TicketStatus.RESOLVED && ticket.getResolvedAt() == null) {
            ticket.setResolvedAt(Instant.now());
        }
        if (request.status() == TicketStatus.CLOSED && ticket.getClosedAt() == null) {
            ticket.setClosedAt(Instant.now());
        }
        SupportTicket saved = ticketRepository.save(ticket);
        if (previous != TicketStatus.RESOLVED && saved.getStatus() == TicketStatus.RESOLVED) {
            emailService.sendTicketResolved(saved);
        }
        return mapper.toResponse(saved, messageRepository.findAllByTicketOrderByCreatedAtAsc(saved));
    }

    // ── Ajout de message ──────────────────────────────────────────

    @Override
    public SupportTicketResponse addMessage(String ticketNumber, AddTicketMessageRequest request) {
        if (request == null || !StringUtils.hasText(request.content())) {
            throw new BadRequestException("Message is required");
        }
        SupportTicket ticket = getTicket(ticketNumber);
        TicketSenderType senderType = request.senderType() == null ? TicketSenderType.AGENT : request.senderType();
        boolean isInternal = Boolean.TRUE.equals(request.internal());

        TicketMessage message = saveMessage(ticket, senderType, request.senderId(),
                request.senderName(), request.content(), isInternal);

        if (senderType == TicketSenderType.AGENT && ticket.getFirstRespondedAt() == null) {
            ticket.setFirstRespondedAt(Instant.now());
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }
        if (senderType == TicketSenderType.CLIENT) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }

        if (!isInternal) {
            if (senderType == TicketSenderType.AGENT) {
                emailService.sendAgentMessage(ticket, message);
            } else if (senderType == TicketSenderType.CLIENT) {
                emailService.sendClientMessage(ticket, message);
            }
        }

        return get(ticketNumber);
    }

    // ── Clôture ───────────────────────────────────────────────────

    @Override
    public SupportTicketResponse close(String ticketNumber) {
        return updateStatus(ticketNumber, new UpdateTicketStatusRequest(TicketStatus.CLOSED));
    }

    // ── CSAT ─────────────────────────────────────────────────────

    @Override
    public SupportTicketResponse submitCsat(String ticketNumber, SubmitCsatRequest request) {
        if (request == null || request.score() == null) {
            throw new BadRequestException("CSAT score is required");
        }
        if (request.score() < 1 || request.score() > 5) {
            throw new BadRequestException("CSAT score must be between 1 and 5");
        }
        SupportTicket ticket = getTicket(ticketNumber);
        if (ticket.getStatus() != TicketStatus.CLOSED && ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new BadRequestException("CSAT can only be submitted for resolved or closed tickets");
        }
        if (ticket.getCsatSubmittedAt() != null) {
            throw new BadRequestException("CSAT already submitted for this ticket");
        }
        ticket.setCsatScore(request.score());
        ticket.setCsatComment(request.comment());
        ticket.setCsatSubmittedAt(Instant.now());
        return mapper.toResponse(ticketRepository.save(ticket),
                messageRepository.findAllByTicketOrderByCreatedAtAsc(ticket));
    }

    // ── Métriques ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SupportMetricsResponse metrics() {
        Instant now = Instant.now();
        return new SupportMetricsResponse(
                ticketRepository.countOpen(),
                ticketRepository.countOverdueFirstResponse(now),
                ticketRepository.countOverdueResolution(now),
                ticketRepository.countResolved()
        );
    }

    // ── Analytics ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SupportAnalyticsResponse analytics(Instant from, Instant to) {
        if (from == null) from = Instant.now().minus(30, ChronoUnit.DAYS);
        if (to == null) to = Instant.now();

        Instant now = Instant.now();
        Map<String, Long> byCategory = toMap(ticketRepository.countByCategoryBetween(from, to));
        Map<String, Long> byPriority = toMap(ticketRepository.countByPriorityBetween(from, to));

        List<AgentWorkloadEntry> agentWorkload = ticketRepository.agentWorkloadBetween(from, to)
                .stream()
                .map(row -> new AgentWorkloadEntry(
                        ((Number) row[0]).longValue(),
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue()))
                .toList();

        Double avgFirstResponse = ticketRepository.avgFirstResponseHoursBetween(from, to);
        Double avgResolution = ticketRepository.avgResolutionHoursBetween(from, to);
        Double avgCsat = ticketRepository.avgCsatScoreBetween(from, to);

        return new SupportAnalyticsResponse(
                from, to,
                ticketRepository.countCreatedBetween(from, to),
                ticketRepository.countResolvedBetween(from, to),
                ticketRepository.countOpenAt(to),
                ticketRepository.countSlaBreachesBetween(now, from, to),
                avgFirstResponse != null ? Math.round(avgFirstResponse * 10.0) / 10.0 : 0.0,
                avgResolution != null ? Math.round(avgResolution * 10.0) / 10.0 : 0.0,
                byCategory,
                byPriority,
                avgCsat != null ? Math.round(avgCsat * 10.0) / 10.0 : 0.0,
                ticketRepository.countCsatResponsesBetween(from, to),
                agentWorkload
        );
    }

    // ── Helpers ───────────────────────────────────────────────────

    private SupportTicket getTicket(String ticketNumber) {
        if (!StringUtils.hasText(ticketNumber)) throw new BadRequestException("Ticket number is required");
        return ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found: " + ticketNumber));
    }

    private TicketMessage saveMessage(SupportTicket ticket, TicketSenderType senderType,
                                      String senderId, String senderName,
                                      String message, boolean internal) {
        return messageRepository.save(TicketMessage.builder()
                .ticket(ticket)
                .senderType(senderType)
                .senderId(senderId)
                .senderName(senderName)
                .message(message)
                .internal(internal)
                .build());
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        return result;
    }

    private long firstResponseHours(TicketPriority priority) {
        return switch (priority) { case URGENT -> 2; case HIGH -> 8; case MEDIUM -> 24; case LOW -> 48; };
    }

    private long resolutionHours(TicketPriority priority) {
        return switch (priority) { case URGENT -> 8; case HIGH -> 24; case MEDIUM -> 72; case LOW -> 120; };
    }
}
