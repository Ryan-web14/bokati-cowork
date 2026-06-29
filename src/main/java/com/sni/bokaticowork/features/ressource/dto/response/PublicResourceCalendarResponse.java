package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PublicResourceCalendarResponse(
        String resourceCode,
        String resourceName,
        LocalDate fromDate,
        LocalDate toDate,
        List<Day> days
) {
    @Builder
    public record Day(
            LocalDate date,
            Integer totalSlots,
            Integer availableSlots,
            Integer totalCapacity,
            Integer remainingCapacity,
            String status,
            List<Window> windows
    ) {
    }

    @Builder
    public record Window(
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            Integer slotDurationMinutes,
            Integer totalCapacity,
            Integer remainingCapacity,
            Boolean available,
            Boolean active
    ) {
    }
}
