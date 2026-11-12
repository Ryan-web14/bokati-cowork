package com.sni.bokaticowork.features.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.model.AccAccrual;
import com.sni.bokaticowork.features.accounting.model.AccDeferralSchedule;
import com.sni.bokaticowork.features.accounting.repository.AccDeferralScheduleRepository;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Lot F20 sur le schema de production : l'abonnement annuel de la section 4.2 laisse 943 562 en 477
 * au 31 decembre 2026 ; relancer un mois ne produit aucune ecriture ; un gel de deux semaines decale
 * la fin de deux semaines ; un avoir de resiliation remet la ligne a zero ; estimation et extourne.
 */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class, AnalyticAllocator.class,
        AccountingLetteringService.class, EntryNumbering.class, CashActorResolver.class, PostingEngine.class, PostingTemplate.class,
        BasicPostingResolvers.class, CashAccountingService.class, SalesAccountingService.class, ExchangeRateService.class,
        ReceivableRiskService.class, DeferralService.class, DeferralReportService.class, VoucherService.class, QrCodeRenderer.class,
        DeferralF20IT.Beans.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DeferralF20IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    private static final String[] RIGHTS = {"ACCOUNTING_DEFERRAL_READ", "ACCOUNTING_DEFERRAL_RUN", "ACCOUNTING_DEFERRAL_EDIT_PERIOD",
            "ACCOUNTING_ACCRUAL_MANAGE", "ACCOUNTING_ENTRY_BALANCE"};

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

    @Autowired private DeferralService deferrals;
    @Autowired private AccountingPeriodService periods;
    @Autowired private AccDeferralScheduleRepository schedules;
    @Autowired private BillingDocumentRepository documents;
    @Autowired private BillingDocumentLineRepository documentLines;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier notifier;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private PlatformSettings platformSettings;

    private String code;

    @BeforeEach
    void setUp() {
        com.sni.bokaticowork.core.settings.platform.PlatformSettingsStubs.fallbacks(platformSettings);
        when(sequences.next(anyString())).thenAnswer(invocation -> "BR-F20-" + SEQ.getAndIncrement() + "-" + UUID.randomUUID().toString().substring(0, 6));
        when(settings.enabled()).thenReturn(true);
        when(settings.familyEnabled(anyString())).thenReturn(true);
        when(settings.startDate()).thenReturn(LocalDate.of(2026, 1, 1));
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.HOLD);
        when(settings.strictChronology()).thenReturn(false);
        when(settings.numberingWidth()).thenReturn(6);
        when(settings.text(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        as("comptable@elleaose.com", RIGHTS);
        // Journal propre au test : ses ecritures de fin de mois futures ne genent pas la chronologie des autres tests sur OD.
        String journal = "D" + code.substring(0, 5);
        periods.createJournal(new AccountingRequests.CreateJournal(journal, "Journal du test F20", com.sni.bokaticowork.features.accounting.enums.JournalType.MISC,
                null, null, true, null, null));
        when(settings.text(eq("accounting.deferral.journal"), anyString())).thenReturn(journal);
        when(settings.text(eq("accounting.accrual.reversal-journal"), anyString())).thenReturn(journal);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Abonnement annuel : 943 562 en 477 au 31/12/2026, relance sans ecriture, gel de deux semaines, avoir de resiliation")
    void annualSubscription() {
        BillingDocument invoice = document("FAC-F20-" + code, BillingDocumentType.INVOICE, null);
        BillingDocumentLine line = line(invoice, "ABO-F20-" + code, "1200000", LocalDate.of(2026, 10, 15), LocalDate.of(2027, 10, 14));

        deferrals.sync();
        AccDeferralSchedule schedule = schedules.findByKindAndSourceTypeAndSourceLineId(DeferralService.REVENUE, DeferralService.BILLING_LINE, line.getId())
                .orElseThrow();
        assertThat(schedule.getAmount()).isEqualByComparingTo("1200000");

        assertThat(need(deferrals.run("2026-10", "REVENUE"), schedule)).isEqualByComparingTo("1144110");
        assertThat(need(deferrals.run("2026-11", "REVENUE"), schedule)).isEqualByComparingTo("1045479");
        DeferralService.RunResult december = deferrals.run("2026-12", "REVENUE");
        assertThat(need(december, schedule)).isEqualByComparingTo("943562");
        assertThat(deferrals.detail("REVENUE", LocalDate.of(2026, 12, 31))).filteredOn(item -> item.scheduleId().equals(schedule.getId()))
                .singleElement().satisfies(item -> assertThat(item.recognized()).isEqualByComparingTo("256438"));

        DeferralService.RunResult again = deferrals.run("2026-12", "REVENUE");
        assertThat(again.lines()).filteredOn(item -> item.scheduleId().equals(schedule.getId()))
                .allSatisfy(item -> assertThat(item.difference()).isEqualByComparingTo("0"));

        AccDeferralSchedule frozen = deferrals.event(schedule.getId(), new DeferralService.EventRequest("FREEZE", LocalDate.of(2027, 1, 10), 14, null,
                "Gel de deux semaines"));
        assertThat(frozen.getEndDate()).isEqualTo(LocalDate.of(2027, 10, 28));

        BillingDocument creditNote = document("AVR-F20-" + code, BillingDocumentType.CREDIT_NOTE, invoice.getDocumentNumber());
        line(creditNote, "ABO-F20-" + code, "1200000", null, null);
        deferrals.sync();
        BigDecimal remaining = deferrals.detail("REVENUE", LocalDate.of(2027, 1, 31)).stream()
                .filter(item -> invoice.getDocumentNumber().equals(item.documentNumber()) || creditNote.getDocumentNumber().equals(item.documentNumber()))
                .map(DeferralService.DetailLine::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(remaining).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Facture non parvenue : estimation en fin de mois, extourne au premier jour du mois suivant, rapprochement")
    void accrualIsReversed() {
        AccAccrual accrual = deferrals.proposeAccrual(new DeferralService.AccrualRequest("ACCRUED_EXPENSE", "ELEC-" + code, "FRN-ELEC",
                "Electricite d'octobre", "605", null, "2026-10", new BigDecimal("150000"), null, "RECURRING"));
        AccAccrual posted = deferrals.postAccrual(accrual.getId());
        assertThat(posted.getEntryNumber()).isNotBlank();
        assertThat(posted.getReversalEntryNumber()).isNotBlank();
        AccAccrual matched = deferrals.matchAccrual(accrual.getId(), "FAC-SNE-" + code, new BigDecimal("162000"));
        assertThat(matched.getGap()).isEqualByComparingTo("12000");
    }

    private static BigDecimal need(DeferralService.RunResult result, AccDeferralSchedule schedule) {
        return result.lines().stream().filter(item -> item.scheduleId().equals(schedule.getId())).findFirst().orElseThrow().need();
    }

    private BillingDocument document(String number, BillingDocumentType type, String original) {
        BigDecimal net = new BigDecimal("1200000");
        BigDecimal tax = new BigDecimal("216000");
        return documents.save(BillingDocument.builder().documentNumber(number).documentType(type).status(BillingDocumentStatus.ISSUED)
                .customerType("CUSTOMER").customerCode("CUS-F20-" + code).customerName("Client " + code).currency("XAF").subtotalAmount(net)
                .discountAmount(BigDecimal.ZERO).taxableAmount(net).vatAmount(tax).additionalCentAmount(BigDecimal.ZERO).taxAmount(tax)
                .totalAmount(net.add(tax)).paidAmount(BigDecimal.ZERO).balanceDue(net.add(tax)).issueDate(LocalDate.of(2026, 10, 15))
                .issuedAt(LocalDate.of(2026, 10, 15).atStartOfDay(ZONE).toInstant()).originalDocumentNumber(original).build());
    }

    private BillingDocumentLine line(BillingDocument document, String itemCode, String ht, LocalDate start, LocalDate end) {
        BigDecimal net = new BigDecimal(ht);
        BigDecimal tax = net.multiply(new BigDecimal("0.18"));
        BillingDocumentLine line = BillingDocumentLine.builder().document(document).lineOrder(1).lineType(BillingLineType.SUBSCRIPTION)
                .itemCode(itemCode).description("Abonnement annuel").quantity(BigDecimal.ONE).unitPrice(net).discountAmount(BigDecimal.ZERO).taxable(true)
                .subtotalAmount(net).taxableAmount(net).vatAmount(tax).additionalCentAmount(BigDecimal.ZERO).taxAmount(tax).totalAmount(net.add(tax)).build();
        line.setServiceStart(start);
        line.setServiceEnd(end);
        return documentLines.save(line);
    }

    private static void as(String user, String... rights) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                Arrays.stream(rights).map(SimpleGrantedAuthority::new).toList()));
    }
}
