package com.sni.bokaticowork.features.accounting;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.repository.AccEntryRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.EntryNumbering;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Lot F9 sur le schema de production : ventilation obligatoire, regles, cles, reflet en classe 9, reclassement, etats. */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class, AnalyticAllocator.class,
        AccountingLetteringService.class, EntryNumbering.class, CashActorResolver.class, AnalyticService.class, AnalyticReportService.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class AnalyticF9IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private static final LocalDate DAY = LocalDate.of(2026, 5, 12);

    @Autowired private AccountingEntryService entries;
    @Autowired private AnalyticService analytic;
    @Autowired private AnalyticReportService reports;
    @Autowired private AccEntryRepository entryRepository;
    @Autowired private EntityManager entityManager;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier notifier;
    @MockitoBean private SequenceGeneratorFacade sequences;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("compta@elleaose.com", null,
                List.of(new SimpleGrantedAuthority("ANALYTIC_SETTINGS_MANAGE"), new SimpleGrantedAuthority("ANALYTIC_RECLASS"))));
        when(sequences.next(anyString())).thenAnswer(invocation -> "BR-F9-" + SEQ.getAndIncrement());
        when(settings.analyticEnabled()).thenReturn(true);
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.HOLD);
        when(settings.strictChronology()).thenReturn(false);
        when(settings.numberingWidth()).thenReturn(6);
        when(settings.text(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        // Sans section par defaut, l'axe Activite oblige a ventiler.
        analytic.saveAxis(new AnalyticService.AxisInput("ACTIVITE", "Activite", "BOTH", null, 2, true));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Analytique active : une depense de classe 6 sans section ne se valide pas ; ventilee, elle se reflete en classe 9")
    void requiredSectionAndReflection() {
        assertThatThrownBy(() -> entries.create(expense("6055", "10000", null))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Activite obligatoire");

        AccountingResponses.Entry entry = entries.create(expense("6055", "10000", Map.of("ACTIVITE", "SALLES")));
        assertThat(entry.status()).isEqualTo(EntryStatus.VALIDATED);
        entityManager.flush();
        List<AnalyticService.Allocation> allocations = analytic.allocations(entryRepository.findByEntryNumber(entry.entryNumber()).orElseThrow().getId());
        assertThat(allocations).extracting(AnalyticService.Allocation::axisCode, AnalyticService.Allocation::sectionCode)
                .contains(org.assertj.core.groups.Tuple.tuple("ACTIVITE", "SALLES"), org.assertj.core.groups.Tuple.tuple("POSTE", "FOURNITURES"));
        assertThat(entryRepository.findBySourceTypeAndSourceCode("ANALYTIC_REFLECTION", entry.entryNumber())).singleElement()
                .satisfies(reflection -> {
                    assertThat(reflection.getJournalCode()).isEqualTo("CAG");
                    assertThat(reflection.getStatus()).isEqualTo(EntryStatus.VALIDATED);
                    assertThat(reflection.getTotalDebit()).isEqualByComparingTo("10000");
                });
    }

    @Test
    @DisplayName("Resultat par section, cle de repartition, reclassement et analyse d'un poste")
    void reports() {
        analytic.saveKey(new AnalyticService.KeyInput("SURFACES", "Surfaces", "ACTIVITE", LocalDate.of(2026, 1, 1),
                List.of(new AnalyticService.KeyLine("BUREAUX", new BigDecimal("60")), new AnalyticService.KeyLine("OPEN_SPACE", new BigDecimal("40")))));
        entries.create(expense("6055", "5000", Map.of("ACTIVITE", "SALLES")));
        entries.create(new AccountingRequests.Entry("OD", DAY, null, "Vente salle", null, null, List.of(
                line("411", "CUS-F9", "11800", null, null), line("7063", null, null, "10000", Map.of("ACTIVITE", "SALLES")),
                line("4431", null, null, "1800", null)), true));
        AccountingResponses.Entry loyer = entries.create(expense("622", "100000", Map.of("ACTIVITE", "KEY:SURFACES")));
        entityManager.flush();

        AnalyticReportService.Result result = reports.result("ACTIVITE", DAY.withDayOfMonth(1), DAY.withDayOfMonth(31));
        AnalyticReportService.SectionResult salles = result.sections().stream().filter(row -> row.section().equals("SALLES")).findFirst().orElseThrow();
        assertThat(salles.products()).isEqualByComparingTo("10000");
        assertThat(salles.directCharges()).isEqualByComparingTo("5000");
        assertThat(salles.margin()).isEqualByComparingTo("5000");
        assertThat(result.sections()).anySatisfy(row -> {
            assertThat(row.section()).isEqualTo("BUREAUX");
            assertThat(row.directCharges()).isEqualByComparingTo("60000");
        });

        Long loyerLine = analytic.allocations(entryRepository.findByEntryNumber(loyer.entryNumber()).orElseThrow().getId()).stream()
                .filter(allocation -> allocation.sectionCode().equals("OPEN_SPACE")).findFirst().orElseThrow().entryLineId();
        analytic.reclass(new AnalyticService.Reclass(loyerLine, "ACTIVITE", "OPEN_SPACE", "SALLES", new BigDecimal("10000"), "Salle louee en open space"));
        entityManager.flush();
        assertThat(reports.result("ACTIVITE", DAY.withDayOfMonth(1), DAY.withDayOfMonth(31)).sections()).anySatisfy(row -> {
            assertThat(row.section()).isEqualTo("SALLES");
            assertThat(row.directCharges()).isEqualByComparingTo("15000");
        });

        AnalyticReportService.ItemAnalysis loyerItem = reports.item("LOYER", DAY.withDayOfMonth(1), DAY.withDayOfMonth(31));
        assertThat(loyerItem.period()).isEqualByComparingTo("100000");
        assertThat(loyerItem.detail()).hasSize(1);
        assertThat(reports.dashboard(DAY.withDayOfMonth(1), DAY.withDayOfMonth(31))).extracting(AnalyticReportService.DashboardRow::item)
                .contains("LOYER", "FOURNITURES");
    }

    @Test
    @DisplayName("Poste propose par un responsable : en attente de validation ; fusion avec historique")
    void userDefinedItems() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("chef@elleaose.com", null,
                List.of(new SimpleGrantedAuthority("ANALYTIC_ITEM_CREATE"))));
        AnalyticService.Section proposed = analytic.createSection("POSTE", new AnalyticService.SectionInput(null, "Plantes vertes", null,
                "Accueil et vie du lieu", "VARIABLE", null, null, null, null, null, null, null));
        assertThat(proposed.code()).isEqualTo("PLANTES_VERTES");
        assertThat(proposed.status()).isEqualTo("PENDING_APPROVAL");
        assertThatThrownBy(() -> analytic.createSection("POSTE", new AnalyticService.SectionInput(null, "Plantes vertes", null, null, null,
                null, null, null, null, null, null, null))).hasMessageContaining("libelle");

        setUp();
        assertThat(analytic.approve("POSTE", "PLANTES_VERTES", true).status()).isEqualTo("ACTIVE");
        AnalyticService.Section target = analytic.merge("POSTE", "PLANTES_VERTES", "NON_CLASSE", true);
        assertThat(target.code()).isEqualTo("NON_CLASSE");
    }

    private static AccountingRequests.Entry expense(String account, String amount, Map<String, String> analytic) {
        return new AccountingRequests.Entry("OD", DAY, null, "Depense " + account, null, null, List.of(
                line(account, null, amount, null, analytic), line("571", null, null, amount, null)), true);
    }

    private static AccountingRequests.EntryLine line(String account, String thirdParty, String debit, String credit, Map<String, String> analytic) {
        return new AccountingRequests.EntryLine(account, thirdParty, null, debit == null ? null : new BigDecimal(debit),
                credit == null ? null : new BigDecimal(credit), null, null, null, null, null, null, null, analytic);
    }
}
