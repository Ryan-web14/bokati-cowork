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
import com.sni.bokaticowork.features.support.service.interfaces.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
@RequiredArgsConstructor
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final SupportTicketMapper mapper;

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
            addMessage(saved.getTicketNumber(), new AddTicketMessageRequest(TicketSenderType.CLIENT, request.ownerCode(), request.contactName(), request.description(), false));
        }
        return get(saved.getTicketNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<SupportTicketResponse> search(TicketStatus status, Long assignedTo, String ownerType, String ownerCode, Pageable pageable) {
        return new PaginatedResponse<>(ticketRepository.search(status, assignedTo, ownerType, ownerCode, pageable)
                .map(ticket -> mapper.toResponse(ticket, messageRepository.findAllByTicketOrderByCreatedAtAsc(ticket))));
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketResponse get(String ticketNumber) {
        SupportTicket ticket = getTicket(ticketNumber);
        return mapper.toResponse(ticket, messageRepository.findAllByTicketOrderByCreatedAtAsc(ticket));
    }

    @Override
    public SupportTicketResponse assign(String ticketNumber, AssignTicketRequest request) {
        SupportTicket ticket = getTicket(ticketNumber);
        ticket.setAssignedTo(request == null ? null : request.assignedTo());
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        return mapper.toResponse(ticketRepository.save(ticket), messageRepository.findAllByTicketOrderByCreatedAtAsc(ticket));
    }

    @Override
    public SupportTicketResponse updateStatus(String ticketNumber, UpdateTicketStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new BadRequestException("Ticket status is required");
        }
        SupportTicket ticket = getTicket(ticketNumber);
        ticket.setStatus(request.status());
        if (request.status() == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(Instant.now());
        }
        if (request.status() == TicketStatus.CLOSED) {
            ticket.setClosedAt(Instant.now());
        }
        return mapper.toResponse(ticketRepository.save(ticket), messageRepository.findAllByTicketOrderByCreatedAtAsc(ticket));
    }

    @Override
    public SupportTicketResponse addMessage(String ticketNumber, AddTicketMessageRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new BadRequestException("Message is required");
        }
        SupportTicket ticket = getTicket(ticketNumber);
        TicketSenderType senderType = request.senderType() == null ? TicketSenderType.AGENT : request.senderType();
        TicketMessage message = TicketMessage.builder()
                .ticket(ticket)
                .senderType(senderType)
                .senderId(request.senderId())
                .senderName(request.senderName())
                .message(request.message())
                .internal(Boolean.TRUE.equals(request.internal()))
                .build();
        messageRepository.save(message);
        if (senderType == TicketSenderType.AGENT && ticket.getFirstRespondedAt() == null) {
            ticket.setFirstRespondedAt(Instant.now());
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }
        return get(ticketNumber);
    }

    @Override
    public SupportTicketResponse close(String ticketNumber) {
        return updateStatus(ticketNumber, new UpdateTicketStatusRequest(TicketStatus.CLOSED));
    }

    @Override
    @Transactional(readOnly = true)
    public SupportMetricsResponse metrics() {
        Instant now = Instant.now();
        var all = ticketRepository.findAll();
        return new SupportMetricsResponse(
                all.stream().filter(t -> t.getStatus() == TicketStatus.OPEN || t.getStatus() == TicketStatus.IN_PROGRESS).count(),
                all.stream().filter(t -> t.getFirstRespondedAt() == null && t.getFirstResponseDueAt() != null && t.getFirstResponseDueAt().isBefore(now)).count(),
                all.stream().filter(t -> t.getResolvedAt() == null && t.getResolutionDueAt() != null && t.getResolutionDueAt().isBefore(now)).count(),
                all.stream().filter(t -> t.getStatus() == TicketStatus.RESOLVED || t.getStatus() == TicketStatus.CLOSED).count()
        );
    }

    private SupportTicket getTicket(String ticketNumber) {
        if (!StringUtils.hasText(ticketNumber)) {
            throw new BadRequestException("Ticket number is required");
        }
        return ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found: " + ticketNumber));
    }

    private long firstResponseHours(TicketPriority priority) {
        return switch (priority) {
            case URGENT -> 2;
            case HIGH -> 8;
            case MEDIUM -> 24;
            case LOW -> 48;
        };
    }

    private long resolutionHours(TicketPriority priority) {
        return switch (priority) {
            case URGENT -> 8;
            case HIGH -> 24;
            case MEDIUM -> 72;
            case LOW -> 120;
        };
    }
}
