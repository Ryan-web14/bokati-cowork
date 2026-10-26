package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayClient;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayRefundCallbackPayload;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Relit chez PawaPay les remboursements mobile money restes en cours.
 *
 * <p>Un remboursement ne se reglait que par son rappel · un rappel perdu le laissait « en cours »
 * pour toujours, et le montant restait compte comme rembourse sans l'etre. La relecture applique la
 * reponse de PawaPay par le meme traitement que le rappel.</p>
 *
 * <p>« Inconnu de PawaPay » n'est un echec qu'apres {@link #NOT_FOUND_GRACE} · juste apres
 * l'initiation, PawaPay peut ne pas encore le connaitre.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobileMoneyRefundSupervision {

    /** Un remboursement est relu au plus tot une minute apres sa creation. */
    static final Duration MIN_AGE = Duration.ofMinutes(1);
    static final Duration NOT_FOUND_GRACE = Duration.ofMinutes(15);

    private static final Set<String> COMPLETED = Set.of("COMPLETED", "SUCCESSFUL");
    private static final Set<String> FAILED = Set.of("FAILED", "REJECTED", "EXPIRED", "CANCELLED");

    private final PaymentTransactionRepository transactionRepository;
    private final PawapayRefundCallbackProcessor refundProcessor;
    private final ObjectProvider<PawapayClient> client;

    /** Une passe · rend le nombre de remboursements tranches. */
    public int sweep(int limit) {
        PawapayClient pawapay = client.getIfAvailable();
        if (pawapay == null) {
            return 0;
        }
        List<PaymentTransaction> pending = transactionRepository.findPendingMobileMoneyRefunds(
                Instant.now().minus(MIN_AGE), limit);
        int settled = 0;
        for (PaymentTransaction refund : pending) {
            try {
                if (settle(pawapay, refund)) {
                    settled++;
                }
            } catch (RuntimeException ex) {
                log.warn("Relecture du remboursement {} impossible : {}", refund.getTransactionNumber(), ex.getMessage());
            }
        }
        return settled;
    }

    boolean settle(PawapayClient pawapay, PaymentTransaction refund) {
        PawapayClient.DepositStatus status = pawapay.getRefundStatus(refund.getProviderReference());
        String verdict = status.providerStatus() == null ? "" : status.providerStatus().toUpperCase(Locale.ROOT);
        if (status.found() && COMPLETED.contains(verdict)) {
            refundProcessor.process(payload(refund, "COMPLETED", null));
            return true;
        }
        if (status.found() && FAILED.contains(verdict)) {
            refundProcessor.process(payload(refund, "FAILED", status.failureReason()));
            return true;
        }
        if (!status.found() && refund.getCreatedAt() != null
                && refund.getCreatedAt().isBefore(Instant.now().minus(NOT_FOUND_GRACE))) {
            // Inconnu de PawaPay apres le delai · la demande ne lui est jamais parvenue, rien n'a bouge.
            refundProcessor.process(payload(refund, "FAILED", null));
            return true;
        }
        return false;
    }

    private static PawapayRefundCallbackPayload payload(PaymentTransaction refund, String status,
                                                        com.fasterxml.jackson.databind.JsonNode failureReason) {
        return new PawapayRefundCallbackPayload(refund.getProviderReference(), null, status, null, null, null, null,
                failureReason);
    }
}
