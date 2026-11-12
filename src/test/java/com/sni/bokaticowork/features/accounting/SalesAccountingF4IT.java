package com.sni.bokaticowork.features.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.model.AccPostingEvent;
import com.sni.bokaticowork.features.accounting.repository.AccEntryLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccEntryRepository;
import com.sni.bokaticowork.features.accounting.repository.AccPostingEventRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingChartService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingEntryService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingLetteringService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingPeriodService;
import com.sni.bokaticowork.features.accounting.service.implementation.CashAccountingService;
import com.sni.bokaticowork.features.accounting.service.implementation.PostingEngine;
import com.sni.bokaticowork.features.accounting.service.implementation.SalesAccountingService;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.BasicPostingResolvers;
import com.sni.bokaticowork.features.accounting.service.support.EntryNumbering;
import com.sni.bokaticowork.features.accounting.service.support.PostingTemplate;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Lot F4 sur le schema de production : facture, encaissement, lettrage, portefeuille, annulation. */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class, com.sni.bokaticowork.features.accounting.service.implementation.AnalyticAllocator.class, AccountingLetteringService.class,
        EntryNumbering.class, CashActorResolver.class, PostingEngine.class, PostingTemplate.class, BasicPostingResolvers.class,
        CashAccountingService.class, SalesAccountingService.class, SalesAccountingF4IT.Json.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SalesAccountingF4IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    @TestConfiguration
    static class Json {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired private SalesAccountingService sales;
    @Autowired private PostingEngine engine;
    @Autowired private AccountingLetteringService lettering;
    @Autowired private BillingDocumentRepository documents;
    @Autowired private BillingDocumentLineRepository documentLines;
    @Autowired private PaymentIntentRepository intents;
    @Autowired private PaymentTransactionRepository transactions;
    @Autowired private PaymentAllocationRepository allocations;
    @Autowired private WalletAccountRepository wallets;
    @Autowired private WalletLedgerEntryRepository ledger;
    @Autowired private CashRegisterRepository registers;
    @Autowired private CashSessionRepository sessions;
    @Autowired private AccPostingEventRepository events;
    @Autowired private AccEntryRepository entries;
    @Autowired private AccEntryLineRepository lines;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier notifier;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private PlatformSettings platformSettings;

    private String code;
    private String customer;

    @BeforeEach
    void setUp() {
        when(sequences.next(anyString())).thenAnswer(invocation -> "BR-F4-" + SEQ.getAndIncrement() + "-" + UUID.randomUUID().toString().substring(0, 6));
        when(settings.enabled()).thenReturn(true);
        when(settings.familyEnabled(anyString())).thenReturn(true);
        when(settings.startDate()).thenReturn(LocalDate.of(2026, 1, 1));
        when(settings.postingBatchSize()).thenReturn(500);
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.HOLD);
        when(settings.strictChronology()).thenReturn(true);
        when(settings.numberingWidth()).thenReturn(6);
        when(settings.paymentDelayMinutes()).thenReturn(0L);
        when(settings.letteringDifferenceThreshold()).thenReturn(new BigDecimal("500"));
        when(settings.text(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        customer = "CUS-F4-" + code;
    }

    @Test
    @DisplayName("Une facture payee en especes laisse le compte client solde et lettre")
    void invoicePaidInCash() {
        BillingDocument invoice = invoice("FAC-F4-" + code, BillingDocumentType.INVOICE, "10000", "1800", null);
        CashSession session = cashSession();
        PaymentTransaction payment = payment("TXN-F4-" + code, PaymentMethod.CASH, "11800", session.getSessionNumber());
        allocations.save(PaymentAllocation.builder().paymentTransaction(payment).billingDocumentNumber(invoice.getDocumentNumber())
                .allocatedAmount(new BigDecimal("11800")).allocatedAt(Instant.now()).build());

        run();

        assertThat(posted("INVOICE_ISSUED", invoice.getDocumentNumber())).isTrue();
        assertThat(posted("PAYMENT_RECEIVED", payment.getTransactionNumber())).isTrue();
        assertThat(lettering.openLines("411", customer)).isEmpty();
        assertThat(lettering.letterings("411", customer)).hasSize(1);
        CashRegister register = registers.findById(session.getCashRegister().getId()).orElseThrow();
        assertThat(lines.balanceOf(register.getTreasuryAccountNumber(), List.of(EntryStatus.VALIDATED))).isEqualByComparingTo("11800");
        assertThat(lines.balanceOf("7061", List.of(EntryStatus.VALIDATED))).isNotNull();
        // La facturation solde la facture a l'imputation : le controle ne signale alors plus d'ecart.
        assertThat(sales.checks(1000).invoiceGaps()).anyMatch(gap -> gap.documentNumber().equals(invoice.getDocumentNumber()));
        BillingDocument paid = documents.findByDocumentNumber(invoice.getDocumentNumber()).orElseThrow();
        paid.setBalanceDue(BigDecimal.ZERO);
        paid.setPaidAmount(paid.getTotalAmount());
        paid.setStatus(BillingDocumentStatus.PAID);
        documents.save(paid);
        assertThat(sales.checks(1000).invoiceGaps()).noneMatch(gap -> gap.documentNumber().equals(invoice.getDocumentNumber()));
    }

    @Test
    @DisplayName("Un rechargement de portefeuille par mobile money credite le 4191 du client ; le compte de liaison est solde")
    void walletTopUp() {
        PaymentTransaction topUp = payment("TXN-W-" + code, PaymentMethod.MOBILE_MONEY, "5000", null);
        WalletAccount wallet = wallets.save(WalletAccount.builder().walletNumber("WAL-" + code).ownerType("CUSTOMER").ownerCode(customer)
                .currency("XAF").status(WalletStatus.ACTIVE).availableBalance(new BigDecimal("5000")).ledgerBalance(new BigDecimal("5000"))
                .heldBalance(BigDecimal.ZERO).openedAt(Instant.now()).build());
        ledger.save(WalletLedgerEntry.builder().entryNumber("WLE-" + code).wallet(wallet).direction(WalletEntryDirection.CREDIT)
                .amount(new BigDecimal("5000")).currency("XAF").balanceAfter(new BigDecimal("5000")).entryType(WalletEntryType.TOPUP)
                .sourceType("PAYMENT_TRANSACTION").sourceCode(topUp.getTransactionNumber()).createdBy("SYSTEM").build());

        run();

        assertThat(posted("PAYMENT_RECEIVED", topUp.getTransactionNumber())).isTrue();
        assertThat(posted("WALLET_ENTRY", "WLE-" + code)).isTrue();
        BigDecimal customer4191 = lines.findByEntryIdInOrderByEntryIdAscLineNoAsc(entriesOf("WALLET_ENTRY", "WLE-" + code)).stream()
                .filter(line -> line.getAccountNumber().equals("4191")).map(line -> line.getCredit().subtract(line.getDebit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(customer4191).isEqualByComparingTo("5000");
        assertThat(lines.balanceOf("5212", List.of(EntryStatus.VALIDATED))).isGreaterThanOrEqualTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("Une facture annulee est contre-passee")
    void cancelledInvoice() {
        BillingDocument invoice = invoice("FAC-X-" + code, BillingDocumentType.INVOICE, "2000", "360", null);
        run();
        String number = events.findByEventTypeAndReference("INVOICE_ISSUED", invoice.getDocumentNumber()).orElseThrow().getEntryNumber();

        BillingDocument cancelled = documents.findByDocumentNumber(invoice.getDocumentNumber()).orElseThrow();
        cancelled.setStatus(BillingDocumentStatus.CANCELLED);
        cancelled.setCancelledAt(Instant.now());
        documents.save(cancelled);
        run();

        assertThat(posted("INVOICE_CANCELLED", invoice.getDocumentNumber())).isTrue();
        assertThat(entries.findByEntryNumber(number).orElseThrow().getStatus()).isEqualTo(EntryStatus.REVERSED);
        assertThat(lettering.openLines("411", customer)).isEmpty();
    }

    @Test
    @DisplayName("Un avoir se lettre avec la facture d'origine")
    void creditNoteLettersTheInvoice() {
        BillingDocument invoice = invoice("FAC-A-" + code, BillingDocumentType.INVOICE, "1000", "180", null);
        invoice("AV-A-" + code, BillingDocumentType.CREDIT_NOTE, "1000", "180", invoice.getDocumentNumber());
        run();
        assertThat(posted("CREDIT_NOTE_ISSUED", "AV-A-" + code)).isTrue();
        assertThat(lettering.openLines("411", customer)).isEmpty();
    }

    private void run() {
        sales.feed();
        engine.processPending();
        sales.feed();
        engine.processPending();
        sales.autoLetter();
    }

    private boolean posted(String type, String reference) {
        AccPostingEvent event = events.findByEventTypeAndReference(type, reference).orElseThrow(() -> new AssertionError(type + " " + reference + " absent"));
        assertThat(event.getStatus()).as(type + " " + reference + " : " + event.getFailureReason()).isEqualTo(com.sni.bokaticowork.features.accounting.enums.PostingEventStatus.POSTED);
        return true;
    }

    private List<Long> entriesOf(String type, String reference) {
        return entries.findBySourceTypeAndSourceCode(type, reference).stream().map(entry -> entry.getId()).toList();
    }

    private BillingDocument invoice(String number, BillingDocumentType type, String ht, String vat, String original) {
        BigDecimal net = new BigDecimal(ht);
        BigDecimal tax = new BigDecimal(vat);
        BillingDocument document = documents.save(BillingDocument.builder().documentNumber(number).documentType(type)
                .status(BillingDocumentStatus.ISSUED).customerType("CUSTOMER").customerCode(customer).customerName("Client " + code)
                .currency("XAF").subtotalAmount(net).discountAmount(BigDecimal.ZERO).taxableAmount(net).vatAmount(tax)
                .additionalCentAmount(BigDecimal.ZERO).taxAmount(tax).totalAmount(net.add(tax)).paidAmount(BigDecimal.ZERO)
                .balanceDue(net.add(tax)).issueDate(LocalDate.now(PostingEngine.AccountingClock.ZONE)).issuedAt(Instant.now())
                .originalDocumentNumber(original).build());
        documentLines.save(BillingDocumentLine.builder().document(document).lineOrder(1).lineType(BillingLineType.BOOKING)
                .description("Bureau").quantity(BigDecimal.ONE).unitPrice(net).discountAmount(BigDecimal.ZERO).taxable(true)
                .subtotalAmount(net).taxableAmount(net).vatAmount(tax).additionalCentAmount(BigDecimal.ZERO).taxAmount(tax)
                .totalAmount(net.add(tax)).build());
        return document;
    }

    private CashSession cashSession() {
        CashRegister register = registers.save(CashRegister.builder().registerCode("CSR-F4-" + code).name("Caisse F4 " + code)
                .businessEntityCode("BIZ").deviceCode("DEV-F4-" + code).active(true).cashControlEnabled(true).build());
        return sessions.save(CashSession.builder().sessionNumber("CSS-F4-" + code).cashRegister(register).status(CashSessionStatus.OPEN)
                .openedBy("caissier-f4-" + code.toLowerCase() + "@elleaose.com").openingAmount(BigDecimal.ZERO).build());
    }

    private PaymentTransaction payment(String number, PaymentMethod method, String amount, String session) {
        PaymentIntent intent = intents.save(PaymentIntent.builder().intentNumber("INT-" + number).customerType("CUSTOMER").customerCode(customer)
                .amount(new BigDecimal(amount)).currency("XAF").status(PaymentIntentStatus.SUCCEEDED).build());
        return transactions.save(PaymentTransaction.builder().transactionNumber(number).paymentIntent(intent).paymentMethod(method)
                .amount(new BigDecimal(amount)).currency("XAF").status(PaymentTransactionStatus.SUCCEEDED)
                .paidAt(Instant.now().minusSeconds(600)).cashSessionNumber(session).build());
    }
}
