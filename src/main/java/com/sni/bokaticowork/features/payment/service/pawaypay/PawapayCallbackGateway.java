package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.payment.model.PawapayCallback;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyPaymentProvider;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyStatusResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayClient;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapaySignatureVerifier;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayCallbackPayload;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayRefundCallbackPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * La porte d'entree des rappels de l'operateur · tout y passe, tout y est consigne.
 *
 * <p>Un rappel arrive par une URL publique. On ne peut donc pas le croire sur parole : sa signature
 * le prouve, ou bien c'est le statut relu aupres de l'operateur, avec notre cle d'API, qui fait
 * foi. Un corps non signe ne declare jamais un paiement recu par lui-meme.</p>
 *
 * <p>Quatre issues, toutes ecrites dans {@code pawapay_callback} :</p>
 * <ul>
 *   <li>{@code PROCESSED} · le sort du depot a ete tranche, par signature ou par relecture ;</li>
 *   <li>{@code DEFERRED} · l'operateur n'a pas encore tranche, ou ne repond pas · le worker reprend ;</li>
 *   <li>{@code IGNORED} · le rappel ne designe rien de connu · aucun effet ;</li>
 *   <li>{@code FAILED} · le traitement a echoue · la trace reste pour rejouer.</li>
 * </ul>
 *
 * <p>Dans tous les cas la reponse HTTP est 200 : un non-2xx fait rejouer PawaPay indefiniment,
 * alors que la trace suffit a rattraper ce qui n'a pas abouti.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PawapayCallbackGateway {

    private final PawapayCallbackJournal journal;
    private final PawapaySignatureVerifier signatureVerifier;
    private final PawapayCallbackProcessor callbackProcessor;
    private final PawapayRefundCallbackProcessor refundCallbackProcessor;
    private final PawapayDepositService depositService;
    private final MobileMoneyPaymentProvider mobileMoneyProvider;
    private final ObjectProvider<PawapayClient> pawapayClient;
    private final ObjectMapper objectMapper;

    // ---------------------------------------------------------------------------------------
    // Depots
    // ---------------------------------------------------------------------------------------

    public PawapayCallback.Outcome receiveDeposit(String rawBody, String signatureHeader, String remoteAddress) {
        boolean signaturePresent = StringUtils.hasText(signatureHeader);
        Long entry = journal.open(PawapayCallback.Kind.DEPOSIT, rawBody, signaturePresent, remoteAddress);

        PawapayCallbackPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, PawapayCallbackPayload.class);
        } catch (Exception ex) {
            log.error("Rappel de depot PawaPay illisible", ex);
            return close(entry, null, null, PawapayCallback.VerifiedVia.NONE,
                    PawapayCallback.Outcome.FAILED, "Corps illisible : " + ex.getMessage());
        }

        String depositId = payload.depositId();
        String reported = payload.status();
        if (!StringUtils.hasText(depositId)) {
            log.warn("Rappel de depot PawaPay sans identifiant · ignore");
            return close(entry, null, reported, PawapayCallback.VerifiedVia.NONE,
                    PawapayCallback.Outcome.IGNORED, "Aucun depositId dans le corps du rappel");
        }

        try {
            if (signatureVerifier.verify(rawBody, signatureHeader)) {
                callbackProcessor.process(payload);
                log.info("Rappel de depot {} traite · signature verifiee, statut annonce {}", depositId, reported);
                return close(entry, depositId, reported, PawapayCallback.VerifiedVia.SIGNATURE,
                        PawapayCallback.Outcome.PROCESSED, null);
            }
            // Sans signature exploitable, le corps du rappel ne decide de rien : c'est le statut
            // relu chez l'operateur qui tranche. Un rappel forge ne peut donc pas faire encaisser.
            log.warn("Rappel de depot {} non signe · statut relu aupres de l'operateur", depositId);
            return settleFromProvider(entry, depositId, reported);
        } catch (Exception ex) {
            log.error("Rappel de depot {} en erreur · trace conservee", depositId, ex);
            return close(entry, depositId, reported, PawapayCallback.VerifiedVia.NONE,
                    PawapayCallback.Outcome.FAILED, String.valueOf(ex.getMessage()));
        }
    }

    /**
     * Relit le statut chez l'operateur et n'agit que sur ce qu'il affirme.
     *
     * <p>{@code PROCESSING} et {@code UNKNOWN} ne produisent aucune ecriture · le premier parce que
     * le client n'a pas encore valide, le second parce qu'une absence de reponse n'est pas un
     * refus. Le worker reprendra la main.</p>
     */
    private PawapayCallback.Outcome settleFromProvider(Long entry, String depositId, String reported) {
        MobileMoneyStatusResponse checked;
        try {
            checked = mobileMoneyProvider.checkStatus(depositId);
        } catch (Exception ex) {
            log.warn("Statut du depot {} illisible chez l'operateur · reprise par le worker : {}",
                    depositId, ex.getMessage());
            depositService.scheduleNextCheck(depositId, "UNKNOWN");
            return close(entry, depositId, reported, PawapayCallback.VerifiedVia.NONE,
                    PawapayCallback.Outcome.DEFERRED, "Operateur injoignable : " + ex.getMessage());
        }

        switch (checked.status() == null ? "" : checked.status()) {
            case "SUCCEEDED" -> {
                callbackProcessor.process(statusPayload(depositId, "COMPLETED", checked));
                return close(entry, depositId, reported, PawapayCallback.VerifiedVia.PROVIDER_STATUS,
                        PawapayCallback.Outcome.PROCESSED, "Confirme par l'operateur");
            }
            case "FAILED" -> {
                callbackProcessor.process(statusPayload(depositId, "FAILED", checked));
                return close(entry, depositId, reported, PawapayCallback.VerifiedVia.PROVIDER_STATUS,
                        PawapayCallback.Outcome.PROCESSED, "Echec confirme par l'operateur");
            }
            case "NOT_FOUND" -> {
                // L'operateur ne connait pas ce depot · le rappel ne correspond a rien de reel.
                log.warn("Rappel de depot {} sans correspondance chez l'operateur · ignore", depositId);
                return close(entry, depositId, reported, PawapayCallback.VerifiedVia.PROVIDER_STATUS,
                        PawapayCallback.Outcome.IGNORED, "Depot inconnu de l'operateur");
            }
            default -> {
                depositService.scheduleNextCheck(depositId, checked.status());
                return close(entry, depositId, reported, PawapayCallback.VerifiedVia.PROVIDER_STATUS,
                        PawapayCallback.Outcome.DEFERRED, "Operateur : " + checked.status());
            }
        }
    }

    private PawapayCallbackPayload statusPayload(String depositId, String status, MobileMoneyStatusResponse checked) {
        return new PawapayCallbackPayload(
                depositId, status, null, null, null, null, null,
                checked.providerTransactionId(), null, null, null, null, null, null, null,
                checked.failureReason());
    }

    // ---------------------------------------------------------------------------------------
    // Remboursements
    // ---------------------------------------------------------------------------------------

    public PawapayCallback.Outcome receiveRefund(String rawBody, String signatureHeader, String remoteAddress) {
        boolean signaturePresent = StringUtils.hasText(signatureHeader);
        Long entry = journal.open(PawapayCallback.Kind.REFUND, rawBody, signaturePresent, remoteAddress);

        PawapayRefundCallbackPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, PawapayRefundCallbackPayload.class);
        } catch (Exception ex) {
            log.error("Rappel de remboursement PawaPay illisible", ex);
            return close(entry, null, null, PawapayCallback.VerifiedVia.NONE,
                    PawapayCallback.Outcome.FAILED, "Corps illisible : " + ex.getMessage());
        }

        String refundId = payload.refundId();
        String reported = payload.status();
        if (!StringUtils.hasText(refundId)) {
            return close(entry, null, reported, PawapayCallback.VerifiedVia.NONE,
                    PawapayCallback.Outcome.IGNORED, "Aucun refundId dans le corps du rappel");
        }

        try {
            if (signatureVerifier.verify(rawBody, signatureHeader)) {
                refundCallbackProcessor.process(payload);
                return close(entry, refundId, reported, PawapayCallback.VerifiedVia.SIGNATURE,
                        PawapayCallback.Outcome.PROCESSED, null);
            }
            // Un remboursement non signe etait purement et simplement jete · une notification
            // perdue laissait le remboursement en cours pour toujours. On relit chez l'operateur.
            return settleRefundFromProvider(entry, payload, refundId, reported);
        } catch (Exception ex) {
            log.error("Rappel de remboursement {} en erreur · trace conservee", refundId, ex);
            return close(entry, refundId, reported, PawapayCallback.VerifiedVia.NONE,
                    PawapayCallback.Outcome.FAILED, String.valueOf(ex.getMessage()));
        }
    }

    private PawapayCallback.Outcome settleRefundFromProvider(Long entry, PawapayRefundCallbackPayload payload,
                                                             String refundId, String reported) {
        PawapayClient client = pawapayClient.getIfAvailable();
        if (client == null) {
            return close(entry, refundId, reported, PawapayCallback.VerifiedVia.NONE,
                    PawapayCallback.Outcome.IGNORED, "Aucun acces a l'operateur pour relire le remboursement");
        }
        PawapayClient.DepositStatus checked;
        try {
            checked = client.getRefundStatus(refundId);
        } catch (Exception ex) {
            log.warn("Statut du remboursement {} illisible chez l'operateur : {}", refundId, ex.getMessage());
            return close(entry, refundId, reported, PawapayCallback.VerifiedVia.NONE,
                    PawapayCallback.Outcome.DEFERRED, "Operateur injoignable : " + ex.getMessage());
        }

        String status = checked.providerStatus() == null ? "" : checked.providerStatus();
        if ("COMPLETED".equals(status) || "SUCCEEDED".equals(status) || "FAILED".equals(status)) {
            String settled = "FAILED".equals(status) ? "FAILED" : "COMPLETED";
            refundCallbackProcessor.process(new PawapayRefundCallbackPayload(
                    refundId, payload.depositId(), settled, payload.amount(), payload.currency(),
                    payload.created(), payload.respondedByPayer(), checked.failureReason()));
            return close(entry, refundId, reported, PawapayCallback.VerifiedVia.PROVIDER_STATUS,
                    PawapayCallback.Outcome.PROCESSED, "Operateur : " + status);
        }
        if ("NOT_FOUND".equals(status)) {
            return close(entry, refundId, reported, PawapayCallback.VerifiedVia.PROVIDER_STATUS,
                    PawapayCallback.Outcome.IGNORED, "Remboursement inconnu de l'operateur");
        }
        return close(entry, refundId, reported, PawapayCallback.VerifiedVia.PROVIDER_STATUS,
                PawapayCallback.Outcome.DEFERRED, "Operateur : " + status);
    }

    // ---------------------------------------------------------------------------------------

    private PawapayCallback.Outcome close(Long entry, String referenceId, String reportedStatus,
                                          PawapayCallback.VerifiedVia verifiedVia,
                                          PawapayCallback.Outcome outcome, String detail) {
        journal.close(entry, referenceId, reportedStatus, verifiedVia, outcome, detail);
        return outcome;
    }
}
