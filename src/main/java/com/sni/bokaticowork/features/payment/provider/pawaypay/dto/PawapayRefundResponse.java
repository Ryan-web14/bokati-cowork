package com.sni.bokaticowork.features.payment.provider.pawaypay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PawapayRefundResponse(
        String refundId,
        String depositId,
        String status,          // ACCEPTED or REJECTED
        String created,
        JsonNode rejectionReason
) {}