package com.sni.bokaticowork.features.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Lot C6 : requetes et reponses des retenues a la source. */
public final class WithholdingDtos {

    private WithholdingDtos() {
    }

    /**
     * Retenue operee par le client sur une facture. Sans {@code amount}, le montant vient du taux
     * du type applique a sa base (TTC, HT ou TVA de la facture) ; un type sans taux exige le montant.
     */
    public record RecordRequest(@NotBlank String typeCode, @DecimalMin("0.01") BigDecimal amount, LocalDate withheldOn,
                                @Size(max = 120) String certificateReference, @Size(max = 120) String certificateDocument) {
    }

    /** Attestation de retenue recue : sa reference et le fichier depose en GED. */
    public record CertificateRequest(@Size(max = 120) String certificateReference, @NotBlank @Size(max = 120) String certificateDocument) {
    }

    public record CancelRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record TypeRequest(@NotBlank @Size(max = 40) String code, @NotBlank @Size(max = 150) String label, BigDecimal rate,
                              @NotBlank String base, @NotBlank @Size(max = 20) String account, Integer displayOrder) {
    }

    public record TypeUpdateRequest(@Size(max = 150) String label, BigDecimal rate, String base, @Size(max = 20) String account,
                                    Integer displayOrder) {
    }

    public record TypeView(String code, String label, BigDecimal rate, String base, String account, Integer displayOrder, boolean active) {
    }

    public record WithholdingView(String withholdingNumber, String documentNumber, String typeCode, String typeLabel, String account,
                                  BigDecimal baseAmount, BigDecimal rate, BigDecimal amount, String currency, String customerType,
                                  String customerCode, String certificateReference, String certificateDocument, Instant certificateAt,
                                  boolean certificateMissing, String status, LocalDate withheldOn, String recordedBy, Instant cancelledAt,
                                  String cancelReason, Instant createdAt) {
    }
}
