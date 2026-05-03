package com.sni.bokaticowork.features.ressource.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
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
    private Boolean active;
}
