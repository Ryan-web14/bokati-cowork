package com.sni.bokaticowork.features.ressource.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CreateResourceClosureRequest {

    @NotBlank
    private String resourceCode;

    @NotNull
    private LocalDateTime startedAt;

    @NotNull
    private LocalDateTime endedAt;

    private String reason;

    /** Lot R5a : CLOSURE (defaut), MAINTENANCE, CLEANING, PRIVATE_EVENT ou OTHER. */
    private String kind;

    /** Lot R5a : NONE (defaut), WEEKLY ou YEARLY. */
    private String recurrence;

    /** Lot R5a : derniere date de la repetition (vide : sans fin). */
    private java.time.LocalDate recurrenceUntil;
    private Boolean active;

    /** Lot R2 (D4) : vrai pour annuler les reservations de la plage et prevenir les clients ; sinon refus s'il y en a. */
    private Boolean cancelBookings;
}
