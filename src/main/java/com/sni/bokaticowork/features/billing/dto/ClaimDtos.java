package com.sni.bokaticowork.features.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Lot C11 : requetes et reponses des reclamations sur facture. */
public final class ClaimDtos {

    private ClaimDtos() {
    }

    /** Reclamation : motif (liste), lignes visees (vide : toute la facture), montant demande. */
    public record OpenRequest(@NotBlank String reasonCode, @Size(max = 4000) String description, List<Integer> lineOrders,
                              @DecimalMin("0") BigDecimal requestedAmount) {
    }

    /** Acceptation : montant accorde (par defaut le montant demande), commentaire au client. */
    public record AcceptRequest(@DecimalMin("0.01") BigDecimal grantedAmount, @Size(max = 2000) String comment) {
    }

    /** Refus : motif obligatoire, communique au client. */
    public record RejectRequest(@NotBlank @Size(max = 2000) String comment) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 40) String code, @NotBlank @Size(max = 150) String label, Integer displayOrder) {
    }

    public record ReasonView(String code, String label, Integer displayOrder, boolean active) {
    }

    public record ClaimView(String claimNumber, String documentNumber, String customerType, String customerCode, String reasonCode,
                            String reasonLabel, String description, List<Integer> lineOrders, BigDecimal requestedAmount, String status,
                            String channel, String openedBy, BigDecimal grantedAmount, String creditNoteNumber, String decisionComment,
                            String decidedBy, Instant decidedAt, Instant createdAt) {
    }
}
