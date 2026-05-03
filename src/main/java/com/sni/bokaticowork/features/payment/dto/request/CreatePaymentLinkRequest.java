package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.Min;

public record CreatePaymentLinkRequest(
        @Min(5) Integer expiresInMinutes
) {
}
