package com.sni.bokaticowork.features.crm.dto;

import com.sni.bokaticowork.features.crm.enums.LeadActivityType;
import com.sni.bokaticowork.features.crm.enums.LeadStage;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class CrmDtos {
    public record CreateLeadRequest(
            @NotBlank String fullName,
            String email,
            String phone,
            String company,
            String source,
            String interest,
            BigDecimal estimatedAmount,
            Integer probability,
            LocalDate expectedCloseDate,
            Long assignedTo
    ) {}

    public record UpdateLeadStageRequest(LeadStage stage, String lostReason) {}

    public record ConvertLeadRequest(String ownerType, String ownerCode) {}

    public record AddLeadActivityRequest(
            LeadActivityType activityType,
            String subject,
            String notes,
            String performedBy,
            Instant performedAt
    ) {}

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
            String fullName,
            String email,
            String phone,
            String company,
            String source,
            String interest,
            LeadStage stage,
            BigDecimal estimatedAmount,
            Integer probability,
            LocalDate expectedCloseDate,
            Long assignedTo,
            String convertedOwnerType,
            String convertedOwnerCode,
            String lostReason,
            Instant createdAt,
            List<LeadActivityResponse> activities
    ) {}

    public record PipelineResponse(Map<LeadStage, List<LeadResponse>> stages) {}

    public record CrmMetricsResponse(long totalLeads, long wonLeads, long lostLeads, BigDecimal pipelineValue) {}
}
