package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.CashRequestStatus;
import com.sni.bokaticowork.features.payment.enums.CashRequestType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CashRequestResponse(
        String requestNumber,
        String sessionNumber,
        String registerCode,
        CashRequestType requestType,
        CashRequestStatus status,
        BigDecimal amount,
        String currency,
        String reason,
        String requestedBy,
        Instant requestedAt,
        String reviewedBy,
        Instant reviewedAt,
        String reviewNote,
        String executedMovementNumber,
        Instant executedAt,
        List<AttachmentRef> attachments
) {
    public record AttachmentRef(String fileName, String contentType, String storagePath, String label) {
    }
}
