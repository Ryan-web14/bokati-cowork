package com.sni.bokaticowork.features.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.FiscalYearStatus;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.model.AccEntryLine;
import com.sni.bokaticowork.features.accounting.repository.AccEntryLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccFiscalYearRepository;
import com.sni.bokaticowork.features.accounting.repository.AccLetteringRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingChartService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingEntryService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingLetteringService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingPeriodService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingReportService;
import com.sni.bokaticowork.features.accounting.service.implementation.AnalyticAllocator;
import com.sni.bokaticowork.features.accounting.service.implementation.BalanceJustificationService;
import com.sni.bokaticowork.features.accounting.service.implementation.ClosingService;
import com.sni.bokaticowork.features.accounting.service.implementation.FinancialStatementService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Lot F12 sur le schema de production, sur un exercice 2020 isole des autres tests : bilan et compte
 * de resultat provisoires equilibres, tableau des flux egal a la tresorerie du bilan, liste de
 * cloture, cloture, a-nouveaux egaux aux soldes de cloture avec le detail des comptes lettrables et
 * la suite du lettrage, edition definitive figee.
 */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class, AnalyticAllocator.class,
        AccountingLetteringService.class, EntryNumbering.class, CashActorResolver.class, AccountingReportService.class,
        FinancialStatementService.class, ClosingService.class, BalanceJustificationService.class, ClosingF12IT.Beans.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ClosingF12IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private static final String[] RIGHTS = {"ACCOUNTING_CLOSING_READ", "ACCOUNTING_CLOSING_MANAGE", "ACCOUNTING_CLOSING_FINALIZE",
            "ACCOUNTING_REPORT_READ", "ACCOUNTING_STATEMENT_EDIT", "ACCOUNTING_LETTERING_UNLETTER", "BALANCE_JUSTIFICATION_PREPARE",
            "BALANCE_JUSTIFICATION_REVIEW"};

    @TestConfiguration
    static class Beans {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired private FinancialStatementService statements;
    @Autowired private ClosingService closing;
    @Autowired private BalanceJustificationService justifications;
    @Autowired private AccountingPeriodService periods;
    @Autowired private AccountingEntryService entries;
    @Autowired private AccountingLetteringService lettering;
    @Autowired private AccFiscalYearRepository fiscalYears;
    @Autowired private AccEntryLineRepository lines;
    @Autowired private AccLetteringRepository letterings;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier notifier;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private PlatformSettings platformSettings;

    private String code;

    @BeforeEach
    void setUp() {
        com.sni.bokaticowork.core.settings.platform.PlatformSettingsStubs.fallbacks(platformSettings);
        when(sequences.next(anyString())).thenAnswer(invocation -> "BR-F12-" + SEQ.getAndIncrement() + "-" + UUID.randomUUID().toString().substring(0, 6));
        when(settings.enabled()).thenReturn(true);
        when(settings.familyEnabled(anyString())).thenReturn(true);
        when(settings.startDate()).thenReturn(LocalDate.of(2020, 1, 1));
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.HOLD);
        when(settings.strictChronology()).thenReturn(false);
        when(settings.numberingWidth()).thenReturn(6);
        when(settings.text(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        as("comptable@elleaose.com", RIGHTS);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Cloture 2020 : etats equilibres, tableau des flux, liste de cloture, a-nouveaux detailles, suite du lettrage, edition figee")
    void closeFiscalYear() {
        year("2020");
        year("2021");
        String customer = "CUS-F12-" + code;
        String supplier = "FRN-F12-" + code;
        String advance = "CUS-AV-F12-" + code;
        post("2020-01-02", "Apport en capital", line("521", null, 10_000_000, 0, null), line("101", null, 0, 10_000_000, null));
        post("2020-02-01", "Mobilier", line("2444", null, 2_400_000, 0, null), line("521", null, 0, 2_400_000, null));
        post("2020-03-01", "Facture A " + code, line("411", customer, 1_180_000, 0, "FAC-A-" + code), line("706", null, 0, 1_000_000, null),
                line("4431", null, 0, 180_000, null));
        post("2020-03-10", "Facture B " + code, line("411", customer, 590_000, 0, "FAC-B-" + code), line("706", null, 0, 500_000, null),
                line("4431", null, 0, 90_000, null));
        post("2020-04-01", "Reglement A " + code, line("521", null, 1_180_000, 0, null), line("411", customer, 0, 1_180_000, "FAC-A-" + code));
        post("2020-05-01", "Loyer " + code, line("622", null, 300_000, 0, null), line("401", supplier, 0, 300_000, "LOY-" + code));
        // Avance d'un client sans facture : client crediteur, reclasse au passif.
        post("2020-06-01", "Avance " + code, line("521", null, 100_000, 0, null), line("411", advance, 0, 100_000, "AV-" + code));
        post("2020-12-31", "Dotation " + code, line("6813", null, 480_000, 0, null), line("2844", null, 0, 480_000, null));
        letter("411", customer, lineId("Facture A " + code, "411"), lineId("Reglement A " + code, "411"));
        // Reglement de la facture B en 2021, lettre avant la cloture : le lettrage passera sur la ligne d'a-nouveau.
        post("2021-01-15", "Reglement B " + code, line("521", null, 590_000, 0, null), line("411", customer, 0, 590_000, "FAC-B-" + code));
        letter("411", customer, lineId("Facture B " + code, "411"), lineId("Reglement B " + code, "411"));

        // Etats provisoires.
        FinancialStatementService.Statement balance = statements.balanceSheet(LocalDate.of(2020, 12, 31));
        assertThat(balance.provisional()).isTrue();
        assertThat(balance.firstYear()).isTrue();
        assertThat(balance.unclassified()).isEmpty();
        assertThat(balance.checks()).allMatch(FinancialStatementService.Check::ok);
        assertThat(balance.net("BZ")).isEqualByComparingTo("11390000");
        assertThat(balance.net("DZ")).isEqualByComparingTo("11390000");
        assertThat(balance.net("AM")).isEqualByComparingTo("1920000");
        assertThat(balance.rows().stream().filter(row -> row.code().equals("AM")).findFirst().orElseThrow().gross()).isEqualByComparingTo("2400000");
        assertThat(balance.net("BI")).isEqualByComparingTo("590000");
        assertThat(balance.net("BS")).isEqualByComparingTo("8880000");
        assertThat(balance.net("CJ")).isEqualByComparingTo("720000");
        assertThat(balance.net("DK")).isEqualByComparingTo("270000");
        assertThat(balance.net("DJ")).isEqualByComparingTo("300000");
        assertThat(balance.net("DI")).isEqualByComparingTo("100000");

        FinancialStatementService.Statement income = statements.incomeStatement(null, LocalDate.of(2020, 12, 31));
        assertThat(income.net("XB")).isEqualByComparingTo("1500000");
        assertThat(income.net("RH")).isEqualByComparingTo("300000");
        assertThat(income.net("RL")).isEqualByComparingTo("480000");
        assertThat(income.net("XD")).isEqualByComparingTo("1200000");
        assertThat(income.net("XI")).isEqualByComparingTo("720000");

        FinancialStatementService.Statement flows = statements.cashFlow("2020");
        assertThat(flows.net("FA")).isEqualByComparingTo("1200000");
        assertThat(flows.net("ZB")).isEqualByComparingTo("1280000");
        assertThat(flows.net("ZC")).isEqualByComparingTo("-2400000");
        assertThat(flows.net("ZD")).isEqualByComparingTo("10000000");
        assertThat(flows.net("ZH")).isEqualByComparingTo("8880000");
        assertThat(flows.checks()).allMatch(FinancialStatementService.Check::ok);
        assertThat(statements.table(balance).rows()).isNotEmpty();

        // Notes annexes : montants repris, texte redige puis valide.
        assertThat(closing.notes("2020")).anyMatch(note -> note.code().equals("7") && note.total().compareTo(new BigDecimal("490000")) == 0);
        closing.saveNote("2020", "2", new ClosingService.NoteRequest("Comptes etablis selon le SYSCOHADA revise, systeme normal.", false));
        assertThat(closing.validateNote("2020", "2", true).status()).isEqualTo("VALIDATED");

        // Justificatif de solde : la facture B reste ouverte au 31/12 (son reglement est de 2021), egale au solde.
        LocalDate yearEnd = LocalDate.of(2020, 12, 31);
        BalanceJustificationService.Justification justification = justifications.justification("411", customer, yearEnd);
        assertThat(justification.items()).singleElement().satisfies(item -> assertThat(item.reference()).isEqualTo("FAC-B-" + code));
        assertThat(justification.difference()).isEqualByComparingTo("0");
        BalanceJustificationService.Collective collective = justifications.collective("411", yearEnd);
        assertThat(collective.ok()).isTrue();
        assertThat(collective.abnormal()).singleElement().satisfies(item -> assertThat(item.thirdPartyCode()).isEqualTo(advance));
        BalanceJustificationService.SavedJustification prepared = justifications.prepare("411", customer, yearEnd, "Facture B reglee en janvier");
        assertThatThrownBy(() -> justifications.review(prepared.id())).isInstanceOf(BadRequestException.class);
        as("reviseur@elleaose.com", RIGHTS);
        assertThat(justifications.review(prepared.id()).reviewedBy()).isNotNull();
        as("comptable@elleaose.com", RIGHTS);
        assertThat(justifications.justificationFile("2020")).anyMatch(item -> item.accountNumber().equals("411") && "JUSTIFIED".equals(item.status()));

        // Confirmations de solde : deux tiers, l'un conforme, l'autre en ecart puis rapproche.
        List<com.sni.bokaticowork.features.accounting.model.AccConfirmationRequest> requests = justifications.select(
                new BalanceJustificationService.SelectionRequest("411", yearEnd, 20, null, null));
        assertThat(requests).hasSize(2);
        assertThat(justifications.select(new BalanceJustificationService.SelectionRequest("411", yearEnd, 20, null, null))).isEmpty();
        var invoiced = requests.stream().filter(item -> customer.equals(item.getThirdPartyCode())).findFirst().orElseThrow();
        var advanced = requests.stream().filter(item -> advance.equals(item.getThirdPartyCode())).findFirst().orElseThrow();
        justifications.send(invoiced.getNumber(), false);
        assertThat(justifications.reply(invoiced.getNumber(), new BalanceJustificationService.ReplyRequest("MAIL", new BigDecimal("590000"), null))
                .getStatus()).isEqualTo("AGREED");
        assertThat(justifications.reply(advanced.getNumber(), new BalanceJustificationService.ReplyRequest("PORTAL", new BigDecimal("-90000"), "Avance de 90 000"))
                .getDifference()).isEqualByComparingTo("-10000");
        assertThat(justifications.reconcile(advanced.getNumber(), "Frais bancaires de 10 000 a la charge du client").getStatus()).isEqualTo("RECONCILED");
        assertThat(justifications.letter(invoiced.getNumber()).rows()).hasSize(1);

        // Liste de cloture : seules les etapes a cocher manquent.
        ClosingService.Checklist checklist = closing.checklist("2020");
        assertThat(checklist.blocking()).containsExactlyInAnyOrder("STAFF_ADVANCES", "CASH_COUNT");
        assertThatThrownBy(() -> closing.close("2020", null)).isInstanceOf(BadRequestException.class);
        closing.tick("2020", "STAFF_ADVANCES", true, "Aucune avance");
        closing.tick("2020", "CASH_COUNT", true, "PV au 31/12");

        ClosingService.OpeningPreview preview = closing.previewOpening("2020");
        assertThat(preview.netResult()).isEqualByComparingTo("720000");
        assertThat(preview.totalDebit()).isEqualByComparingTo(preview.totalCredit());

        ClosingService.CloseResult result = closing.close("2020", null);
        assertThat(result.netResult()).isEqualByComparingTo("720000");
        assertThat(result.openingEntryNumber()).isNotNull();
        assertThat(result.carriedLines()).isEqualTo(3);
        assertThat(fiscalYears.findByCode("2020").orElseThrow().getStatus()).isEqualTo(FiscalYearStatus.CLOSED);
        assertThatThrownBy(() -> periods.periodFor(LocalDate.of(2020, 6, 1), true)).isInstanceOf(BadRequestException.class);

        // A-nouveaux : soldes de cloture, resultat en 131, facture B reprise avec sa reference.
        List<AccEntryLine> opening = lines.findByEntryIdOrderByLineNoAsc(jdbc.queryForObject(
                "SELECT id FROM acc_entry WHERE entry_number = ?", Long.class, result.openingEntryNumber()));
        assertThat(sum(opening, "521", true)).isEqualByComparingTo("8880000");
        assertThat(sum(opening, "2844", false)).isEqualByComparingTo("480000");
        assertThat(sum(opening, "131", false)).isEqualByComparingTo("720000");
        AccEntryLine carriedInvoice = opening.stream().filter(item -> item.getAccountNumber().equals("411") && customer.equals(item.getThirdPartyCode()))
                .findFirst().orElseThrow();
        assertThat(carriedInvoice.getReference()).isEqualTo("FAC-B-" + code);
        assertThat(carriedInvoice.getCarriedFromLineId()).isEqualTo(lineId("Facture B " + code, "411"));

        // Suite du lettrage : le reglement de 2021 est lettre avec la ligne d'a-nouveau ; la facture fournisseur reste ouverte en 2021.
        assertThat(lettering.openLines("411", customer)).isEmpty();
        assertThat(lettering.openLines("401", supplier)).singleElement()
                .satisfies(item -> assertThat(item.entryDate()).isEqualTo(LocalDate.of(2021, 1, 1)));
        var carry = letterings.findAll().stream().filter(item -> item.getKind() == com.sni.bokaticowork.features.accounting.enums.LetteringKind.CARRY_FORWARD
                && customer.equals(item.getThirdPartyCode())).findFirst().orElseThrow();
        assertThatThrownBy(() -> lettering.unletter(carry.getId(), "essai")).isInstanceOf(BadRequestException.class);

        // Exercice suivant : bilan equilibre avec le resultat en instance.
        FinancialStatementService.Statement next = statements.balanceSheet(LocalDate.of(2021, 1, 31));
        assertThat(next.net("BZ")).isEqualByComparingTo(next.net("DZ"));
        assertThat(next.net("BS")).isEqualByComparingTo("9470000");
        assertThat(next.net("CJ")).isEqualByComparingTo("720000");
        assertThat(next.firstYear()).isFalse();
        assertThat(next.rows().stream().filter(row -> row.code().equals("BS")).findFirst().orElseThrow().previous()).isEqualByComparingTo("8880000");

        // Edition definitive figee et verifiable ; l'exercice clos ne se reclot pas.
        ClosingService.Edition edition = closing.finalize("2020", new ClosingService.EditionRequest("BILAN", null));
        assertThat(edition.number()).startsWith("EF-2020-BILAN-");
        assertThat(closing.verify(edition.number())).isTrue();
        assertThat(statements.balanceSheet(LocalDate.of(2020, 12, 31)).provisional()).isFalse();
        assertThatThrownBy(() -> closing.close("2020", null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Formule des totaux : signes, ordre libre, poste absent nul")
    void formula() {
        java.util.Map<String, BigDecimal> values = java.util.Map.of("TA", new BigDecimal("100"), "RA", new BigDecimal("30"), "RB", new BigDecimal("-5"));
        assertThat(invokeFormula("TA-RA-RB", values)).isEqualByComparingTo("75");
        assertThat(invokeFormula("-RA+TA+ZZ", values)).isEqualByComparingTo("70");
    }

    // ================================================================== outils

    private void year(String yearCode) {
        if (fiscalYears.findByCode(yearCode).isEmpty()) {
            int value = Integer.parseInt(yearCode);
            periods.createFiscalYear(new AccountingRequests.CreateFiscalYear(yearCode, LocalDate.of(value, 1, 1), LocalDate.of(value, 12, 31)));
        }
    }

    private void post(String date, String label, AccountingRequests.EntryLine... entryLines) {
        entries.create(new AccountingRequests.Entry("OD", LocalDate.parse(date), null, label, null, null, Arrays.asList(entryLines), true));
    }

    private static AccountingRequests.EntryLine line(String account, String thirdParty, long debit, long credit, String reference) {
        return new AccountingRequests.EntryLine(account, thirdParty, null, debit > 0 ? BigDecimal.valueOf(debit) : null,
                credit > 0 ? BigDecimal.valueOf(credit) : null, null, null, null, null, null, null, reference);
    }

    private Long lineId(String label, String account) {
        return jdbc.queryForObject("SELECT l.id FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id WHERE e.label = ? AND l.account_number = ?",
                Long.class, label, account);
    }

    private void letter(String account, String thirdParty, Long... ids) {
        List<AccountingRequests.LetterLine> request = new ArrayList<>();
        for (Long id : ids) {
            request.add(new AccountingRequests.LetterLine(id, null));
        }
        lettering.letter(new AccountingRequests.Letter(account, thirdParty, request, false, false));
    }

    private static BigDecimal sum(List<AccEntryLine> entryLines, String account, boolean debit) {
        return entryLines.stream().filter(item -> item.getAccountNumber().equals(account)).map(item -> debit ? item.getDebit() : item.getCredit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal invokeFormula(String formula, java.util.Map<String, BigDecimal> values) {
        try {
            var method = FinancialStatementService.class.getDeclaredMethod("formula", String.class, java.util.Map.class);
            method.setAccessible(true);
            return (BigDecimal) method.invoke(null, formula, values);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void as(String user, String... rights) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                Arrays.stream(rights).map(SimpleGrantedAuthority::new).toList()));
    }
}
