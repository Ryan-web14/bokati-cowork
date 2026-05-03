package com.sni.bokaticowork.features.visitor.dto;

import com.sni.bokaticowork.features.visitor.enums.VisitorPassStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class VisitorDtos {
    public record CreateVisitorPassRequest(
            @NotBlank String fullName,
            String email,
            String phone,
            String company,
            String hostMemberCode,
            String hostName,
            @NotBlank String validFrom,
            @NotBlank String validUntil,
            String purpose,
            String createdBy
    ) {}

    public record VisitorPassResponse(
            String passNumber,
            String fullName,
            String email,
            String phone,
            String company,
            String hostMemberCode,
            String hostName,
            Instant validFrom,
            Instant validUntil,
            String purpose,
            VisitorPassStatus status,
            String qrValue
    ) {}

    public record CheckInRequest(String agent, String notes) {}

    public record VisitorLogResponse(
            String passNumber,
            String visitorName,
            String hostName,
            Instant checkedInAt,
            Instant checkedOutAt,
            String checkInAgent,
            String checkOutAgent,
            String notes
    ) {}
}
