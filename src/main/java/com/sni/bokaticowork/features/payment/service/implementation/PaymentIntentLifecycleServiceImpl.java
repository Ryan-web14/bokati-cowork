package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.payment.dto.request.CancelPaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.RelaunchPaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentIntentLifecycleService;
import com.sni.bokaticowork.features.payment.service.support.PaymentIntentStates;
import com.sni.bokaticowork.features.payment.service.support.PaymentOriginResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Annulation et relance d'une intention de paiement.
 *
 * <p>La relance rouvre <b>la meme</b> intention (meme numero, meme lien de paiement) plutot que
 * d'en creer une autre : ce qui la reference (reservation, lien envoye au client, ecran de caisse)
 * reste valable. Son montant est ramene au reste du des factures qu'elle regle, si elles ont ete
 * payees en partie entre-temps.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentIntentLifecycleServiceImpl implements PaymentIntentLifecycleService {

    static final String BILLING_DOCUMENT_SOURCE = "BILLING_DOCUMENT";
    static final String MULTI_BILLING_DOCUMENT_SOURCE = "MULTI_BILLING_DOCUMENT";
    static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);
    static final Duration MAX_WINDOW = Duration.ofDays(30);

    private static final Set<BillingDocumentStatus> CLOSED_DOCUMENTS = Set.of(
            BillingDocumentStatus.CANCELLED,
            BillingDocumentStatus.REFUNDED, BillingDocumentStatus.WRITTEN_OFF);

    private final PaymentIntentRepository intentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final BillingDocumentRepository billingDocumentRepository;
    private final PaymentMapper mapper;
    private final PaymentOriginResolver paymentOrigin;

    @Override
    public PaymentIntentResponse cancel(String intentNumber, CancelPaymentIntentRequest request) {
        PaymentIntent intent = intent(intentNumber);
        if (intent.getStatus() == PaymentIntentStatus.CANCELLED) {
            return mapper.toIntentResponse(intent);
        }
        if (!PaymentIntentStates.OPEN.contains(intent.getStatus())) {
            throw new BadRequestException("L'intention " + intent.getIntentNumber() + " est " + intent.getStatus()
                    + " : seule une intention en cours peut être annulée");
        }
        if (processing(intent).signum() > 0) {
            throw new ConflictException("Un paiement mobile money est en cours sur l'intention " + intent.getIntentNumber()
                    + " : attendre sa confirmation ou son échec avant d'annuler");
        }
        Instant now = Instant.now();
        intent.setStatus(PaymentIntentStatus.CANCELLED);
        intent.setCancelledAt(now);
        intent.setCancelledBy(actor());
        intent.setCancellationReason(request == null || !StringUtils.hasText(request.reason()) ? null : request.reason().trim());
        // Le lien envoye au client cesse de proposer le paiement.
        if (intent.getPaymentLinkToken() != null) {
            intent.setPaymentLinkExpiresAt(now);
        }
        intentRepository.save(intent);
        log.info("Intention {} annulee par {}", intent.getIntentNumber(), intent.getCancelledBy());
        return mapper.toIntentResponse(intent);
    }

    @Override
    public PaymentIntentResponse relaunch(String intentNumber, RelaunchPaymentIntentRequest request) {
        PaymentIntent intent = intent(intentNumber);
        boolean lapsed = PaymentIntentStates.lapsed(intent);
        if (!PaymentIntentStates.RELAUNCHABLE.contains(intent.getStatus()) && !lapsed) {
            throw new BadRequestException("L'intention " + intent.getIntentNumber() + " est " + intent.getStatus()
                    + " : seule une intention expirée, annulée ou en échec peut être relancée");
        }
        if (processing(intent).signum() > 0) {
            throw new ConflictException("Un paiement mobile money est encore en cours sur l'intention " + intent.getIntentNumber()
                    + " : attendre sa confirmation ou son échec avant de la relancer");
        }
        BigDecimal paid = money(transactionRepository.sumSucceededAmountByPaymentIntentId(intent.getId()));
        BigDecimal remaining = money(intent.getAmount()).subtract(paid);
        if (remaining.signum() <= 0) {
            throw new BadRequestException("L'intention " + intent.getIntentNumber() + " est déjà entièrement payée");
        }
        BigDecimal outstanding = outstandingOfDocuments(intent);
        if (outstanding != null && outstanding.compareTo(remaining) < 0) {
            // Les factures ont ete reglees en partie par ailleurs · on ne fait pas payer deux fois.
            intent.setAmount(paid.add(outstanding));
        }
        assertNoOtherOpenIntent(intent);

        Instant now = Instant.now();
        Instant expiresAt = newExpiry(intent, request == null ? null : request.expiresAt(), now);
        intent.setStatus(PaymentIntentStatus.PENDING);
        intent.setExpiresAt(expiresAt);
        intent.setExpiryAlertSentAt(null);
        intent.setCancelledAt(null);
        intent.setCancelledBy(null);
        intent.setCancellationReason(null);
        intent.setRelaunchCount((intent.getRelaunchCount() == null ? 0 : intent.getRelaunchCount()) + 1);
        intent.setLastRelaunchedAt(now);
        intent.setLastRelaunchedBy(actor());
        if (intent.getPaymentLinkToken() != null) {
            intent.setPaymentLinkExpiresAt(expiresAt);
        }
        intentRepository.save(intent);
        log.info("Intention {} relancee par {} · echeance {}", intent.getIntentNumber(), intent.getLastRelaunchedBy(), expiresAt);
        return mapper.toIntentResponse(intent);
    }

    /** Reste du des factures reglees par l'intention · nul si elle ne regle pas de facture. */
    private BigDecimal outstandingOfDocuments(PaymentIntent intent) {
        String sourceType = intent.getSourceType();
        if (!BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(sourceType) && !MULTI_BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(sourceType)) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (String number : codes(intent.getSourceCode())) {
            BillingDocument document = billingDocumentRepository.findByDocumentNumber(number)
                    .orElseThrow(() -> new BadRequestException("La facture " + number + " de l'intention est introuvable"));
            if (CLOSED_DOCUMENTS.contains(document.getStatus())) {
                throw new BadRequestException("La facture " + number + " est " + document.getStatus()
                        + " : l'intention ne peut pas être relancée");
            }
            if (document.getBalanceDue() != null && document.getBalanceDue().signum() > 0) {
                total = total.add(document.getBalanceDue());
            }
        }
        if (total.signum() <= 0) {
            throw new BadRequestException("Les factures de l'intention " + intent.getIntentNumber()
                    + " sont déjà réglées : rien à relancer");
        }
        return money(total);
    }

    private void assertNoOtherOpenIntent(PaymentIntent intent) {
        if (!StringUtils.hasText(intent.getSourceType()) || !StringUtils.hasText(intent.getSourceCode())) {
            return;
        }
        intentRepository.findLatestReusable(intent.getCustomerType(), intent.getCustomerCode(),
                        intent.getSourceType(), intent.getSourceCode())
                .filter(other -> !other.getIntentNumber().equals(intent.getIntentNumber()))
                .filter(other -> other.getExpiresAt() == null || other.getExpiresAt().isAfter(Instant.now()))
                .ifPresent(other -> {
                    throw new ConflictException("L'intention " + other.getIntentNumber()
                            + " est déjà en cours pour " + intent.getSourceCode() + " : l'utiliser ou l'annuler d'abord");
                });
    }

    /** La duree de validite d'origine repart de maintenant, entre 15 minutes et 30 jours. */
    static Instant newExpiry(PaymentIntent intent, Instant requested, Instant now) {
        if (requested != null) {
            if (!requested.isAfter(now)) {
                throw new BadRequestException("La nouvelle échéance doit être dans le futur");
            }
            return requested;
        }
        if (intent.getExpiresAt() == null) {
            return null;
        }
        Duration window = intent.getCreatedAt() == null
                ? DEFAULT_WINDOW
                : Duration.between(intent.getCreatedAt(), intent.getExpiresAt());
        if (window.compareTo(DEFAULT_WINDOW) < 0) {
            window = DEFAULT_WINDOW;
        }
        if (window.compareTo(MAX_WINDOW) > 0) {
            window = MAX_WINDOW;
        }
        return now.plus(window);
    }

    private BigDecimal processing(PaymentIntent intent) {
        return money(transactionRepository.sumProcessingAmountByPaymentIntentId(intent.getId()));
    }

    private PaymentIntent intent(String intentNumber) {
        if (!StringUtils.hasText(intentNumber)) {
            throw new BadRequestException("Le numéro de l'intention est obligatoire");
        }
        return intentRepository.findByIntentNumber(intentNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Intention de paiement introuvable : " + intentNumber));
    }

    private String actor() {
        String actor = paymentOrigin.currentActor();
        return StringUtils.hasText(actor) ? actor : "SYSTEM";
    }

    private static List<String> codes(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(StringUtils::hasText).distinct().toList();
    }

    private static BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(4, RoundingMode.HALF_UP);
    }
}
