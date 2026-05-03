package com.sni.bokaticowork.features.payment.dto.response;

public record MobileMoneyProviderOptionResponse(
        String code,
        String displayName,
        String countryCode,
        String currency
) {
}
