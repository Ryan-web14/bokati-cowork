package com.sni.bokaticowork.features.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.model.AccEntryLine;
import com.sni.bokaticowork.features.accounting.model.AccVatReturn;
import com.sni.bokaticowork.features.accounting.repository.AccEntryLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccEntryRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.*;
import com.sni.bokaticowork.features.accounting.service.support.*;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Lot F11 sur le schema de production : l'etat de TVA d'un mois egale la somme des taxes des
 * factures emises du mois (avoirs et annulations deduits), validation par une autre personne,
 * liquidation ; regime des encaissements avec TVA en attente puis exigible au prorata des reglements.
 */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class, AnalyticAllocator.class,
        AccountingLetteringService.class, EntryNumbering.class, CashActorResolver.class, PostingEngine.class, PostingTemplate.class,
        BasicPostingResolvers.class, CashAccountingService.class, SalesAccountingService.class, ExchangeRateService.class, VatService.class,
        VoucherService.class, QrCodeRenderer.class, VatF11IT.Config.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class VatF11IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private static final String[] RIGHTS = {"VAT_RETURN_READ", "VAT_RETURN_PREPARE", "VAT_RETURN_VALIDATE"};

    @TestConfiguration
    static class Config {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        SpringTemplateEngine templateEngine() {
            org.thymeleaf.templateresolver.ClassLoaderTemplateResolver resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/");
            resolver.setSuffix(".html");
            resolver.setCharacterEncoding("UTF-8");
            SpringTemplateEngine engine = new SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            return engine;
        }
    }

    @Autowired private SalesAccountingService sales;
    @Autowired private PostingEngine engine;
    @Autowired private VatService vat;
    @Autowired private AccountingEntryService entryService;
    @Autowired private BillingDocumentRepository documents;
    @Autowired private BillingDocumentLineRepository documentLines;
    @Autowired private PaymentIntentRepository intents;
    @Autowired private PaymentTransactionRepository transactions;
    @Autowired private PaymentAllocationRepository allocations;
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
        as("preparateur@elleaose.com", RIGHTS);
        when(sequences.next(anyString())).thenAnswer(invocation -> "BR-F11-" + SEQ.getAndIncrement() + "-" + UUID.randomUUID().toString().substring(0, 6));
        when(settings.enabled()).thenReturn(true);
        when(settings.familyEnabled(anyString())).thenReturn(true);
        when(settings.startDate()).thenReturn(LocalDate.of(2026, 1, 1));
        when(settings.postingBatchSize()).thenReturn(500);
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.HOLD);
        when(settings.strictChronology()).thenReturn(false);
        when(settings.numberingWidth()).thenReturn(6);
        when(settings.paymentDelayMinutes()).thenReturn(0L);
        when(settings.letteringDifferenceThreshold()).thenReturn(new BigDecimal("500"));
        when(settings.text(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        customer = "CUS-F11-" + code;
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Regime des debits : la TVA du mois egale la somme des taxes des factures, avoirs et annulations ; validation, liquidation")
    void invoiceRegimeReturnMatchesInvoices() {
        LocalDate day = LocalDate.of(2026, 7, 10);
        invoice("FAC-T1-" + code, BillingDocumentType.INVOICE, "10000", "1800", "0", null, day);
        invoice("FAC-T2-" + code, BillingDocumentType.INVOICE, "5000", "900", "90", null, day.plusDays(1));
        invoice("AV-T1-" + code, BillingDocumentType.CREDIT_NOTE, "1000", "180", "0", "FAC-T1-" + code, day.plusDays(2));
        BillingDocument cancelled = invoice("FAC-T3-" + code, BillingDocumentType.INVOICE, "2000", "360", "0", null, day.plusDays(3));
        run();
        cancelled.setStatus(BillingDocumentStatus.CANCELLED);
        cancelled.setCancelledAt(day.plusDays(4).atTime(10, 0).toInstant(ZoneOffset.UTC));
        documents.save(cancelled);
        run();
        // Achat avec TVA recuperable, saisi au journal des operations diverses.
        entryService.create(new AccountingRequests.Entry("OD", day.plusDays(5), null, "Achat fournitures " + code, null, null, List.of(
                new AccountingRequests.EntryLine("6055", null, "Fournitures", new BigDecimal("10000"), null, null, null, null, null, null, null, null),
                new AccountingRequests.EntryLine("4452", null, "TVA", new BigDecimal("500"), null, null, null, null, "TVA18", new BigDecimal("2778"), null, null),
                new AccountingRequests.EntryLine("571", null, "Caisse", null, new BigDecimal("10500"), null, null, null, null, null, null, null)), true));

        AccVatReturn draft = vat.prepare("2026-07");
        assertThat(draft.getRegime()).isEqualTo("INVOICE");
        assertThat(draft.getCollected()).isEqualByComparingTo("2610");
        assertThat(draft.getInvoiceTaxTotal()).isEqualByComparingTo("2610");
        assertThat(draft.getControlDifference()).isEqualByComparingTo("0");
        assertThat(draft.getDeductible()).isEqualByComparingTo("500");
        assertThat(draft.getDueAmount()).isEqualByComparingTo("2110");
        assertThat(draft.getCreditCarriedForward()).isEqualByComparingTo("0");
        VatService.Detail detail = vat.detail(draft.getId());
        assertThat(detail.documents()).hasSize(5).allSatisfy(line -> assertThat(line.difference()).isEqualByComparingTo("0"));
        // Toutes les factures du test sont scellees : le total des scellees egale la TVA collectee.
        assertThat(detail.sealedTaxTotal()).isEqualByComparingTo("2610");
        assertThat(detail.byCode()).anySatisfy(line -> {
            assertThat(line.taxCode()).isEqualTo("TVA18");
            assertThat(line.tax()).isEqualByComparingTo("2520");
            assertThat(line.base()).isEqualByComparingTo("14000");
        }).anySatisfy(line -> {
            assertThat(line.taxCode()).isEqualTo("CA");
            assertThat(line.tax()).isEqualByComparingTo("90");
        });

        assertThatThrownBy(() -> vat.validate(draft.getId())).isInstanceOf(BadRequestException.class).hasMessageContaining("autre personne");
        as("controleur@elleaose.com", RIGHTS);
        AccVatReturn validated = vat.validate(draft.getId());
        assertThat(validated.getStatus()).isEqualTo("VALIDATED");
        assertThatThrownBy(() -> vat.prepare("2026-07")).isInstanceOf(BadRequestException.class);

        AccVatReturn liquidated = vat.liquidate(draft.getId());
        assertThat(liquidated.getStatus()).isEqualTo("LIQUIDATED");
        List<AccEntryLine> liquidation = lines.findByEntryIdInOrderByEntryIdAscLineNoAsc(
                List.of(entries.findByEntryNumber(liquidated.getLiquidationEntryNumber()).orElseThrow().getId()));
        assertThat(liquidation).anySatisfy(line -> {
            assertThat(line.getAccountNumber()).isEqualTo("4441");
            assertThat(line.getCredit()).isEqualByComparingTo("2110");
        });
        // Apres liquidation, les comptes de TVA du mois sont soldes ; la piece n'entre pas dans un recalcul.
        BigDecimal july4431 = liquidationNet("4431", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        assertThat(july4431).isEqualByComparingTo("0");
        assertThat(new String(vat.csv(draft.getId()), StandardCharsets.UTF_8)).contains("FAC-T1-" + code).contains("AV-T1-" + code);
        byte[] pdf = vat.pdf(draft.getId());
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(vat.get(draft.getId()).getVoucherNumber()).startsWith("DTV");
    }

    @Test
    @DisplayName("Regime des encaissements : la TVA attend en 4438 et devient exigible au prorata de chaque reglement")
    void paymentRegimeChargesVatOnPayments() {
        when(settings.text(eq("accounting.vat.chargeability"), anyString())).thenReturn("PAYMENT");
        LocalDate day = LocalDate.of(2026, 8, 5);
        BillingDocument invoice = invoice("FAC-E1-" + code, BillingDocumentType.INVOICE, "10000", "1800", "0", null, day);
        run();
        assertThat(balance("4438", invoice.getDocumentNumber())).isEqualByComparingTo("-1800");

        pay("TXN-E1-" + code, invoice, "5900", day.plusDays(5));
        run();
        assertThat(balance("4438", invoice.getDocumentNumber())).isEqualByComparingTo("-900");
        AccVatReturn draft = vat.prepare("2026-08");
        assertThat(draft.getRegime()).isEqualTo("PAYMENT");
        assertThat(draft.getCollected()).isEqualByComparingTo("900");
        assertThat(draft.getControlDifference()).isNull();
        assertThat(draft.getPendingBalance()).isGreaterThanOrEqualTo(new BigDecimal("900"));

        pay("TXN-E2-" + code, invoice, "5900", day.plusDays(9));
        run();
        assertThat(balance("4438", invoice.getDocumentNumber())).isEqualByComparingTo("0");
        assertThat(vat.prepare("2026-08").getCollected()).isEqualByComparingTo("1800");
    }

    @Test
    @DisplayName("Prorata de deduction : seule la part deductible diminue la TVA due, le reste passe en charge a la liquidation")
    void deductionProrata() {
        when(settings.text(eq("accounting.vat.deduction-prorata"), anyString())).thenReturn("80");
        LocalDate day = LocalDate.of(2026, 10, 8);
        invoice("FAC-P1-" + code, BillingDocumentType.INVOICE, "10000", "1800", "0", null, day);
        run();
        entryService.create(new AccountingRequests.Entry("OD", day.plusDays(1), null, "Achat prorata " + code, null, null, List.of(
                new AccountingRequests.EntryLine("6055", null, "Fournitures", new BigDecimal("5000"), null, null, null, null, null, null, null, null),
                new AccountingRequests.EntryLine("4452", null, "TVA", new BigDecimal("900"), null, null, null, null, "TVA18", new BigDecimal("5000"), null, null),
                new AccountingRequests.EntryLine("571", null, "Caisse", null, new BigDecimal("5900"), null, null, null, null, null, null, null)), true));

        AccVatReturn draft = vat.prepare("2026-10");
        assertThat(draft.getDeductibleGross()).isEqualByComparingTo("900");
        assertThat(draft.getDeductionProrata()).isEqualByComparingTo("80");
        assertThat(draft.getDeductible()).isEqualByComparingTo("720");
        assertThat(draft.getNonDeductible()).isEqualByComparingTo("180");
        assertThat(draft.getDueAmount()).isEqualByComparingTo("1080");
        as("controleur@elleaose.com", RIGHTS);
        vat.validate(draft.getId());
        AccVatReturn liquidated = vat.liquidate(draft.getId());
        List<AccEntryLine> liquidation = lines.findByEntryIdInOrderByEntryIdAscLineNoAsc(
                List.of(entries.findByEntryNumber(liquidated.getLiquidationEntryNumber()).orElseThrow().getId()));
        assertThat(liquidation).anySatisfy(line -> {
            assertThat(line.getAccountNumber()).isEqualTo("658");
            assertThat(line.getDebit()).isEqualByComparingTo("180");
        }).anySatisfy(line -> {
            assertThat(line.getAccountNumber()).isEqualTo("4452");
            assertThat(line.getCredit()).isEqualByComparingTo("900");
        }).anySatisfy(line -> {
            assertThat(line.getAccountNumber()).isEqualTo("4441");
            assertThat(line.getCredit()).isEqualByComparingTo("1080");
        });
    }

    // ------------------------------------------------------------------ outils

    private void run() {
        sales.feed();
        engine.processPending();
        sales.feed();
        engine.processPending();
    }

    private BigDecimal balance(String account, String reference) {
        return lines.findAll().stream().filter(line -> account.equals(line.getAccountNumber()) && reference.equals(line.getReference()))
                .filter(line -> entries.findById(line.getEntryId()).map(entry -> entry.getStatus() == EntryStatus.VALIDATED
                        || entry.getStatus() == EntryStatus.REVERSED).orElse(false))
                .map(line -> line.getDebit().subtract(line.getCredit())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal liquidationNet(String account, LocalDate from, LocalDate to) {
        return lines.findPosted(account, null, from, to, List.of(EntryStatus.VALIDATED, EntryStatus.REVERSED)).stream()
                .filter(line -> line.getAccountNumber().equals(account))
                .map(line -> line.getCredit().subtract(line.getDebit())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BillingDocument invoice(String number, BillingDocumentType type, String ht, String vatAmount, String extra, String original, LocalDate date) {
        BigDecimal net = new BigDecimal(ht);
        BigDecimal tax = new BigDecimal(vatAmount);
        BigDecimal taxes = tax.add(new BigDecimal(extra));
        BillingDocument document = documents.save(BillingDocument.builder().documentNumber(number).documentType(type)
                .status(BillingDocumentStatus.ISSUED).customerType("CUSTOMER").customerCode(customer).customerName("Client " + code)
                .locked(true).fiscalNumber("FISC-" + number)
                .currency("XAF").subtotalAmount(net).discountAmount(BigDecimal.ZERO).taxableAmount(net).vatAmount(tax)
                .additionalCentAmount(new BigDecimal(extra)).taxAmount(taxes).totalAmount(net.add(taxes)).paidAmount(BigDecimal.ZERO)
                .balanceDue(net.add(taxes)).issueDate(date).issuedAt(date.atTime(9, 0).toInstant(ZoneOffset.UTC))
                .originalDocumentNumber(original).build());
        documentLines.save(BillingDocumentLine.builder().document(document).lineOrder(1).lineType(BillingLineType.BOOKING)
                .description("Bureau").quantity(BigDecimal.ONE).unitPrice(net).discountAmount(BigDecimal.ZERO).taxable(true).vatRate(new BigDecimal("18"))
                .subtotalAmount(net).taxableAmount(net).vatAmount(tax).additionalCentAmount(new BigDecimal(extra)).taxAmount(taxes)
                .totalAmount(net.add(taxes)).build());
        return document;
    }

    private void pay(String number, BillingDocument invoice, String amount, LocalDate date) {
        Instant paidAt = date.atTime(10, 0).toInstant(ZoneOffset.UTC);
        PaymentIntent intent = intents.save(PaymentIntent.builder().intentNumber("INT-" + number).customerType("CUSTOMER").customerCode(customer)
                .amount(new BigDecimal(amount)).currency("XAF").status(PaymentIntentStatus.SUCCEEDED).build());
        PaymentTransaction payment = transactions.save(PaymentTransaction.builder().transactionNumber(number).paymentIntent(intent)
                .paymentMethod(PaymentMethod.MOBILE_MONEY).amount(new BigDecimal(amount)).currency("XAF").status(PaymentTransactionStatus.SUCCEEDED)
                .paidAt(paidAt).build());
        allocations.save(PaymentAllocation.builder().paymentTransaction(payment).billingDocumentNumber(invoice.getDocumentNumber())
                .allocatedAmount(new BigDecimal(amount)).allocatedAt(paidAt).build());
    }

    private static void as(String user, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }
}
