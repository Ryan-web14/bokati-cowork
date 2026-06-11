package com.sni.bokaticowork.features.payment.dto.request;

import com.sni.bokaticowork.features.payment.enums.CashRequestType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record CreateCashRequestRequest(
        @NotNull CashRequestType requestType,
        @NotNull @Positive BigDecimal amount,
        String currency,
        String reason,
        String requestedBy,
        List<CashRequestAttachmentInput> attachments
) {
    public record CashRequestAttachmentInput(
            String fileName,
            String contentType,
            String storagePath,
            String label
    ) {
    }
}
