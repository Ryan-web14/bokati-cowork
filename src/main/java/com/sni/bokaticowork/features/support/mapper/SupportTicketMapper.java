package com.sni.bokaticowork.features.support.mapper;

import com.sni.bokaticowork.features.support.dto.SupportDtos.*;
import com.sni.bokaticowork.features.support.model.QuickReply;
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
                ticket.getCsatScore(),
                ticket.getCsatComment(),
                ticket.getCsatSubmittedAt(),
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

    public QuickReplyResponse toQuickReplyResponse(QuickReply reply) {
        return new QuickReplyResponse(
                reply.getId(),
                reply.getCategory(),
                reply.getTitle(),
                reply.getBody(),
                reply.getActive()
        );
    }
}
