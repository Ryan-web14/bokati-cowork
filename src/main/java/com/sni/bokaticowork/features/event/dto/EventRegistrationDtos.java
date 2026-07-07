package com.sni.bokaticowork.features.event.dto;

import com.sni.bokaticowork.features.event.enums.RegistrationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public class EventRegistrationDtos {

    // ── Requêtes ──────────────────────────────────────────────────

    public record PublicEventRegistrationRequest(
            @NotBlank @Size(max = 180) String firstname,
            @NotBlank @Size(max = 180) String lastname,
            @NotBlank @Size(max = 30) String phone,
            @Size(max = 30) String whatsappPhone,
            @NotBlank @Email @Size(max = 250) String email
    ) {}

    public record RejectEventRegistrationRequest(
            @Size(max = 255) String reason
    ) {}

    // ── Réponses ──────────────────────────────────────────────────

    public record EventSummaryResponse(
            String code,
            String name,
            String description,
            LocalDate eventDate,
            boolean active
    ) {}

    public record EventRegistrationResponse(
            Long id,
            String registrationNumber,
            String eventCode,
            String eventName,
            String firstname,
            String lastname,
            String fullName,
            String email,
            String phone,
            String whatsappPhone,
            RegistrationStatus status,
            String rejectionReason,
            String memberId,
            String customerId,
            Instant createdAt,
            Instant validatedAt
    ) {}
}
