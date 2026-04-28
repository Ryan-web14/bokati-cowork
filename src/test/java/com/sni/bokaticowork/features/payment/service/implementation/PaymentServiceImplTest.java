package com.sni.bokaticowork.features.payment.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.request.CreateInvoiceFromBillableItemsRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentRecoveryIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.PayInvoiceRequest;
import com.sni.bokaticowork.features.payment.dto.request.RegisterCashPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
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
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.payment.service.support.PaymentAllocationService;
import com.sni.bokaticowork.features.payment.service.support.PaymentTransactionWorkflowEvent;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.BillableItemResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionBillingMapper;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentIntentRepository intentRepository;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private BillingDocumentRepository billingDocumentRepository;

    @Mock
    private BillableItemRepository billableItemRepository;

    @Mock
    private BillingDocumentService billingDocumentService;

    @Mock
    private WalletService walletService;

    @Mock
    private CashRegisterService cashRegisterService;

    @Mock
    private PaymentAllocationService allocationService;

    @Mock
    private SequenceGeneratorFacade sequenceGenerator;

    @Mock
    private PaymentMapper mapper;

    @Mock
    private SubscriptionBillingMapper billingMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private BillingEmailService billingEmailService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"recovery\":true}");
        lenient().when(mapper.toIntentResponse(any(PaymentIntent.class))).thenAnswer(invocation -> toIntentResponse(invocation.getArgument(0)));
        lenient().when(mapper.toTransactionResponse(any(PaymentTransaction.class))).thenAnswer(invocation -> toTransactionResponse(invocation.getArgument(0)));
    }

    @Test
    void shouldPreviewRecoveryAcrossOpenDocumentsAndPendingBillables() {
        BillingDocument openInvoice = billingDocument("INV-001", "MEMBER", "MBR-0001", "XAF", "100.00", BillingDocumentStatus.ISSUED);
        BillableItem pendingBooking = billableItem("BIL-001", "BOOKING", "MBR-0001", "XAF", "50.00");

        when(billingDocumentRepository.findRecoverableDocuments("MEMBER", "MBR-0001")).thenReturn(List.of(openInvoice));
        when(billableItemRepository.findRecoverableItems("MEMBER", "MBR-0001")).thenReturn(List.of(pendingBooking));
        when(billingDocumentService.get("INV-001")).thenReturn(documentResponse("INV-001", "MEMBER", "MBR-0001", "XAF", "100.00"));
        when(billingMapper.toBillableItemResponse(pendingBooking)).thenReturn(billableItemResponse("BIL-001", "BOOKING", "MBR-0001", "XAF", "50.00"));

        var response = paymentService.previewRecovery("MEMBER", "MBR-0001", null);

        assertEquals(new BigDecimal("100.00"), response.openDocumentAmount());
        assertEquals(new BigDecimal("50.00"), response.pendingBillableAmount());
        assertEquals(new BigDecimal("150.00"), response.totalPayableAmount());
        assertEquals(1, response.documents().size());
        assertEquals(1, response.pendingBillableItems().size());
        assertNull(response.paymentIntent());
        verify(billingDocumentService, never()).createInvoiceFromBillableItems(any());
    }

    @Test
    void shouldCreateRecoveryIntentByAutoInvoicingPendingReservationItems() {
        BillableItem pendingBooking = billableItem("BIL-BOOK-001", "BOOKING_RESERVATION", "MBR-0001", "XAF", "50.00");
        BillingDocument generatedInvoice = billingDocument("INV-NEW", "MEMBER", "MBR-0001", "XAF", "50.00", BillingDocumentStatus.ISSUED);

        when(billingDocumentRepository.findRecoverableDocuments("MEMBER", "MBR-0001"))
                .thenReturn(List.of())
                .thenReturn(List.of(generatedInvoice));
        when(billableItemRepository.findRecoverableItems("MEMBER", "MBR-0001")).thenReturn(List.of(pendingBooking));
        when(billingDocumentService.createInvoiceFromBillableItems(any(CreateInvoiceFromBillableItemsRequest.class)))
                .thenReturn(documentResponse("INV-NEW", "MEMBER", "MBR-0001", "XAF", "50.00"));
        when(billingDocumentService.serviceByNumber("INV-NEW"))
                .thenReturn(billingDocument("INV-NEW", "MEMBER", "MBR-0001", "XAF", "50.00", BillingDocumentStatus.DRAFT));
        when(billingDocumentService.get("INV-NEW")).thenReturn(documentResponse("INV-NEW", "MEMBER", "MBR-0001", "XAF", "50.00"));
        when(billingMapper.toBillableItemResponse(pendingBooking))
                .thenReturn(billableItemResponse("BIL-BOOK-001", "BOOKING_RESERVATION", "MBR-0001", "XAF", "50.00"));
        when(intentRepository.findLatestReusable(eq("MEMBER"), eq("MBR-0001"), eq("PAYABLE_RECOVERY"), anyString())).thenReturn(Optional.empty());
        when(intentRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(sequenceGenerator.next("payment_intent")).thenReturn("PIN-00000001");
        when(intentRepository.save(any(PaymentIntent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createRecoveryIntent(new CreatePaymentRecoveryIntentRequest(
                "MEMBER",
                "MBR-0001",
                null,
                Instant.parse("2026-04-27T12:00:00Z"),
                "{\"origin\":\"portal\"}"
        ));

        ArgumentCaptor<CreateInvoiceFromBillableItemsRequest> invoiceCaptor = ArgumentCaptor.forClass(CreateInvoiceFromBillableItemsRequest.class);
        verify(billingDocumentService).createInvoiceFromBillableItems(invoiceCaptor.capture());
        assertEquals(List.of("BIL-BOOK-001"), invoiceCaptor.getValue().billableNumbers());
        verify(billingDocumentService).issue("INV-NEW");

        ArgumentCaptor<PaymentIntent> intentCaptor = ArgumentCaptor.forClass(PaymentIntent.class);
        verify(intentRepository).save(intentCaptor.capture());
        PaymentIntent savedIntent = intentCaptor.getValue();
        assertEquals(new BigDecimal("50.0000"), savedIntent.getAmount());
        assertEquals("PAYABLE_RECOVERY", savedIntent.getSourceType());
        assertNotNull(savedIntent.getMetadataJson());

        assertEquals(new BigDecimal("50.00"), response.openDocumentAmount());
        assertEquals(new BigDecimal("50.00"), response.pendingBillableAmount());
        assertEquals(new BigDecimal("50.00"), response.totalPayableAmount());
        assertNotNull(response.paymentIntent());
        assertEquals("PAYABLE_RECOVERY", response.paymentIntent().sourceType());
    }

    @Test
    void shouldReuseExistingRecoveryIntentWhenCustomerReturnsToPay() {
        BillingDocument openInvoice = billingDocument("INV-001", "CUSTOMER", "CUS-0001", "XAF", "100.00", BillingDocumentStatus.ISSUED);
        PaymentIntent existingIntent = PaymentIntent.builder()
                .intentNumber("INT-CUS-20260426-00000009")
                .customerType("CUSTOMER")
                .customerCode("CUS-0001")
                .amount(new BigDecimal("100.00"))
                .currency("XAF")
                .status(PaymentIntentStatus.PENDING)
                .sourceType("PAYABLE_RECOVERY")
                .sourceCode("fingerprint-001")
                .idempotencyKey("PAYMENT_RECOVERY:fingerprint-001")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        ReflectionTestUtils.setField(existingIntent, "metadataJson", "{\"recovery\":true}");

        when(billingDocumentRepository.findRecoverableDocuments("CUSTOMER", "CUS-0001")).thenReturn(List.of(openInvoice));
        when(billableItemRepository.findRecoverableItems("CUSTOMER", "CUS-0001")).thenReturn(List.of());
        when(billingDocumentService.get("INV-001")).thenReturn(documentResponse("INV-001", "CUSTOMER", "CUS-0001", "XAF", "100.00"));
        when(intentRepository.findLatestReusable(eq("CUSTOMER"), eq("CUS-0001"), eq("PAYABLE_RECOVERY"), anyString()))
                .thenReturn(Optional.of(existingIntent));

        var response = paymentService.createRecoveryIntent(new CreatePaymentRecoveryIntentRequest(
                "CUSTOMER",
                "CUS-0001",
                "XAF",
                Instant.parse("2026-04-27T12:00:00Z"),
                null
        ));

        verify(intentRepository, never()).save(any(PaymentIntent.class));
        verify(billingDocumentService, never()).createInvoiceFromBillableItems(any());
        assertNotNull(response.paymentIntent());
        assertEquals("INT-CUS-20260426-00000009", response.paymentIntent().intentNumber());
        assertEquals(new BigDecimal("100.00"), response.totalPayableAmount());
    }

    @Test
    void shouldCreateBillingIntentFromRequestedAmountIncludingAdvance() {
        BillingDocument invoice = billingDocument("INV-ADV-001", "MEMBER", "MBR-0001", "XAF", "100.00", BillingDocumentStatus.ISSUED);

        when(billingDocumentService.serviceByNumber("INV-ADV-001")).thenReturn(invoice);
        when(intentRepository.findLatestReusable("MEMBER", "MBR-0001", "BILLING_DOCUMENT", "INV-ADV-001")).thenReturn(Optional.empty());
        when(sequenceGenerator.next("payment_intent")).thenReturn("PIN-00000002");
        when(intentRepository.save(any(PaymentIntent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentIntentResponse response = paymentService.createIntentFromBillingDocument(new CreatePaymentIntentFromBillingDocumentRequest(
                "INV-ADV-001",
                new BigDecimal("150.00"),
                null,
                Instant.parse("2026-04-27T15:00:00Z"),
                null
        ));

        assertEquals(new BigDecimal("150.0000"), response.amount());
        assertEquals("BILLING_DOCUMENT", response.sourceType());
        assertEquals("INV-ADV-001", response.sourceCode());
    }

    @Test
    void shouldCreditWalletWhenInvoicePaymentExceedsInvoiceBalance() {
        BillingDocument invoice = billingDocument("INV-ADV-002", "MEMBER", "MBR-0001", "XAF", "100.00", BillingDocumentStatus.ISSUED);
        PaymentIntent pendingIntent = PaymentIntent.builder()
                .intentNumber("INT-MEM-20260427-00000005")
                .customerType("MEMBER")
                .customerCode("MBR-0001")
                .amount(new BigDecimal("150.0000"))
                .currency("XAF")
                .status(PaymentIntentStatus.PENDING)
                .sourceType("BILLING_DOCUMENT")
                .sourceCode("INV-ADV-002")
                .build();
        PaymentTransaction savedTransaction = PaymentTransaction.builder()
                .transactionNumber("TXN-CAS-20260427-00000009")
                .paymentIntent(pendingIntent)
                .paymentMethod(PaymentMethod.CASH)
                .provider("CASH")
                .providerReference("MANUAL-001")
                .receiptNumber("REC-2026-000001")
                .amount(new BigDecimal("150.0000"))
                .currency("XAF")
                .status(PaymentTransactionStatus.SUCCEEDED)
                .paidAt(Instant.parse("2026-04-27T10:30:00Z"))
                .receivedBy("admin-001")
                .build();

        when(billingDocumentService.serviceByNumber("INV-ADV-002")).thenReturn(invoice);
        when(intentRepository.findLatestReusable("MEMBER", "MBR-0001", "BILLING_DOCUMENT", "INV-ADV-002")).thenReturn(Optional.empty());
        when(sequenceGenerator.next("payment_intent")).thenReturn("PIN-00000005");
        when(sequenceGenerator.next("payment_transaction")).thenReturn("PTX-00000009");
        when(sequenceGenerator.next("receipt")).thenReturn("REC-2026-000001");
        when(intentRepository.save(any(PaymentIntent.class))).thenAnswer(invocation -> {
            PaymentIntent intent = invocation.getArgument(0);
            if (intent.getIntentNumber() == null) {
                intent.setIntentNumber("INT-MEM-20260427-00000005");
            }
            return intent;
        });
        when(transactionRepository.save(any(PaymentTransaction.class))).thenReturn(savedTransaction);
        when(allocationService.allocateIfBillingDocument(savedTransaction)).thenReturn(new BigDecimal("50.0000"));
        when(walletService.getOrCreate("MEMBER", "MBR-0001", "XAF")).thenReturn(new com.sni.bokaticowork.features.payment.dto.response.WalletResponse(
                "WAL-001",
                "MEMBER",
                "MBR-0001",
                "XAF",
                com.sni.bokaticowork.features.payment.enums.WalletStatus.ACTIVE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null
        ));
        WalletAccount walletAccount = WalletAccount.builder()
                .walletNumber("WAL-001")
                .ownerType("MEMBER")
                .ownerCode("MBR-0001")
                .currency("XAF")
                .build();
        when(walletService.serviceWallet("WAL-001")).thenReturn(walletAccount);
        when(billingDocumentService.get("INV-ADV-002")).thenReturn(documentResponse("INV-ADV-002", "MEMBER", "MBR-0001", "XAF", "0.00"));

        paymentService.payInvoice("INV-ADV-002", new PayInvoiceRequest(
                PaymentMethod.CASH,
                new BigDecimal("150.00"),
                null,
                null,
                "MANUAL-001",
                "admin-001",
                null,
                null
        ));

        verify(walletService).credit(eq(walletAccount), eq(new BigDecimal("50.0000")), eq(WalletEntryType.OVERPAYMENT_CREDIT), eq("PAYMENT_INTENT"), eq("INT-MEM-20260427-00000005"), eq("OVERPAYMENT"), eq("admin-001"));
    }

    @Test
    void shouldGenerateProviderReferenceWhenCashPaymentDoesNotProvideOne() {
        PaymentIntent intent = PaymentIntent.builder()
                .intentNumber("INT-MEM-20260427-00000010")
                .customerType("MEMBER")
                .customerCode("MBR-0001")
                .amount(new BigDecimal("100.0000"))
                .currency("XAF")
                .status(PaymentIntentStatus.PENDING)
                .sourceType("BILLING_DOCUMENT")
                .sourceCode("INV-001")
                .build();
        ReflectionTestUtils.setField(intent, "id", 88L);

        when(intentRepository.findByIntentNumber("INT-MEM-20260427-00000010")).thenReturn(Optional.of(intent));
        when(sequenceGenerator.next("payment_transaction")).thenReturn("PTX-00000010");
        when(sequenceGenerator.next("receipt")).thenReturn("REC-2026-000010");
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(allocationService.allocateIfBillingDocument(any(PaymentTransaction.class))).thenReturn(BigDecimal.ZERO);

        PaymentTransactionResponse response = paymentService.registerCashPayment(
                "INT-MEM-20260427-00000010",
                new RegisterCashPaymentRequest("cashier-001", null, null, null)
        );

        assertNotNull(response.providerReference());
        assertTrue(response.providerReference().startsWith("PREF-"));
        assertTrue(response.providerReference().contains(response.transactionNumber()));
        verify(eventPublisher).publishEvent((Object) argThat(event -> event instanceof PaymentTransactionWorkflowEvent workflow
                && workflow.status() == PaymentTransactionStatus.SUCCEEDED
                && workflow.transactionNumber().equals(response.transactionNumber())));
    }

    @Test
    void shouldPublishRefundWorkflowEvent() {
        PaymentIntent intent = PaymentIntent.builder()
                .intentNumber("INT-MEM-20260427-00000011")
                .customerType("MEMBER")
                .customerCode("MBR-0001")
                .amount(new BigDecimal("100.0000"))
                .currency("XAF")
                .status(PaymentIntentStatus.SUCCEEDED)
                .sourceType("BILLING_DOCUMENT")
                .sourceCode("INV-001")
                .build();
        PaymentTransaction original = PaymentTransaction.builder()
                .transactionNumber("TXN-CAS-20260427-00000011")
                .paymentIntent(intent)
                .paymentMethod(PaymentMethod.CASH)
                .provider("CASH")
                .providerReference("PREF-001")
                .amount(new BigDecimal("100.0000"))
                .currency("XAF")
                .status(PaymentTransactionStatus.SUCCEEDED)
                .paidAt(Instant.parse("2026-04-27T10:00:00Z"))
                .build();

        when(transactionRepository.findByTransactionNumber("TXN-CAS-20260427-00000011")).thenReturn(Optional.of(original));
        when(sequenceGenerator.next("payment_transaction")).thenReturn("PTX-00000011");
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentTransactionResponse response = paymentService.refund(
                "TXN-CAS-20260427-00000011",
                new com.sni.bokaticowork.features.payment.dto.request.RefundPaymentRequest(null, "Client request", "admin-001")
        );

        verify(eventPublisher).publishEvent((Object) argThat(event -> event instanceof PaymentTransactionWorkflowEvent workflow
                && workflow.status() == PaymentTransactionStatus.REFUNDED
                && workflow.transactionNumber().equals(response.transactionNumber())));
    }

    @Test
    void shouldListTransactionsForIntent() {
        PaymentIntent intent = PaymentIntent.builder()
                .intentNumber("INT-MEM-20260426-00000001")
                .customerType("MEMBER")
                .customerCode("MBR-0001")
                .amount(new BigDecimal("100.0000"))
                .currency("XAF")
                .status(PaymentIntentStatus.SUCCEEDED)
                .build();
        ReflectionTestUtils.setField(intent, "id", 77L);

        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionNumber("TXN-CAS-20260426-00000001")
                .paymentIntent(intent)
                .paymentMethod(PaymentMethod.CASH)
                .provider("CASH")
                .providerReference("RECU-001")
                .amount(new BigDecimal("100.0000"))
                .currency("XAF")
                .status(PaymentTransactionStatus.SUCCEEDED)
                .paidAt(Instant.parse("2026-04-26T10:30:00Z"))
                .receivedBy("admin-001")
                .metadataJson("{\"origin\":\"frontdesk\"}")
                .build();

        when(intentRepository.findByIntentNumber("INT-MEM-20260426-00000001")).thenReturn(Optional.of(intent));
        when(transactionRepository.findAllByPaymentIntentIdOrderByCreatedAtDesc(77L)).thenReturn(List.of(transaction));

        List<PaymentTransactionResponse> response = paymentService.listIntentTransactions("INT-MEM-20260426-00000001");

        assertEquals(1, response.size());
        assertEquals("TXN-CAS-20260426-00000001", response.getFirst().transactionNumber());
        assertEquals("INT-MEM-20260426-00000001", response.getFirst().intentNumber());
        verify(transactionRepository).findAllByPaymentIntentIdOrderByCreatedAtDesc(77L);
    }

    private PaymentIntentResponse toIntentResponse(PaymentIntent intent) {
        return new PaymentIntentResponse(
                intent.getIntentNumber(),
                intent.getCustomerType(),
                intent.getCustomerCode(),
                null,
                null,
                null,
                null,
                false,
                intent.getAmount(),
                intent.getCurrency(),
                intent.getStatus(),
                intent.getPurpose(),
                intent.getSourceType(),
                intent.getSourceCode(),
                intent.getSourceType(),
                intent.getSourceCode(),
                intent.getSourceCode(),
                false,
                intent.getIdempotencyKey(),
                intent.getExpiresAt(),
                intent.getMetadataJson()
        );
    }

    private PaymentTransactionResponse toTransactionResponse(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
                transaction.getTransactionNumber(),
                transaction.getPaymentIntent().getIntentNumber(),
                transaction.getPaymentMethod(),
                transaction.getProvider(),
                transaction.getProviderReference(),
                transaction.getReceiptNumber(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getPaidAt(),
                transaction.getReceivedBy(),
                transaction.getFailureReason(),
                transaction.getMetadataJson()
        );
    }

    private BillingDocument billingDocument(String documentNumber,
                                            String customerType,
                                            String customerCode,
                                            String currency,
                                            String balanceDue,
                                            BillingDocumentStatus status) {
        return BillingDocument.builder()
                .documentNumber(documentNumber)
                .documentType(BillingDocumentType.INVOICE)
                .status(status)
                .customerType(customerType)
                .customerCode(customerCode)
                .customerName(customerCode)
                .currency(currency)
                .totalAmount(new BigDecimal(balanceDue))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal(balanceDue))
                .issueDate(LocalDate.of(2026, 4, 26))
                .createdAt(Instant.parse("2026-04-26T10:00:00Z"))
                .build();
    }

    private BillingDocumentResponse documentResponse(String documentNumber,
                                                     String customerType,
                                                     String customerCode,
                                                     String currency,
                                                     String balanceDue) {
        BigDecimal amount = new BigDecimal(balanceDue);
        return new BillingDocumentResponse(
                documentNumber,
                BillingDocumentType.INVOICE,
                BillingDocumentStatus.ISSUED,
                customerType,
                customerCode,
                customerCode,
                null,
                null,
                null,
                false,
                "BILLABLE_ITEM",
                documentNumber,
                "BILLABLE_ITEM",
                documentNumber,
                documentNumber,
                false,
                "Invoice " + documentNumber,
                null,
                null,
                currency,
                amount,
                BigDecimal.ZERO,
                amount,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                amount,
                BigDecimal.ZERO,
                amount,
                LocalDate.of(2026, 4, 26),
                LocalDate.of(2026, 4, 26),
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private BillableItem billableItem(String billableNumber,
                                      String sourceType,
                                      String subscriberCode,
                                      String currency,
                                      String amount) {
        return BillableItem.builder()
                .billableNumber(billableNumber)
                .sourceType(sourceType)
                .sourceId(sourceType + "-001")
                .subscriberType(SubscriberType.valueOf(subscriberCode.startsWith("CUS") ? "CUSTOMER" : "MEMBER"))
                .subscriberCode(subscriberCode)
                .description("Pending " + sourceType)
                .amount(new BigDecimal(amount))
                .currency(currency)
                .status(BillableItemStatus.PENDING)
                .createdAt(Instant.parse("2026-04-26T09:00:00Z"))
                .build();
    }

    private BillableItemResponse billableItemResponse(String billableNumber,
                                                      String sourceType,
                                                      String subscriberCode,
                                                      String currency,
                                                      String amount) {
        return new BillableItemResponse(
                billableNumber,
                sourceType,
                sourceType + "-001",
                SubscriberType.valueOf(subscriberCode.startsWith("CUS") ? "CUSTOMER" : "MEMBER"),
                subscriberCode,
                "Pending " + sourceType,
                new BigDecimal(amount),
                currency,
                null,
                null,
                null,
                BillableItemStatus.PENDING,
                null
        );
    }
}
