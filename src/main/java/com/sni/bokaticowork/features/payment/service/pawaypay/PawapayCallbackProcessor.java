package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayCallbackPayload;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.support.PaymentAllocationService;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PawapayCallbackProcessor {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentIntentRepository intentRepository;
    private final PaymentAllocationService allocationService;
    private final OutboxService outboxService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final PawapayDepositService depositService;
    private final PawapayDepositRepository depositRepository;
    private final MobileMoneyClientNotifier clientNotifier;
    private final com.sni.bokaticowork.features.payment.service.support.SelfServicePaymentNotifier selfServiceNotifier;
    private final MobileMoneyAlertRecipients alertRecipients;
    private final com.sni.bokaticowork.features.payment.service.interfaces.WalletService walletService;

    public void process(PawapayCallbackPayload payload) {
        String depositId = payload.depositId();
        if (depositId == null) {
            log.warn("Received PawaPay callback with no depositId");
            return;
        }

        PaymentTransaction transaction = resolveTransaction(payload);

        if (transaction == null) {
            log.warn("PawaPay callback received for unknown depositId={}, clientReferenceId={}",
                    depositId, payload.clientReferenceId());
            return;
        }

        String status = payload.status() == null ? "" : payload.status().trim().toUpperCase();
        if ("COMPLETED".equals(status) || "SUCCESSFUL".equals(status) || "SUCCEEDED".equals(status)) {
            if (transaction.getStatus() != PaymentTransactionStatus.SUCCEEDED) {
                String mismatch = amountMismatch(transaction, payload);
                if (mismatch != null) {
                    holdForReview(transaction, depositId, mismatch);
                    return;
                }
            }
            handleCompleted(transaction, depositId);
            depositService.markCompleted(depositId, payload);
        } else if ("FAILED".equals(status) || "REJECTED".equals(status) || "EXPIRED".equals(status)) {
            if (isTerminal(transaction.getStatus())) {
                log.info("Ignoring terminal PawaPay failure callback for depositId {} because transaction is already {}",
                        depositId, transaction.getStatus());
                return;
            }
            handleFailed(transaction, payload);
        } else {
            log.warn("Unexpected PawaPay callback status '{}' for depositId {}", payload.status(), depositId);
        }
    }

    /**
     * Le depot designe par le rappel · par identifiant, jamais par ressemblance.
     *
     * <p>On cherchait auparavant, a defaut d'identifiant connu, le dernier depot du meme numero
     * pour le meme montant. Un rappel forge avec un numero et un montant plausibles encaissait
     * donc le depot de quelqu'un d'autre. Un rappel qui ne designe rien ne fait plus rien.</p>
     */
    private PaymentTransaction resolveTransaction(PawapayCallbackPayload payload) {
        return resolveTransaction(payload.depositId(), payload.clientReferenceId())
                .map(transaction -> ensureProviderReference(transaction, payload.depositId()))
                .orElse(null);
    }

    private Optional<PaymentTransaction> resolveTransaction(String depositId, String clientReferenceId) {
        return transactionRepository.findByProviderReference(depositId)
                .or(() -> findDepositTransaction(depositId, clientReferenceId))
                .or(() -> clientReferenceId == null || clientReferenceId.isBlank()
                        ? Optional.empty()
                        : transactionRepository.findByTransactionNumber(clientReferenceId.trim()));
    }

    private Optional<PaymentTransaction> findDepositTransaction(String depositId, String clientReferenceId) {
        Optional<PaymentTransaction> byDepositId = depositRepository.findByDepositId(depositId)
                .map(deposit -> deposit.getPaymentTransaction());
        if (byDepositId.isPresent()) {
            return byDepositId;
        }
        if (clientReferenceId == null || clientReferenceId.isBlank()) {
            return Optional.empty();
        }
        return depositRepository.findByClientReferenceId(clientReferenceId.trim())
                .map(deposit -> deposit.getPaymentTransaction());
    }

    private PaymentTransaction ensureProviderReference(PaymentTransaction transaction, String depositId) {
        if (transaction.getProviderReference() == null || transaction.getProviderReference().isBlank()) {
            transaction.setProviderReference(depositId);
            return transactionRepository.save(transaction);
        }
        return transaction;
    }

    private void handleCompleted(PaymentTransaction transaction, String depositId) {
        if (transaction.getStatus() != PaymentTransactionStatus.SUCCEEDED) {
            Instant paidAt = Instant.now();
            transaction.setStatus(PaymentTransactionStatus.SUCCEEDED);
            transaction.setPaidAt(paidAt);
            if (transaction.getReceiptNumber() == null || transaction.getReceiptNumber().isBlank()) {
                transaction.setReceiptNumber(sequenceGenerator.next("receipt"));
            }
            transaction.setReceiptIssuedAt(transaction.getReceiptIssuedAt() == null ? paidAt : transaction.getReceiptIssuedAt());
            if (transaction.getProviderReference() == null || transaction.getProviderReference().isBlank()) {
                transaction.setProviderReference(depositId);
            }
            transactionRepository.save(transaction);
            try {
                creditOverpayment(transaction, allocationService.allocateIfBillingDocument(transaction));
            } catch (Exception ex) {
                // On ne fait pas echouer l'encaissement : l'argent est bien recu et la transaction
                // doit passer SUCCEEDED. Mais l'echec d'imputation laissait auparavant la facture
                // impayee sans aucun rattrapage - le client avait paye et relancait quand meme.
                // On le transforme en evenement outbox rejouable avec backoff.
                log.error("Echec d'imputation du paiement {} · rattrapage programme via l'outbox",
                        transaction.getTransactionNumber(), ex);
                outboxService.publish(
                        "PAYMENT_ALLOCATION_RETRY",
                        "PAYMENT",
                        transaction.getTransactionNumber(),
                        java.util.Map.of(
                                "transactionNumber", transaction.getTransactionNumber(),
                                "reason", String.valueOf(ex.getMessage()))
                );
            }
            outboxService.publish(
                    "PAYMENT_TRANSACTION_WORKFLOW",
                    "PAYMENT",
                    transaction.getTransactionNumber(),
                    java.util.Map.of("transactionNumber", transaction.getTransactionNumber(), "status", PaymentTransactionStatus.SUCCEEDED.name())
            );
            // Le client a paye depuis son espace, seul · c'est ici, a la confirmation de
            // l'operateur, que la caisse peut l'apprendre. Le canal a ete retenu a l'initiation.
            selfServiceNotifier.paymentSettled(transaction);
        }

        reconcileIntent(transaction);

        log.info("PawaPay deposit completed · transaction={}, depositId={}",
                transaction.getTransactionNumber(), depositId);
    }

    /**
     * L'operateur annonce-t-il un autre montant, ou une autre devise, que ce qu'on lui a demande ?
     *
     * <p>Un depot se confirme sur son identifiant · le corps du rappel, ou la relecture de statut,
     * dit aussi combien a ete preleve. On le croyait sans regarder : un rappel forge ou une anomalie
     * chez l'operateur soldait une facture de 50 000 XAF pour 10 XAF recus. Un montant absent ne
     * prouve rien et ne bloque rien ; un montant different bloque.</p>
     */
    static String amountMismatch(PaymentTransaction transaction, PawapayCallbackPayload payload) {
        if (StringUtils.hasText(payload.amount()) && transaction.getAmount() != null) {
            BigDecimal reported;
            try {
                reported = new BigDecimal(payload.amount().trim());
            } catch (NumberFormatException ex) {
                return "montant annonce illisible (" + payload.amount() + ")";
            }
            if (reported.compareTo(transaction.getAmount()) != 0) {
                return "montant annonce " + reported.toPlainString() + ", attendu "
                        + transaction.getAmount().stripTrailingZeros().toPlainString();
            }
        }
        if (StringUtils.hasText(payload.currency()) && StringUtils.hasText(transaction.getCurrency())
                && !payload.currency().trim().equalsIgnoreCase(transaction.getCurrency())) {
            return "devise annoncee " + payload.currency().trim() + ", attendue " + transaction.getCurrency();
        }
        return null;
    }

    /**
     * Ni encaisse ni refuse · mis de cote pour qu'un agent tranche avec le releve de l'operateur.
     *
     * <p>Declarer l'echec ferait repayer un client peut-etre preleve ; declarer le succes solderait
     * la facture pour un montant qui n'est pas le bon. La transaction reste en cours, le depot passe
     * a rapprocher, et les super-administrateurs sont prevenus.</p>
     */
    private void holdForReview(PaymentTransaction transaction, String depositId, String mismatch) {
        log.error("Depot PawaPay {} confirme avec un ecart · {} · transaction {} laissee en cours, a rapprocher",
                depositId, mismatch, transaction.getTransactionNumber());
        PawapayDeposit deposit = depositService.markAmountMismatch(depositId, mismatch);
        if (deposit == null) {
            return;
        }
        for (String recipient : alertRecipients.recipients()) {
            java.util.Map<String, Object> alert = new java.util.HashMap<>();
            alert.put("adminEmail", recipient);
            alert.put("templateCode", "MOBILE_MONEY_DEPOSIT_UNRESOLVED");
            alert.put("anomaly", "L'opérateur a confirmé ce paiement avec un écart : " + mismatch + ".");
            alert.put("depositId", deposit.getDepositId());
            alert.put("intentNumber", deposit.getIntentNumber());
            alert.put("transactionNumber", deposit.getTransactionNumber());
            alert.put("phoneNumber", deposit.getPhoneNumber());
            alert.put("provider", deposit.getProvider());
            alert.put("amount", deposit.getAmount() == null ? "" : deposit.getAmount().toPlainString());
            alert.put("currency", deposit.getCurrency());
            outboxService.publish("MOBILE_MONEY_DEPOSIT_UNRESOLVED", "PAYMENT",
                    deposit.getDepositId() + ":mismatch:" + recipient, alert);
        }
        clientNotifier.depositPendingReview(deposit);
    }

    /**
     * Ce qui reste d'un paiement de facture une fois la facture soldee · credite au portefeuille.
     *
     * <p>Un depot laisse « a rapprocher » finit parfois par aboutir alors que le client a deja paye
     * autrement. L'imputation ne prend que le solde du, et le reste disparaissait : le client etait
     * preleve sans contrepartie. Il est desormais porte a son portefeuille, une seule fois. Les
     * rechargements et les autres sources ne passent pas par ici · leur montant entier est leur
     * objet, et ils sont credites ailleurs.</p>
     */
    private void creditOverpayment(PaymentTransaction transaction, BigDecimal remainder) {
        PaymentIntent intent = transaction.getPaymentIntent();
        if (remainder == null || remainder.signum() <= 0 || !billingSource(intent)) {
            return;
        }
        var wallet = walletService.getOrCreate(intent.getCustomerType(), intent.getCustomerCode(), intent.getCurrency());
        walletService.credit(walletService.serviceWallet(wallet.walletNumber()), remainder,
                com.sni.bokaticowork.features.payment.enums.WalletEntryType.OVERPAYMENT_CREDIT, "PAYMENT_TRANSACTION",
                transaction.getTransactionNumber(), "OVERPAYMENT", "SYSTEM",
                "OVERPAYMENT:" + transaction.getTransactionNumber());
        log.warn("Trop-percu de {} {} sur la transaction {} · credite au portefeuille {}",
                remainder.toPlainString(), intent.getCurrency(), transaction.getTransactionNumber(), wallet.walletNumber());
    }

    static boolean billingSource(PaymentIntent intent) {
        String source = intent == null || intent.getSourceType() == null ? "" : intent.getSourceType().trim().toUpperCase();
        return source.equals("BILLING_DOCUMENT") || source.equals("MULTI_BILLING_DOCUMENT") || source.equals("PAYABLE_RECOVERY");
    }

    private void reconcileIntent(PaymentTransaction transaction) {
        // flush first · sumSucceededAmountByPaymentIntentId is a native query and Hibernate
        // does not auto-flush before native queries, so without this the just-saved
        // SUCCEEDED transaction is invisible to the SUM and the intent lands on PENDING.
        transactionRepository.flush();
        PaymentIntent intent = transaction.getPaymentIntent();
        BigDecimal paidAmount = transactionRepository.sumSucceededAmountByPaymentIntentId(intent.getId());
        BigDecimal processingAmount = transactionRepository.sumProcessingAmountByPaymentIntentId(intent.getId());
        if (paidAmount.compareTo(intent.getAmount()) >= 0) {
            intent.setStatus(PaymentIntentStatus.SUCCEEDED);
        } else if (processingAmount.signum() > 0) {
            intent.setStatus(PaymentIntentStatus.PROCESSING);
        } else {
            intent.setStatus(PaymentIntentStatus.PENDING);
        }
        intentRepository.save(intent);
    }

    private void handleFailed(PaymentTransaction transaction, PawapayCallbackPayload payload) {
        String reason = payload.failureReason() != null
                ? payload.failureReason().toString()
                : "Payment failed at operator";

        transaction.setStatus(PaymentTransactionStatus.FAILED);
        transaction.setFailureReason(reason);
        transactionRepository.save(transaction);
        PawapayDeposit deposit = depositService.markFailed(transaction.getProviderReference(), payload, reason);

        // Le refus arrive souvent plusieurs minutes apres que le client a ferme sa page · sans ce
        // message il voyait sa facture rester ouverte sans jamais savoir pourquoi.
        if (deposit != null) {
            clientNotifier.depositFailed(deposit);
        }

        reconcileIntent(transaction);

        // Un essai qui echoue n'annule pas l'intention · le client s'est trompe de code, son solde
        // etait insuffisant, il a refuse par megarde. Annuler l'intention le privait de tout moyen
        // de payer cette facture : elle redevient payable, sauf si son delai est passe.
        PaymentIntent intent = transaction.getPaymentIntent();
        if (intent.getStatus() == PaymentIntentStatus.PROCESSING || intent.getStatus() == PaymentIntentStatus.PENDING) {
            boolean expired = intent.getExpiresAt() != null && intent.getExpiresAt().isBefore(Instant.now());
            intent.setStatus(expired ? PaymentIntentStatus.EXPIRED : PaymentIntentStatus.PENDING);
            intentRepository.save(intent);
            log.info("Intention {} de nouveau {} apres l'echec du depot", intent.getIntentNumber(), intent.getStatus());
        }

        log.info("PawaPay deposit failed · transaction={}, depositId={}, reason={}",
                transaction.getTransactionNumber(), transaction.getProviderReference(), reason);
    }

    private boolean isTerminal(PaymentTransactionStatus status) {
        return status == PaymentTransactionStatus.SUCCEEDED
                || status == PaymentTransactionStatus.FAILED
                || status == PaymentTransactionStatus.CANCELLED
                || status == PaymentTransactionStatus.REVERSED
                || status == PaymentTransactionStatus.REFUNDED;
    }
}
