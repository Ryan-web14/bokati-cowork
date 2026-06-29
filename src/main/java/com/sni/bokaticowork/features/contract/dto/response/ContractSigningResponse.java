package com.sni.bokaticowork.features.contract.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ContractSigningResponse(
        UUID token,
        String contractCode,
        String contractTitle,
        String signerEmail,
        String signerName,
        Instant expiresAt,
        Instant signedAt,
        boolean revoked
) {}
