package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.sni.bokaticowork.features.payment.model.PawapayCallback;
import com.sni.bokaticowork.features.payment.repository.PawapayCallbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Le registre des rappels de l'operateur · ecrit d'abord, conclu ensuite.
 *
 * <p>Un rappel qui echoue a etre traite annule sa transaction. Si son enregistrement partageait
 * cette transaction, il disparaitrait avec elle : on perdrait la trace du seul evenement qui
 * pouvait expliquer un paiement manquant. Les deux ecritures se font donc dans leur propre
 * transaction, qui survit a l'echec du traitement.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PawapayCallbackJournal {

    private static final int RAW_BODY_MAX = 20_000;

    private final PawapayCallbackRepository repository;

    /** Consigne l'arrivee du rappel, avant tout traitement. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long open(PawapayCallback.Kind kind, String rawBody, boolean signaturePresent, String remoteAddress) {
        try {
            PawapayCallback entry = PawapayCallback.builder()
                    .kind(kind)
                    .rawBody(truncate(rawBody))
                    .signaturePresent(signaturePresent)
                    .verifiedVia(PawapayCallback.VerifiedVia.NONE)
                    .outcome(PawapayCallback.Outcome.DEFERRED)
                    .remoteAddress(remoteAddress)
                    .receivedAt(Instant.now())
                    .build();
            return repository.save(entry).getId();
        } catch (Exception ex) {
            // Le registre ne doit jamais empecher le traitement d'un paiement reel.
            log.error("Impossible de consigner le rappel PawaPay · traitement poursuivi", ex);
            return null;
        }
    }

    /** Conclut l'enregistrement · ce qu'on a verifie et ce qu'on en a fait. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void close(Long id, String referenceId, String reportedStatus,
                      PawapayCallback.VerifiedVia verifiedVia, PawapayCallback.Outcome outcome, String detail) {
        if (id == null) {
            return;
        }
        try {
            repository.findById(id).ifPresent(entry -> {
                entry.setReferenceId(referenceId);
                entry.setReportedStatus(reportedStatus);
                entry.setVerifiedVia(verifiedVia);
                entry.setOutcome(outcome);
                entry.setDetail(detail);
                entry.setProcessedAt(Instant.now());
                repository.save(entry);
            });
        } catch (Exception ex) {
            log.error("Impossible de conclure le rappel PawaPay {}", id, ex);
        }
    }

    private String truncate(String rawBody) {
        String body = rawBody == null ? "" : rawBody;
        return body.length() <= RAW_BODY_MAX ? body : body.substring(0, RAW_BODY_MAX);
    }
}
