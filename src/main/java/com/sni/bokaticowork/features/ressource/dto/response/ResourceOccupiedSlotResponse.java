package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ResourceOccupiedSlotResponse(
        Long id,
        String resourceCode,
        String resourceName,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        int slotDurationMinutes,
        int totalCapacity,
        int remainingCapacity,
        int usedCapacity,
        double occupancyRate,
        String occupancyLevel,
        boolean active
) {
}
