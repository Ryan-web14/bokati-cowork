package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Declaration d'une absence de caisse.
 *
 * @param userEmail           l'absent ; l'utilisateur connecte si vide
 * @param kindCode            motif (liste {@code ABSENCE_REASON}) ; {@code DEPARTURE} pour un depart
 * @param endsOn              fin, facultative pour une absence imprevue
 * @param replacementEmail    remplacant ; propose d'office si vide
 * @param noReplacementPolicy que faire du fonds sans remplacant
 * @param unplanned           absence imprevue (maladie, urgence)
 * @param force               departer malgre des elements en cours (responsable, avec {@code forceNote})
 */
public record CashAbsenceRequest(
        String userEmail,
        @NotBlank String kindCode,
        String reasonText,
        @NotNull LocalDate startsOn,
        LocalDate endsOn,
        String replacementEmail,
        com.sni.bokaticowork.features.payment.enums.CashNoReplacementPolicy noReplacementPolicy,
        Boolean unplanned,
        Boolean force,
        String forceNote
) {
}
