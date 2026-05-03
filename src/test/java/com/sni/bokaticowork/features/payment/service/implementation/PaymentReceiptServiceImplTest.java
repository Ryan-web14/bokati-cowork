package com.sni.bokaticowork.features.payment.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReceiptServiceImplTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentAllocationRepository paymentAllocationRepository;

    @Mock
    private BillingDocumentService billingDocumentService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private SequenceGeneratorFacade sequenceGenerator;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentReceiptServiceImpl paymentReceiptService;

    @Test
    void shouldBuildReceiptWithAllocationAndAdvanceBreakdown() {
        PaymentIntent intent = PaymentIntent.builder()
                .intentNumber("INT-MEM-20260427-00000011")
                .customerType("MEMBER")
                .customerCode("MBR-0001")
                .amount(new BigDecimal("150.0000"))
                .currency("XAF")
                .status(PaymentIntentStatus.SUCCEEDED)
                .purpose("INVOICE_PAYMENT")
                .sourceType("BILLING_DOCUMENT")
                .sourceCode("INV-001")
                .build();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .id(10L)
                .transactionNumber("TXN-CAS-20260427-00000011")
                .paymentIntent(intent)
                .paymentMethod(PaymentMethod.CASH)
                .provider("CASH")
                .providerReference("REF-001")
                .amount(new BigDecimal("150.0000"))
                .currency("XAF")
                .status(PaymentTransactionStatus.SUCCEEDED)
                .paidAt(Instant.parse("2026-04-27T10:30:00Z"))
                .receivedBy("cashier-001")
                .build();
        PaymentAllocation allocation = PaymentAllocation.builder()
                .billingDocumentNumber("INV-001")
                .allocatedAmount(new BigDecimal("100.0000"))
                .paymentTransaction(transaction)
                .build();

        when(paymentTransactionRepository.findByTransactionNumber("TXN-CAS-20260427-00000011")).thenReturn(Optional.of(transaction));
        when(sequenceGenerator.next("receipt")).thenReturn("REC-2026-000011");
        when(paymentTransactionRepository.save(transaction)).thenReturn(transaction);
        when(paymentAllocationRepository.findAllByPaymentTransactionId(10L)).thenReturn(List.of(allocation));
        when(billingDocumentService.get("INV-001")).thenReturn(new BillingDocumentResponse(
                "INV-001",
                BillingDocumentType.INVOICE,
                BillingDocumentStatus.PARTIALLY_PAID,
                "MEMBER",
                "MBR-0001",
                "John Doe",
                "john@example.com",
                "242000000",
                null,
                true,
                "BILLABLE_ITEM",
                "BIL-001",
                "BOOKING",
                "BKG-001",
                "Reservation BKG-001",
                true,
                "Facture abonnement",
                null,
                null,
                "XAF",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                LocalDate.of(2026, 4, 27),
                LocalDate.of(2026, 4, 27),
                null,
                null,
                Instant.parse("2026-04-27T10:30:00Z"),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        ));

        PaymentReceiptResponse response = paymentReceiptService.getByTransactionNumber("TXN-CAS-20260427-00000011");

        assertEquals("REC-2026-000011", response.receiptNumber());
        assertEquals(new BigDecimal("150.0000"), response.paidAmount());
        assertEquals(new BigDecimal("100.0000"), response.allocatedAmount());
        assertEquals(new BigDecimal("50.0000"), response.advanceAmount());
        assertEquals(1, response.allocations().size());
        assertEquals("John Doe", response.customerName());
        assertNotNull(response.receiptIssuedAt());
    }
}
