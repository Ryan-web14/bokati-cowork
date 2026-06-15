package com.sni.bokaticowork.features.portal.booking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientCheckAvailabilityRequest {

    @NotBlank
    private String resourceCode;

    @NotNull
    private LocalDateTime startedAt;

    @NotNull
    private LocalDateTime endedAt;

    @Min(1)
    private Integer quantity = 1;
}
