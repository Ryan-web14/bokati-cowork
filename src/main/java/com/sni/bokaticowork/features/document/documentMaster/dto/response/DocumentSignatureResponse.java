package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSignatureStatus;

import java.time.Instant;

public record DocumentSignatureResponse(
        Long id,
        String documentCode,
        String signerType,
        Long signerId,
        String signerName,
        String signerEmail,
        DocumentSignatureStatus signatureStatus,
        Instant signedAt,
        String ipAddress,
        String userAgent
) {
}
