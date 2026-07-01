package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.booking.dto.request.BookingStatusChangeRequest;
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
            recordAutomaticCashSession(transaction);
            sendPaidBillingDocuments(transaction);
            TransactionContextResolver.SourceView source = contextResolver.resolveSource(intent.getSourceType(), intent.getSourceCode());
            if (!StringUtils.hasText(source.type()) || !StringUtils.hasText(source.code())) {
                return;
            }
            handleSucceededTransaction(transaction, source);
            return;
        }
        if (event.status() == PaymentTransactionStatus.REFUNDED || event.status() == PaymentTransactionStatus.REVERSED) {
            TransactionContextResolver.SourceView source = contextResolver.resolveSource(intent.getSourceType(), intent.getSourceCode());
            if (StringUtils.hasText(source.type()) && StringUtils.hasText(source.code())) {
                handleRefundedTransaction(transaction, source);
            }
            try {
                refundEmailNotifier.notify(transaction);
            } catch (Exception ex) {
                log.warn("Failed to send refund email for transaction {} — refund workflow already completed",
                        transaction.getTransactionNumber(), ex);
            }
        }
    }

    private void recordAutomaticCashSession(PaymentTransaction transaction) {
        try {
            cashRegisterService.recordAutomaticPayment(transaction);
        } catch (Exception ex) {
            log.error("Failed to record automatic cash session movement for transaction {}", transaction.getTransactionNumber(), ex);
        }
    }

    private void sendPaidBillingDocuments(PaymentTransaction transaction) {
        PaymentIntent intent = transaction.getPaymentIntent();
        String sourceType = intent.getSourceType();
        if (!BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(sourceType) && !MULTI_BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(sourceType)) {
            return;
        }
        boolean fullySettled = intent.getStatus() == PaymentIntentStatus.SUCCEEDED;
        splitCodes(intent.getSourceCode()).forEach(documentNumber -> {
            if (fullySettled) {
                sefcValidateDocument(documentNumber, transaction.getTransactionNumber());
            }
            try {
                if (billingEmailService.sendDocument(documentNumber)) {
                    log.info("Sent billing document {} after payment {}", documentNumber, transaction.getTransactionNumber());
                }
            } catch (Exception ex) {
                log.warn("Failed to send billing document {} after payment {}", documentNumber, transaction.getTransactionNumber(), ex);
            }
        });
    }

    private void sefcValidateDocument(String documentNumber, String transactionNumber) {
        try {
            billingDocumentService.validate(documentNumber);
            log.info("SEFC validated document {} after full settlement of payment {}", documentNumber, transactionNumber);
        } catch (Exception ex) {
            log.warn("SEFC auto-validation of {} after payment {} skipped — {}", documentNumber, transactionNumber, ex.getMessage());
        }
    }

    private void handleSucceededTransaction(PaymentTransaction transaction, TransactionContextResolver.SourceView source) {
        if ("SUBSCRIPTION".equalsIgnoreCase(source.type())) {
            Subscription subscription = subscriptionService.getForService(source.code());
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
        }
    }

    private void confirmBookingIfFullyPaid(PaymentTransaction transaction, String bookingNumber) {
        if (transaction.getPaymentIntent().getStatus() != PaymentIntentStatus.SUCCEEDED) {
            log.debug("Booking {} payment partially received — awaiting full settlement before auto-confirm", bookingNumber);
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
            log.warn("Auto-confirmation of booking {} after payment {} skipped — {}",
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
            default -> log.debug("No asynchronous refund workflow configured for source {} {}", source.type(), source.code());
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
