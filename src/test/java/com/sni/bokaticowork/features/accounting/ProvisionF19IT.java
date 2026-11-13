package com.sni.bokaticowork.features.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.accounting.ReceivableRiskQuery;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.model.AccReceivableWriteoff;
import com.sni.bokaticowork.features.accounting.repository.AccReceivableStatusRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.*;
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
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.spring6.SpringTemplateEngine;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Lot F19 sur le schema de production : l'exemple de la section 6.5 du plan rejoue a l'identique ;
 * une facture WRITTEN_OFF de la facturation soldee en comptabilite ; un client bloque ne reserve pas
 * sans derogation ; la cloture refusee tant que la revue des provisions n'est pas validee.
 */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class, AnalyticAllocator.class,
        AccountingLetteringService.class, EntryNumbering.class, CashActorResolver.class, PostingEngine.class, PostingTemplate.class,
        BasicPostingResolvers.class, CashAccountingService.class, SalesAccountingService.class, ExchangeRateService.class,
        ReceivableRiskService.class, RiskProvisionService.class, ProvisionReportService.class, VoucherService.class, QrCodeRenderer.class,
        ProvisionF19IT.Beans.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProvisionF19IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    private static final String[] RIGHTS = {"RECEIVABLE_RISK_READ", "RECEIVABLE_RISK_CLASSIFY", "RECEIVABLE_RISK_IMPAIR",
            "RECEIVABLE_RISK_WRITE_OFF_PREPARE", "RECEIVABLE_RISK_WRITE_OFF_APPROVE", "RECEIVABLE_RISK_WRITE_OFF_DIRECTION",
            "RISK_PROVISION_MANAGE", "RISK_PROVISION_REVIEW", "ACCOUNTING_ENTRY_BALANCE"};

    @TestConfiguration
    static class Beans {
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
    @Autowired private ReceivableRiskService receivables;
    @Autowired private RiskProvisionService provisions;
    @Autowired private AccountingPeriodService periods;
    @Autowired private AccReceivableStatusRepository statuses;
    @Autowired private BillingDocumentRepository documents;
    @Autowired private BillingDocumentLineRepository documentLines;
    @Autowired private PaymentIntentRepository intents;
    @Autowired private PaymentTransactionRepository transactions;
    @Autowired private PaymentAllocationRepository allocations;
    @Autowired private CashRegisterRepository registers;
    @Autowired private CashSessionRepository sessions;
    @Autowired private DataSource dataSource;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier notifier;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private PlatformSettings platformSettings;

    private String code;
    private String customer;

    @BeforeEach
    void setUp() {
        com.sni.bokaticowork.core.settings.platform.PlatformSettingsStubs.fallbacks(platformSettings);
        when(sequences.next(anyString())).thenAnswer(invocation -> "BR-F19-" + SEQ.getAndIncrement() + "-" + UUID.randomUUID().toString().substring(0, 6));
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
        customer = "CUS-F19-" + code;
        as("comptable@elleaose.com", RIGHTS);
        // Journal propre au test : ses ecritures datees de decembre ne genent pas la chronologie stricte des autres tests sur OD.
        String journal = "R" + code.substring(0, 5);
        periods.createJournal(new AccountingRequests.CreateJournal(journal, "Journal du test F19", com.sni.bokaticowork.features.accounting.enums.JournalType.MISC,
                null, null, true, null, null));
        when(settings.text(eq("accounting.receivable.journal"), anyString())).thenReturn(journal);
        when(settings.text(eq("accounting.provision.journal"), anyString())).thenReturn(journal);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Exemple 6.5 : classement, depreciation de 250 000, encaissement partiel credite au 4162 et reprise de 100 000, perte et reprise du solde")
    void example65() {
        BillingDocument invoice = invoice("FAC-F19-" + code, "500000", "90000", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        run();

        receivables.classify(invoice.getDocumentNumber(), new ReceivableRiskService.ClassifyRequest("DOUBTFUL", "Relance HANDOVER sans effet",
                LocalDate.of(2026, 6, 30), null, null, null));
        assertThat(balance("4162", invoice.getDocumentNumber())).isEqualByComparingTo("590000");
        assertThat(balance("411", invoice.getDocumentNumber())).isEqualByComparingTo("0");

        ReceivableRiskService.ImpairmentRun december = receivables.runImpairment("2026-12");
        assertThat(december.lines()).filteredOn(line -> line.documentNumber().equals(invoice.getDocumentNumber()))
                .singleElement().satisfies(line -> {
                    assertThat(line.baseHt()).isEqualByComparingTo("500000");
                    assertThat(line.rate()).isEqualByComparingTo("50");
                    assertThat(line.amount()).isEqualByComparingTo("250000");
                });
        assertThat(balance("4912", invoice.getDocumentNumber())).isEqualByComparingTo("-250000");

        CashSession session = cashSession();
        // Le plan date l'encaissement du 15/03/2027 ; un paiement futur n'est pas encore repris par la comptabilite : il est date du jour.
        PaymentTransaction payment = payment("TXN-F19-" + code, "236000", session.getSessionNumber(), LocalDate.now(ZONE));
        allocations.save(PaymentAllocation.builder().paymentTransaction(payment).billingDocumentNumber(invoice.getDocumentNumber())
                .allocatedAmount(new BigDecimal("236000")).allocatedAt(Instant.now()).build());
        run();

        assertThat(balance("4162", invoice.getDocumentNumber())).isEqualByComparingTo("354000");
        assertThat(balance("411", invoice.getDocumentNumber())).isEqualByComparingTo("0");
        assertThat(balance("4912", invoice.getDocumentNumber())).isEqualByComparingTo("-150000");
        assertThat(balance("7594", invoice.getDocumentNumber())).isEqualByComparingTo("-100000");

        AccReceivableWriteoff prepared = receivables.prepareWriteOff(new ReceivableRiskService.WriteOffRequest(List.of(invoice.getDocumentNumber()),
                "SETTLEMENT", "Transaction signee", "GED-TRANSACTION-1", true));
        assertThat(prepared.getAmountHt()).isEqualByComparingTo("300000");
        assertThat(prepared.getAmountVat()).isEqualByComparingTo("54000");
        assertThat(prepared.getApprovalLevel()).isEqualTo("DIRECTION");
        assertThatThrownBy(() -> receivables.approveWriteOff(prepared.getId())).isInstanceOf(ForbiddenException.class);

        as("direction@elleaose.com", RIGHTS);
        AccReceivableWriteoff approved = receivables.approveWriteOff(prepared.getId());
        assertThat(approved.getImpairmentReleased()).isEqualByComparingTo("150000");
        assertThat(approved.getVoucherNumber()).isNotBlank();
        assertThat(balance("4162", invoice.getDocumentNumber())).isEqualByComparingTo("0");
        assertThat(balance("4912", invoice.getDocumentNumber())).isEqualByComparingTo("0");
        assertThat(balance("6511", invoice.getDocumentNumber())).isEqualByComparingTo("300000");
        assertThat(balance("4431", invoice.getDocumentNumber())).isNotNull();
        assertThat(balance("7594", invoice.getDocumentNumber())).isEqualByComparingTo("-250000");
        assertThat(documents.findByDocumentNumber(invoice.getDocumentNumber()).orElseThrow().getStatus()).isEqualTo(BillingDocumentStatus.WRITTEN_OFF);
        assertThat(statuses.findByDocumentNumber(invoice.getDocumentNumber()).orElseThrow().getStatus()).isEqualTo("WRITTEN_OFF");
    }

    @Test
    @DisplayName("Une facture passee WRITTEN_OFF dans la facturation est soldee en comptabilite")
    void billingWriteOffIsSynced() {
        BillingDocument invoice = invoice("FAC-F19W-" + code, "100000", "18000", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1));
        run();
        BillingDocument stored = documents.findByDocumentNumber(invoice.getDocumentNumber()).orElseThrow();
        stored.setStatus(BillingDocumentStatus.WRITTEN_OFF);
        documents.save(stored);

        assertThat(receivables.syncBillingWriteOffs(LocalDate.now(ZONE))).isGreaterThanOrEqualTo(1);

        assertThat(balance("411", invoice.getDocumentNumber())).isEqualByComparingTo("0");
        assertThat(balance("6511", invoice.getDocumentNumber())).isEqualByComparingTo("100000");
        assertThat(statuses.findByDocumentNumber(invoice.getDocumentNumber()).orElseThrow().getStatus()).isEqualTo("WRITTEN_OFF");
    }

    @Test
    @DisplayName("Client en BLOCK : pas de nouvelle vente sans derogation ; avec le droit de derogation, un avertissement trace")
    void blockedCustomer() {
        BillingDocument invoice = invoice("FAC-F19B-" + code, "200000", "36000", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1));
        run();
        receivables.classify(invoice.getDocumentNumber(), new ReceivableRiskService.ClassifyRequest("DOUBTFUL", "Client injoignable",
                LocalDate.of(2026, 9, 1), null, null, null));
        when(settings.text(eq("accounting.doubtful.customer-policy"), anyString())).thenReturn("BLOCK");

        assertThat(receivables.policyFor("CUSTOMER", customer)).isEqualTo(ReceivableRiskQuery.Policy.BLOCK);
        as("accueil@elleaose.com", "BOOKING_CREATE");
        assertThatThrownBy(() -> receivables.guardNewSale("CUSTOMER", customer, "la reservation")).isInstanceOf(ForbiddenException.class);
        as("manager@elleaose.com", "CUSTOMER_CREDIT_OVERRIDE");
        assertThat(receivables.guardNewSale("CUSTOMER", customer, "la reservation")).contains("Derogation");
        assertThat(receivables.policyFor("CUSTOMER", "CUS-SANS-RISQUE-" + code)).isEqualTo(ReceivableRiskQuery.Policy.NONE);

        // PREPAY : une vente payee d'avance passe, une vente a credit est refusee sans derogation.
        when(settings.text(eq("accounting.doubtful.customer-policy"), anyString())).thenReturn("PREPAY");
        as("accueil@elleaose.com", "BOOKING_CREATE");
        assertThat(receivables.guardNewSale("CUSTOMER", customer, "la reservation")).contains("payable d'avance");
        assertThatThrownBy(() -> receivables.guardCreditSale("CUSTOMER", customer, "la reservation facturee sur compte"))
                .isInstanceOf(ForbiddenException.class);
        as("manager@elleaose.com", "CUSTOMER_CREDIT_OVERRIDE");
        receivables.guardCreditSale("CUSTOMER", customer, "la reservation facturee sur compte");
    }

    @Test
    @DisplayName("La cloture est refusee tant que la revue des provisions n'est pas validee par une autre personne")
    void closingNeedsValidatedReview() {
        RiskProvisionService.RiskView risk = provisions.create(new RiskProvisionService.RiskRequest("CLIENT_LITIGATION", "Litige F19 " + code,
                customer, null, null, "Remboursement reclame", null, null, null, "PROBABLE", new BigDecimal("400000"), "Avis du conseil",
                LocalDate.of(2028, 6, 30), true, null, null));
        RiskProvisionService.RiskView provisioned = provisions.provision(risk.risk().getCode(),
                new RiskProvisionService.ProvisionRequest("ADJUST", null, LocalDate.of(2026, 10, 31), "Constitution", false));
        assertThat(provisioned.risk().getProvisionAmount()).isEqualByComparingTo("400000");
        assertThat(provisioned.risk().getProvisionAccount()).isEqualTo("191");

        String year = "2096";
        assertThat(provisions.closingCheck(2096)).anyMatch(finding -> finding.subject().equals("Revue des provisions"));
        var review = provisions.prepareReview(new RiskProvisionService.ReviewRequest(year, null));
        assertThatThrownBy(() -> provisions.validateReview(review.getId())).isInstanceOf(ForbiddenException.class);
        as("daf@elleaose.com", RIGHTS);
        assertThat(provisions.validateReview(review.getId()).getVoucherNumber()).isNotBlank();
        assertThat(provisions.closingCheck(2096)).noneMatch(finding -> finding.subject().equals("Revue des provisions"));

        RiskProvisionService.RiskView released = provisions.provision(risk.risk().getCode(),
                new RiskProvisionService.ProvisionRequest("USED_RELEASE", null, LocalDate.of(2026, 11, 5), "Transaction payee", true));
        assertThat(released.risk().getStatus()).isEqualTo("CLOSED");
    }

    // ------------------------------------------------------------------ outils

    private void run() {
        sales.feed();
        engine.processPending();
        sales.feed();
        engine.processPending();
        sales.autoLetter();
    }

    private BigDecimal balance(String account, String reference) {
        BigDecimal value = new JdbcTemplate(dataSource).queryForObject("SELECT COALESCE(SUM(l.debit - l.credit), 0) FROM acc_entry_line l "
                + "JOIN acc_entry e ON e.id = l.entry_id WHERE l.account_number = ? AND l.reference = ? AND e.status IN ('VALIDATED', 'REVERSED')",
                BigDecimal.class, account, reference);
        return value == null ? BigDecimal.ZERO : value;
    }

    private BillingDocument invoice(String number, String ht, String vat, LocalDate issue, LocalDate due) {
        BigDecimal net = new BigDecimal(ht);
        BigDecimal tax = new BigDecimal(vat);
        BillingDocument document = documents.save(BillingDocument.builder().documentNumber(number).documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.ISSUED).customerType("CUSTOMER").customerCode(customer).customerName("Client " + code)
                .currency("XAF").subtotalAmount(net).discountAmount(BigDecimal.ZERO).taxableAmount(net).vatAmount(tax)
                .additionalCentAmount(BigDecimal.ZERO).taxAmount(tax).totalAmount(net.add(tax)).paidAmount(BigDecimal.ZERO)
                .balanceDue(net.add(tax)).issueDate(issue).dueDate(due).issuedAt(issue.atStartOfDay(ZONE).toInstant()).build());
        documentLines.save(BillingDocumentLine.builder().document(document).lineOrder(1).lineType(BillingLineType.BOOKING)
                .description("Bureau").quantity(BigDecimal.ONE).unitPrice(net).discountAmount(BigDecimal.ZERO).taxable(true)
                .subtotalAmount(net).taxableAmount(net).vatAmount(tax).additionalCentAmount(BigDecimal.ZERO).taxAmount(tax)
                .totalAmount(net.add(tax)).build());
        return document;
    }

    private CashSession cashSession() {
        CashRegister register = registers.save(CashRegister.builder().registerCode("CSR-F19-" + code).name("Caisse F19 " + code)
                .businessEntityCode("BIZ").deviceCode("DEV-F19-" + code).active(true).cashControlEnabled(true).build());
        return sessions.save(CashSession.builder().sessionNumber("CSS-F19-" + code).cashRegister(register).status(CashSessionStatus.OPEN)
                .openedBy("caissier-f19-" + code.toLowerCase() + "@elleaose.com").openingAmount(BigDecimal.ZERO).build());
    }

    private PaymentTransaction payment(String number, String amount, String session, LocalDate paidOn) {
        PaymentIntent intent = intents.save(PaymentIntent.builder().intentNumber("INT-" + number).customerType("CUSTOMER").customerCode(customer)
                .amount(new BigDecimal(amount)).currency("XAF").status(PaymentIntentStatus.SUCCEEDED).build());
        return transactions.save(PaymentTransaction.builder().transactionNumber(number).paymentIntent(intent).paymentMethod(PaymentMethod.CASH)
                .amount(new BigDecimal(amount)).currency("XAF").status(PaymentTransactionStatus.SUCCEEDED)
                // Un paiement du jour est date d'une minute avant maintenant : a 10 h il serait futur la nuit, et la comptabilite l'ignorerait.
                .paidAt(paidOn.equals(LocalDate.now(ZONE)) ? Instant.now().minusSeconds(60) : paidOn.atTime(10, 0).atZone(ZONE).toInstant())
                .cashSessionNumber(session).build());
    }

    private static void as(String user, String... rights) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                Arrays.stream(rights).map(SimpleGrantedAuthority::new).toList()));
    }
}
