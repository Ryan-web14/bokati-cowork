package com.sni.bokaticowork.features.payment.provider;

public record MobileMoneyStatusResponse(
        String providerReference,
        String status,
        String message
) {
}
