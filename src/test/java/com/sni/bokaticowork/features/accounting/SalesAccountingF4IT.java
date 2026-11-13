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
import com.sni.bokaticowork.features.accounting.service.implementation.ExchangeRateService;
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
        CashAccountingService.class, SalesAccountingService.class, ExchangeRateService.class, SalesAccountingF4IT.Json.class})
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
    @Autowired private com.sni.bokaticowork.features.billing.repository.BillingDepositRepository depositRepository;
    @Autowired private com.sni.bokaticowork.features.billing.repository.BillingCustomerChangeRepository changeRepository;
    @Autowired private ExchangeRateService rates;
    @Autowired private com.sni.bokaticowork.features.accounting.repository.CashExchangeRateRepository rateRepository;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier notifier;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private PlatformSettings platformSettings;

    private String code;
    private String customer;

    @BeforeEach
    void setUp() {
        com.sni.bokaticowork.core.settings.platform.PlatformSettingsStubs.fallbacks(platformSettings);
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
    @DisplayName("Lot C1 : une ligne exoneree reste en produit, pas en remise ; l'ecriture est equilibree")
    void exemptLineStaysInRevenue() {
        BillingDocument invoice = invoice("FAC-EXO-" + code, BillingDocumentType.INVOICE, "10000", "1800", null);
        invoice.setSubtotalAmount(new BigDecimal("15000"));
        invoice.setTotalAmount(new BigDecimal("16800"));
        invoice.setBalanceDue(new BigDecimal("16800"));
        documents.save(invoice);
        documentLines.save(BillingDocumentLine.builder().document(invoice).lineOrder(2).lineType(BillingLineType.SERVICE)
                .description("Domiciliation (exonérée)").quantity(BigDecimal.ONE).unitPrice(new BigDecimal("5000")).discountAmount(BigDecimal.ZERO)
                .taxable(false).subtotalAmount(new BigDecimal("5000")).taxableAmount(BigDecimal.ZERO).vatAmount(BigDecimal.ZERO)
                .additionalCentAmount(BigDecimal.ZERO).taxAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("5000")).build());

        run();

        assertThat(posted("INVOICE_ISSUED", invoice.getDocumentNumber())).isTrue();
        List<com.sni.bokaticowork.features.accounting.model.AccEntryLine> entryLines =
                lines.findByEntryIdInOrderByEntryIdAscLineNoAsc(entriesOf("INVOICE_ISSUED", invoice.getDocumentNumber()));
        BigDecimal debit = entryLines.stream().map(line -> line.getDebit()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = entryLines.stream().map(line -> line.getCredit()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debit).isEqualByComparingTo("16800");
        assertThat(credit).isEqualByComparingTo("16800");
        assertThat(entryLines).noneMatch(line -> line.getAccountNumber().startsWith("709") && line.getDebit().signum() > 0);
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

    @Test
    @DisplayName("Multidevise : facture en USD au taux du document, paiement au taux du jour, ecart de change au lettrage (776)")
    void foreignInvoicePaidAtAnotherRate() {
        LocalDate today = LocalDate.now(PostingEngine.AccountingClock.ZONE);
        upsertRate("USD", "610", today);
        BillingDocument invoice = invoice("FAC-USD-" + code, BillingDocumentType.INVOICE, "100", "18", null);
        invoice.setCurrency("USD");
        invoice.setExchangeRate(new BigDecimal("600"));
        documents.save(invoice);
        PaymentTransaction payment = payment("TXN-USD-" + code, PaymentMethod.MOBILE_MONEY, "118", null, "USD");
        allocations.save(PaymentAllocation.builder().paymentTransaction(payment).billingDocumentNumber(invoice.getDocumentNumber())
                .allocatedAmount(new BigDecimal("118")).allocatedAt(Instant.now()).build());

        run();

        assertThat(posted("INVOICE_ISSUED", invoice.getDocumentNumber())).isTrue();
        assertThat(posted("PAYMENT_RECEIVED", payment.getTransactionNumber())).isTrue();
        List<com.sni.bokaticowork.features.accounting.model.AccEntryLine> sale = lines.findByEntryIdInOrderByEntryIdAscLineNoAsc(
                entriesOf("INVOICE_ISSUED", invoice.getDocumentNumber()));
        com.sni.bokaticowork.features.accounting.model.AccEntryLine client = sale.stream().filter(line -> line.getAccountNumber().equals("411")).findFirst().orElseThrow();
        assertThat(client.getDebit()).isEqualByComparingTo("70800");
        assertThat(client.getCurrency()).isEqualTo("USD");
        assertThat(client.getForeignAmount()).isEqualByComparingTo("118");
        List<com.sni.bokaticowork.features.accounting.model.AccEntryLine> cash = lines.findByEntryIdInOrderByEntryIdAscLineNoAsc(
                entriesOf("PAYMENT_RECEIVED", payment.getTransactionNumber()));
        assertThat(cash.stream().filter(line -> line.getAccountNumber().equals("411")).map(line -> line.getCredit()).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("71980");
        // 118 USD factures a 600 et encaisses a 610 : gain de change de 1 180 XAF, compte client solde.
        assertThat(lettering.openLines("411", customer)).as("lettrages : " + lettering.letterings("411", customer) + " ; lignes : " + sale + cash).isEmpty();
        assertThat(lettering.letterings("411", customer)).singleElement().satisfies(letter -> {
            assertThat(letter.difference()).isEqualByComparingTo("-1180");
            assertThat(letter.differenceEntryNumber()).isNotNull();
            List<com.sni.bokaticowork.features.accounting.model.AccEntryLine> gap = lines.findByEntryIdInOrderByEntryIdAscLineNoAsc(
                    List.of(entries.findByEntryNumber(letter.differenceEntryNumber()).orElseThrow().getId()));
            assertThat(gap).anySatisfy(line -> {
                assertThat(line.getAccountNumber()).isEqualTo("776");
                assertThat(line.getCredit()).isEqualByComparingTo("1180");
            });
        });
    }

    @Test
    @DisplayName("Multidevise : sans taux du jour, le paiement en devise attend (erreur lisible) puis passe une fois le taux saisi")
    void foreignPaymentWaitsForARate() {
        LocalDate today = LocalDate.now(PostingEngine.AccountingClock.ZONE);
        String currency = "Q" + code.replaceAll("[^A-Z]", "X").substring(0, 2);
        PaymentTransaction payment = payment("TXN-Q-" + code, PaymentMethod.MOBILE_MONEY, "10", null, currency);
        var first = sales.feed();
        assertThat(first.errors()).anyMatch(error -> error.contains(payment.getTransactionNumber()) && error.contains("Aucun taux " + currency));
        upsertRate(currency, "1000", today);
        run();
        assertThat(posted("PAYMENT_RECEIVED", payment.getTransactionNumber())).isTrue();
        assertThat(lines.findByEntryIdInOrderByEntryIdAscLineNoAsc(entriesOf("PAYMENT_RECEIVED", payment.getTransactionNumber())).stream()
                .map(line -> line.getDebit()).reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("Taux recuperes : conversion par la parite de l'euro ; un taux saisi a la main n'est jamais remplace")
    void fetchedRatesKeepManualOnes() {
        LocalDate day = LocalDate.of(2031, 1, 1).plusDays(SEQ.getAndIncrement() * 7L + (code.hashCode() & 0xff));
        upsertRate("GBP", "800", day);
        ExchangeRateService.ProviderRates provider = ExchangeRateService.parseEuroRates(
                "{\"amount\":1.0,\"base\":\"EUR\",\"date\":\"" + day + "\",\"rates\":{\"USD\":1.1,\"GBP\":0.85}}");
        rates.store(provider, "api.frankfurter.app");
        assertThat(rateRepository.findByCurrencyAndValidOn("USD", day).orElseThrow()).satisfies(rate -> {
            assertThat(rate.getRate()).isEqualByComparingTo("596.324545");
            assertThat(rate.getSource()).isEqualTo("AUTO");
        });
        assertThat(rateRepository.findByCurrencyAndValidOn("GBP", day).orElseThrow()).satisfies(rate -> {
            assertThat(rate.getRate()).isEqualByComparingTo("800");
            assertThat(rate.getSource()).isEqualTo("MANUAL");
        });
        assertThat(rateRepository.findByCurrencyAndValidOn("EUR", day).orElseThrow().getRate()).isEqualByComparingTo("655.957");
        assertThat(rates.convert(new BigDecimal("10"), "USD", "XAF", day)).isEqualByComparingTo("5963.2455");
        assertThat(rates.convert(new BigDecimal("1"), "EUR", "USD", day)).isEqualByComparingTo("1.1000");
    }

    private void upsertRate(String currency, String value, LocalDate day) {
        var rate = rateRepository.findByCurrencyAndValidOn(currency, day)
                .orElseGet(() -> com.sni.bokaticowork.features.accounting.model.CashExchangeRate.builder().currency(currency).validOn(day).build());
        rate.setRate(new BigDecimal(value));
        rate.setSetBy("TEST");
        rate.setSource("MANUAL");
        rateRepository.save(rate);
    }

    @Test
    @DisplayName("Lot B3 : deux factures d'acompte creditent le 4191 ; la facture finale les deduit, le 4191 est solde, le 411 ne garde que le reste")
    void depositsAreSettledByTheFinalInvoice() {
        BillingDocument quote = invoice("DEV-F4-" + code, BillingDocumentType.QUOTE, "1000000", "0", null);
        BillingDocument first = depositInvoice("ACO1-F4-" + code, "300000", quote);
        BillingDocument second = depositInvoice("ACO2-F4-" + code, "200000", quote);
        CashSession session = cashSession();
        PaymentTransaction pay1 = payment("TXN-A1-" + code, PaymentMethod.CASH, "300000", session.getSessionNumber());
        allocations.save(PaymentAllocation.builder().paymentTransaction(pay1).billingDocumentNumber(first.getDocumentNumber())
                .allocatedAmount(new BigDecimal("300000")).allocatedAt(Instant.now()).build());
        PaymentTransaction pay2 = payment("TXN-A2-" + code, PaymentMethod.CASH, "200000", session.getSessionNumber());
        allocations.save(PaymentAllocation.builder().paymentTransaction(pay2).billingDocumentNumber(second.getDocumentNumber())
                .allocatedAmount(new BigDecimal("200000")).allocatedAt(Instant.now()).build());
        BillingDocument finalInvoice = invoice("FAC-FIN-" + code, BillingDocumentType.INVOICE, "1000000", "0", null);
        int rank = 0;
        for (BillingDocument deposit : List.of(first, second)) {
            depositRepository.save(com.sni.bokaticowork.features.billing.model.BillingDeposit.builder().documentId(quote.getId())
                    .documentNumber(quote.getDocumentNumber()).finalDocumentNumber(finalInvoice.getDocumentNumber()).rank(++rank)
                    .mode(com.sni.bokaticowork.features.billing.enums.BillingDepositMode.AMOUNT).value(deposit.getTotalAmount())
                    .amount(deposit.getTotalAmount()).status(com.sni.bokaticowork.features.billing.enums.BillingDepositStatus.DEDUCTED)
                    .depositInvoiceNumber(deposit.getDocumentNumber()).paidAmount(deposit.getTotalAmount()).deductedAmount(deposit.getTotalAmount())
                    .deductedOn(finalInvoice.getDocumentNumber()).deductedAt(Instant.now()).build());
        }

        run();

        assertThat(posted("INVOICE_ISSUED", first.getDocumentNumber())).isTrue();
        assertThat(events.findByEventTypeAndReference("INVOICE_ISSUED", first.getDocumentNumber()).orElseThrow().getRuleCode())
                .isEqualTo("SALES_DEPOSIT_INVOICE");
        assertThat(posted("DEPOSIT_APPLIED", first.getDocumentNumber())).isTrue();
        assertThat(posted("DEPOSIT_APPLIED", second.getDocumentNumber())).isTrue();
        BigDecimal deposits = lines.findPosted("4191", customer, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1), List.of(EntryStatus.VALIDATED))
                .stream().map(line -> line.getCredit().subtract(line.getDebit())).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(deposits).isEqualByComparingTo("0");
        BigDecimal receivable = lines.findPosted("411", customer, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1), List.of(EntryStatus.VALIDATED))
                .stream().map(line -> line.getDebit().subtract(line.getCredit())).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(receivable).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("Lot B6 : facture scellee et payee en partie passee de A a B, reglements transferes ; 411 de A solde, 411 de B au reste du")
    void customerChangeWithTransferredPayments() {
        String customerA = customer;
        BillingDocument original = invoice("FAC-A-" + code, BillingDocumentType.INVOICE, "100000", "18000", null);
        CashSession session = cashSession();
        PaymentTransaction payment = payment("TXN-B6-" + code, PaymentMethod.CASH, "50000", session.getSessionNumber());
        allocations.save(PaymentAllocation.builder().paymentTransaction(payment).billingDocumentNumber(original.getDocumentNumber())
                .allocatedAmount(new BigDecimal("50000")).allocatedAt(Instant.now()).build());
        BillingDocument unpaidNote = invoice("AV1-B6-" + code, BillingDocumentType.CREDIT_NOTE, "57627.12", "10372.88", original.getDocumentNumber());
        BillingDocument paidNote = invoice("AV2-B6-" + code, BillingDocumentType.CREDIT_NOTE, "42372.88", "7627.12", original.getDocumentNumber());
        original.setStatus(BillingDocumentStatus.CANCELLED);
        original.setCancelledAt(Instant.now());
        documents.save(original);
        String customerB = "CUS-F4B-" + code;
        customer = customerB;
        BillingDocument replacement = invoice("FAC-B-" + code, BillingDocumentType.INVOICE, "100000", "18000", null);
        customer = customerA;
        changeRepository.save(com.sni.bokaticowork.features.billing.model.BillingCustomerChange.builder()
                .originalDocumentNumber(original.getDocumentNumber()).newDocumentNumber(replacement.getDocumentNumber()).mode("REISSUE")
                .unpaidCreditNote(unpaidNote.getDocumentNumber()).paidCreditNote(paidNote.getDocumentNumber()).fromType("CUSTOMER")
                .fromCode(customerA).toType("CUSTOMER").toCode(customerB).paymentsMode("TRANSFER").transferredAmount(new BigDecimal("50000"))
                .reason("Facturer la maison mere").createdBy("awa").build());

        run();

        assertThat(posted("RECEIVABLE_TRANSFER", replacement.getDocumentNumber())).isTrue();
        // L'avoir porte la contre-passation : l'annulation de la facture d'origine n'en ajoute pas une seconde.
        assertThat(events.findByEventTypeAndReference("INVOICE_CANCELLED", original.getDocumentNumber())).isEmpty();
        assertThat(balance411(customerA)).isEqualByComparingTo("0");
        assertThat(balance411(customerB)).isEqualByComparingTo("68000");
    }

    private BigDecimal balance411(String thirdParty) {
        return lines.findPosted("411", thirdParty, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1), List.of(EntryStatus.VALIDATED))
                .stream().map(line -> line.getDebit().subtract(line.getCredit())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BillingDocument depositInvoice(String number, String amount, BillingDocument quote) {
        BillingDocument document = invoice(number, BillingDocumentType.INVOICE, amount, "0", null);
        document.setDepositForDocumentNumber(quote.getDocumentNumber());
        return documents.save(document);
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
        return payment(number, method, amount, session, "XAF");
    }

    private PaymentTransaction payment(String number, PaymentMethod method, String amount, String session, String currency) {
        PaymentIntent intent = intents.save(PaymentIntent.builder().intentNumber("INT-" + number).customerType("CUSTOMER").customerCode(customer)
                .amount(new BigDecimal(amount)).currency(currency).status(PaymentIntentStatus.SUCCEEDED).build());
        return transactions.save(PaymentTransaction.builder().transactionNumber(number).paymentIntent(intent).paymentMethod(method)
                .amount(new BigDecimal(amount)).currency(currency).status(PaymentTransactionStatus.SUCCEEDED)
                .paidAt(Instant.now().minusSeconds(600)).cashSessionNumber(session).build());
    }
}
