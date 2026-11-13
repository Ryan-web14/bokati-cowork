package com.sni.bokaticowork.features.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.repository.AccFiscalYearRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingChartService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingEntryService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingLetteringService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingPeriodService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingReportService;
import com.sni.bokaticowork.features.accounting.service.implementation.AnalyticAllocator;
import com.sni.bokaticowork.features.accounting.service.implementation.BalanceJustificationService;
import com.sni.bokaticowork.features.accounting.service.implementation.ClosingService;
import com.sni.bokaticowork.features.accounting.service.implementation.FinancialStatementService;
import com.sni.bokaticowork.features.accounting.service.implementation.LiasseService;
import com.sni.bokaticowork.features.accounting.service.implementation.ManagementReportService;
import com.sni.bokaticowork.features.accounting.service.implementation.TaxResultService;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.EntryNumbering;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Lot F15 sur le schema de production, sur un exercice 2022 isole : resultat fiscal (reintegration
 * d'un compte non deductible, deduction saisie, impot au taux et minimum de perception), ecriture
 * de l'impot sans effet sur le resultat fiscal, soldes de gestion mensuels, tableau de bord,
 * ratios, previsionnel coherent, liasse et son export Excel.
 */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class, AnalyticAllocator.class,
        AccountingLetteringService.class, EntryNumbering.class, CashActorResolver.class, AccountingReportService.class,
        FinancialStatementService.class, ClosingService.class, BalanceJustificationService.class, TaxResultService.class,
        ManagementReportService.class, LiasseService.class, LiasseF15IT.Beans.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LiasseF15IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private static final String[] RIGHTS = {"ACCOUNTING_CLOSING_READ", "ACCOUNTING_CLOSING_MANAGE", "ACCOUNTING_CLOSING_FINALIZE",
            "ACCOUNTING_REPORT_READ", "ACCOUNTING_STATEMENT_EDIT"};

    @TestConfiguration
    static class Beans {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired private TaxResultService taxes;
    @Autowired private ManagementReportService management;
    @Autowired private LiasseService liasse;
    @Autowired private AccountingPeriodService periods;
    @Autowired private AccountingEntryService entries;
    @Autowired private AccFiscalYearRepository fiscalYears;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier notifier;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private PlatformSettings platformSettings;

    @BeforeEach
    void setUp() {
        com.sni.bokaticowork.core.settings.platform.PlatformSettingsStubs.fallbacks(platformSettings);
        when(sequences.next(anyString())).thenAnswer(invocation -> "BR-F15-" + SEQ.getAndIncrement() + "-" + UUID.randomUUID().toString().substring(0, 6));
        when(settings.enabled()).thenReturn(true);
        when(settings.familyEnabled(anyString())).thenReturn(true);
        when(settings.startDate()).thenReturn(LocalDate.of(2022, 1, 1));
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.HOLD);
        when(settings.strictChronology()).thenReturn(false);
        when(settings.numberingWidth()).thenReturn(6);
        when(settings.text(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("comptable@elleaose.com", null,
                Arrays.stream(RIGHTS).map(SimpleGrantedAuthority::new).toList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Exercice 2022 : resultat fiscal et impot, ecriture d'impot neutre, SIG, tableau de bord, ratios, previsionnel, liasse")
    void liasse() {
        if (fiscalYears.findByCode("2022").isEmpty()) {
            periods.createFiscalYear(new AccountingRequests.CreateFiscalYear("2022", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)));
        }
        post("2022-03-15", "Ventes comptant F15", line("521", 2_000_000, 0), line("706", 0, 2_000_000));
        post("2022-04-10", "Loyer F15", line("622", 500_000, 0), line("521", 0, 500_000));
        taxes.nonDeductible("622", new BigDecimal("20"));
        try {
            taxes.add("2022", new TaxResultService.AdjustmentRequest("DEDUCTION", "PV", "Plus-value exoneree", null, new BigDecimal("50000"), null));
            assertThatThrownBy(() -> taxes.add("2022", new TaxResultService.AdjustmentRequest("AUTRE", null, "x", null, BigDecimal.ONE, null)))
                    .isInstanceOf(BadRequestException.class);

            TaxResultService.TaxResult result = taxes.compute("2022");
            assertThat(result.accountingResult()).isEqualByComparingTo("1500000");
            assertThat(result.totalReintegrations()).isEqualByComparingTo("100000");
            assertThat(result.totalDeductions()).isEqualByComparingTo("50000");
            assertThat(result.taxResult()).isEqualByComparingTo("1550000");
            assertThat(result.tax()).isEqualByComparingTo("434000");
            assertThat(result.minimumTax()).isEqualByComparingTo("20000");
            assertThat(result.taxDue()).isEqualByComparingTo("434000");

            // L'impot comptabilise (891) est reintegre : le resultat fiscal ne bouge pas.
            TaxResultService.TaxResult posted = taxes.post("2022");
            assertThat(posted.status()).isEqualTo("POSTED");
            assertThat(posted.accountingResult()).isEqualByComparingTo("1066000");
            assertThat(posted.taxResult()).isEqualByComparingTo("1550000");
            assertThat(posted.taxBooked()).isEqualByComparingTo("434000");
            assertThatThrownBy(() -> taxes.post("2022")).isInstanceOf(BadRequestException.class);
        } finally {
            taxes.nonDeductible("622", null);
        }

        ManagementReportService.Sig sig = management.sig("2022");
        assertThat(sig.months()).hasSize(12);
        assertThat(sig.months().get(2).values().get("XB")).isEqualByComparingTo("2000000");
        assertThat(sig.months().get(11).cumulative().get("XI")).isEqualByComparingTo("1066000");
        assertThat(management.sigTable(sig).rows()).hasSize(12);

        ManagementReportService.Dashboard dashboard = management.dashboard(LocalDate.of(2022, 12, 31));
        assertThat(dashboard.revenueYearToDate()).isEqualByComparingTo("2000000");
        assertThat(dashboard.netResult()).isEqualByComparingTo("1066000");
        assertThat(dashboard.revenueMonth()).isEqualByComparingTo("0");
        assertThat(management.dashboard(LocalDate.of(2022, 4, 30)).expensesByNature())
                .anyMatch(item -> item.label().startsWith("62") && item.value().compareTo(new BigDecimal("500000")) == 0);

        List<ManagementReportService.Ratio> ratios = management.ratios(LocalDate.of(2022, 12, 31));
        assertThat(ratios).hasSize(11);
        assertThat(ratios).filteredOn(item -> item.code().equals("NET_MARGIN")).singleElement()
                .satisfies(item -> assertThat(item.value()).isEqualByComparingTo("53.30"));

        ManagementReportService.Forecast forecast = management.forecast(LocalDate.of(2022, 12, 31), "WEEKS");
        assertThat(forecast.buckets()).hasSize(13);
        BigDecimal net = forecast.buckets().stream().map(ManagementReportService.Bucket::net).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(forecast.buckets().get(12).closing()).isEqualByComparingTo(forecast.opening().add(net));
        assertThat(management.forecast(LocalDate.of(2022, 12, 31), "MONTHS").buckets()).hasSize(12);

        LiasseService.Liasse built = liasse.build("2022");
        assertThat(built.sheets()).extracting(LiasseService.Sheet::code)
                .containsExactly("IDENTIFICATION", "DIRIGEANTS", "EFFECTIFS", "BILAN", "COMPTE_RESULTAT", "TFT", "NOTES", "RESULTAT_FISCAL");
        assertThat(built.provisional()).isTrue();
        assertThat(built.sheets().get(0).rows()).anyMatch(row -> "Denomination sociale".equals(row.get(0)));
        // Le taux non deductible du 622 est retire : 1 066 000 + 434 000 d'impot reintegre - 50 000 deduits.
        assertThat(built.sheets().get(7).rows()).anyMatch(row -> "RF".equals(row.get(0)) && new BigDecimal("1450000").compareTo((BigDecimal) row.get(2)) == 0);
        String excel = new String(liasse.excel(built), StandardCharsets.UTF_8);
        assertThat(excel).contains("<Workbook").contains("ss:Name=\"RESULTAT_FISCAL\"").contains("1450000");
        assertThatThrownBy(() -> liasse.finalize("2022")).isInstanceOf(BadRequestException.class);
    }

    private void post(String date, String label, AccountingRequests.EntryLine... entryLines) {
        entries.create(new AccountingRequests.Entry("OD", LocalDate.parse(date), null, label, null, null, Arrays.asList(entryLines), true));
    }

    private static AccountingRequests.EntryLine line(String account, long debit, long credit) {
        return new AccountingRequests.EntryLine(account, null, null, debit > 0 ? BigDecimal.valueOf(debit) : null,
                credit > 0 ? BigDecimal.valueOf(credit) : null, null, null, null, null, null, null, null);
    }
}
