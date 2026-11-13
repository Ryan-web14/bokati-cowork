package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dto.request.DepositPlanRequest;
import com.sni.bokaticowork.features.billing.dto.request.DepositSettlementRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.DepositResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDepositMode;
import com.sni.bokaticowork.features.billing.enums.BillingDepositStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDeposit;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.BillingDepositRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot B3 : devis de 1 000 000 avec deux acomptes de 30 % et 20 % ; deux factures d'acompte encaissees ;
 * la facture finale deduit 500 000 et ne reclame que le reste.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DepositB3Test {

    @Mock private BillingDepositRepository deposits;
    @Mock private BillingDocumentRepository documents;
    @Mock private BillingDocumentLineRepository lines;
    @Mock private BillingDocumentServiceImpl billing;
    @Mock private BillingEventWriter eventWriter;

    @InjectMocks
    private DepositService service;

    private final List<BillingDeposit> saved = new ArrayList<>();
    private final AtomicInteger invoiceSeq = new AtomicInteger(1);
    private BillingDocument quote;

    @BeforeEach
    void setUp() {
        quote = BillingDocument.builder().id(1L).documentNumber("DEV-1").documentType(BillingDocumentType.QUOTE)
                .status(BillingDocumentStatus.ACCEPTED).totalAmount(new BigDecimal("1000000")).currency("XAF")
                .customerType("CUSTOMER").customerCode("CUS-1").customerName("Société").build();
        when(billing.serviceByNumber("DEV-1")).thenReturn(quote);
        when(documents.findByDocumentNumber("DEV-1")).thenReturn(Optional.of(quote));
        when(deposits.save(any(BillingDeposit.class))).thenAnswer(invocation -> {
            BillingDeposit deposit = invocation.getArgument(0);
            if (!saved.contains(deposit)) {
                deposit.setId((long) saved.size() + 1);
                saved.add(deposit);
            }
            return deposit;
        });
        when(deposits.findByDocumentNumberOrderByRankAsc("DEV-1")).thenAnswer(invocation -> List.copyOf(saved));
        when(billing.create(any(CreateBillingDocumentRequest.class))).thenAnswer(invocation -> {
            String number = "FAC-A" + invoiceSeq.getAndIncrement();
            BillingDocument invoice = BillingDocument.builder().documentNumber(number).documentType(BillingDocumentType.INVOICE)
                    .status(BillingDocumentStatus.DRAFT).build();
            when(billing.serviceByNumber(number)).thenReturn(invoice);
            BillingDocumentResponse response = mock(BillingDocumentResponse.class);
            when(response.documentNumber()).thenReturn(number);
            return response;
        });
    }

    @Test
    void shouldPlanTwoDepositsInPercentAndIssueTheirInvoices() {
        List<DepositResponse> plan = plan();

        assertThat(plan).extracting(DepositResponse::amount).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("300000"), new BigDecimal("200000"));
        assertThat(plan).extracting(DepositResponse::status).containsOnly("INVOICED");
        assertThat(plan).extracting(DepositResponse::depositInvoiceNumber).containsExactly("FAC-A1", "FAC-A2");
        verify(billing).issue("FAC-A1", null);
        verify(billing).issue("FAC-A2", null);
        assertThat(quote.getStatus()).isEqualTo(BillingDocumentStatus.DEPOSIT_REQUESTED);
        ArgumentCaptor<CreateBillingDocumentRequest> invoice = ArgumentCaptor.forClass(CreateBillingDocumentRequest.class);
        verify(billing, org.mockito.Mockito.times(2)).create(invoice.capture());
        assertThat(invoice.getAllValues().get(0).lines().get(0).description()).startsWith("Acompte n° 1 sur le devis DEV-1");
        assertThat(invoice.getAllValues().get(0).lines().get(0).taxable()).isFalse();
    }

    @Test
    void shouldRefuseDepositsAboveTheTotal() {
        assertThatThrownBy(() -> service.plan("DEV-1", new DepositPlanRequest(List.of(
                new DepositPlanRequest.Deposit("Commande", BillingDepositMode.PERCENTAGE, new BigDecimal("70"), null, null, true),
                new DepositPlanRequest.Deposit("Livraison", BillingDepositMode.AMOUNT, new BigDecimal("400000"), null, null, true)))))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("dépassent");
    }

    @Test
    void shouldComputeAPercentageOfSelectedLines() {
        BillingDocumentLine office = BillingDocumentLine.builder().lineOrder(1).totalAmount(new BigDecimal("600000")).build();
        BillingDocumentLine room = BillingDocumentLine.builder().lineOrder(2).totalAmount(new BigDecimal("400000")).build();

        BigDecimal amount = DepositService.amountOf(quote, List.of(office, room),
                new DepositPlanRequest.Deposit(null, BillingDepositMode.PERCENTAGE, new BigDecimal("50"), List.of(2), null, true));

        assertThat(amount).isEqualByComparingTo("200000");
    }

    @Test
    void shouldPassTheQuoteToDepositPaidOnceBothDepositsAreCashed() {
        plan();

        pay("FAC-A1", "300000");
        assertThat(quote.getStatus()).isEqualTo(BillingDocumentStatus.DEPOSIT_REQUESTED);
        pay("FAC-A2", "200000");

        assertThat(saved).extracting(BillingDeposit::getStatus).containsOnly(BillingDepositStatus.PAID);
        assertThat(quote.getStatus()).isEqualTo(BillingDocumentStatus.DEPOSIT_PAID);
    }

    @Test
    void shouldDeductBothDepositsFromTheFinalInvoice() {
        plan();
        pay("FAC-A1", "300000");
        pay("FAC-A2", "200000");
        BillingDocument finalInvoice = BillingDocument.builder().documentNumber("FAC-9").documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.ISSUED).totalAmount(new BigDecimal("1000000")).balanceDue(new BigDecimal("1000000")).build();
        when(billing.serviceByNumber("FAC-9")).thenReturn(finalInvoice);
        service.transferQuoteDeposits(quote, "FAC-9");
        when(deposits.findByFinalDocumentNumberOrderByRankAsc("FAC-9")).thenAnswer(invocation -> List.copyOf(saved));

        service.deductOnFinalInvoice(finalInvoice);

        verify(billing).applyPayment("FAC-9", new BigDecimal("300000"));
        verify(billing).applyPayment("FAC-9", new BigDecimal("200000"));
        assertThat(saved).extracting(BillingDeposit::getStatus).containsOnly(BillingDepositStatus.DEDUCTED);
        assertThat(saved).extracting(BillingDeposit::getDeductedOn).containsOnly("FAC-9");
    }

    @Test
    void shouldDropAnUnpaidDepositWhenTheFinalInvoiceIsIssued() {
        plan();
        BillingDocument depositInvoice = BillingDocument.builder().documentNumber("FAC-A2").status(BillingDocumentStatus.ISSUED).build();
        when(documents.findByDocumentNumber("FAC-A2")).thenReturn(Optional.of(depositInvoice));
        pay("FAC-A1", "300000");
        service.transferQuoteDeposits(quote, "FAC-9");
        when(deposits.findByFinalDocumentNumberOrderByRankAsc("FAC-9")).thenAnswer(invocation -> List.copyOf(saved));
        when(billing.serviceByNumber("FAC-9")).thenReturn(BillingDocument.builder().documentNumber("FAC-9")
                .balanceDue(new BigDecimal("1000000")).build());

        service.deductOnFinalInvoice(BillingDocument.builder().documentNumber("FAC-9").build());

        verify(billing).cancelAndArchive(eq("FAC-A2"), anyString());
        assertThat(saved.get(1).getStatus()).isEqualTo(BillingDepositStatus.CANCELLED);
    }

    @Test
    void shouldRefundACashedDepositOfARejectedQuote() {
        plan();
        pay("FAC-A1", "300000");
        quote.setStatus(BillingDocumentStatus.REJECTED);
        BillingDocumentResponse creditNote = mock(BillingDocumentResponse.class);
        when(creditNote.documentNumber()).thenReturn("AV-1");
        when(billing.createCreditNote(eq("FAC-A1"), any(CreateCreditNoteRequest.class))).thenReturn(creditNote);
        when(deposits.findById(1L)).thenReturn(Optional.of(saved.get(0)));

        DepositResponse settled = service.settle(1L, new DepositSettlementRequest("REFUND", "Devis refusé par le client"));

        assertThat(settled.status()).isEqualTo("REFUNDED");
        assertThat(settled.settlementDocument()).isEqualTo("AV-1");
        verify(billing).refundCreditNoteToWallet(eq("AV-1"), anyString(), isNull(), isNull());
    }

    @Test
    void shouldRefuseDepositsOnAnIssuedInvoice() {
        BillingDocument invoice = BillingDocument.builder().documentNumber("FAC-5").documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.ISSUED).totalAmount(new BigDecimal("100000")).build();
        when(billing.serviceByNumber("FAC-5")).thenReturn(invoice);

        assertThatThrownBy(() -> service.plan("FAC-5", new DepositPlanRequest(List.of(
                new DepositPlanRequest.Deposit(null, BillingDepositMode.AMOUNT, new BigDecimal("10000"), null, null, true)))))
                .hasMessageContaining("échéancier");
        verify(billing, never()).create(any());
    }

    private List<DepositResponse> plan() {
        return service.plan("DEV-1", new DepositPlanRequest(List.of(
                new DepositPlanRequest.Deposit("À la commande", BillingDepositMode.PERCENTAGE, new BigDecimal("30"), null, LocalDate.now(), true),
                new DepositPlanRequest.Deposit("À mi-parcours", BillingDepositMode.PERCENTAGE, new BigDecimal("20"), null,
                        LocalDate.now().plusDays(30), true))));
    }

    private void pay(String depositInvoiceNumber, String amount) {
        BillingDeposit deposit = saved.stream().filter(d -> depositInvoiceNumber.equals(d.getDepositInvoiceNumber())).findFirst().orElseThrow();
        when(deposits.findFirstByDepositInvoiceNumber(depositInvoiceNumber)).thenReturn(Optional.of(deposit));
        service.onDepositInvoicePayment(BillingDocument.builder().documentNumber(depositInvoiceNumber).paidAmount(new BigDecimal(amount))
                .depositForDocumentNumber("DEV-1").build());
    }
}
