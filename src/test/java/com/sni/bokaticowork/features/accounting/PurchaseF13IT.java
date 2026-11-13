package com.sni.bokaticowork.features.accounting;

import com.sni.bokaticowork.core.accounting.AccountingEventPublisher;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.model.AccBudgetLine;
import com.sni.bokaticowork.features.accounting.model.AccEntryLine;
import com.sni.bokaticowork.features.accounting.model.AccSupplier;
import com.sni.bokaticowork.features.accounting.repository.AccBudgetCommitmentRepository;
import com.sni.bokaticowork.features.accounting.repository.AccBudgetLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccEntryLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccEntryRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.EntryNumbering;
import com.sni.bokaticowork.features.fund.enums.FundNeedStatus;
import com.sni.bokaticowork.features.fund.model.FundNeedLine;
import com.sni.bokaticowork.features.fund.model.FundNeedStatement;
import com.sni.bokaticowork.features.fund.repository.FundNeedLineRepository;
import com.sni.bokaticowork.features.fund.repository.FundNeedStatementRepository;
import com.sni.bokaticowork.features.fund.service.FundNotifier;
import com.sni.bokaticowork.features.fund.service.FundRequestService;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.inventory.procurement.enums.SupplierStatus;
import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
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
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Lot F13 sur le schema de production : une commande validee engage le budget ; facture sur
 * commande avec retenue a la source, ecriture au journal des achats, realisation de l'engagement,
 * reglement lettre sur le 401 ; doublon refuse ; avoir.
 */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class, AnalyticAllocator.class,
        AccountingLetteringService.class, EntryNumbering.class, CashActorResolver.class, PurchaseService.class, BudgetEngine.class,
        BudgetService.class, FundRequestService.class, VoucherService.class, QrCodeRenderer.class, PurchaseF13IT.Templates.class, com.sni.bokaticowork.features.approval.service.ApprovalEngine.class, com.sni.bokaticowork.features.approval.service.ApprovalHierarchy.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class PurchaseF13IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private static final String[] PURCHASER = {"PURCHASE_READ", "SUPPLIER_MANAGE", "PURCHASE_ORDER_MANAGE", "SUPPLIER_INVOICE_MANAGE",
            "SUPPLIER_PAYMENT_MANAGE"};

    @TestConfiguration
    static class Templates {
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

    @Autowired private PurchaseService purchases;
    @Autowired private BudgetService budgets;
    @Autowired private BudgetEngine budgetEngine;
    @Autowired private AccBudgetLineRepository budgetLines;
    @Autowired private AccBudgetCommitmentRepository commitments;
    @Autowired private FundNeedStatementRepository needs;
    @Autowired private FundNeedLineRepository needLines;
    @Autowired private AccountingLetteringService lettering;
    @Autowired private AccEntryRepository entries;
    @Autowired private AccEntryLineRepository lines;
    @Autowired private SupplierRepository inventorySuppliers;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier accountingNotifier;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private CashRegisterService cashRegisterService;
    @MockitoBean private AccountingEventPublisher accountingEvents;
    @MockitoBean private FundNotifier fundNotifier;
    @MockitoBean private com.sni.bokaticowork.features.approval.service.ApprovalNotifier approvalNotifier;

    private Long budgetId;
    private String code;

    @BeforeEach
    void setUp() {
        when(sequences.next(anyString())).thenAnswer(invocation -> invocation.getArgument(0, String.class).toUpperCase() + "-F13-" + SEQ.getAndIncrement());
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.HOLD);
        when(settings.numberingWidth()).thenReturn(6);
        when(settings.strictChronology()).thenReturn(false);
        when(settings.text(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        as("daf@elleaose.com", "BUDGET_PREPARE", "BUDGET_CONSOLIDATE", "BUDGET_APPROVE", "BUDGET_ITEM_MANAGE");
        BudgetService.Budget budget = budgets.create(new BudgetService.CreateBudget(String.valueOf(LocalDate.now().getYear()), "Budget achats", null, "EMPTY", null));
        budgetId = budget.id();
        budgets.upsertLine(budgetId, new BudgetService.LineInput("ACCUEIL", null, new BigDecimal("500000"), null, null, "ALERT", null, null));
        budgets.transition(budgetId, "submit", null);
        budgets.transition(budgetId, "consolidate", null);
        budgets.transition(budgetId, "approve", null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Commande issue d'un etat de besoin, validee par une autre personne : le budget est engage ; facture avec retenue, reglement lettre")
    void orderCommitsBudgetThenInvoiceAndPayment() {
        as("acheteur@elleaose.com", PURCHASER);
        AccSupplier supplier = purchases.saveSupplier(AccSupplier.builder().code("FRN-" + code).name("Papeterie " + code).niu("M0" + code)
                .withholdingRate(new BigDecimal("5")).build());
        FundNeedStatement need = needs.save(FundNeedStatement.builder().number("EDB-" + code).requester("agent@elleaose.com").subject("Fournitures")
                .status(FundNeedStatus.VALIDATED).totalAmount(new BigDecimal("100000")).build());
        needLines.save(FundNeedLine.builder().statementId(need.getId()).lineNo(1).designation("Ramettes de papier").quantity(new BigDecimal("20"))
                .unitPrice(new BigDecimal("5000")).totalAmount(new BigDecimal("100000")).natureCode("ACHAT_FOURNITURES_BUREAU").build());

        PurchaseService.OrderView order = purchases.orderFromNeed(need.getNumber(), supplier.getCode(), null);
        assertThat(order.order().getTotalHt()).isEqualByComparingTo("100000");
        assertThat(order.order().getTotalVat()).isEqualByComparingTo("18000");
        assertThat(order.lines()).singleElement().satisfies(line -> assertThat(line.getAccountNumber()).isEqualTo("6055"));
        assertThat(needs.findByNumber(need.getNumber()).orElseThrow().getStatus()).isEqualTo(FundNeedStatus.TRANSFORMED);

        as("acheteur@elleaose.com", "PURCHASE_ORDER_VALIDATE");
        assertThatThrownBy(() -> purchases.validateOrder(order.order().getNumber(), null)).isInstanceOf(ForbiddenException.class);
        as("responsable@elleaose.com", "PURCHASE_ORDER_VALIDATE");
        assertThat(purchases.validateOrder(order.order().getNumber(), null).order().getStatus()).isEqualTo("VALIDATED");
        // Critere du lot : la commande validee engage le budget (hors taxe).
        assertThat(budgetEngine.amounts(line("ACCUEIL")).committed()).isEqualByComparingTo("100000");

        as("comptable@elleaose.com", PURCHASER);
        PurchaseService.InvoiceView invoice = purchases.createInvoice(new PurchaseService.InvoiceRequest(supplier.getCode(), "F-2026-" + code, null,
                LocalDate.now(), null, order.order().getNumber(), null, null, null, null, null, null));
        assertThat(invoice.invoice().getTotalTtc()).isEqualByComparingTo("118000");
        assertThat(invoice.invoice().getWithholdingAmount()).isEqualByComparingTo("5000");
        assertThat(invoice.invoice().getNetToPay()).isEqualByComparingTo("113000");
        assertThatThrownBy(() -> purchases.createInvoice(new PurchaseService.InvoiceRequest(supplier.getCode(), "F-2026-" + code, null, LocalDate.now(),
                null, null, null, null, null, null, null, List.of(new PurchaseService.LineInput("Doublon", BigDecimal.ONE, new BigDecimal("1000"), null,
                "6055", null, null, null))))).isInstanceOf(ConflictException.class);

        PurchaseService.InvoiceView posted = purchases.postInvoice(invoice.invoice().getNumber());
        List<AccEntryLine> entry = lines.findByEntryIdOrderByLineNoAsc(entries.findByEntryNumber(posted.invoice().getEntryNumber()).orElseThrow().getId());
        assertThat(entry).anySatisfy(l -> {
            assertThat(l.getAccountNumber()).isEqualTo("6055");
            assertThat(l.getDebit()).isEqualByComparingTo("100000");
        }).anySatisfy(l -> {
            assertThat(l.getAccountNumber()).isEqualTo("4452");
            assertThat(l.getDebit()).isEqualByComparingTo("18000");
            assertThat(l.getTaxCode()).isEqualTo("TVA18");
        }).anySatisfy(l -> {
            assertThat(l.getAccountNumber()).isEqualTo("401");
            assertThat(l.getThirdPartyCode()).isEqualTo(supplier.getCode());
            assertThat(l.getCredit()).isEqualByComparingTo("113000");
        }).anySatisfy(l -> {
            assertThat(l.getAccountNumber()).isEqualTo("447");
            assertThat(l.getCredit()).isEqualByComparingTo("5000");
        });
        assertThat(purchases.order(order.order().getNumber()).order().getStatus()).isEqualTo("INVOICED");
        assertThat(commitments.findBySourceTypeAndReference(PurchaseService.ORDER_SOURCE, order.order().getNumber()))
                .allSatisfy(c -> assertThat(c.getStatus()).isEqualTo("SETTLED"));
        assertThat(purchases.payables(null)).anySatisfy(p -> {
            assertThat(p.number()).isEqualTo(invoice.invoice().getNumber());
            assertThat(p.remaining()).isEqualByComparingTo("113000");
        });
        PurchaseService.WithholdingStatement withholdings = purchases.withholdings(YearMonth.now().toString());
        assertThat(withholdings.lines()).anySatisfy(w -> {
            assertThat(w.invoiceNumber()).isEqualTo(invoice.invoice().getNumber());
            assertThat(w.amount()).isEqualByComparingTo("5000");
        });

        PurchaseService.InvoiceView paid = purchases.pay(invoice.invoice().getNumber(),
                new PurchaseService.PaymentRequest(null, LocalDate.now(), "5211", "BQ-5211", "VIR-" + code));
        assertThat(paid.invoice().getStatus()).isEqualTo("PAID");
        assertThat(lettering.openLines("401", supplier.getCode())).isEmpty();
        assertThat(purchases.statement(supplier.getCode(), null, null).closing()).isEqualByComparingTo("0");
        assertThat(new String(purchases.orderPdf(order.order().getNumber()), 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Un avoir fournisseur diminue le reste a payer de sa facture et se lettre avec elle")
    void creditNote() {
        as("comptable@elleaose.com", PURCHASER);
        AccSupplier supplier = purchases.saveSupplier(AccSupplier.builder().code("FRN-A" + code).name("Prestataire " + code).defaultAccount("6055").build());
        PurchaseService.InvoiceView invoice = purchases.createInvoice(new PurchaseService.InvoiceRequest(supplier.getCode(), "FA-" + code, null, LocalDate.now(),
                null, null, null, null, null, null, null, List.of(new PurchaseService.LineInput("Fournitures", BigDecimal.ONE, new BigDecimal("10000"), null,
                null, null, null, null))));
        purchases.postInvoice(invoice.invoice().getNumber());
        PurchaseService.InvoiceView note = purchases.createInvoice(new PurchaseService.InvoiceRequest(supplier.getCode(), "AV-" + code, "CREDIT_NOTE",
                LocalDate.now(), null, null, invoice.invoice().getNumber(), null, null, null, null, List.of(new PurchaseService.LineInput("Retour",
                BigDecimal.ONE, new BigDecimal("2000"), null, null, null, null, null))));
        purchases.postInvoice(note.invoice().getNumber());

        assertThat(purchases.payables(null)).anySatisfy(p -> {
            assertThat(p.number()).isEqualTo(invoice.invoice().getNumber());
            assertThat(p.remaining()).isEqualByComparingTo("9440");
        });
        assertThat(purchases.invoice(invoice.invoice().getNumber()).invoice().getStatus()).isEqualTo("PARTIALLY_PAID");
    }

    @Test
    @DisplayName("Commande issue d'un etat de besoin dont la ligne porte une fiche fournisseur : le fournisseur comptable est cree depuis la fiche et reutilise")
    void orderFromNeedTakesInventorySupplier() {
        as("acheteur@elleaose.com", PURCHASER);
        Supplier inventory = inventorySuppliers.saveAndFlush(Supplier.builder().supplierCode("SUP-" + code).name("Papeterie liee " + code)
                .taxId("M1" + code).phone("060000000").email("papeterie@exemple.cg").status(SupplierStatus.ACTIVE).build());
        FundNeedStatement need = needs.save(FundNeedStatement.builder().number("EDB-L-" + code).requester("agent@elleaose.com").subject("Fournitures")
                .status(FundNeedStatus.VALIDATED).totalAmount(new BigDecimal("50000")).build());
        needLines.save(FundNeedLine.builder().statementId(need.getId()).lineNo(1).designation("Ramettes de papier").quantity(new BigDecimal("10"))
                .unitPrice(new BigDecimal("5000")).totalAmount(new BigDecimal("50000")).natureCode("ACHAT_FOURNITURES_BUREAU")
                .supplierCode(inventory.getSupplierCode()).supplier(inventory.getName()).build());

        PurchaseService.OrderView order = purchases.orderFromNeed(need.getNumber(), null, null);

        assertThat(order.supplier().getInventorySupplierId()).isEqualTo(inventory.getId());
        assertThat(order.supplier().getName()).isEqualTo(inventory.getName());
        assertThat(order.supplier().getNiu()).isEqualTo("M1" + code);
        assertThat(order.supplier().getAccountNumber()).isEqualTo("401");
        assertThat(order.order().getSupplierCode()).isEqualTo(order.supplier().getCode());
        assertThat(purchases.supplierFromInventory(inventory.getSupplierCode()).getCode()).isEqualTo(order.supplier().getCode());
    }

    private AccBudgetLine line(String item) {
        return budgetLines.findByBudgetIdOrderByItemCodeAscSectionCodeAsc(budgetId).stream()
                .filter(l -> l.getItemCode().equals(item)).findFirst().orElseThrow();
    }

    private static void as(String user, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }
}
