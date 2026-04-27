package com.sni.bokaticowork.features.payment.provider;

public record MobileMoneyRefundResponse(
        String providerReference,
        String refundReference,
        String status,
        String message
) {
}
