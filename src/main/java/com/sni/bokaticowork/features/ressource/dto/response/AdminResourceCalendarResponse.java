package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AdminResourceCalendarResponse(
        String resourceCode,
        String resourceName,
        LocalDate fromDate,
        LocalDate toDate,
        List<Day> days
) {
    @Builder
    public record Day(
            LocalDate date,
            int totalSlots,
            int availableSlots,
            int occupiedSlots,
            int totalCapacity,
            int remainingCapacity,
            int usedCapacity,
            double occupancyRate,
            String occupancyLevel,
            String status,
            List<Window> windows
    ) {
    }

    @Builder
    public record Window(
            Long id,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            int slotDurationMinutes,
            int totalCapacity,
            int remainingCapacity,
            int usedCapacity,
            double occupancyRate,
            String occupancyLevel,
            boolean available,
            boolean active
    ) {
    }
}
