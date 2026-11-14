package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.booking.dto.request.BookingStatusChangeRequest;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractService;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.SubscriptionStatusChangeRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.PassService;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTransactionWorkflowProcessor {

    private static final String BILLING_DOCUMENT_SOURCE = "BILLING_DOCUMENT";
    private static final String MULTI_BILLING_DOCUMENT_SOURCE = "MULTI_BILLING_DOCUMENT";
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final PaymentTransactionRepository transactionRepository;
    private final com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository billingDocumentRepository;
    private final BillingDocumentService billingDocumentService;
    private final TransactionContextResolver contextResolver;
    private final SubscriptionService subscriptionService;
    private final PassService passService;
    private final BookingService bookingService;
    private final ContractService contractService;
    private final RefundEmailNotifier refundEmailNotifier;
    private final BillingEmailService billingEmailService;
    private final CashRegisterService cashRegisterService;
    private final com.sni.bokaticowork.features.subscription.subscription.service.support.pass.PassRenewalOperator passRenewalOperator;
    private final com.sni.bokaticowork.features.payment.transfer.service.WalletTopUpService walletTopUpService;
    private final @org.springframework.context.annotation.Lazy com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionGraceService graceService;
    private final @org.springframework.context.annotation.Lazy com.sni.bokaticowork.features.marketplace.service.MarketplaceOrderService marketplaceOrderService;
    private final com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository allocationRepository;
    private final @org.springframework.context.annotation.Lazy com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionBillingLinkService billingLinkService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private com.sni.bokaticowork.features.billing.service.support.PaymentWhatsAppNotifier paymentWhatsApp;

    /** WhatsApp facture_payee apres le paiement ; absent, seul le courriel part. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPaymentWhatsApp(com.sni.bokaticowork.features.billing.service.support.PaymentWhatsAppNotifier paymentWhatsApp) {
        this.paymentWhatsApp = paymentWhatsApp;
    }

    @Transactional
    public void process(PaymentTransactionWorkflowEvent event) {
        PaymentTransaction transaction = transactionRepository.findByTransactionNumber(event.transactionNumber())
                .orElse(null);
        if (transaction == null) {
            log.warn("Ignoring asynchronous payment workflow for missing transaction {}", event.transactionNumber());
            return;
        }

        PaymentIntent intent = transaction.getPaymentIntent();
        if (event.status() == PaymentTransactionStatus.SUCCEEDED) {
            // Business logic first · in order of dependency
            // Un rechargement de portefeuille se regle ici et non dans le rappel de l'operateur :
            // ce flux est rejoue par la boite de sortie en cas d'echec, le rappel ne l'est pas.
            // L'ecriture est idempotente, un rejeu ne credite pas deux fois.
            walletTopUpService.settle(transaction);
            recordAutomaticCashSession(transaction);
            validatePaidBillingDocuments(transaction);
            activateSubscriptionsOnLinkedInvoices(transaction);
            TransactionContextResolver.SourceView source = contextResolver.resolveSource(intent.getSourceType(), intent.getSourceCode());
            if (StringUtils.hasText(source.type()) && StringUtils.hasText(source.code())) {
                handleSucceededTransaction(transaction, source);
            }
            // Notifications only after all business logic is complete
            sendPaidBillingDocumentEmails(transaction);
            // Parrainage : traite apres la validation du paiement, jamais dans sa transaction
            eventPublisher.publishEvent(new com.sni.bokaticowork.features.subscription.promotion.pricing.referral.event.RefereePaidEvent(
                    intent.getCustomerType(), intent.getCustomerCode(), transaction.getTransactionNumber()));
            return;
        }
        if (event.status() == PaymentTransactionStatus.REFUNDED || event.status() == PaymentTransactionStatus.REVERSED) {
            // Business logic first · seul un remboursement total annule les factures et ce
            // qu'elles reglaient. Un remboursement partiel ne fait que prevenir le client.
            TransactionContextResolver.SourceView source = contextResolver.resolveSource(intent.getSourceType(), intent.getSourceCode());
            if (event.fullRefund() && StringUtils.hasText(source.type()) && StringUtils.hasText(source.code())) {
                handleRefundedTransaction(transaction, source);
            }
            // Notification after business logic
            try {
                refundEmailNotifier.notify(transaction);
            } catch (Exception ex) {
                log.warn("Failed to send refund email for transaction {} · refund workflow already completed",
                        transaction.getTransactionNumber(), ex);
            }
        }
    }

    /** Un abonnement rattache a une facture existante est valide quand ce paiement la solde. */
    private void activateSubscriptionsOnLinkedInvoices(PaymentTransaction transaction) {
        try {
            allocationRepository.findAllByPaymentTransactionId(transaction.getId()).stream()
                    .map(com.sni.bokaticowork.features.payment.model.PaymentAllocation::getBillingDocumentNumber)
                    .filter(StringUtils::hasText).distinct()
                    .forEach(number -> billingLinkService.activateIfLinkedInvoicePaid(number, SYSTEM_ACTOR));
        } catch (Exception ex) {
            log.warn("Validation des abonnements rattaches apres le paiement {} en echec", transaction.getTransactionNumber(), ex);
        }
    }

    private void recordAutomaticCashSession(PaymentTransaction transaction) {
        try {
            cashRegisterService.recordAutomaticPayment(transaction);
        } catch (Exception ex) {
            log.error("Failed to record automatic cash session movement for transaction {}", transaction.getTransactionNumber(), ex);
        }
    }

    private void validatePaidBillingDocuments(PaymentTransaction transaction) {
        PaymentIntent intent = transaction.getPaymentIntent();
        String sourceType = intent.getSourceType();
        if (!BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(sourceType) && !MULTI_BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(sourceType)) {
            return;
        }
        if (intent.getStatus() != PaymentIntentStatus.SUCCEEDED) {
            return;
        }
        splitCodes(intent.getSourceCode()).forEach(documentNumber ->
                sefcValidateDocument(documentNumber, transaction.getTransactionNumber()));
    }

    private void sendPaidBillingDocumentEmails(PaymentTransaction transaction) {
        PaymentIntent intent = transaction.getPaymentIntent();
        String sourceType = intent.getSourceType();
        if (!BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(sourceType) && !MULTI_BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(sourceType)) {
            return;
        }
        List<String> documentNumbers = splitCodes(intent.getSourceCode());
        documentNumbers.forEach(documentNumber -> {
            notifyPaidByWhatsApp(documentNumber, documentNumbers.size() == 1 ? transaction.getAmount() : null, transaction.getTransactionNumber());
            try {
                // Envoi automatique apres paiement · se coupe si le membre a refuse la confirmation de paiement
                if (billingEmailService.sendDocumentIfAllowed(documentNumber,
                        com.sni.bokaticowork.core.enums.EventType.PAYMENT_RECEIVED)) {
                    log.info("Sent billing document {} after payment {}", documentNumber, transaction.getTransactionNumber());
                }
            } catch (Exception ex) {
                log.warn("Failed to send billing document {} after payment {}", documentNumber, transaction.getTransactionNumber(), ex);
            }
        });
    }

    /** Facture soldee : WhatsApp au client, apres la validation de la transaction. */
    private void notifyPaidByWhatsApp(String documentNumber, java.math.BigDecimal paid, String transactionNumber) {
        if (paymentWhatsApp == null) {
            return;
        }
        try {
            paymentWhatsApp.notify(billingDocumentService.get(documentNumber), paid);
        } catch (Exception ex) {
            log.warn("WhatsApp de la facture {} apres le paiement {} non envoye : {}", documentNumber, transactionNumber, ex.getMessage());
        }
    }

    private void sefcValidateDocument(String documentNumber, String transactionNumber) {
        try {
            billingDocumentService.validate(documentNumber);
            log.info("SEFC validated document {} after full settlement of payment {}", documentNumber, transactionNumber);
        } catch (Exception ex) {
            log.warn("SEFC auto-validation of {} after payment {} skipped · {}", documentNumber, transactionNumber, ex.getMessage());
        }
    }

    private void handleSucceededTransaction(PaymentTransaction transaction, TransactionContextResolver.SourceView source) {
        if ("SUBSCRIPTION".equalsIgnoreCase(source.type())) {
            Subscription subscription = subscriptionService.getForService(source.code());
            // La facture de renouvellement est soldee · la periode payee commence ici.
            if (subscriptionService.applyPaidRenewal(source.code(), "Paiement " + transaction.getTransactionNumber())) {
                return;
            }
            if (subscription.getStatus() == SubscriptionStatus.GRACE_PERIOD) {
                // Un paiement pendant la tolerance rend les droits sans intervention
                graceService.exit(subscription, "Paiement " + transaction.getTransactionNumber());
                realignIfPaidLate(transaction, subscription);
                return;
            }
            if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                // Deja actif · seule la periode peut avoir a bouger, si l'echeance a ete payee
                // en retard. La duree payee doit etre la duree recue.
                realignIfPaidLate(transaction, subscription);
                return;
            }
            if (subscription.getStatus() != SubscriptionStatus.PENDING_ACTIVATION) {
                return;
            }
            if (!isSubscriptionPaymentSettled(transaction, subscription)) {
                return;
            }
            subscriptionService.activate(source.code(), new SubscriptionStatusChangeRequest(
                    "Activation automatique apres paiement " + transaction.getTransactionNumber(),
                    SYSTEM_ACTOR,
                    Boolean.FALSE
            ));
            return;
        }

        if ("PASS".equalsIgnoreCase(source.type())) {
            Pass pass = passService.getForService(source.code());
            if (pass.getStatus() == PassStatus.PENDING_ACTIVATION) {
                passService.activate(source.code(),
                        "Activation après paiement " + transaction.getTransactionNumber());
            }
            return;
        }

        if ("PASS_RENEWAL".equalsIgnoreCase(source.type())) {
            Pass pass = passService.getForService(source.code());
            passRenewalOperator.handleRenewalPaymentSucceeded(pass, transaction.getTransactionNumber());
            return;
        }

        if ("BOOKING".equalsIgnoreCase(source.type())) {
            confirmBookingIfFullyPaid(transaction, source.code());
            return;
        }

        // Facture d'un lot multi-ressources : chacune des reservations qu'elle regle est confirmee.
        if (TransactionContextResolver.BOOKINGS_SOURCE.equalsIgnoreCase(source.type())) {
            bookingNumbers(source.code()).forEach(number -> confirmBookingIfFullyPaid(transaction, number));
            return;
        }

        if (com.sni.bokaticowork.features.marketplace.service.MarketplaceOrderService.SOURCE_TYPE.equalsIgnoreCase(source.type())) {
            syncMarketplaceOrderAfterCommit(source.code());
        }
    }

    /**
     * La commande lit le solde de sa facture · elle le fait une fois ce reglement valide, dans sa
     * propre transaction, pour qu'un incident sur la commande ne defasse pas le paiement. Le passage
     * periodique rattrape ce qui manquerait ici.
     */
    private void syncMarketplaceOrderAfterCommit(String orderNumber) {
        if (marketplaceOrderService == null) {
            return;
        }
        Runnable sync = () -> {
            try {
                marketplaceOrderService.sync(orderNumber);
            } catch (Exception ex) {
                log.warn("Marketplace order {} not synced after payment · retried by the worker: {}", orderNumber, ex.getMessage());
            }
        };
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            sync.run();
            return;
        }
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        sync.run();
                    }
                });
    }

    private static java.util.List<String> bookingNumbers(String codes) {
        return java.util.Arrays.stream(codes.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private void confirmBookingIfFullyPaid(PaymentTransaction transaction, String bookingNumber) {
        if (transaction.getPaymentIntent().getStatus() != PaymentIntentStatus.SUCCEEDED) {
            log.debug("Booking {} payment partially received · awaiting full settlement before auto-confirm", bookingNumber);
            return;
        }
        // Une reservation reglee au portefeuille est confirmee des le blocage des fonds : son debit
        // arrive donc sur une reservation deja confirmee, voire deja terminee. Sans ce garde-fou,
        // chacun de ces debits produirait un avertissement pour un conflit de statut attendu.
        BookingStatus status = bookingService.get(bookingNumber).status();
        if (status != BookingStatus.DRAFT && status != BookingStatus.PENDING_PAYMENT) {
            log.debug("Booking {} already past confirmation ({}) · payment {} recorded without status change",
                    bookingNumber, status, transaction.getTransactionNumber());
            return;
        }
        try {
            bookingService.confirm(bookingNumber, new BookingStatusChangeRequest(
                    "Confirmation automatique suite au paiement " + transaction.getTransactionNumber(),
                    SYSTEM_ACTOR,
                    Boolean.TRUE
            ));
            log.info("Booking {} auto-confirmed after payment {}", bookingNumber, transaction.getTransactionNumber());
        } catch (Exception ex) {
            log.warn("Auto-confirmation of booking {} after payment {} skipped · {}",
                    bookingNumber, transaction.getTransactionNumber(), ex.getMessage());
        }
    }

    private void handleRefundedTransaction(PaymentTransaction transaction, TransactionContextResolver.SourceView source) {
        String reason = automaticRefundReason(transaction);
        cancelAndArchiveLinkedBillingDocuments(transaction.getPaymentIntent(), reason);
        String sourceType = source.type().trim().toUpperCase(Locale.ROOT);

        switch (sourceType) {
            case "SUBSCRIPTION" -> {
                Subscription subscription = subscriptionService.getForService(source.code());
                if (subscription.getStatus() != SubscriptionStatus.CANCELLED) {
                    subscriptionService.cancel(source.code(), new SubscriptionStatusChangeRequest(reason, SYSTEM_ACTOR, Boolean.FALSE));
                }
                closeContractIfPresent(subscription.getContractCode(), reason);
            }
            case "PASS" -> {
                Pass pass = passService.getForService(source.code());
                if (pass.getStatus() != PassStatus.CANCELLED) {
                    passService.cancel(source.code(), reason);
                }
                closeContractIfPresent(pass.getContractCode(), reason);
            }
            case "BOOKING" -> bookingService.systemCancel(source.code(), reason);
            case TransactionContextResolver.BOOKINGS_SOURCE ->
                    bookingNumbers(source.code()).forEach(number -> bookingService.systemCancel(number, reason));
            default -> log.debug("No asynchronous refund workflow configured for source {} {}", source.type(), source.code());
        }
    }

    /**
     * Une echeance payee en retard fait repartir la periode du jour du reglement.
     *
     * <p>Le renouvellement avance la periode des qu'il facture · un client qui paie dix jours plus
     * tard recevait donc une periode commencee dix jours plus tot. Il payait un mois et en
     * recevait vingt jours.</p>
     *
     * <p>Seul un reglement complet recale la periode : sur un paiement partiel, l'echeance n'est
     * pas honoree et il n'y a rien a reancrer.</p>
     */
    private void realignIfPaidLate(PaymentTransaction transaction, Subscription subscription) {
        try {
            if (!isSubscriptionPaymentSettled(transaction, subscription)) {
                return;
            }
            Instant paidAt = transaction.getPaidAt() == null ? Instant.now() : transaction.getPaidAt();
            LocalDate paidOn = LocalDate.ofInstant(paidAt, ZoneId.systemDefault());
            subscriptionService.realignOnLatePayment(subscription.getSubscriptionNumber(), paidOn);
        } catch (Exception ex) {
            // L'argent est recu · un recalage impossible ne doit pas defaire l'encaissement.
            log.warn("Abonnement {} · recalage de periode apres paiement {} impossible",
                    subscription.getSubscriptionNumber(), transaction.getTransactionNumber(), ex);
        }
    }

    private boolean isSubscriptionPaymentSettled(PaymentTransaction transaction, Subscription subscription) {
        PaymentIntent intent = transaction.getPaymentIntent();
        String sourceType = intent.getSourceType();
        if (BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(sourceType) || MULTI_BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(sourceType)) {
            List<BillingDocument> relatedDocuments = billingDocumentsForIntent(intent).stream()
                    .filter(document -> sourceMatchesSubscription(document, subscription.getSubscriptionNumber()))
                    .toList();
            return !relatedDocuments.isEmpty()
                    && relatedDocuments.stream()
                    .allMatch(document -> document.getBalanceDue() != null && document.getBalanceDue().signum() <= 0);
        }
        return transaction.getAmount() != null
                && subscription.getTotalAmount() != null
                && transaction.getAmount().compareTo(subscription.getTotalAmount()) >= 0;
    }

    private boolean sourceMatchesSubscription(BillingDocument document, String subscriptionNumber) {
        TransactionContextResolver.SourceView source = contextResolver.resolveBillingDocumentSource(document);
        return "SUBSCRIPTION".equalsIgnoreCase(source.type())
                && subscriptionNumber.equalsIgnoreCase(source.code());
    }

    private List<com.sni.bokaticowork.features.billing.model.BillingDocument> billingDocumentsForIntent(PaymentIntent intent) {
        return splitCodes(intent.getSourceCode()).stream()
                .map(billingDocumentRepository::findByDocumentNumber)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private List<String> splitCodes(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private void closeContractIfPresent(String contractCode, String reason) {
        if (!StringUtils.hasText(contractCode)) {
            return;
        }
        try {
            Contract contract = contractService.serviceByCode(contractCode.trim());
            if (contract.getStatus() == ContractStatus.CANCELLED
                    || contract.getStatus() == ContractStatus.TERMINATED
                    || contract.getStatus() == ContractStatus.EXPIRED) {
                return;
            }
            if (contract.getStatus() == ContractStatus.ACTIVE || contract.getStatus() == ContractStatus.SUSPENDED) {
                contractService.terminate(contract.getContractCode(), reason);
                return;
            }
            contractService.cancel(contract.getContractCode(), reason);
        } catch (Exception ex) {
            log.warn("Failed to close contract {} after refund workflow", contractCode, ex);
        }
    }

    private void cancelAndArchiveLinkedBillingDocuments(PaymentIntent intent, String reason) {
        String sourceType = intent.getSourceType();
        if (BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(sourceType) || MULTI_BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(sourceType)) {
            splitCodes(intent.getSourceCode()).forEach(documentNumber -> {
                try {
                    billingDocumentService.cancelAndArchive(documentNumber, reason);
                } catch (Exception ex) {
                    log.warn("Failed to cancel and archive billing document {}", documentNumber, ex);
                }
            });
        }
    }

    private String automaticRefundReason(PaymentTransaction transaction) {
        String status = transaction.getStatus() == null ? "REFUND" : transaction.getStatus().name();
        String label = status.equals("REVERSED") ? "annulation" : "remboursement";
        return "Annulation automatique suite au " + label + " du paiement " + transaction.getTransactionNumber();
    }
}
