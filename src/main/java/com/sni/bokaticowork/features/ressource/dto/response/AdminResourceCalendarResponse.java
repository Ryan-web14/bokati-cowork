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
        List<Day> days,
        /** Lot R5a : fermetures et blocages de la plage (occurrences des repetitions comprises). */
        List<Block> blocks
) {
    /** Lot R5a : un blocage du calendrier. */
    @Builder
    public record Block(Long closureId, String kind, String reason, LocalDateTime startedAt, LocalDateTime endedAt,
                        String recurrence) {
    }

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
            List<Window> windows,
            /** Lot R5a : jour ferie, horaires exceptionnels ou fermeture du jour. */
            String dayLabel
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
            boolean active,
            /** Soiree reservee aux abonnes · l'accueil peut la reserver pour n'importe qui. */
            boolean subscribersOnly
    ) {
    }
}
