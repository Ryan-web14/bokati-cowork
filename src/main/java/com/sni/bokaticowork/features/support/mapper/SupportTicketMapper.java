package com.sni.bokaticowork.features.support.mapper;

import com.sni.bokaticowork.features.support.dto.SupportDtos.SupportTicketResponse;
import com.sni.bokaticowork.features.support.dto.SupportDtos.TicketMessageResponse;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.TicketMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SupportTicketMapper {
    public SupportTicketResponse toResponse(SupportTicket ticket, List<TicketMessage> messages) {
        return new SupportTicketResponse(
                ticket.getTicketNumber(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCategory(),
                ticket.getOwnerType(),
                ticket.getOwnerCode(),
                ticket.getContactName(),
                ticket.getContactEmail(),
                ticket.getContactPhone(),
                ticket.getAssignedTo(),
                ticket.getRelatedType(),
                ticket.getRelatedCode(),
                ticket.getFirstResponseDueAt(),
                ticket.getResolutionDueAt(),
                ticket.getFirstRespondedAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                messages == null ? List.of() : messages.stream().map(this::toMessageResponse).toList()
        );
    }

    public TicketMessageResponse toMessageResponse(TicketMessage message) {
        return new TicketMessageResponse(
                message.getId(),
                message.getSenderType(),
                message.getSenderId(),
                message.getSenderName(),
                message.getMessage(),
                message.getInternal(),
                message.getCreatedAt()
        );
    }
}
