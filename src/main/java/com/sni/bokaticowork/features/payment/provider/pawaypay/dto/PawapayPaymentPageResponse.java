package com.sni.bokaticowork.features.payment.provider.pawaypay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PawapayPaymentPageResponse(
        String redirectUrl
) {
}
