package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/** Scelle (PV PVS) et son ouverture. */
public record CashSealResponse(
        String sealNumber,
        String registerCode,
        String envelopeNumber,
        BigDecimal amount,
        String countNumber,
        String sealedBy,
        String witness,
        String depositedIn,
        Instant sealedAt,
        Instant unsealedAt,
        String unsealedBy,
        String unsealWitness,
        String unsealCount,
        BigDecimal unsealAmount,
        BigDecimal difference,
        String absenceNumber
) {
}
