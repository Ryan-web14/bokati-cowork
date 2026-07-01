package com.sni.bokaticowork.features.payment.provider.pawaypay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PawapayDepositRequest(
        String depositId,
        Payer payer,
        String amount,
        String currency,
        String preAuthorisationCode,
        String callbackUrl,
        String clientReferenceId,
        String customerMessage,
        List<Map<String, Object>> metadata
) {
    public record Payer(String type, AccountDetails accountDetails) {}
    public record AccountDetails(String phoneNumber, String provider) {}
}
