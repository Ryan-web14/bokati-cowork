package com.sni.bokaticowork.features.payment.provider;

public record MobileMoneyInitiationResponse(
        String providerReference,
        String status,
        String message
) {
}
