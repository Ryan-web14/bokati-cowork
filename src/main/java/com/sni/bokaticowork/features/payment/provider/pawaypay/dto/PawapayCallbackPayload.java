package com.sni.bokaticowork.features.payment.provider.pawaypay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PawapayCallbackPayload(
        String depositId,
        String status,              // COMPLETED or FAILED
        String amount,
        String currency,
        String clientReferenceId,
        String country,
        String correspondent,
        String providerTransactionId,
        JsonNode metadata,
        JsonNode payer,
        String customerTimestamp,
        String statementDescription,
        String created,
        String respondedByPayer,
        JsonNode correspondentIds,
        JsonNode failureReason
) {}
