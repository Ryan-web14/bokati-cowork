package com.sni.bokaticowork.features.support.dto;

import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.enums.TicketPriority;
import com.sni.bokaticowork.features.support.enums.TicketSenderType;
import com.sni.bokaticowork.features.support.enums.TicketStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class SupportDtos {
    public record CreateTicketRequest(
            @NotBlank String title,
            String description,
            TicketPriority priority,
            TicketCategory category,
            String ownerType,
            String ownerCode,
            String contactName,
            String contactEmail,
            String contactPhone,
            String relatedType,
            String relatedCode
    ) {}

    public record AddTicketMessageRequest(
            TicketSenderType senderType,
            String senderId,
            String senderName,
            @NotBlank String message,
            Boolean internal
    ) {}

    public record AssignTicketRequest(Long assignedTo) {}
    public record UpdateTicketStatusRequest(TicketStatus status) {}

    public record TicketMessageResponse(
            Long id,
            TicketSenderType senderType,
            String senderId,
            String senderName,
            String message,
            Boolean internal,
            Instant createdAt
    ) {}

    public record SupportTicketResponse(
            String ticketNumber,
            String title,
            String description,
            TicketStatus status,
            TicketPriority priority,
            TicketCategory category,
            String ownerType,
            String ownerCode,
            String contactName,
            String contactEmail,
            String contactPhone,
            Long assignedTo,
            String relatedType,
            String relatedCode,
            Instant firstResponseDueAt,
            Instant resolutionDueAt,
            Instant firstRespondedAt,
            Instant resolvedAt,
            Instant closedAt,
            Instant createdAt,
            Instant updatedAt,
            List<TicketMessageResponse> messages
    ) {}

    public record SupportMetricsResponse(
            long openTickets,
            long overdueFirstResponse,
            long overdueResolution,
            long resolvedTickets
    ) {}
}
