package com.sni.bokaticowork.features.payment.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CashDelegationResponse(
        Long id,
        String delegator,
        String delegate,
        List<String> permissions,
        LocalDate startsOn,
        LocalDate endsOn,
        String note,
        Boolean activeNow,
        Instant cancelledAt
) {
}
