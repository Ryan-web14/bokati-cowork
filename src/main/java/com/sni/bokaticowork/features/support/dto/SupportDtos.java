package com.sni.bokaticowork.features.support.dto;

import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.enums.TicketEventType;
import com.sni.bokaticowork.features.support.enums.TicketPriority;
import com.sni.bokaticowork.features.support.enums.TicketSenderType;
import com.sni.bokaticowork.features.support.enums.TicketStatus;
import com.sni.bokaticowork.features.task.dto.TaskDtos.ChecklistRequest;
import com.sni.bokaticowork.features.task.enums.TaskPriority;
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

    public record AddTagRequest(@NotBlank String tag) {}

    public record RoutingRuleRequest(
            @NotBlank String name,
            Boolean active,
            TicketCategory category,
            TicketPriority priority,
            String ownerType,
            String relatedType,
            Long assignedTo,
            String teamCode,
            Integer sortOrder
    ) {}

    public record CreateTicketTaskRequest(
            @NotBlank String title,
            String description,
            Long assignedTo,
            TaskPriority priority,
            Instant dueAt,
            List<ChecklistRequest> checklist
    ) {}

    public record QuickReplyRequest(
            @NotNull TicketCategory category,
            @NotBlank String title,
            @NotBlank String body
    ) {}

    // ── Réponses ─────────────────────────────────────────────────

    public record AttachmentResponse(
            Long id,
            String fileName,
            String contentType,
            Long fileSize,
            Boolean internal,
            Long uploadedBy,
            Instant createdAt
    ) {}

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
            Integer escalationLevel,
            Instant escalatedAt,
            String escalationReason,
            Instant createdAt,
            Instant updatedAt,
            List<TicketMessageResponse> messages,
            List<AttachmentResponse> attachments,
            List<String> tags
    ) {}

    public record SupportMetricsResponse(
            long openTickets,
            long overdueFirstResponse,
            long overdueResolution,
            long resolvedTickets
    ) {}

    public record TagResponse(
            Long id,
            String name
    ) {}

    public record TicketEventResponse(
            Long id,
            TicketEventType eventType,
            String actorType,
            String actorId,
            String description,
            Instant createdAt
    ) {}

    public record RoutingRuleResponse(
            Long id,
            String name,
            Boolean active,
            TicketCategory category,
            TicketPriority priority,
            String ownerType,
            String relatedType,
            Long assignedTo,
            String teamCode,
            Integer sortOrder,
            Instant createdAt,
            Instant updatedAt
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
            double slaBreachRate,
            double avgFirstResponseHours,
            double avgResolutionHours,
            long reopenedCount,
            long waitingClientCount,
            Map<String, Long> byCategory,
            Map<String, Long> byPriority,
            Map<String, Long> topTags,
            Map<String, Long> volumeByRelatedType,
            Map<String, Long> backlogByAgent,
            double avgCsatScore,
            long csatResponseCount,
            Map<String, Double> csatByCategory,
            Map<String, Double> csatByAgent,
            List<AgentWorkloadEntry> agentWorkload
    ) {}

    public record AgentWorkloadEntry(
            Long agentId,
            long assigned,
            long resolved
    ) {}

    public record AgentPerformanceEntry(
            Long agentId,
            long assignedCount,
            long resolvedCount,
            double avgFirstResponseHours,
            double avgResolutionHours,
            Double avgCsatScore
    ) {}

    public record OwnerTicketSummaryResponse(
            String ownerType,
            String ownerCode,
            long openTickets,
            long slaBreaches,
            Double avgCsatScore,
            boolean atRisk,
            List<SupportTicketResponse> recentTickets
    ) {}
}
