package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.UnallocatedPaymentDtos;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot C9 : un virement de 590 000 sans reference est propose en premier sur la facture ouverte de
 * 590 000 du meme client ; l'affectation est marquee LATE ; elle ne depasse ni l'attente ni le du.
 */
class UnallocatedPaymentC9Test {

    private final PaymentTransactionRepository transactions = mock(PaymentTransactionRepository.class);
    private final PaymentAllocationRepository allocations = mock(PaymentAllocationRepository.class);
    private final BillingDocumentRepository documents = mock(BillingDocumentRepository.class);
    private final StaffProfileService staff = mock(StaffProfileService.class);
    private final BillingDocumentServiceImpl billing = mock(BillingDocumentServiceImpl.class);
    private UnallocatedPaymentService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<BillingDocumentServiceImpl> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(billing);
        service = new UnallocatedPaymentService(mock(JdbcTemplate.class), transactions, allocations, documents, staff, provider);
        when(staff.currentAuthor()).thenReturn(new StaffProfileService.Author("compta@elleaose.com", "Comptabilité", null));
        PaymentIntent intent = PaymentIntent.builder().customerType("CUSTOMER").customerCode("ORG-3").build();
        when(transactions.findByTransactionNumber("TXN-9")).thenReturn(Optional.of(PaymentTransaction.builder().id(9L).transactionNumber("TXN-9")
                .paymentIntent(intent).amount(new BigDecimal("700000"))
                .status(com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus.SUCCEEDED).build()));
        when(allocations.findAllByPaymentTransactionId(9L)).thenReturn(List.of());
        when(documents.findByDocumentNumber("FAC-50")).thenReturn(Optional.of(invoice("FAC-50", "ORG-3", "590000")));
        when(documents.findByDocumentNumber("FAC-51")).thenReturn(Optional.of(invoice("FAC-51", "ORG-4", "100000")));
    }

    @Test
    void shouldRankTheInvoiceOfTheSameAmountFirst() {
        UnallocatedPaymentDtos.Candidate exact = UnallocatedPaymentService.candidate(invoice("FAC-50", "ORG-3", "590000"), new BigDecimal("590000"), "");
        UnallocatedPaymentDtos.Candidate other = UnallocatedPaymentService.candidate(invoice("FAC-52", "ORG-3", "120000"), new BigDecimal("590000"), "");
        UnallocatedPaymentDtos.Candidate named = UnallocatedPaymentService.candidate(invoice("FAC-53", "ORG-3", "120000"), new BigDecimal("590000"),
                "VIREMENT FAC-53 MBOTE");

        assertThat(exact.score()).isGreaterThan(named.score()).isGreaterThan(other.score());
        assertThat(exact.reasons()).contains("même montant");
        assertThat(named.proposedAmount()).isEqualByComparingTo("120000");
    }

    @Test
    void shouldAllocateLateAndKeepTheRestPending() {
        UnallocatedPaymentDtos.AllocationResult result = service.allocate("TXN-9",
                List.of(new UnallocatedPaymentDtos.Allocation("FAC-50", new BigDecimal("590000"))));

        assertThat(result.stillPending()).isEqualByComparingTo("110000");
        verify(billing).applyPayment("FAC-50", new BigDecimal("590000"));
        ArgumentCaptor<PaymentAllocation> saved = ArgumentCaptor.forClass(PaymentAllocation.class);
        verify(allocations).save(saved.capture());
        assertThat(saved.getValue().getAllocationMode()).isEqualTo("LATE");
    }

    @Test
    void shouldRefuseMoreThanPendingOrAnotherCustomer() {
        assertThatThrownBy(() -> service.allocate("TXN-9", List.of(new UnallocatedPaymentDtos.Allocation("FAC-50", new BigDecimal("800000")))))
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("AFFECTATION_MONTANT"));
        assertThatThrownBy(() -> service.allocate("TXN-9", List.of(new UnallocatedPaymentDtos.Allocation("FAC-51", new BigDecimal("100000")))))
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("AFFECTATION_INVALIDE"));
    }

    private static BillingDocument invoice(String number, String customer, String due) {
        return BillingDocument.builder().documentNumber(number).customerCode(customer).balanceDue(new BigDecimal(due))
                .dueDate(LocalDate.now().plusDays(60)).build();
    }
}
