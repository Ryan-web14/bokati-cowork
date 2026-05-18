package com.sni.bokaticowork.features.billing.dto.response;

import com.sni.bokaticowork.features.billing.enums.BillingSignatureStatus;

import java.time.Instant;

public record BillingDocumentSignatureResponse(
        String signerName,
        String signerEmail,
        BillingSignatureStatus status,
        Instant signedAt,
        Instant expiresAt
) {
}
