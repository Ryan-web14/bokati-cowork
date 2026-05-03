package com.sni.bokaticowork.features.payment.provider.pawaypay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PawapayDepositStatusResponse(
        String depositId,
        String status,          // CREATED, ACCEPTED, APPROVED, COMPLETED, FAILED
        String amount,
        String currency,
        String correspondent,
        JsonNode correspondentIds,
        JsonNode failureReason,
        String created,
        String respondedByPayer
) {}