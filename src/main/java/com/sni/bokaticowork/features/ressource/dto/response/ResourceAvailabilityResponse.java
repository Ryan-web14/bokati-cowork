package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourceAvailabilityResponse {

    private Long id;
    private String resourceCode;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer slotDurationMinutes;
    private Integer totalCapacity;
    private Integer remainingCapacity;
    private Boolean available;
    private Boolean active;
}
