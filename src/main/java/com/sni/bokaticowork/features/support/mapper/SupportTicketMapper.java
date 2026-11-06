package com.sni.bokaticowork.features.support.mapper;

import com.sni.bokaticowork.features.support.dto.SupportDtos.*;
import com.sni.bokaticowork.features.support.model.QuickReply;
import com.sni.bokaticowork.features.support.model.SupportRoutingRule;
import com.sni.bokaticowork.features.support.model.SupportTag;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.SupportTicketEvent;
import com.sni.bokaticowork.features.support.model.SupportTicketTag;
import com.sni.bokaticowork.features.support.model.TicketAttachment;
import com.sni.bokaticowork.features.support.model.TicketMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SupportTicketMapper {

    /** Le ticket complet, avec son fil et ses pieces jointes. */
    public SupportTicketResponse toResponse(SupportTicket ticket, List<TicketMessageResponse> messages,
                                             List<TicketAttachment> attachments, List<SupportTicketTag> tags) {
        List<AttachmentResponse> files = attachments.stream().map(this::toAttachmentResponse).toList();
        return response(ticket, ticket.getDescription(), messages, files, tags.stream().map(t -> t.getTag().getName()).toList(),
                messages.size(), files.size(),
                (int) messages.stream().filter(message -> !Boolean.TRUE.equals(message.internal())).count(),
                (int) files.stream().filter(file -> !Boolean.TRUE.equals(file.internal())).count());
    }

    /** Une ligne de liste · ni fil, ni pieces jointes, seulement leur nombre. */
    public SupportTicketResponse toSummary(SupportTicket ticket, String description, List<String> tags,
                                           int messageCount, int attachmentCount,
                                           int visibleMessageCount, int visibleAttachmentCount) {
        return response(ticket, description, List.of(), List.of(), tags, messageCount, attachmentCount,
                visibleMessageCount, visibleAttachmentCount);
    }

    private SupportTicketResponse response(SupportTicket ticket, String description, List<TicketMessageResponse> messages,
                                           List<AttachmentResponse> attachments, List<String> tags,
                                           int messageCount, int attachmentCount,
                                           int visibleMessageCount, int visibleAttachmentCount) {
        return new SupportTicketResponse(
                ticket.getTicketNumber(),
                ticket.getTitle(),
                description,
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
                ticket.getEscalationLevel(),
                ticket.getEscalatedAt(),
                ticket.getEscalationReason(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                messages,
                attachments,
                tags,
                messageCount,
                attachmentCount,
                visibleMessageCount,
                visibleAttachmentCount
        );
    }

    public AttachmentResponse toAttachmentResponse(TicketAttachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getInternal(),
                attachment.getUploadedBy(),
                attachment.getCreatedAt()
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

    public TagResponse toTagResponse(SupportTag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }

    public TicketEventResponse toEventResponse(SupportTicketEvent event) {
        return new TicketEventResponse(
                event.getId(),
                event.getEventType(),
                event.getActorType(),
                event.getActorId(),
                event.getDescription(),
                event.getCreatedAt()
        );
    }

    public RoutingRuleResponse toRoutingRuleResponse(SupportRoutingRule rule) {
        return new RoutingRuleResponse(
                rule.getId(),
                rule.getName(),
                rule.getActive(),
                rule.getCategory(),
                rule.getPriority(),
                rule.getOwnerType(),
                rule.getRelatedType(),
                rule.getAssignedTo(),
                rule.getTeamCode(),
                rule.getSortOrder(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
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
