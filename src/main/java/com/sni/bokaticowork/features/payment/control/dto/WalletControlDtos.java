package com.sni.bokaticowork.features.payment.control.dto;

import com.sni.bokaticowork.features.payment.control.model.WalletAdminAction;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Les contrats du centre de controle · petits, et ensemble. */
public final class WalletControlDtos {

    private WalletControlDtos() {
    }

    public record AdminActionRequest(
            @NotNull(message = "Le type d'action est requis") WalletAdminAction.Type type,
            BigDecimal amount,
            String reference,
            @NotBlank(message = "Un motif est requis pour toute action administrative") String reason,
            Instant until
    ) {
    }

    public record RejectRequest(@NotBlank(message = "Un motif de refus est requis") String reason) {
    }

    public record FlagReviewRequest(boolean holdWallet) {
    }

    public record FlagResolveRequest(boolean confirmed,
                                     @NotBlank(message = "Une décision motivée est requise") String resolution) {
    }

    public record TreasuryRequest(
            @NotNull(message = "La date est requise") LocalDate date,
            String currency,
            BigDecimal ledgerAccountBalance,
            @NotNull(message = "La trésorerie disponible est requise") BigDecimal availableCash,
            String explanation
    ) {
    }

    public record AdminActionView(String actionNumber, String walletNumber, WalletAdminAction.Type type,
                                  BigDecimal amount, String currency, String reference, String reason,
                                  WalletAdminAction.Status status, String requestedBy, String approvedBy,
                                  Instant approvedAt, String rejectedBy, String rejectionReason,
                                  Instant executedAt, String resultReference, Instant createdAt) {
        public static AdminActionView of(WalletAdminAction action) {
            return new AdminActionView(action.getActionNumber(), action.getWallet().getWalletNumber(),
                    action.getActionType(), action.getAmount(), action.getCurrency(), action.getReference(),
                    action.getReason(), action.getStatus(), action.getRequestedBy(), action.getApprovedBy(),
                    action.getApprovedAt(), action.getRejectedBy(), action.getRejectionReason(),
                    action.getExecutedAt(), action.getResultReference(), action.getCreatedAt());
        }
    }

    public record FlagView(String flagNumber, String walletNumber, WalletRiskFlag.Type type,
                           WalletRiskFlag.Severity severity, WalletRiskFlag.Status status, String details,
                           String reference, Instant detectedAt, String detectedBy, String reviewedBy,
                           Instant reviewedAt, String resolution) {
        public static FlagView of(WalletRiskFlag flag) {
            return new FlagView(flag.getFlagNumber(), flag.getWallet().getWalletNumber(), flag.getFlagType(),
                    flag.getSeverity(), flag.getStatus(), flag.getDetails(), flag.getReference(),
                    flag.getDetectedAt(), flag.getDetectedBy(), flag.getReviewedBy(), flag.getReviewedAt(),
                    flag.getResolution());
        }
    }
}
