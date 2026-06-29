package com.sni.bokaticowork.features.subscription.subscription.dto;

import com.sni.bokaticowork.features.subscription.subscription.enums.PassDurationUnit;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.TargetAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class PassPlanDtos {

    private PassPlanDtos() {}

    // ── Requests ──

    public record CreatePassPlanRequest(
            @NotBlank String name,
            String description,
            @NotNull PassType passType,
            @NotNull TargetAudience targetAudience,
            Boolean visible,
            Integer sortOrder,
            Integer requiredKycLevel
    ) {}

    public record UpdatePassPlanRequest(
            String name,
            String description,
            Boolean visible,
            Integer sortOrder,
            Integer requiredKycLevel
    ) {}

    public record CreatePassPlanVersionRequest(
            @NotBlank String name,
            String description,
            @NotNull Integer duration,
            @NotNull PassDurationUnit durationUnit,
            Integer maxUses,
            Boolean autoRenewable,
            Integer requiredKycLevel,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String termsJson
    ) {}

    public record SetPassPlanPriceRequest(
            @NotBlank String currency,
            @NotNull BigDecimal amount,
            BigDecimal setupFee,
            BigDecimal depositAmount,
            Boolean taxIncluded,
            String taxCode
    ) {}

    public record AddPassPlanEntitlementRequest(
            @NotBlank String entitlementCode,
            BigDecimal quantity,
            Boolean unlimited,
            Boolean rolloverAllowed,
            BigDecimal rolloverLimit,
            Integer validForDays,
            Integer priority
    ) {}

    // ── Responses ──

    public record PassPlanResponse(
            Long id,
            String code,
            String name,
            String description,
            PassType passType,
            TargetAudience targetAudience,
            PlanStatus status,
            Boolean visible,
            Integer sortOrder,
            Integer requiredKycLevel,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record PassPlanVersionResponse(
            Long id,
            String planCode,
            Integer versionNumber,
            String name,
            String description,
            PlanStatus status,
            Integer duration,
            PassDurationUnit durationUnit,
            Integer maxUses,
            Boolean autoRenewable,
            Integer requiredKycLevel,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record PassPlanPriceResponse(
            Long id,
            Long versionId,
            String currency,
            BigDecimal amount,
            BigDecimal setupFee,
            BigDecimal depositAmount,
            Boolean taxIncluded,
            String taxCode,
            Instant createdAt
    ) {}

    public record PassPlanEntitlementResponse(
            Long id,
            Long versionId,
            String entitlementCode,
            String entitlementName,
            BigDecimal quantity,
            Boolean unlimited,
            Boolean rolloverAllowed,
            BigDecimal rolloverLimit,
            Integer validForDays,
            Integer priority,
            Instant createdAt
    ) {}
}
