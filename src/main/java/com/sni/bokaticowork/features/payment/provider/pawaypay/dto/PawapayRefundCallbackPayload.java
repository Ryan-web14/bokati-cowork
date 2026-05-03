package com.sni.bokaticowork.features.payment.provider.pawaypay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PawapayRefundCallbackPayload(
        String refundId,
        String depositId,
        String status,          // COMPLETED or FAILED
        String amount,
        String currency,
        String created,
        String respondedByPayer,
        JsonNode failureReason
) {}