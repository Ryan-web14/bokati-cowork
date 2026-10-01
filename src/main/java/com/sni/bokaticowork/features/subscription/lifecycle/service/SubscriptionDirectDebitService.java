package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.payment.dto.request.PayInvoiceRequest;
import com.sni.bokaticowork.features.payment.dto.response.PayInvoiceResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionDebitAttempt;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionDebitMandate;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionDebitAttemptRepository;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionDebitMandateRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Le prelevement automatique · depuis le portefeuille, avec accord prealable.
 *
 * <p>Sans mandat, rien n'est preleve. Avec un mandat, chaque echeance est tentee une fois, apres
 * que la facture est emise, et le resultat est ecrit : reussi, solde insuffisant, au-dela du
 * plafond, ou echoue. Un echec ouvre la tolerance ; trois echecs de suite suspendent le mandat,
 * parce qu'un mandat qui echoue a chaque fois n'est plus un mandat, c'est une relance.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionDirectDebitService {

    static final int MAX_CONSECUTIVE_FAILURES = 3;
    static final String ACTOR = "SYSTEM:DIRECT_DEBIT";

    private final SubscriptionDebitMandateRepository mandateRepository;
    private final SubscriptionDebitAttemptRepository attemptRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BillingDocumentRepository billingDocumentRepository;
    private final WalletService walletService;
    private final DebitGateway gateway;
    private final SubscriptionGraceService graceService;
    private final SubscriptionEventWriter eventWriter;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final OutboxService outboxService;

    public record Consent(String walletNumber, SubscriptionDebitMandate.Channel channel, String reference, BigDecimal maxAmountPerDebit) {
    }

    // ---- Le mandat ------------------------------------------------------------------------------

    @Transactional
    public SubscriptionDebitMandate give(String subscriptionNumber, Consent consent, String givenBy) {
        Subscription subscription = subscription(subscriptionNumber);
        if (consent.channel() == null) {
            throw new BadRequestException("Le canal du consentement est requis · portail, formulaire signé, courriel, guichet");
        }
        if (!StringUtils.hasText(consent.walletNumber())) {
            throw new BadRequestException("Le portefeuille à prélever est requis");
        }
        WalletAccount wallet = walletService.serviceWallet(consent.walletNumber().trim());
        if (!wallet.getOwnerType().equalsIgnoreCase(subscription.getSubscriberType().name())
                || !wallet.getOwnerCode().equalsIgnoreCase(subscription.getSubscriberCode())) {
            throw new ConflictException("mandate", "le portefeuille n'appartient pas au souscripteur");
        }
        if (consent.maxAmountPerDebit() != null && consent.maxAmountPerDebit().signum() <= 0) {
            throw new BadRequestException("Le plafond par prélèvement est positif, ou absent");
        }
        mandateRepository.findCurrentBySubscription(subscription.getId()).ifPresent(current -> {
            throw new ConflictException("mandate", "un mandat existe déjà · " + current.getMandateCode() + " (" + current.getStatus() + ")");
        });
        SubscriptionDebitMandate mandate = mandateRepository.save(SubscriptionDebitMandate.builder()
                .mandateCode(sequenceGenerator.next("subscription_debit_mandate"))
                .subscription(subscription)
                .walletNumber(wallet.getWalletNumber())
                .consentGivenAt(Instant.now())
                .consentGivenBy(givenBy)
                .consentChannel(consent.channel())
                .consentReference(trim(consent.reference()))
                .maxAmountPerDebit(consent.maxAmountPerDebit())
                .build());
        log.info("Abonnement {} · mandat {} donne par {} via {}", subscriptionNumber, mandate.getMandateCode(), givenBy, consent.channel());
        return mandate;
    }

    @Transactional
    public SubscriptionDebitMandate revoke(String mandateCode, String reason, String actor) {
        SubscriptionDebitMandate mandate = get(mandateCode);
        if (mandate.getStatus() == SubscriptionDebitMandate.Status.REVOKED) {
            throw new BadRequestException("Ce mandat est déjà révoqué");
        }
        mandate.setStatus(SubscriptionDebitMandate.Status.REVOKED);
        mandate.setRevokedAt(Instant.now());
        mandate.setRevokedBy(actor);
        mandate.setRevocationReason(trim(reason));
        return mandateRepository.save(mandate);
    }

    /** Un mandat suspendu (apres echecs) reprend quand l'abonne l'a rechargé et le demande. */
    @Transactional
    public SubscriptionDebitMandate reactivate(String mandateCode, String actor) {
        SubscriptionDebitMandate mandate = get(mandateCode);
        if (mandate.getStatus() != SubscriptionDebitMandate.Status.SUSPENDED) {
            throw new BadRequestException("Seul un mandat suspendu se réactive · celui-ci est " + mandate.getStatus());
        }
        mandate.setStatus(SubscriptionDebitMandate.Status.ACTIVE);
        mandate.setConsecutiveFailures(0);
        log.info("Mandat {} reactive par {}", mandateCode, actor);
        return mandateRepository.save(mandate);
    }

    @Transactional(readOnly = true)
    public SubscriptionDebitMandate get(String mandateCode) {
        return mandateRepository.findByMandateCode(mandateCode)
                .orElseThrow(() -> new ResourceNotFoundException("Mandat introuvable"));
    }

    @Transactional(readOnly = true)
    public Optional<SubscriptionDebitMandate> currentOf(Subscription subscription) {
        return mandateRepository.findCurrentBySubscription(subscription.getId());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionDebitMandate> ofSubscription(String subscriptionNumber) {
        return mandateRepository.findBySubscription_IdOrderByCreatedAtDesc(subscription(subscriptionNumber).getId());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionDebitAttempt> attempts(String subscriptionNumber) {
        return attemptRepository.findBySubscriptionIdOrderByExecutedAtDesc(subscription(subscriptionNumber).getId());
    }

    // ---- Le prelevement -----------------------------------------------------------------------

    /**
     * A appeler dans la transaction du renouvellement · la tentative part une fois la facture
     * validee en base, dans sa propre transaction, pour qu'un echec ne defasse pas le renouvellement.
     */
    public void collectAfterCommit(Long subscriptionId, String invoiceNumber) {
        if (invoiceNumber == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            collect(subscriptionId, invoiceNumber);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    collect(subscriptionId, invoiceNumber);
                } catch (RuntimeException ex) {
                    log.warn("Prelevement de la facture {} · erreur inattendue : {}", invoiceNumber, ex.getMessage());
                }
            }
        });
    }

    /** Tente le prelevement d'une facture · rend la tentative, vide sans mandat actif. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<SubscriptionDebitAttempt> collect(Long subscriptionId, String invoiceNumber) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable"));
        SubscriptionDebitMandate mandate = mandateRepository.findCurrentBySubscription(subscriptionId)
                .filter(m -> m.getStatus() == SubscriptionDebitMandate.Status.ACTIVE).orElse(null);
        if (mandate == null) {
            return Optional.empty();
        }
        BillingDocument invoice = billingDocumentRepository.findByDocumentNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
        BigDecimal amount = invoice.getBalanceDue();
        if (amount == null || amount.signum() <= 0) {
            return Optional.empty();
        }
        SubscriptionDebitAttempt.Status status;
        String message;
        String transactionNumber = null;

        if (mandate.getMaxAmountPerDebit() != null && amount.compareTo(mandate.getMaxAmountPerDebit()) > 0) {
            status = SubscriptionDebitAttempt.Status.OVER_LIMIT;
            message = "Échéance " + amount.toPlainString() + " au-delà du plafond du mandat " + mandate.getMaxAmountPerDebit().toPlainString();
        } else {
            WalletAccount wallet = walletService.serviceWallet(mandate.getWalletNumber());
            if (wallet.getAvailableBalance() == null || wallet.getAvailableBalance().compareTo(amount) < 0) {
                status = SubscriptionDebitAttempt.Status.INSUFFICIENT_FUNDS;
                message = "Solde disponible " + (wallet.getAvailableBalance() == null ? "0" : wallet.getAvailableBalance().toPlainString())
                        + " pour une échéance de " + amount.toPlainString();
            } else {
                try {
                    PayInvoiceResponse paid = gateway.pay(invoiceNumber, mandate.getWalletNumber(), amount,
                            "DIRECT_DEBIT:" + mandate.getMandateCode() + ":" + invoiceNumber);
                    status = SubscriptionDebitAttempt.Status.SUCCEEDED;
                    transactionNumber = paid.transaction() == null ? null : paid.transaction().transactionNumber();
                    message = "Prélevé";
                } catch (RuntimeException ex) {
                    status = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("insufficient")
                            ? SubscriptionDebitAttempt.Status.INSUFFICIENT_FUNDS : SubscriptionDebitAttempt.Status.FAILED;
                    message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                }
            }
        }

        SubscriptionDebitAttempt attempt = attemptRepository.save(SubscriptionDebitAttempt.builder()
                .mandate(mandate).subscriptionId(subscriptionId).invoiceNumber(invoiceNumber)
                .amount(amount).currency(invoice.getCurrency()).status(status).message(message)
                .transactionNumber(transactionNumber).build());

        if (status == SubscriptionDebitAttempt.Status.SUCCEEDED) {
            mandate.setConsecutiveFailures(0);
            mandate.setLastDebitAt(Instant.now());
            eventWriter.writeEvent(subscription, SubscriptionEventType.DIRECT_DEBIT_SUCCEEDED,
                    "{\"invoice\":\"" + invoiceNumber + "\",\"amount\":" + amount.toPlainString() + "}");
            graceService.exit(subscription, "Prélèvement réussi · " + invoiceNumber);
        } else {
            mandate.setConsecutiveFailures(mandate.getConsecutiveFailures() + 1);
            if (mandate.getConsecutiveFailures() >= MAX_CONSECUTIVE_FAILURES) {
                mandate.setStatus(SubscriptionDebitMandate.Status.SUSPENDED);
            }
            eventWriter.writeEvent(subscription, SubscriptionEventType.DIRECT_DEBIT_FAILED,
                    "{\"invoice\":\"" + invoiceNumber + "\",\"status\":\"" + status + "\"}");
            notifyFailure(subscription, mandate, attempt);
            graceService.enter(subscription, "Prélèvement " + status + " · " + invoiceNumber);
        }
        mandateRepository.save(mandate);
        log.info("Abonnement {} · prelevement {} · {} ({})", subscription.getSubscriptionNumber(), invoiceNumber, status, message);
        return Optional.of(attempt);
    }

    private void notifyFailure(Subscription subscription, SubscriptionDebitMandate mandate, SubscriptionDebitAttempt attempt) {
        String email = SubscriptionGraceService.recipientEmail(subscription);
        if (!StringUtils.hasText(email)) {
            return;
        }
        outboxService.publish("SUBSCRIPTION_DIRECT_DEBIT_FAILED", "SUBSCRIPTION", subscription.getSubscriptionNumber(), Map.of(
                "recipientEmail", email,
                "subject", "Prélèvement impossible · abonnement " + subscription.getSubscriptionNumber(),
                "templateCode", "subscription_direct_debit_failed",
                "subscriptionNumber", subscription.getSubscriptionNumber(),
                "invoiceNumber", attempt.getInvoiceNumber(),
                "amount", attempt.getAmount().toPlainString(),
                "currency", attempt.getCurrency(),
                "status", attempt.getStatus().name(),
                "mandateSuspended", String.valueOf(mandate.getStatus() == SubscriptionDebitMandate.Status.SUSPENDED)
        ));
    }

    private Subscription subscription(String number) {
        return subscriptionRepository.findBySubscriptionNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable"));
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * Le paiement dans sa propre transaction · s'il echoue, il ne laisse rien, et la tentative qui
     * l'ecrit n'est pas entrainee dans sa chute.
     */
    @Component
    @RequiredArgsConstructor
    public static class DebitGateway {

        private final @Lazy PaymentService paymentService;

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public PayInvoiceResponse pay(String invoiceNumber, String walletNumber, BigDecimal amount, String idempotencyKey) {
            return paymentService.payInvoice(invoiceNumber, new PayInvoiceRequest(PaymentMethod.WALLET, amount, walletNumber,
                    null, null, ACTOR, idempotencyKey, null));
        }
    }
}
