package com.sni.bokaticowork.features.ressource.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CreateResourcePolicyRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Integer minBookingDurationMinutes;

    @NotNull
    private Integer maxBookingDurationMinutes;

    @NotNull
    private Integer minBookingNoticeMinutes;

    private Integer cancellationNoticeMinutes;
    private Boolean allowCancellation;
    private Boolean active;
}
