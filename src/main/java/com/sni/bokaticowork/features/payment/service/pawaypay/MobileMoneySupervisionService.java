package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.mail.Recipients;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyCallbackResponse;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.model.PawapayCallback;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyPaymentProvider;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyStatusResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayProperties;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayCallbackPayload;
import com.sni.bokaticowork.features.payment.repository.PawapayCallbackRepository;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Le suivi des depots mobile money · ce que le worker fait en boucle et ce qu'un agent demande.
 *
 * <p>Un depot relu a la main et un depot relu par le worker doivent aboutir au meme resultat.
 * La regle vit donc ici une seule fois : le worker la deroule sur les echeances, l'ecran
 * d'administration la declenche sur un depot precis.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobileMoneySupervisionService {

    /** Ce qu'on a pu conclure d'une relecture. */
    public enum Verdict {
        /** L'operateur a tranche · le depot est encaisse ou refuse. */
        SETTLED,
        /** Rien de nouveau · une prochaine echeance a ete posee. */
        PENDING,
        /** On a cesse d'attendre · le depot est a rapprocher a la main. */
        UNRESOLVED
    }

    private final PawapayDepositRepository depositRepository;
    private final PawapayCallbackRepository callbackRepository;
    private final MobileMoneyPaymentProvider mobileMoneyProvider;
    private final PawapayCallbackProcessor callbackProcessor;
    private final PawapayDepositService depositService;
    private final PawapayProperties properties;
    private final OutboxService outboxService;

    // ---------------------------------------------------------------------------------------
    // Relecture
    // ---------------------------------------------------------------------------------------

    /** Les depots dont la verification est echue. */
    @Transactional(readOnly = true)
    public List<PawapayDeposit> due(int batch) {
        return depositRepository.findDueForStatusCheck(Instant.now(), batch);
    }

    /**
     * Relit le statut d'un depot et en tire les consequences · ou n'en tire aucune.
     *
     * <p>{@code PROCESSING} et {@code UNKNOWN} n'autorisent aucune ecriture : le client n'a pas
     * encore valide, ou l'operateur n'a pas repondu. Declarer un echec sur cette base a deja
     * annule des paiements que le client confirmait une demi-heure plus tard.</p>
     */
    public Verdict settle(PawapayDeposit deposit) {
        MobileMoneyStatusResponse status = mobileMoneyProvider.checkStatus(deposit.getDepositId());
        String verdict = status.status() == null ? "" : status.status();

        if ("SUCCEEDED".equals(verdict)) {
            callbackProcessor.process(payload(deposit, "COMPLETED", status));
            return Verdict.SETTLED;
        }
        if ("FAILED".equals(verdict)) {
            callbackProcessor.process(payload(deposit, "FAILED", status));
            return Verdict.SETTLED;
        }
        if ("NOT_FOUND".equals(verdict)) {
            // L'operateur ne connait pas ce depot · il n'est jamais parti, rien n'a ete preleve.
            // C'est le seul echec que nous sommes fondes a declarer nous-memes.
            log.info("Depot {} inconnu de l'operateur · aucune demande ne lui est parvenue", deposit.getDepositId());
            callbackProcessor.process(payload(deposit, "FAILED", status));
            return Verdict.SETTLED;
        }

        if (depositService.waitedTooLong(deposit)) {
            // Un depot deja declare a rapprocher ne se redeclare pas · l'alerte a deja ete envoyee
            // et la relire une nouvelle fois ne produirait qu'un second courriel identique.
            if (!"UNRESOLVED".equals(deposit.getStatus())) {
                depositService.markUnresolved(deposit.getDepositId());
                alertUnresolved(deposit);
            }
            return Verdict.UNRESOLVED;
        }
        depositService.scheduleNextCheck(deposit.getDepositId(), status.status());
        return Verdict.PENDING;
    }

    /** Relecture demandee par un agent · meme regle, sur un depot precis. */
    public MobileMoneyDepositResponse recheck(String depositId) {
        PawapayDeposit deposit = depositRepository.findByDepositId(depositId)
                .orElseThrow(() -> new ResourceNotFoundException("Dépôt mobile money introuvable : " + depositId));
        // On relit un depot qui attend encore, et aussi un depot laisse a rapprocher · c'est
        // justement celui pour lequel un agent veut savoir si l'operateur a fini par trancher.
        if (deposit.pending() || "UNRESOLVED".equals(deposit.getStatus())) {
            settle(deposit);
        }
        return depositService.get(depositId);
    }

    private PawapayCallbackPayload payload(PawapayDeposit deposit, String status, MobileMoneyStatusResponse checked) {
        return new PawapayCallbackPayload(
                deposit.getDepositId(), status, null, null, deposit.getClientReferenceId(), null, null,
                checked.providerTransactionId(), null, null, null, null, null, null, null,
                checked.failureReason());
    }

    /** Un depot sans reponse definitive ne se range pas tout seul · quelqu'un doit le rapprocher. */
    private void alertUnresolved(PawapayDeposit deposit) {
        log.warn("Depot {} sans reponse definitive apres {} h · a rapprocher",
                deposit.getDepositId(), properties.getPollingMaxHours());
        for (String recipient : Recipients.split(properties.getAlertEmail())) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("adminEmail", recipient);
            payload.put("templateCode", "MOBILE_MONEY_DEPOSIT_UNRESOLVED");
            payload.put("depositId", deposit.getDepositId());
            payload.put("intentNumber", deposit.getIntentNumber());
            payload.put("transactionNumber", deposit.getTransactionNumber());
            payload.put("phoneNumber", deposit.getPhoneNumber());
            payload.put("provider", deposit.getProvider());
            payload.put("amount", deposit.getAmount() == null ? "" : deposit.getAmount().toPlainString());
            payload.put("currency", deposit.getCurrency());
            payload.put("hours", String.valueOf(properties.getPollingMaxHours()));
            outboxService.publish("MOBILE_MONEY_DEPOSIT_UNRESOLVED", "PAYMENT",
                    deposit.getDepositId() + ":" + recipient, payload);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Consultation
    // ---------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PaginatedResponse<MobileMoneyDepositResponse> deposits(String status, Pageable pageable) {
        return new PaginatedResponse<>((StringUtils.hasText(status)
                ? depositRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase(Locale.ROOT), pageable)
                : depositRepository.findAllByOrderByCreatedAtDesc(pageable))
                .map(depositService::toResponse));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<MobileMoneyCallbackResponse> callbacks(String outcome, Pageable pageable) {
        return new PaginatedResponse<>((StringUtils.hasText(outcome)
                ? callbackRepository.findByOutcomeOrderByReceivedAtDesc(parseOutcome(outcome), pageable)
                : callbackRepository.findAllByOrderByReceivedAtDesc(pageable))
                .map(MobileMoneyCallbackResponse::of));
    }

    private PawapayCallback.Outcome parseOutcome(String outcome) {
        try {
            return PawapayCallback.Outcome.valueOf(outcome.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new com.sni.bokaticowork.core.exception.customs.BadRequestException(
                    "Issue inconnue : " + outcome + ". Valeurs possibles : PROCESSED, DEFERRED, IGNORED, FAILED.");
        }
    }
}
