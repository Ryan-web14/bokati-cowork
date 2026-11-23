package com.sni.bokaticowork.features.ressource.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CreateResourcePricingRuleRequest {

    @NotBlank
    private String resourceCode;

    @NotBlank
    private String bookingUnit;

    /** Prix d'une regle de base ; facultatif et ignore pour un ajustement (23/11/2026). */
    private Integer price;

    private String label;
    private Integer dayOfWeek;
    private LocalTime startsAt;
    private LocalTime endsAt;
    private String adjustmentType;
    private Integer adjustmentValue;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private Integer lastMinuteMinutes;
    private Integer priority;
    /** Lot R1 : jours d'application (1 = lundi ... 7 = dimanche), par exemple « 6,7 » ; remplace dayOfWeek. */
    private String daysOfWeek;
    /** PUBLIC, MEMBER, SUBSCRIBER, COMPANY, PARTNER ; vide = tous. */
    private String customerSegment;
    private String planCode;
    private Integer minParticipants;
    private Integer maxParticipants;
    private Integer minDurationMinutes;
    private Integer maxDurationMinutes;
    private Integer minLeadMinutes;
    /** PORTAL ou TEAM ; vide = tous. */
    private String channel;

    /** Lot R5a : ONLY (jours feries seulement), EXCLUDED (hors jours feries) ou vide. */
    private String holidayMode;

    /** Lot B10 : clients vises (prix au cas par cas) ; vide = tous. */
    private java.util.List<com.sni.bokaticowork.features.ressource.dto.request.AudienceRef> audience;
    private Boolean stackable;
    private Boolean perParticipant;

    private Boolean active;
}
