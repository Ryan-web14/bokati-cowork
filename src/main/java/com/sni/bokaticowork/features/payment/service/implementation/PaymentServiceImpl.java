package com.sni.bokaticowork.features.payment.service.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.features.billing.dto.request.CreateInvoiceFromBillableItemsRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentForDocumentsRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentRecoveryIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.PayInvoiceRequest;
import com.sni.bokaticowork.features.payment.dto.request.RefundPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.request.RegisterCashPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.response.PayInvoiceResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentRecoveryResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.repository.specification.criteria.PaymentIntentSearchCriteria;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.payment.service.support.PaymentAllocationService;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    @org.springframework.context.annotation.Lazy
    private final BillingEmailService billingEmailService;

    @Override
    public PaymentIntentResponse createIntent(CreatePaymentIntentRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = intentRepository.findByIdempotencyKey(request.idempotencyKey().trim());
            if (existing.isPresent()) {
                return mapper.toIntentResponse(existing.get());
            }
        }
        validatePositive(request.amount());
        String customerCtx = CodeComposer.abbrev(request.customerType());
        long intentSeq = CodeComposer.extractSeq(sequenceGenerator.next("payment_intent"));
        String intentNumber = CodeComposer.withDay("INT", customerCtx, LocalDate.now(), intentSeq);

        PaymentIntent intent = PaymentIntent.builder()
                .intentNumber(intentNumber)
                .customerType(request.customerType().trim())
                .customerCode(request.customerCode().trim())
                .amount(money(request.amount()))
                .currency(request.currency().trim().toUpperCase())
                .status(PaymentIntentStatus.PENDING)
                .purpose(trim(request.purpose()))
                .sourceType(trim(request.sourceType()))
                .sourceCode(trim(request.sourceCode()))
                .idempotencyKey(trim(request.idempotencyKey()))
                .expiresAt(request.expiresAt())
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
        return createIntent(new CreatePaymentIntentRequest(
                document.getCustomerType(),
                document.getCustomerCode(),
                document.getBalanceDue(),
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
        }
        if (first == null || total.signum() <= 0) {
            throw new BadRequestException("No payable billing document found");
        }
        return createIntent(new CreatePaymentIntentRequest(
                first.getCustomerType(),
                first.getCustomerCode(),
                total,
                first.getCurrency(),
                "MULTI_BILLING_DOCUMENT_PAYMENT",
                MULTI_BILLING_DOCUMENT_SOURCE,
                String.join(",", request.documentNumbers()),
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
    public PaymentTransactionResponse registerCashPayment(String intentNumber, RegisterCashPaymentRequest request) {
        PaymentIntent intent = pendingIntent(intentNumber);
        if (!"XAF".equalsIgnoreCase(intent.getCurrency())) {
            throw new BadRequestException("Cash payments are only allowed in XAF");
        }
        PaymentTransaction transaction = saveSucceededTransaction(intent, PaymentMethod.CASH, "CASH", request.providerReference(), request.receivedBy(), request.metadataJson());
        if (StringUtils.hasText(request.cashSessionNumber())) {
            cashRegisterService.recordPayment(request.cashSessionNumber(), transaction.getAmount(), transaction.getTransactionNumber(), request.receivedBy());
        }
        completeIntent(intent);
        creditOverpayment(intent, allocationService.allocateIfBillingDocument(transaction), request.receivedBy());
        return mapper.toTransactionResponse(transaction);
    }

    @Override
    public PaymentTransactionResponse payWithWallet(String intentNumber, WalletPaymentRequest request) {
        PaymentIntent intent = pendingIntent(intentNumber);
        WalletAccount wallet = walletService.serviceWallet(request.walletNumber());
        if (!wallet.getOwnerType().equalsIgnoreCase(intent.getCustomerType()) || !wallet.getOwnerCode().equalsIgnoreCase(intent.getCustomerCode())) {
            throw new BadRequestException("Wallet owner does not match payment intent customer");
        }
        if (!wallet.getCurrency().equalsIgnoreCase(intent.getCurrency())) {
            throw new BadRequestException("Wallet currency does not match payment intent currency");
        }
        walletService.debit(wallet, intent.getAmount(), WalletEntryType.PAYMENT, "PAYMENT_INTENT", intent.getIntentNumber(), intent.getIntentNumber(), request.createdBy());
        PaymentTransaction transaction = saveSucceededTransaction(intent, PaymentMethod.WALLET, "INTERNAL_WALLET", wallet.getWalletNumber(), request.createdBy(), request.metadataJson());
        completeIntent(intent);
        creditOverpayment(intent, allocationService.allocateIfBillingDocument(transaction), request.createdBy());
        return mapper.toTransactionResponse(transaction);
    }

    @Override
    public PayInvoiceResponse payInvoice(String documentNumber, PayInvoiceRequest request) {
        BillingDocument document = billingDocumentService.serviceByNumber(documentNumber);
        if (document.getBalanceDue().signum() <= 0) {
            throw new BadRequestException("La facture n'a pas de solde restant à payer");
        }
        BigDecimal balanceDue = document.getBalanceDue();
        BigDecimal amount = (request.amount() != null && request.amount().signum() > 0)
                ? money(request.amount()).min(balanceDue)
                : balanceDue;

        PaymentIntent intent = buildAndSaveInvoiceIntent(document, amount, request.idempotencyKey());

        // Idempotency: if intent already succeeded, return current state without re-processing
        if (intent.getStatus() == PaymentIntentStatus.SUCCEEDED) {
            PaymentTransactionResponse existingTxn = transactionRepository
                    .findAllByPaymentIntentIdOrderByCreatedAtDesc(intent.getId())
                    .stream().findFirst().map(mapper::toTransactionResponse).orElse(null);
            return new PayInvoiceResponse(billingDocumentService.get(documentNumber), existingTxn);
        }

        PaymentTransaction transaction = processPayment(intent, request, document);
        completeIntent(intent);
        BigDecimal overpayment = allocationService.allocateIfBillingDocument(transaction);
        creditOverpayment(intent, overpayment, request.processedBy());

        BillingDocumentResponse updatedDocument = billingDocumentService.get(documentNumber);
        billingEmailService.sendPaymentConfirmation(updatedDocument, mapper.toTransactionResponse(transaction));

        return new PayInvoiceResponse(updatedDocument, mapper.toTransactionResponse(transaction));
    }

    private PaymentIntent buildAndSaveInvoiceIntent(BillingDocument document, BigDecimal amount, String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = intentRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return existing.get();
            }
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
                        wallet.getWalletNumber(), request.processedBy(), request.metadataJson());
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
    public PaginatedResponse<PaymentIntentResponse> listIntents(PaymentIntentStatus status, String customerType, String customerCode, String sourceType, String sourceCode, String searchText, Pageable pageable) {
        PaymentIntentSearchCriteria criteria = new PaymentIntentSearchCriteria(
                status,
                trim(customerType),
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
        String normalizedCustomerType = required(customerType, "Customer type is required");
        String normalizedCustomerCode = required(customerCode, "Customer code is required");
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
        BigDecimal amount = request.amount() == null ? original.getAmount() : request.amount();
        validatePositive(amount);
        if (amount.compareTo(original.getAmount()) > 0) {
            throw new BadRequestException("Refund amount cannot exceed transaction amount");
        }
        allocationService.reverseAllocations(original, amount);
        if (original.getPaymentMethod() == PaymentMethod.WALLET) {
            WalletAccount wallet = walletService.serviceWallet(original.getProviderReference());
            walletService.credit(wallet, amount, WalletEntryType.REFUND, "PAYMENT_TRANSACTION", original.getTransactionNumber(), request.reason(), request.processedBy());
        }
        String refundMethodCtx = CodeComposer.abbrev(original.getPaymentMethod().name());
        long refundSeq = CodeComposer.extractSeq(sequenceGenerator.next("payment_transaction"));
        String refundTxnNumber = CodeComposer.withDay("TXN", refundMethodCtx, LocalDate.now(), refundSeq);

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
                .build());
        return mapper.toTransactionResponse(refund);
    }

    private PaymentTransaction saveSucceededTransaction(PaymentIntent intent, PaymentMethod method, String provider, String providerReference, String receivedBy, String metadataJson) {
        String methodCtx = CodeComposer.abbrev(method.name());
        long txnSeq = CodeComposer.extractSeq(sequenceGenerator.next("payment_transaction"));
        String txnNumber = CodeComposer.withDay("TXN", methodCtx, LocalDate.now(), txnSeq);

        return transactionRepository.save(PaymentTransaction.builder()
                .transactionNumber(txnNumber)
                .paymentIntent(intent)
                .paymentMethod(method)
                .provider(provider)
                .providerReference(trim(providerReference))
                .amount(intent.getAmount())
                .currency(intent.getCurrency())
                .status(PaymentTransactionStatus.SUCCEEDED)
                .paidAt(Instant.now())
                .receivedBy(trim(receivedBy))
                .metadataJson(trim(metadataJson))
                .build());
    }

    private void completeIntent(PaymentIntent intent) {
        intent.setStatus(PaymentIntentStatus.SUCCEEDED);
        intentRepository.save(intent);
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

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(4, RoundingMode.HALF_UP);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
