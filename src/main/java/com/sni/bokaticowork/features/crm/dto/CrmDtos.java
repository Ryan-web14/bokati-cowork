package com.sni.bokaticowork.features.crm.dto;

import com.sni.bokaticowork.features.crm.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class CrmDtos {

    // ── Requêtes Lead ─────────────────────────────────────────────

    public record CreateLeadRequest(
            @NotBlank String fullName,
            String email,
            String phone,
            String company,
            String note,
            LeadSource source,
            LeadInterest interest,
            BigDecimal estimatedAmount,
            Integer probability,
            LocalDate expectedCloseDate,
            Long assignedTo
    ) {}

    public record UpdateLeadRequest(
            String fullName,
            String email,
            String phone,
            String company,
            String note,
            LeadSource source,
            LeadInterest interest,
            BigDecimal estimatedAmount,
            Integer probability,
            LocalDate expectedCloseDate,
            Long assignedTo
    ) {}

    public record UpdateLeadStageRequest(LeadStage stage, String lostReason) {}

    public record ConvertLeadRequest(String ownerType, String ownerCode) {}

    public record AddLeadActivityRequest(
            @NotNull LeadActivityType activityType,
            String subject,
            String notes,
            String performedBy,
            Instant performedAt
    ) {}

    // ── Requêtes Opportunity ──────────────────────────────────────

    public record CreateOpportunityRequest(
            @NotBlank String title,
            BigDecimal estimatedAmount,
            Integer probability,
            LocalDate expectedCloseDate,
            Long assignedTo,
            String notes
    ) {}

    public record UpdateOpportunityStageRequest(
            @NotNull OpportunityStage stage,
            String lostReason
    ) {}

    public record GenerateQuoteRequest(
            @NotBlank String currency,
            String performedBy
    ) {}

    // ── Réponses Lead ─────────────────────────────────────────────

    public record LeadActivityResponse(
            Long id,
            LeadActivityType activityType,
            String subject,
            String notes,
            String performedBy,
            Instant performedAt
    ) {}

    public record LeadResponse(
            Long id,
            String leadNumber,
            String fullName,
            String email,
            String phone,
            String company,
            String note,
            LeadSource source,
            LeadInterest interest,
            LeadStage stage,
            BigDecimal estimatedAmount,
            Integer probability,
            Integer scoredProbability,
            LocalDate expectedCloseDate,
            Long assignedTo,
            String convertedOwnerType,
            String convertedOwnerCode,
            String lostReason,
            Instant lastActivityAt,
            Instant createdAt,
            Instant updatedAt,
            List<LeadActivityResponse> activities
    ) {}

    // ── Réponses Opportunity ──────────────────────────────────────

    public record OpportunityResponse(
            Long id,
            String opportunityNumber,
            Long leadId,
            String leadNumber,
            String leadFullName,
            String title,
            BigDecimal estimatedAmount,
            Integer probability,
            OpportunityStage stage,
            LocalDate expectedCloseDate,
            Long assignedTo,
            String notes,
            Instant wonAt,
            Instant lostAt,
            String lostReason,
            Instant createdAt,
            Instant updatedAt
    ) {}

    // ── Pipeline + Métriques ──────────────────────────────────────

    public record PipelineResponse(Map<LeadStage, List<LeadResponse>> stages) {}

    public record CrmMetricsResponse(
            long totalLeads,
            long newLeads,
            long wonLeads,
            long lostLeads,
            long activeLeads,
            BigDecimal pipelineValue,
            double conversionRate
    ) {}

    // ── Analytics ─────────────────────────────────────────────────

    public record CrmAnalyticsResponse(
            Instant from,
            Instant to,
            long totalCreated,
            long totalWon,
            long totalLost,
            double conversionRate,
            double avgDaysToWin,
            Map<String, Long> bySource,
            Map<String, Long> byStage,
            BigDecimal pipelineValue
    ) {}
}
