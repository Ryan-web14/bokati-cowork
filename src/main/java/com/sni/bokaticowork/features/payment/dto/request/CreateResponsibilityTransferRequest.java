package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transfert de responsabilite d'une caisse (PV de remise {@code PRC-}).
 *
 * @param toUserEmail   nouveau titulaire (ou interimaire si {@code permanent} est faux)
 * @param reasonCode    code de la liste {@code RESPONSIBILITY_TRANSFER_REASON}
 * @param effectiveAt   date d'effet ; maintenant si absente
 * @param endsAt        fin de l'interim, obligatoire si le transfert n'est pas permanent
 * @param countedAmount montant compte contradictoirement a la remise
 * @param witnessEmail  temoin, obligatoire si le titulaire sortant est absent (le comptage est alors
 *                      fait par le superviseur qui cree le transfert et ce temoin)
 * @param keepFormerAsBackup le titulaire sortant reste remplacant (transfert permanent)
 */
public record CreateResponsibilityTransferRequest(
        @NotBlank String toUserEmail,
        @NotBlank String reasonCode,
        String reasonText,
        @NotNull Boolean permanent,
        Instant effectiveAt,
        Instant endsAt,
        BigDecimal countedAmount,
        String witnessEmail,
        Boolean outgoingAbsent,
        Boolean keepFormerAsBackup
) {
}
