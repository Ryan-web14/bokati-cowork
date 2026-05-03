package com.sni.bokaticowork.features.payment.provider.pawaypay.dto;

public record PawapayRefundRequest(
        String refundId,
        String depositId,
        String amount,
        String currency
) {}
