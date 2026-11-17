package com.sni.bokaticowork.features.payment.cash.dto.response;

import com.sni.bokaticowork.features.payment.cash.enums.CashDeclarationStatus;
import com.sni.bokaticowork.features.payment.cash.model.CashPaymentDeclaration;

import java.math.BigDecimal;
import java.time.Instant;

/** Une annonce d encaissement en especes, telle que la voient le client et la caisse. */
public record CashPaymentDeclarationResponse(
        String declarationNumber,
        String documentNumber,
        String bookingNumber,
        String customerType,
        String customerCode,
        String customerName,
        BigDecimal amount,
        String currency,
        CashDeclarationStatus status,
        String note,
        Instant declaredAt,
        String declaredBy,
        Instant expiresAt,
        Instant confirmedAt,
        String confirmedBy,
        BigDecimal confirmedAmount,
        String cashSessionNumber,
        String transactionNumber,
        Instant closedAt,
        String closedBy,
        /** Motif d'une annulation ou d'une expiration ; nul pour une annonce confirmee. */
        String closeReason,
        /** Note de la caisse a la confirmation ; nulle sinon. */
        String confirmationNote
) {
    public static CashPaymentDeclarationResponse of(CashPaymentDeclaration declaration) {
        return new CashPaymentDeclarationResponse(
                declaration.getDeclarationNumber(),
                declaration.getDocumentNumber(),
                declaration.getBookingNumber(),
                declaration.getCustomerType(),
                declaration.getCustomerCode(),
                declaration.getCustomerName(),
                declaration.getAmount(),
                declaration.getCurrency(),
                declaration.getStatus(),
                declaration.getNote(),
                declaration.getDeclaredAt(),
                declaration.getDeclaredBy(),
                declaration.getExpiresAt(),
                declaration.getConfirmedAt(),
                declaration.getConfirmedBy(),
                declaration.getConfirmedAmount(),
                declaration.getCashSessionNumber(),
                declaration.getTransactionNumber(),
                declaration.getClosedAt(),
                declaration.getClosedBy(),
                declaration.getStatus() == CashDeclarationStatus.CONFIRMED ? null : declaration.getCloseReason(),
                declaration.getStatus() == CashDeclarationStatus.CONFIRMED ? declaration.getCloseReason() : null);
    }
}
