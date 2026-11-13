package com.sni.bokaticowork.features.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Lot C7 : requetes et reponses des depots de garantie. */
public final class SecurityDepositDtos {

    private SecurityDepositDtos() {
    }

    /**
     * Mouvement : {@code kind} RELEASE (restitution), RETAIN (retenue, motif requis) ou APPLY
     * (imputation sur {@code targetDocumentNumber}). Sans montant, tout le reste de la caution.
     */
    public record MovementRequest(@NotBlank String kind, BigDecimal amount, @Size(max = 500) String reason, String targetDocumentNumber) {
    }

    public record FlagRequest(@NotNull Boolean securityDeposit) {
    }

    /**
     * Decision CD10 : caution facturee avant le lot C7, a reclasser par le comptable ({@code posted} :
     * deja comptabilisee). {@code taxAmount} : TVA et centimes extraits a tort.
     */
    public record LegacyLine(String documentNumber, String customerCode, String customerName, LocalDate issueDate, String description,
                             BigDecimal totalAmount, BigDecimal netAmount, BigDecimal taxAmount, boolean posted) {
    }

    public record MovementView(String movementNumber, String kind, BigDecimal amount, String reason, String targetDocumentNumber,
                               LocalDate movementDate, String createdBy, Instant createdAt) {
    }

    public record DepositView(String depositNumber, String documentNumber, String subscriptionNumber, String customerType, String customerCode,
                              String customerName, BigDecimal amount, BigDecimal releasedAmount, BigDecimal retainedAmount,
                              BigDecimal appliedAmount, BigDecimal remaining, String currency, String status, Instant createdAt,
                              List<MovementView> movements) {
    }
}
