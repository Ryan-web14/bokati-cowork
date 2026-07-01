package com.sni.bokaticowork.features.payment.service.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.features.billing.dto.request.CreateInvoiceFromBillableItemsRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentForDocumentsRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentRecoveryIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.request.PayInvoiceRequest;
import com.sni.bokaticowork.features.payment.dto.request.RefundPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.request.RegisterCashPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyInitiationRequest;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyInitiationResponse;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyPaymentProvider;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyRefundRequest;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyRefundResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import com.sni.bokaticowork.features.payment.dto.response.PayInvoiceResponse;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentRecoveryResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.repository.specification.criteria.PaymentIntentSearchCriteria;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayDepositService;
import com.sni.bokaticowork.features.payment.service.support.PaymentAllocationService;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.BillableItemResponse;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionBillingMapper;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String BILLING_DOCUMENT_SOURCE = "BILLING_DOCUMENT";
    private static final String MULTI_BILLING_DOCUMENT_SOURCE = "MULTI_BILLING_DOCUMENT";
    private static final String RECOVERY_SOURCE = "PAYABLE_RECOVERY";

    private final PaymentIntentRepository intentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final BillingDocumentRepository billingDocumentRepository;
    private final BillableItemRepository billableItemRepository;
    private final BillingDocumentService billingDocumentService;
    private final WalletService walletService;
    private final CashRegisterService cashRegisterService;
    private final PaymentAllocationService allocationService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final PaymentMapper mapper;
    private final SubscriptionBillingMapper billingMapper;
    private final ObjectMapper objectMapper;
    private final MobileMoneyPaymentProvider mobileMoneyProvider;
    private final PawapayDepositService pawapayDepositService;
    @org.springframework.context.annotation.Lazy
    private final BillingEmailService billingEmailService;
    private final OutboxService outboxService;

    @Override
    public PaymentIntentResponse createIntent(CreatePaymentIntentRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = intentRepository.findByIdempotencyKey(request.idempotencyKey().trim());
            if (existing.isPresent()) {
                return mapper.toIntentResponse(existing.get());
            }
        }
        String normalizedCustomerCode = required(request.customerCode(), "Customer code is required");
        String normalizedCustomerType = normalizeCustomerTypeRequired(request.customerType(), normalizedCustomerCode);
        validatePositive(request.amount());
        String customerCtx = CodeComposer.abbrev(normalizedCustomerType);
        long intentSeq = CodeComposer.extractSeq(sequenceGenerator.next("payment_intent"));
        String intentNumber = CodeComposer.withDay("INT", customerCtx, LocalDate.now(), intentSeq);

        PaymentIntent intent = PaymentIntent.builder()
                .intentNumber(intentNumber)
                .customerType(normalizedCustomerType)
                .customerCode(normalizedCustomerCode)
                .amount(money(request.amount()))
                .currency(request.currency().trim().toUpperCase())
                .status(PaymentIntentStatus.PENDING)
                .purpose(trim(request.purpose()))
                .sourceType(trim(request.sourceType()))
                .sourceCode(trim(request.sourceCode()))
                .idempotencyKey(trim(request.idempotencyKey()))
                .expiresAt(defaultExpiresAt(request.expiresAt()))
                .metadataJson(trim(request.metadataJson()))
                .build();
        return mapper.toIntentResponse(intentRepository.save(intent));
    }

    @Override
    public PaymentIntentResponse createIntentFromBillingDocument(CreatePaymentIntentFromBillingDocumentRequest request) {
        BillingDocument document = billingDocumentService.serviceByNumber(request.documentNumber());
        if (document.getBalanceDue().signum() <= 0) {
            throw new BadRequestException("Billing document has no balance due");
        }
        BigDecimal amount = requestedBillingAmount(request.amount(), document.getBalanceDue());
        PaymentIntent reusable = findReusableBillingIntent(
                document.getCustomerType(),
                document.getCustomerCode(),
                BILLING_DOCUMENT_SOURCE,
                document.getDocumentNumber(),
                amount
        );
        if (reusable != null) {
            return mapper.toIntentResponse(reusable);
        }
        return createIntent(new CreatePaymentIntentRequest(
                document.getCustomerType(),
                document.getCustomerCode(),
                amount,
                document.getCurrency(),
                "BILLING_DOCUMENT_PAYMENT",
                BILLING_DOCUMENT_SOURCE,
                document.getDocumentNumber(),
                request.idempotencyKey(),
                request.expiresAt(),
                request.metadataJson()
        ));
    }

    @Override
    public PaymentIntentResponse createIntentForBillingDocuments(CreatePaymentIntentForDocumentsRequest request) {
        BillingDocument first = null;
        BigDecimal total = BigDecimal.ZERO;
        List<String> payableDocumentNumbers = new ArrayList<>();
        for (String documentNumber : request.documentNumbers()) {
            BillingDocument document = billingDocumentService.serviceByNumber(documentNumber);
            if (document.getBalanceDue().signum() <= 0) {
                continue;
            }
            if (first == null) {
                first = document;
            } else {
                if (!first.getCustomerType().equalsIgnoreCase(document.getCustomerType())
                        || !first.getCustomerCode().equalsIgnoreCase(document.getCustomerCode())
                        || !first.getCurrency().equalsIgnoreCase(document.getCurrency())) {
                    throw new BadRequestException("All billing documents must have the same customer and currency");
                }
            }
            total = total.add(document.getBalanceDue());
            payableDocumentNumbers.add(document.getDocumentNumber());
        }
        if (first == null || total.signum() <= 0) {
            throw new BadRequestException("No payable billing document found");
        }
        BigDecimal amount = requestedBillingAmount(request.amount(), total);
        String sourceCode = String.join(",", payableDocumentNumbers);
        PaymentIntent reusable = findReusableBillingIntent(
                first.getCustomerType(),
                first.getCustomerCode(),
                MULTI_BILLING_DOCUMENT_SOURCE,
                sourceCode,
                amount
        );
        if (reusable != null) {
            return mapper.toIntentResponse(reusable);
        }
        return createIntent(new CreatePaymentIntentRequest(
                first.getCustomerType(),
                first.getCustomerCode(),
                amount,
                first.getCurrency(),
                "MULTI_BILLING_DOCUMENT_PAYMENT",
                MULTI_BILLING_DOCUMENT_SOURCE,
                sourceCode,
                request.idempotencyKey(),
                request.expiresAt(),
                request.metadataJson()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentRecoveryResponse previewRecovery(String customerType, String customerCode, String currency) {
        RecoveryContext context = buildRecoveryContext(customerType, customerCode, currency, false);
        return toRecoveryResponse(context, null);
    }

    @Override
    public PaymentRecoveryResponse createRecoveryIntent(CreatePaymentRecoveryIntentRequest request) {
        RecoveryContext context = buildRecoveryContext(request.customerType(), request.customerCode(), request.currency(), true);
        if (context.documents().isEmpty()) {
            throw new BadRequestException("No payable item found for this customer");
        }

        BigDecimal total = context.openDocumentAmount();
        if (total.signum() <= 0) {
            throw new BadRequestException("No payable amount found for this customer");
        }

        String fingerprint = fingerprint(context.customerType(), context.customerCode(), context.currency(), context.documents());
        PaymentIntent reusable = findReusableRecoveryIntent(context.customerType(), context.customerCode(), fingerprint, total);
        if (reusable != null) {
            return toRecoveryResponse(context, mapper.toIntentResponse(reusable));
        }

        PaymentIntentResponse created = createIntent(buildRecoveryIntentRequest(context, fingerprint, request));
        return toRecoveryResponse(context, created);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BillingDocumentResponse> listPayableDocuments(BillingDocumentType documentType,
                                                                           String customerType,
                                                                           String customerCode,
                                                                           String lineSourceType,
                                                                           String lineSourceCode,
                                                                           String searchText,
                                                                           Pageable pageable) {
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return new PaginatedResponse<>(billingDocumentRepository.searchPayable(
                documentType == null ? null : documentType.name(),
                normalizeCustomerType(customerType, customerCode),
                trim(customerCode),
                trim(lineSourceType),
                trim(lineSourceCode),
                trim(searchText),
                unsortedPageable
        ).map(document -> billingDocumentService.get(document.getDocumentNumber())));
    }

    @Override
    public PaymentTransactionResponse registerCashPayment(String intentNumber, RegisterCashPaymentRequest request) {
        PaymentIntent intent = pendingIntent(intentNumber);
        if (!"XAF".equalsIgnoreCase(intent.getCurrency())) {
            throw new BadRequestException("Cash payments are only allowed in XAF");
        }
        BigDecimal paymentAmount = requestedIntentAmount(request.amount(), intent);
        PaymentTransaction transaction = saveSucceededTransaction(intent, PaymentMethod.CASH, "CASH", request.providerReference(), request.receivedBy(), request.metadataJson(), paymentAmount);
        if (StringUtils.hasText(request.cashSessionNumber())) {
            cashRegisterService.recordPayment(request.cashSessionNumber(), transaction.getAmount(), transaction.getTransactionNumber(), request.receivedBy());
        }
        else{
            throw new BadRequestException("Cash session number is required for cash payments");
        }
        reconcileIntent(intent);
        creditOverpayment(intent, allocationService.allocateIfBillingDocument(transaction), request.receivedBy());
        publishTransactionWorkflow(transaction.getTransactionNumber(), PaymentTransactionStatus.SUCCEEDED);
        return mapper.toTransactionResponse(transaction);
    }

    @Override
    public PaymentTransactionResponse payWithWallet(String intentNumber, WalletPaymentRequest request) {
        PaymentIntent intent = pendingIntent(intentNumber);
        WalletAccount wallet = walletService.serviceWallet(request.walletNumber());
        String normalizedWalletOwnerType = normalizeCustomerType(wallet.getOwnerType(), wallet.getOwnerCode());
        if (normalizedWalletOwnerType == null
                || !normalizedWalletOwnerType.equalsIgnoreCase(intent.getCustomerType())
                || !wallet.getOwnerCode().equalsIgnoreCase(intent.getCustomerCode())) {
            throw new BadRequestException("Wallet owner does not match payment intent customer");
        }
        if (!wallet.getCurrency().equalsIgnoreCase(intent.getCurrency())) {
            throw new BadRequestException("Wallet currency does not match payment intent currency");
        }
        BigDecimal paymentAmount = requestedIntentAmount(request.amount(), intent);
        walletService.debit(wallet, paymentAmount, WalletEntryType.PAYMENT, "PAYMENT_INTENT", intent.getIntentNumber(), intent.getIntentNumber(), request.createdBy());
        PaymentTransaction transaction = saveSucceededTransaction(intent, PaymentMethod.WALLET, "INTERNAL_WALLET", wallet.getWalletNumber(), request.createdBy(), request.metadataJson(), paymentAmount);
        reconcileIntent(intent);
        creditOverpayment(intent, allocationService.allocateIfBillingDocument(transaction), request.createdBy());
        publishTransactionWorkflow(transaction.getTransactionNumber(), PaymentTransactionStatus.SUCCEEDED);
        return mapper.toTransactionResponse(transaction);
    }

    @Override
    public MobileMoneyDepositResponse initiateMobileMoneyDeposit(String intentNumber, InitiateMobileMoneyDepositRequest request) {
        PaymentIntent intent = pendingIntent(intentNumber);
        BigDecimal depositAmount = requestedIntentAmount(request.amount(), intent);

        String methodCtx = CodeComposer.abbrev("MOBILE_MONEY");
        long txnSeq = CodeComposer.extractSeq(sequenceGenerator.next("payment_transaction"));
        String txnNumber = CodeComposer.withDay("TXN", methodCtx, LocalDate.now(), txnSeq);

        PaymentTransaction transaction = transactionRepository.save(PaymentTransaction.builder()
                .transactionNumber(txnNumber)
                .paymentIntent(intent)
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .provider("PAWAYPAY")
                .amount(depositAmount)
                .currency(intent.getCurrency())
                .status(PaymentTransactionStatus.PROCESSING)
                .metadataJson(buildMobileMoneyMetadata(request))
                .build());

        intent.setStatus(PaymentIntentStatus.PROCESSING);
        intentRepository.save(intent);

        PawapayDeposit deposit = pawapayDepositService.prepareDeposit(intent, transaction, request);
        MobileMoneyInitiationRequest providerRequest = pawapayDepositService.toProviderRequest(deposit);

        MobileMoneyInitiationResponse response = mobileMoneyProvider.initiate(providerRequest);
        deposit = pawapayDepositService.markInitiationResult(deposit.getDepositId(), response);

        if ("PROCESSING".equals(response.status())) {
            transaction.setProviderReference(response.providerReference());
            transactionRepository.save(transaction);
            return pawapayDepositService.toResponse(deposit);
        }

        transaction.setStatus(PaymentTransactionStatus.FAILED);
        transaction.setFailureReason(response.message());
        transactionRepository.save(transaction);
        intent.setStatus(PaymentIntentStatus.PENDING);
        intentRepository.save(intent);

        return pawapayDepositService.toResponse(deposit);
    }

    private String buildMobileMoneyMetadata(InitiateMobileMoneyDepositRequest request) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("operator", request.correspondent().providerCode());
        meta.put("operatorDisplayName", request.correspondent().getDisplayName());
        meta.put("countryCode", request.correspondent().getCountryCode());
        meta.put("phoneNumber", request.phoneNumber());
        if (request.createdBy() != null) {
            meta.put("createdBy", request.createdBy());
        }
        if (StringUtils.hasText(request.metadataJson())) {
            meta.put("clientMetadata", request.metadataJson().trim());
        }
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException ex) {
            return "{\"operator\":\"" + request.correspondent().providerCode() + "\"}";
        }
    }

    @Override
    public PayInvoiceResponse payInvoice(String documentNumber, PayInvoiceRequest request) {
        BillingDocument document = billingDocumentService.serviceByNumber(documentNumber);
        if (document.getBalanceDue().signum() <= 0) {
            throw new BadRequestException("La facture n'a pas de solde restant à payer");
        }
        BigDecimal amount = requestedBillingAmount(request.amount(), document.getBalanceDue());

        PaymentIntent intent = buildAndSaveInvoiceIntent(document, amount, request.idempotencyKey());

        // Idempotency: if intent already succeeded, return current state without re-processing
        if (intent.getStatus() == PaymentIntentStatus.SUCCEEDED) {
            PaymentTransactionResponse existingTxn = transactionRepository
                    .findAllByPaymentIntentIdOrderByCreatedAtDesc(intent.getId())
                    .stream().findFirst().map(mapper::toTransactionResponse).orElse(null);
            return new PayInvoiceResponse(billingDocumentService.get(documentNumber), existingTxn);
        }

        PaymentTransaction transaction = processPayment(intent, request, document);
        reconcileIntent(intent);
        BigDecimal overpayment = allocationService.allocateIfBillingDocument(transaction);
        creditOverpayment(intent, overpayment, request.processedBy());

        BillingDocumentResponse updatedDocument = billingDocumentService.get(documentNumber);
        // Publish workflow event before sending email — activation/contract generation must not be blocked by email failure
        publishTransactionWorkflow(transaction.getTransactionNumber(), PaymentTransactionStatus.SUCCEEDED);
        try {
            billingEmailService.sendPaymentConfirmation(updatedDocument, mapper.toTransactionResponse(transaction));
        } catch (Exception ex) {
            log.warn("Failed to send payment confirmation email for transaction {} — workflow already published",
                    transaction.getTransactionNumber(), ex);
        }

        return new PayInvoiceResponse(updatedDocument, mapper.toTransactionResponse(transaction));
    }

    private PaymentIntent buildAndSaveInvoiceIntent(BillingDocument document, BigDecimal amount, String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = intentRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        PaymentIntent reusable = findReusableBillingIntent(
                document.getCustomerType(),
                document.getCustomerCode(),
                BILLING_DOCUMENT_SOURCE,
                document.getDocumentNumber(),
                amount
        );
        if (reusable != null) {
            return reusable;
        }
        String customerCtx = CodeComposer.abbrev(document.getCustomerType());
        long seq = CodeComposer.extractSeq(sequenceGenerator.next("payment_intent"));
        String intentNumber = CodeComposer.withDay("INT", customerCtx, LocalDate.now(), seq);
        return intentRepository.save(PaymentIntent.builder()
                .intentNumber(intentNumber)
                .customerType(document.getCustomerType())
                .customerCode(document.getCustomerCode())
                .amount(money(amount))
                .currency(document.getCurrency())
                .status(PaymentIntentStatus.PENDING)
                .purpose("INVOICE_PAYMENT")
                .sourceType(BILLING_DOCUMENT_SOURCE)
                .sourceCode(document.getDocumentNumber())
                .idempotencyKey(trim(idempotencyKey))
                .build());
    }

    private PaymentTransaction processPayment(PaymentIntent intent, PayInvoiceRequest request, BillingDocument document) {
        return switch (request.paymentMethod()) {
            case WALLET -> {
                if (!StringUtils.hasText(request.walletNumber())) {
                    throw new BadRequestException("Le numéro de portefeuille est requis pour un paiement par wallet");
                }
                WalletAccount wallet = walletService.serviceWallet(request.walletNumber());
                if (!wallet.getOwnerType().equalsIgnoreCase(document.getCustomerType())
                        || !wallet.getOwnerCode().equalsIgnoreCase(document.getCustomerCode())) {
                    throw new BadRequestException("Le portefeuille n'appartient pas au client de la facture");
                }
                if (!wallet.getCurrency().equalsIgnoreCase(document.getCurrency())) {
                    throw new BadRequestException("La devise du portefeuille ne correspond pas à celle de la facture");
                }
                walletService.debit(wallet, intent.getAmount(), WalletEntryType.PAYMENT,
                        "PAYMENT_INTENT", intent.getIntentNumber(), intent.getIntentNumber(), request.processedBy());
                yield saveSucceededTransaction(intent, PaymentMethod.WALLET, "INTERNAL_WALLET",
                        wallet.getWalletNumber(), resolveMemberProcessing(request.processedBy()), request.metadataJson());
            }
            case CASH -> {
                if (!"XAF".equalsIgnoreCase(intent.getCurrency())) {
                    throw new BadRequestException("Les paiements en espèces ne sont autorisés qu'en XAF");
                }
                PaymentTransaction cashTxn = saveSucceededTransaction(intent, PaymentMethod.CASH, "CASH",
                        request.providerReference(), request.processedBy(), request.metadataJson());
                if (StringUtils.hasText(request.cashSessionNumber())) {
                    cashRegisterService.recordPayment(request.cashSessionNumber(), cashTxn.getAmount(),
                            cashTxn.getTransactionNumber(), request.processedBy());
                }
                yield cashTxn;
            }
            default -> saveSucceededTransaction(intent, request.paymentMethod(),
                    request.paymentMethod().name(), request.providerReference(),
                    request.processedBy(), request.metadataJson());
        };
    }

    private String resolveMemberProcessing(String processedBy){

        if(processedBy.contains("MBR")){
            return "SYSTEM";
        }
        return processedBy;
    }

    @Override
    public PaymentTransactionResponse refund(String transactionNumber, RefundPaymentRequest request) {
        return refundOrReverse(transactionNumber, request, PaymentTransactionStatus.REFUNDED);
    }

    @Override
    public PaymentTransactionResponse reverse(String transactionNumber, RefundPaymentRequest request) {
        return refundOrReverse(transactionNumber, request, PaymentTransactionStatus.REVERSED);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentIntentResponse getIntent(String intentNumber) {
        return mapper.toIntentResponse(serviceIntent(intentNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> listIntentTransactions(String intentNumber) {
        PaymentIntent intent = serviceIntent(intentNumber);
        return transactionRepository.findAllByPaymentIntentIdOrderByCreatedAtDesc(intent.getId()).stream()
                .map(mapper::toTransactionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> listRefunds(String transactionNumber) {
        PaymentTransaction original = transactionRepository.findByTransactionNumber(transactionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found"));
        return transactionRepository.findAllByOriginalTransactionNumber(original.getTransactionNumber()).stream()
                .map(mapper::toTransactionResponse)
                .toList();
    }

    @Override
    public String retryMobileMoneyDeposit(String intentNumber, String phoneNumber, String providerCode) {
        CongoCorrespondent correspondent;
        try {
            correspondent = CongoCorrespondent.valueOf(providerCode);
        } catch (IllegalArgumentException ex) {
            correspondent = Arrays.stream(CongoCorrespondent.values())
                    .filter(c -> providerCode.equals(c.providerCode()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unknown mobile money provider: " + providerCode));
        }
        MobileMoneyDepositResponse response = initiateMobileMoneyDeposit(intentNumber,
                new InitiateMobileMoneyDepositRequest(null, phoneNumber, correspondent, null, "SYSTEM", null));
        return response.transactionNumber();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<PaymentIntentResponse> listIntents(PaymentIntentStatus status, String customerType, String customerCode, String sourceType, String sourceCode, String searchText, Pageable pageable) {
        PaymentIntentSearchCriteria criteria = new PaymentIntentSearchCriteria(
                status,
                normalizeCustomerType(customerType, customerCode),
                trim(customerCode),
                trim(sourceType),
                trim(sourceCode),
                trim(searchText)
        );
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return new PaginatedResponse<>(intentRepository.search(
                criteria.statusValue(),
                criteria.customerType(),
                criteria.customerCode(),
                criteria.sourceType(),
                criteria.sourceCode(),
                criteria.searchText(),
                unsortedPageable
        ).map(mapper::toIntentResponse));
    }

    private RecoveryContext buildRecoveryContext(String customerType,
                                                 String customerCode,
                                                 String currency,
                                                 boolean invoicePendingBillables) {
        String normalizedCustomerCode = required(customerCode, "Customer code is required");
        String normalizedCustomerType = normalizeCustomerTypeRequired(customerType, normalizedCustomerCode);
        String normalizedCurrency = trim(currency);

        List<BillingDocument> openDocuments = new ArrayList<>(billingDocumentRepository.findRecoverableDocuments(normalizedCustomerType, normalizedCustomerCode));
        List<BillableItem> pendingBillables = new ArrayList<>(billableItemRepository.findRecoverableItems(normalizedCustomerType, normalizedCustomerCode));

        String resolvedCurrency = resolveCurrency(normalizedCurrency, openDocuments, pendingBillables);
        openDocuments = filterDocumentsByCurrency(openDocuments, resolvedCurrency);
        pendingBillables = filterBillablesByCurrency(pendingBillables, resolvedCurrency);

        BigDecimal pendingBillableAmount = sumBillables(pendingBillables);

        if (invoicePendingBillables && !pendingBillables.isEmpty()) {
            BillingDocumentResponse createdInvoice = billingDocumentService.createInvoiceFromBillableItems(
                    new CreateInvoiceFromBillableItemsRequest(
                            "Recouvrement " + normalizedCustomerCode,
                            "Facture generee automatiquement pour elements payables non encore factures",
                            LocalDate.now(),
                            LocalDate.now(),
                            pendingBillables.stream().map(BillableItem::getBillableNumber).toList()
                    )
            );
            BillingDocument invoice = billingDocumentService.serviceByNumber(createdInvoice.documentNumber());
            if (invoice.getStatus() == com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus.DRAFT) {
                billingDocumentService.issue(invoice.getDocumentNumber());
            }
            openDocuments = new ArrayList<>(billingDocumentRepository.findRecoverableDocuments(normalizedCustomerType, normalizedCustomerCode));
            openDocuments = filterDocumentsByCurrency(openDocuments, resolvedCurrency);
        }

        List<BillingDocumentResponse> documentResponses = openDocuments.stream()
                .map(mapperDocument -> billingDocumentService.get(mapperDocument.getDocumentNumber()))
                .toList();
        List<BillableItemResponse> billableResponses = pendingBillables.stream()
                .map(billingMapper::toBillableItemResponse)
                .toList();

        BigDecimal openDocumentAmount = openDocuments.stream()
                .map(BillingDocument::getBalanceDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RecoveryContext(
                normalizedCustomerType,
                normalizedCustomerCode,
                resolvedCurrency,
                documentResponses,
                billableResponses,
                openDocumentAmount,
                pendingBillableAmount,
                invoicePendingBillables ? openDocumentAmount : openDocumentAmount.add(pendingBillableAmount)
        );
    }

    private PaymentRecoveryResponse toRecoveryResponse(RecoveryContext context, PaymentIntentResponse intent) {
        return new PaymentRecoveryResponse(
                context.customerType(),
                context.customerCode(),
                context.currency(),
                context.openDocumentAmount(),
                context.pendingBillableAmount(),
                context.totalPayableAmount(),
                context.documents(),
                context.pendingBillableItems(),
                intent
        );
    }

    private CreatePaymentIntentRequest buildRecoveryIntentRequest(RecoveryContext context,
                                                                  String fingerprint,
                                                                  CreatePaymentRecoveryIntentRequest request) {
        String baseIdempotencyKey = recoveryIdempotencyBase(fingerprint);
        String idempotencyKey = nextRecoveryIdempotencyKey(baseIdempotencyKey);

        return new CreatePaymentIntentRequest(
                context.customerType(),
                context.customerCode(),
                context.openDocumentAmount(),
                context.currency(),
                "PAYMENT_RECOVERY",
                RECOVERY_SOURCE,
                fingerprint,
                idempotencyKey,
                request.expiresAt(),
                recoveryMetadata(context, fingerprint, request.metadataJson())
        );
    }

    private PaymentIntent findReusableRecoveryIntent(String customerType,
                                                     String customerCode,
                                                     String fingerprint,
                                                     BigDecimal amount) {
        return intentRepository.findLatestReusable(customerType, customerCode, RECOVERY_SOURCE, fingerprint)
                .filter(intent -> intent.getExpiresAt() == null || intent.getExpiresAt().isAfter(Instant.now()))
                .filter(intent -> money(intent.getAmount()).compareTo(money(amount)) == 0)
                .orElse(null);
    }

    private String nextRecoveryIdempotencyKey(String baseIdempotencyKey) {
        return intentRepository.findByIdempotencyKey(baseIdempotencyKey)
                .filter(existing -> existing.getStatus() == PaymentIntentStatus.PENDING
                        || existing.getStatus() == PaymentIntentStatus.PROCESSING
                        || existing.getStatus() == PaymentIntentStatus.AUTHORIZED)
                .map(PaymentIntent::getIdempotencyKey)
                .orElseGet(() -> intentRepository.findByIdempotencyKey(baseIdempotencyKey).isPresent()
                        ? baseIdempotencyKey + ":" + Instant.now().toEpochMilli()
                        : baseIdempotencyKey);
    }

    private String recoveryMetadata(RecoveryContext context, String fingerprint, String clientMetadataJson) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recovery", true);
        payload.put("fingerprint", fingerprint);
        payload.put("customerType", context.customerType());
        payload.put("customerCode", context.customerCode());
        payload.put("currency", context.currency());
        payload.put("documentNumbers", context.documents().stream().map(BillingDocumentResponse::documentNumber).toList());
        payload.put("pendingBillableNumbers", context.pendingBillableItems().stream().map(BillableItemResponse::billableNumber).toList());
        if (StringUtils.hasText(clientMetadataJson)) {
            payload.put("clientMetadata", clientMetadataJson.trim());
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"recovery\":true}";
        }
    }

    private String fingerprint(String customerType,
                               String customerCode,
                               String currency,
                               List<BillingDocumentResponse> documents) {
        String payload = customerType + "|" + customerCode + "|" + currency + "|" +
                documents.stream()
                        .map(BillingDocumentResponse::documentNumber)
                        .sorted()
                        .reduce((left, right) -> left + "," + right)
                        .orElse("");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String recoveryIdempotencyBase(String fingerprint) {
        return "PAYMENT_RECOVERY:" + fingerprint;
    }

    private String resolveCurrency(String requestedCurrency,
                                   List<BillingDocument> documents,
                                   List<BillableItem> billables) {
        if (StringUtils.hasText(requestedCurrency)) {
            return requestedCurrency.trim().toUpperCase();
        }

        List<String> currencies = new ArrayList<>();
        documents.stream().map(BillingDocument::getCurrency).filter(StringUtils::hasText).map(String::trim).map(String::toUpperCase).forEach(currencies::add);
        billables.stream().map(BillableItem::getCurrency).filter(StringUtils::hasText).map(String::trim).map(String::toUpperCase).forEach(currencies::add);
        List<String> distinct = currencies.stream().distinct().toList();
        if (distinct.size() > 1) {
            throw new BadRequestException("Multiple payable currencies found. Specify a currency to recover payment");
        }
        return distinct.isEmpty() ? null : distinct.get(0);
    }

    private List<BillingDocument> filterDocumentsByCurrency(List<BillingDocument> documents, String currency) {
        if (!StringUtils.hasText(currency)) {
            return documents;
        }
        return documents.stream()
                .filter(document -> currency.equalsIgnoreCase(document.getCurrency()))
                .sorted(Comparator.comparing(BillingDocument::getCreatedAt))
                .toList();
    }

    private List<BillableItem> filterBillablesByCurrency(List<BillableItem> items, String currency) {
        if (!StringUtils.hasText(currency)) {
            return items;
        }
        return items.stream()
                .filter(item -> currency.equalsIgnoreCase(item.getCurrency()))
                .sorted(Comparator.comparing(BillableItem::getCreatedAt))
                .toList();
    }

    private BigDecimal sumBillables(List<BillableItem> billables) {
        return billables.stream()
                .map(BillableItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String required(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String normalizeCustomerTypeRequired(String customerType, String customerCode) {
        String normalized = normalizeCustomerType(customerType, customerCode);
        if (normalized == null) {
            throw new BadRequestException("Customer type is required");
        }
        return normalized;
    }

    private PaymentIntent pendingIntent(String intentNumber) {
        PaymentIntent intent = serviceIntent(intentNumber);
        if (intent.getStatus() != PaymentIntentStatus.PENDING && intent.getStatus() != PaymentIntentStatus.PROCESSING) {
            throw new BadRequestException("Payment intent is not payable");
        }
        if (intent.getExpiresAt() != null && intent.getExpiresAt().isBefore(Instant.now())) {
            intent.setStatus(PaymentIntentStatus.EXPIRED);
            intentRepository.save(intent);
            throw new BadRequestException("Payment intent has expired");
        }
        return intent;
    }

    private PaymentIntent serviceIntent(String intentNumber) {
        if (!StringUtils.hasText(intentNumber)) {
            throw new BadRequestException("Payment intent number is required");
        }
        return intentRepository.findByIntentNumber(intentNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Payment intent not found"));
    }

    private PaymentTransactionResponse refundOrReverse(String transactionNumber, RefundPaymentRequest request, PaymentTransactionStatus status) {
        PaymentTransaction original = transactionRepository.findByTransactionNumber(transactionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found"));

        BigDecimal refundedSoFar = refundedTotal(original.getTransactionNumber());
        BigDecimal remaining = original.getAmount().subtract(refundedSoFar);
        if (remaining.signum() <= 0) {
            String verb = status == PaymentTransactionStatus.REVERSED ? "reversed" : "refunded";
            throw new ConflictException("payment_transaction", "transaction " + transactionNumber + " has already been fully " + verb);
        }

        BigDecimal amount = request.amount() == null ? remaining : request.amount();
        validatePositive(amount);
        if (amount.compareTo(remaining) > 0) {
            throw new BadRequestException("Refund amount cannot exceed remaining refundable amount (" + remaining + ")");
        }
        boolean fullyRefunded = refundedSoFar.add(amount).compareTo(original.getAmount()) >= 0;
        allocationService.reverseAllocations(original, amount);
        if (original.getPaymentMethod() == PaymentMethod.WALLET) {
            WalletAccount wallet = walletService.serviceWallet(original.getProviderReference());
            walletService.credit(wallet, amount, WalletEntryType.REFUND, "PAYMENT_TRANSACTION", original.getTransactionNumber(), request.reason(), request.processedBy());
        }
        String refundMethodCtx = CodeComposer.abbrev(original.getPaymentMethod().name());
        long refundSeq = CodeComposer.extractSeq(sequenceGenerator.next("payment_transaction"));
        String refundTxnNumber = CodeComposer.withDay("TXN", refundMethodCtx, LocalDate.now(), refundSeq);

        // Mobile money refunds are async — initiate via provider and wait for callback
        if (original.getPaymentMethod() == PaymentMethod.MOBILE_MONEY
                && StringUtils.hasText(original.getProviderReference())) {
            return initiateMobileMoneyRefund(original, amount, request, status, refundTxnNumber, fullyRefunded);
        }

        PaymentTransaction refund = transactionRepository.save(PaymentTransaction.builder()
                .transactionNumber(refundTxnNumber)
                .paymentIntent(original.getPaymentIntent())
                .paymentMethod(original.getPaymentMethod())
                .provider(original.getProvider())
                .providerReference(original.getProviderReference())
                .amount(money(amount))
                .currency(original.getCurrency())
                .status(status)
                .paidAt(Instant.now())
                .receivedBy(trim(request.processedBy()))
                .failureReason(trim(request.reason()))
                .metadataJson(originalLinkMetadata(original.getTransactionNumber()))
                .build());

        if (fullyRefunded) {
            original.setStatus(status);
            transactionRepository.save(original);

            PaymentIntent intent = original.getPaymentIntent();
            if (status == PaymentTransactionStatus.REFUNDED) {
                intent.setStatus(PaymentIntentStatus.REFUNDED);
            } else if (status == PaymentTransactionStatus.REVERSED) {
                intent.setStatus(PaymentIntentStatus.REVERSED);
            }
            intentRepository.save(intent);
        }

        publishTransactionWorkflow(refund.getTransactionNumber(), refund.getStatus());
        return mapper.toTransactionResponse(refund);
    }

    private BigDecimal refundedTotal(String transactionNumber) {
        return transactionRepository.findAllByOriginalTransactionNumber(transactionNumber).stream()
                .filter(t -> t.getStatus() == PaymentTransactionStatus.REFUNDED || t.getStatus() == PaymentTransactionStatus.REVERSED)
                .map(PaymentTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String originalLinkMetadata(String originalTransactionNumber) {
        try {
            return objectMapper.writeValueAsString(Map.of("originalTransactionNumber", originalTransactionNumber));
        } catch (JsonProcessingException e) {
            return "{\"originalTransactionNumber\":\"" + originalTransactionNumber + "\"}";
        }
    }

    private PaymentTransactionResponse initiateMobileMoneyRefund(
            PaymentTransaction original,
            BigDecimal amount,
            RefundPaymentRequest request,
            PaymentTransactionStatus targetStatus,
            String refundTxnNumber,
            boolean fullyRefunded) {

        PaymentTransaction refund = transactionRepository.save(PaymentTransaction.builder()
                .transactionNumber(refundTxnNumber)
                .paymentIntent(original.getPaymentIntent())
                .paymentMethod(original.getPaymentMethod())
                .provider(original.getProvider())
                .amount(money(amount))
                .currency(original.getCurrency())
                .status(PaymentTransactionStatus.PROCESSING)
                .receivedBy(trim(request.processedBy()))
                .failureReason(trim(request.reason()))
                .metadataJson(originalLinkMetadata(original.getTransactionNumber()))
                .build());

        MobileMoneyRefundResponse response = mobileMoneyProvider.refund(new MobileMoneyRefundRequest(
                original.getProviderReference(), amount, request.reason(), original.getCurrency()
        ));

        if ("PROCESSING".equals(response.status())) {
            refund.setProviderReference(response.refundReference());
            transactionRepository.save(refund);
            return mapper.toTransactionResponse(refund);
        }

        if ("NOT_IMPLEMENTED".equals(response.status())) {
            // Noop provider in use — finalize synchronously
            refund.setStatus(targetStatus);
            refund.setPaidAt(Instant.now());
            transactionRepository.save(refund);
            if (fullyRefunded) {
                original.setStatus(targetStatus);
                transactionRepository.save(original);
                PaymentIntent intent = original.getPaymentIntent();
                intent.setStatus(targetStatus == PaymentTransactionStatus.REFUNDED
                        ? PaymentIntentStatus.REFUNDED : PaymentIntentStatus.REVERSED);
                intentRepository.save(intent);
            }
            publishTransactionWorkflow(refund.getTransactionNumber(), refund.getStatus());
            return mapper.toTransactionResponse(refund);
        }

        refund.setStatus(PaymentTransactionStatus.FAILED);
        refund.setFailureReason(response.message());
        transactionRepository.save(refund);
        throw new BadRequestException("Mobile money refund rejected: " + response.message());
    }

    private PaymentTransaction saveSucceededTransaction(PaymentIntent intent, PaymentMethod method, String provider, String providerReference, String receivedBy, String metadataJson) {
        return saveSucceededTransaction(intent, method, provider, providerReference, receivedBy, metadataJson, intent.getAmount());
    }

    private PaymentTransaction saveSucceededTransaction(PaymentIntent intent, PaymentMethod method, String provider, String providerReference, String receivedBy, String metadataJson, BigDecimal amount) {
        String methodCtx = CodeComposer.abbrev(method.name());
        long txnSeq = CodeComposer.extractSeq(sequenceGenerator.next("payment_transaction"));
        String txnNumber = CodeComposer.withDay("TXN", methodCtx, LocalDate.now(), txnSeq);
        Instant paidAt = Instant.now();
        String resolvedProviderReference = resolveProviderReference(provider, method, providerReference, txnNumber, paidAt);

        return transactionRepository.save(PaymentTransaction.builder()
                .transactionNumber(txnNumber)
                .paymentIntent(intent)
                .paymentMethod(method)
                .provider(provider)
                .providerReference(resolvedProviderReference)
                .receiptNumber(sequenceGenerator.next("receipt"))
                .receiptIssuedAt(paidAt)
                .amount(money(amount))
                .currency(intent.getCurrency())
                .status(PaymentTransactionStatus.SUCCEEDED)
                .paidAt(paidAt)
                .receivedBy(trim(receivedBy))
                .metadataJson(trim(metadataJson))
                .build());
    }

    private String resolveProviderReference(String provider, PaymentMethod method, String providerReference, String txnNumber, Instant paidAt) {
        String manualReference = trim(providerReference);
        if (StringUtils.hasText(manualReference)) {
            return manualReference;
        }
        String providerCtx = CodeComposer.abbrev(StringUtils.hasText(provider) ? provider : method.name());
        return "PREF-" + providerCtx + "-" + txnNumber + "-" + paidAt.toEpochMilli();
    }

    private void reconcileIntent(PaymentIntent intent) {
        IntentBalance balance = intentBalance(intent);
        if (balance.remaining().signum() <= 0) {
            intent.setStatus(PaymentIntentStatus.SUCCEEDED);
        } else if (balance.processing().signum() > 0) {
            intent.setStatus(PaymentIntentStatus.PROCESSING);
        } else {
            intent.setStatus(PaymentIntentStatus.PENDING);
        }
        intentRepository.save(intent);
    }

    private void publishTransactionWorkflow(String transactionNumber, PaymentTransactionStatus status) {
        outboxService.publish(
                "PAYMENT_TRANSACTION_WORKFLOW",
                "PAYMENT",
                transactionNumber,
                java.util.Map.of("transactionNumber", transactionNumber, "status", status.name())
        );
    }

    private void creditOverpayment(PaymentIntent intent, BigDecimal overpayment, String createdBy) {
        if (overpayment == null || overpayment.signum() <= 0) {
            return;
        }
        var wallet = walletService.getOrCreate(intent.getCustomerType(), intent.getCustomerCode(), intent.getCurrency());
        WalletAccount account = walletService.serviceWallet(wallet.walletNumber());
        walletService.credit(account, overpayment, WalletEntryType.OVERPAYMENT_CREDIT, "PAYMENT_INTENT", intent.getIntentNumber(), "OVERPAYMENT", createdBy);
    }

    private void validatePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Payment amount must be positive");
        }
    }

    private BigDecimal requestedBillingAmount(BigDecimal requestedAmount, BigDecimal fallbackAmount) {
        if (requestedAmount == null || requestedAmount.signum() <= 0) {
            return money(fallbackAmount);
        }
        return money(requestedAmount);
    }

    private Instant defaultExpiresAt(Instant expiresAt) {
        return expiresAt != null ? expiresAt : Instant.now().plusSeconds(900);
    }

    private BigDecimal requestedIntentAmount(BigDecimal requestedAmount, PaymentIntent intent) {
        IntentBalance balance = intentBalance(intent);
        if (balance.remaining().signum() <= 0) {
            throw new BadRequestException("Payment intent is already fully paid");
        }
        if (balance.payable().signum() <= 0) {
            throw new BadRequestException("Payment intent has pending mobile money transactions. Wait for their final status before adding another payment");
        }
        BigDecimal amount = requestedAmount == null ? balance.payable() : money(requestedAmount);
        validatePositive(amount);
        if (amount.compareTo(balance.payable()) > 0) {
            throw new BadRequestException("Payment amount cannot exceed remaining payable intent balance");
        }
        return amount;
    }

    private IntentBalance intentBalance(PaymentIntent intent) {
        BigDecimal paid = money(transactionRepository.sumSucceededAmountByPaymentIntentId(intent.getId()));
        BigDecimal processing = money(transactionRepository.sumProcessingAmountByPaymentIntentId(intent.getId()));
        BigDecimal total = money(intent.getAmount());
        BigDecimal remaining = total.subtract(paid);
        if (remaining.signum() < 0) {
            remaining = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal payable = remaining.subtract(processing);
        if (payable.signum() < 0) {
            payable = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return new IntentBalance(total, paid, processing, remaining, payable);
    }

    private record IntentBalance(
            BigDecimal total,
            BigDecimal paid,
            BigDecimal processing,
            BigDecimal remaining,
            BigDecimal payable
    ) {
    }

    private PaymentIntent findReusableBillingIntent(String customerType,
                                                    String customerCode,
                                                    String sourceType,
                                                    String sourceCode,
                                                    BigDecimal amount) {
        return intentRepository.findLatestReusable(customerType, customerCode, sourceType, sourceCode)
                .filter(intent -> intent.getExpiresAt() == null || intent.getExpiresAt().isAfter(Instant.now()))
                .filter(intent -> money(intent.getAmount()).compareTo(money(amount)) == 0)
                .orElse(null);
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(4, RoundingMode.HALF_UP);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeCustomerType(String customerType, String customerCode) {
        String normalized = trim(customerType);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (normalized.equals("CLIENT") || normalized.equals("CUSTOMER")) {
            String inferred = inferCustomerTypeFromCode(customerCode);
            if (inferred != null) {
                return inferred;
            }
        }
        return switch (normalized) {
            case "MEMBRE" -> "MEMBER";
            case "CLIENT" -> "CUSTOMER";
            case "BUSINESS", "COMPANY", "ENTREPRISE" -> "BUSINESS_ENTITY";
            default -> normalized;
        };
    }

    private String inferCustomerTypeFromCode(String customerCode) {
        if (!StringUtils.hasText(customerCode)) {
            return null;
        }
        String normalizedCode = customerCode.trim().toUpperCase(Locale.ROOT);
        if (normalizedCode.startsWith("MBR-")) {
            return "MEMBER";
        }
        if (normalizedCode.startsWith("CUS-")) {
            return "CUSTOMER";
        }
        if (normalizedCode.startsWith("BUS-") || normalizedCode.startsWith("BIZ-")) {
            return "BUSINESS_ENTITY";
        }
        return null;
    }

    private record RecoveryContext(
            String customerType,
            String customerCode,
            String currency,
            List<BillingDocumentResponse> documents,
            List<BillableItemResponse> pendingBillableItems,
            BigDecimal openDocumentAmount,
            BigDecimal pendingBillableAmount,
            BigDecimal totalPayableAmount
    ) {
    }
}
