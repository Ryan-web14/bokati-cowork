package com.sni.bokaticowork.features.accounting;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingChartService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingEntryService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingLetteringService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingPeriodService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingReportService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Lot F1 sur le schema de production : plan initial, numerotation definitive sans trou, politiques
 * des pieces desequilibrees, contre-passation, lettrage, balance, et declencheurs d'immuabilite.
 */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class,
        AccountingLetteringService.class, AccountingReportService.class, EntryNumbering.class, CashActorResolver.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class AccountingFoundationF1IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger PROVISIONAL = new AtomicInteger(1);
    private static final LocalDate DAY = LocalDate.of(2026, 3, 10);

    @Autowired private DataSource dataSource;
    @Autowired private EntityManager entityManager;
    @Autowired private AccountingEntryService entries;
    @Autowired private AccountingLetteringService lettering;
    @Autowired private AccountingReportService reports;
    @Autowired private AccountingChartService chart;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier notifier;
    @MockitoBean private SequenceGeneratorFacade sequences;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("compta@elleaose.com", null,
                List.of(new SimpleGrantedAuthority("ACCOUNTING_ENTRY_BALANCE"))));
        when(sequences.next(anyString())).thenAnswer(invocation -> "BR-IT-" + PROVISIONAL.getAndIncrement());
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.HOLD);
        when(settings.suspenseAccount()).thenReturn("4718");
        when(settings.strictChronology()).thenReturn(true);
        when(settings.numberingWidth()).thenReturn(6);
        when(settings.letteringDifferenceThreshold()).thenReturn(new BigDecimal("500"));
        when(settings.letteringLossAccount()).thenReturn("658");
        when(settings.letteringGainAccount()).thenReturn("758");
        when(settings.unbalancedBlocksPeriodClose()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Le plan SYSCOHADA, l'exercice 2026 et ses journaux sont charges")
    void seed() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM acc_account", Integer.class)).isGreaterThanOrEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM acc_period p JOIN acc_fiscal_year y ON y.id = p.fiscal_year_id WHERE y.code = '2026'", Integer.class)).isEqualTo(12);
        assertThat(jdbc.queryForList("SELECT code FROM acc_journal", String.class)).contains("VT", "AC", "OD", "AN");
        assertThat(chart.get("4718").label()).contains("quilibrage");
    }

    @Test
    @DisplayName("Les pieces equilibrees recoivent des numeros consecutifs ; la continuite est verifiee")
    void consecutiveNumbers() {
        AccountingResponses.Entry first = entries.create(sale("Facture 1", "10000", "10000", true));
        AccountingResponses.Entry second = entries.create(sale("Facture 2", "5000", "5000", true));

        assertThat(first.status()).isEqualTo(EntryStatus.VALIDATED);
        assertThat(first.entryNumber()).isEqualTo("VT-2026-000001");
        assertThat(second.entryNumber()).isEqualTo("VT-2026-000002");
        assertThat(second.contentHash()).isNotBlank().isNotEqualTo(first.contentHash());
        entityManager.flush();

        AccountingResponses.NumberingCheck check = reports.checkContinuity("VT", "2026");
        assertThat(check.ok()).isTrue();
        assertThat(check.gaps()).isEmpty();
        assertThat(check.entryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Mode HOLD : une piece desequilibree reste en attente sans numero, puis se corrige")
    void holdPolicy() {
        AccountingResponses.Entry pending = entries.create(sale("Facture boiteuse", "10000", "9000", true));
        assertThat(pending.status()).isEqualTo(EntryStatus.PENDING_BALANCE);
        assertThat(pending.entryNumber()).isNull();
        assertThat(entries.pending()).extracting(AccountingResponses.Entry::provisionalNumber).contains(pending.provisionalNumber());

        AccountingResponses.Entry fixed = entries.update(pending.provisionalNumber(), sale("Facture corrigee", "10000", "10000", false));
        assertThat(fixed.status()).isEqualTo(EntryStatus.VALIDATED);
        assertThat(fixed.entryNumber()).isEqualTo("VT-2026-000001");
    }

    @Test
    @DisplayName("Mode compte d'attente : la difference passe sur 4718 et la piece est validee")
    void suspensePolicy() {
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.SUSPENSE_ACCOUNT);
        AccountingResponses.Entry entry = entries.create(sale("Facture a ecart", "10000", "9800", true));
        assertThat(entry.status()).isEqualTo(EntryStatus.VALIDATED);
        assertThat(entry.lines()).anySatisfy(line -> {
            assertThat(line.accountNumber()).isEqualTo("4718");
            assertThat(line.credit()).isEqualByComparingTo("200");
        });
        assertThat(entry.totalDebit()).isEqualByComparingTo(entry.totalCredit());
    }

    @Test
    @DisplayName("Contre-passation, lettrage client et balance equilibree")
    void reverseLetterAndBalance() {
        AccountingResponses.Entry invoice = entries.create(sale("Facture 10", "20000", "20000", true));
        AccountingResponses.Entry payment = entries.create(new AccountingRequests.Entry("OD", DAY, null, "Reglement facture 10", null, null,
                List.of(line("571", null, "20000", null), line("411", "CUS-IT-1", null, "20000")), true));
        AccountingResponses.Entry wrong = entries.create(sale("Facture en double", "3000", "3000", true));
        entityManager.flush();

        AccountingResponses.Entry reversal = entries.reverse(wrong.entryNumber(), new AccountingRequests.Reverse("Doublon", DAY));
        assertThat(reversal.status()).isEqualTo(EntryStatus.VALIDATED);
        assertThat(reversal.reversalOf()).isEqualTo(wrong.entryNumber());
        entityManager.flush();

        List<AccountingResponses.OpenLine> open = lettering.openLines("411", "CUS-IT-1");
        Long invoiceLine = open.stream().filter(line -> invoice.entryNumber().equals(line.entryNumber())).findFirst().orElseThrow().lineId();
        Long paymentLine = open.stream().filter(line -> payment.entryNumber().equals(line.entryNumber())).findFirst().orElseThrow().lineId();
        AccountingResponses.Lettering letter = lettering.letter(new AccountingRequests.Letter("411", "CUS-IT-1",
                List.of(new AccountingRequests.LetterLine(invoiceLine, null), new AccountingRequests.LetterLine(paymentLine, null)), false, false));
        // La contre-passation a deja lettre la piece annulee avec son inverse sous AA.
        assertThat(lettering.letterings("411", "CUS-IT-1")).extracting(AccountingResponses.Lettering::code).contains("AA");
        assertThat(open).extracting(AccountingResponses.OpenLine::entryNumber).doesNotContain(wrong.entryNumber(), reversal.entryNumber());
        assertThat(letter.code()).isEqualTo("AB");
        assertThat(letter.difference()).isEqualByComparingTo("0");
        entityManager.flush();
        assertThat(lettering.openLines("411", "CUS-IT-1")).extracting(AccountingResponses.OpenLine::entryNumber)
                .doesNotContain(invoice.entryNumber(), payment.entryNumber());

        AccountingResponses.TrialBalance balance = reports.trialBalance(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null);
        assertThat(balance.balanced()).isTrue();
        assertThat(balance.totals().movementDebit()).isEqualByComparingTo(balance.totals().movementCredit());
    }

    @Test
    @DisplayName("La base refuse de modifier une piece validee")
    void validatedEntryIsImmutable() {
        AccountingResponses.Entry entry = entries.create(sale("Facture figee", "1000", "1000", true));
        entityManager.flush();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThatThrownBy(() -> jdbc.update("UPDATE acc_entry SET label = 'autre' WHERE entry_number = ?", entry.entryNumber()))
                .hasMessageContaining("validee");
    }

    @Test
    @DisplayName("La base refuse de supprimer une piece validee")
    void validatedEntryCannotBeDeleted() {
        AccountingResponses.Entry entry = entries.create(sale("Facture gardee", "1000", "1000", true));
        entityManager.flush();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM acc_entry WHERE entry_number = ?", entry.entryNumber()))
                .hasMessageContaining("contre-passe");
    }

    @Test
    @DisplayName("La base refuse de valider une piece desequilibree sans marqueur")
    void unbalancedValidationIsRejectedByTheDatabase() {
        AccountingResponses.Entry draft = entries.create(sale("Brouillard", "1000", "900", false));
        entityManager.flush();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThatThrownBy(() -> jdbc.update("UPDATE acc_entry SET status = 'VALIDATED', entry_number = 'VT-2026-999999' "
                + "WHERE provisional_number = ?", draft.provisionalNumber()))
                .hasMessageContaining("desequilibree");
    }

    @Test
    @DisplayName("Le compteur de numerotation n'avance que d'un pas")
    void counterOnlyMovesByOne() {
        entries.create(sale("Facture compteur", "1000", "1000", true));
        entityManager.flush();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThatThrownBy(() -> jdbc.update("UPDATE acc_numbering_counter SET last_number = last_number + 5 WHERE journal_code = 'VT'"))
                .hasMessageContaining("un numero a la fois");
    }

    private static AccountingRequests.Entry sale(String label, String debit, String credit, boolean validate) {
        return new AccountingRequests.Entry("VT", DAY, null, label, null, null,
                List.of(line("411", "CUS-IT-1", debit, null), line("7061", null, null, credit)), validate);
    }

    private static AccountingRequests.EntryLine line(String account, String thirdParty, String debit, String credit) {
        return new AccountingRequests.EntryLine(account, thirdParty, null, debit == null ? null : new BigDecimal(debit),
                credit == null ? null : new BigDecimal(credit), null, null, null, null, null, null, null);
    }
}
