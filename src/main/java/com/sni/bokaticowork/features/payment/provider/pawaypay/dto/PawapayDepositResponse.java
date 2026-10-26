package com.sni.bokaticowork.features.payment.provider.pawaypay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PawapayDepositResponse(
        String depositId,
        String status,          // ACCEPTED or REJECTED
        String created,
        // v2 renvoie failureReason, v1 rejectionReason · sans l alias, le motif d un refus v2 etait perdu.
        @com.fasterxml.jackson.annotation.JsonAlias("failureReason") JsonNode rejectionReason
) {}