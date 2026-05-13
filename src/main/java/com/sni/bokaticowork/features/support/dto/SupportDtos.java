package com.sni.bokaticowork.features.support.dto;

import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.enums.TicketPriority;
import com.sni.bokaticowork.features.support.enums.TicketSenderType;
import com.sni.bokaticowork.features.support.enums.TicketStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class SupportDtos {

    // ── Requêtes ──────────────────────────────────────────────────

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
            @NotBlank String content,
            Boolean internal
    ) {}

    public record AssignTicketRequest(String assignedTo) {}

    public record UpdateTicketStatusRequest(TicketStatus status) {}

    public record SubmitCsatRequest(
            @NotNull @Min(1) @Max(5) Integer score,
            String comment
    ) {}

    public record QuickReplyRequest(
            @NotNull TicketCategory category,
            @NotBlank String title,
            @NotBlank String body
    ) {}

    // ── Réponses ─────────────────────────────────────────────────

    public record TicketMessageResponse(
            Long id,
            TicketSenderType senderType,
            String senderId,
            String senderName,
            String content,
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
            Integer csatScore,
            String csatComment,
            Instant csatSubmittedAt,
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

    public record QuickReplyResponse(
            Long id,
            TicketCategory category,
            String title,
            String body,
            Boolean active
    ) {}

    public record SupportAnalyticsResponse(
            Instant from,
            Instant to,
            long totalCreated,
            long totalResolved,
            long openAtEndOfPeriod,
            long slaBreachCount,
            double avgFirstResponseHours,
            double avgResolutionHours,
            Map<String, Long> byCategory,
            Map<String, Long> byPriority,
            double avgCsatScore,
            long csatResponseCount,
            List<AgentWorkloadEntry> agentWorkload
    ) {}

    public record AgentWorkloadEntry(
            Long agentId,
            long assigned,
            long resolved
    ) {}
}
