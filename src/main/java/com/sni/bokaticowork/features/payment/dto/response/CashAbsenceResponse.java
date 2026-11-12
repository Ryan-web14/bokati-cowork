package com.sni.bokaticowork.features.payment.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Absence de caissier, avec le remplacant et les caisses concernees. */
public record CashAbsenceResponse(
        String absenceNumber,
        String userEmail,
        String kindCode,
        String kindLabel,
        String reason,
        LocalDate startsOn,
        LocalDate endsOn,
        String replacementUser,
        List<String> registerCodes,
        com.sni.bokaticowork.features.payment.enums.CashNoReplacementPolicy noReplacementPolicy,
        com.sni.bokaticowork.features.payment.enums.CashAbsenceStatus status,
        Boolean unplanned,
        String declaredBy,
        String approvedBy,
        Instant approvedAt,
        Boolean forced,
        String forcedNote,
        List<String> warnings
) {
}
