package com.sni.bokaticowork.features.crm.dto;

import com.sni.bokaticowork.features.crm.enums.ProposalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ProposalDtos {

    // ── Requests ─────────────────────────────────────────────────

    public record CreateProposalRequest(
            @NotBlank String title,
            Long opportunityId,
            Long leadId,
            String recipientType,
            String recipientCode,
            String recipientName,
            String recipientEmail,
            LocalDate validUntil,
            String currency,
            String notes,
            String internalNotes,
            Long assignedTo
    ) {}

    public record UpdateProposalRequest(
            String title,
            String recipientName,
            String recipientEmail,
            LocalDate validUntil,
            String notes,
            String internalNotes,
            Long assignedTo
    ) {}

    public record AddProposalLineRequest(
            @NotBlank String label,
            String description,
            String serviceType,
            String serviceRefCode,
            BigDecimal quantity,
            @NotNull BigDecimal unitPrice,
            BigDecimal discountPercent,
            BigDecimal taxRate,
            Boolean taxIncluded,
            Integer sortOrder,
            String currency
    ) {}

    public record UpdateProposalLineRequest(
            String label,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountPercent,
            BigDecimal taxRate,
            Boolean taxIncluded,
            Integer sortOrder
    ) {}

    public record RejectProposalRequest(
            String reason
    ) {}

    // ── Responses ────────────────────────────────────────────────

    public record ProposalResponse(
            Long id,
            String proposalNumber,
            String title,
            Long opportunityId,
            Long leadId,
            String recipientType,
            String recipientCode,
            String recipientName,
            String recipientEmail,
            ProposalStatus status,
            LocalDate validUntil,
            BigDecimal subtotalAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String currency,
            String notes,
            String internalNotes,
            String convertedInvoiceNumber,
            String convertedContractCode,
            String convertedSubscriptionNumber,
            String convertedPassNumber,
            Instant sentAt,
            Instant viewedAt,
            Instant acceptedAt,
            Instant rejectedAt,
            String rejectionReason,
            Long createdBy,
            Long assignedTo,
            Instant createdAt,
            Instant updatedAt,
            List<ProposalLineResponse> lines
    ) {}

    public record ProposalLineResponse(
            Long id,
            Integer sortOrder,
            String serviceType,
            String serviceRefCode,
            String label,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountPercent,
            BigDecimal taxRate,
            Boolean taxIncluded,
            BigDecimal subtotalAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String currency
    ) {}
}
