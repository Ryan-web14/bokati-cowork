package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.model.PawapayCallback;

import java.time.Instant;

/**
 * Un rappel de l'operateur, tel qu'il a ete recu et conclu.
 *
 * <p>Le corps brut n'est pas repris ici · il peut contenir le numero du payeur et n'a d'interet
 * que pour une enquete, qui se mene sur la table.</p>
 */
public record MobileMoneyCallbackResponse(
        Long id,
        String kind,
        String referenceId,
        String reportedStatus,
        boolean signaturePresent,
        String verifiedVia,
        String outcome,
        String detail,
        String remoteAddress,
        Instant receivedAt,
        Instant processedAt
) {
    public static MobileMoneyCallbackResponse of(PawapayCallback entry) {
        return new MobileMoneyCallbackResponse(
                entry.getId(),
                entry.getKind() == null ? null : entry.getKind().name(),
                entry.getReferenceId(),
                entry.getReportedStatus(),
                entry.isSignaturePresent(),
                entry.getVerifiedVia() == null ? null : entry.getVerifiedVia().name(),
                entry.getOutcome() == null ? null : entry.getOutcome().name(),
                entry.getDetail(),
                entry.getRemoteAddress(),
                entry.getReceivedAt(),
                entry.getProcessedAt());
    }
}
