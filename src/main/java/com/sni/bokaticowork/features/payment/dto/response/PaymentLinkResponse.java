package com.sni.bokaticowork.features.payment.dto.response;

import java.time.Instant;

public record PaymentLinkResponse(
        String intentNumber,
        String checkoutUrl,
        String token,
        Instant expiresAt,
        String emailedTo
) {
}
