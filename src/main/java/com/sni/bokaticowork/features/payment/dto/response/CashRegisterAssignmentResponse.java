package com.sni.bokaticowork.features.payment.dto.response;

import java.time.Instant;

/** Une affectation, avec l'indication qu'elle est en vigueur maintenant. */
public record CashRegisterAssignmentResponse(
        Long id,
        String registerCode,
        String userEmail,
        com.sni.bokaticowork.features.payment.enums.CashAssignmentRole role,
        com.sni.bokaticowork.features.payment.enums.CashAssignmentQuality quality,
        Integer backupRank,
        Instant validFrom,
        Instant validTo,
        Boolean emergency,
        String source,
        String sourceReference,
        String grantedBy,
        String note,
        Instant revokedAt,
        String revokedBy,
        Boolean effectiveNow
) {
}
