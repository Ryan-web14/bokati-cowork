package com.sni.bokaticowork.features.booking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingSuggestionResponse(
        String resourceCode,
        String resourceName,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer remainingCapacity,
        BigDecimal estimatedAmount,
        String reason
) {
}
