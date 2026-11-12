package com.sni.bokaticowork.features.accounting;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.AccEntryLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccEntryRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.EntryNumbering;
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
import org.thymeleaf.spring6.SpringTemplateEngine;

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
 * Lot F17 sur le schema de production : une echeance d'emprunt s'ecrit seule ; un journal de paie
 * importe s'equilibre et solde les avances retenues ; comptes courants d'associes et hors bilan.
 */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class, AnalyticAllocator.class,
        AccountingLetteringService.class, EntryNumbering.class, CashActorResolver.class, LoanService.class, PartnerAccountService.class,
        CommitmentService.class, PayrollService.class, VoucherService.class, QrCodeRenderer.class, FinancingF17IT.Templates.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
class FinancingF17IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private static final String[] RIGHTS = {"FINANCING_READ", "FINANCING_MANAGE", "PAYROLL_IMPORT", "PAYROLL_POST", "PAYROLL_SETTINGS_MANAGE"};

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

    @Autowired private LoanService loans;
    @Autowired private PartnerAccountService partners;
    @Autowired private CommitmentService commitments;
    @Autowired private PayrollService payroll;
    @Autowired private AccountingEntryService entryService;
    @Autowired private AccountingLetteringService lettering;
    @Autowired private AccEntryRepository entries;
    @Autowired private AccEntryLineRepository lines;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier notifier;
    @MockitoBean private SequenceGeneratorFacade sequences;

    private String code;

    @BeforeEach
    void setUp() {
        as("tresorier@elleaose.com", RIGHTS);
        when(sequences.next(anyString())).thenAnswer(invocation -> "BR-F17-" + SEQ.getAndIncrement());
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.HOLD);
        when(settings.numberingWidth()).thenReturn(6);
        when(settings.strictChronology()).thenReturn(false);
        when(settings.text(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Emprunt : deblocage, echeances ecrites seules a leur date, remboursement anticipe (nouvelle version), interets courus et extourne, note")
    void loanLifecycle() {
        LoanService.LoanView created = loans.create(new LoanService.LoanRequest("EMP-" + code, "BANK", null, "Banque Postale", "Amenagement",
                new BigDecimal("1200000"), LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 15), new BigDecimal("12"), null, null, 1, 12, null, null,
                "ANNUITY", new BigDecimal("20000"), BigDecimal.ZERO, null, null, null, null, null, "BANQUE-PRINCIPALE", null, null, null));
        assertThat(created.schedule()).hasSize(12);
        String loanCode = created.loan().getCode();
        AccLoan loan = loans.disburse(loanCode).loan();
        assertThat(loan.getStatus()).isEqualTo("ACTIVE");
        assertThat(entries.findByEntryNumber(loan.getDisbursementEntry()).orElseThrow().getStatus()).isEqualTo(EntryStatus.VALIDATED);

        // Le traitement du jour ecrit seul les echeances arrivees.
        SecurityContextHolder.clearContext();
        assertThat(loans.postDue(LocalDate.of(2026, 3, 20))).isGreaterThanOrEqualTo(2);
        as("tresorier@elleaose.com", RIGHTS);
        LoanService.LoanView view = loans.view(loanCode);
        assertThat(view.schedule().stream().filter(item -> "POSTED".equals(item.getStatus())).count()).isEqualTo(2);
        AccLoanInstallment first = view.schedule().get(0);
        List<AccEntryLine> firstLines = lines.findByEntryIdOrderByLineNoAsc(entries.findByEntryNumber(first.getEntryNumber()).orElseThrow().getId());
        assertThat(firstLines).anySatisfy(line -> {
            assertThat(line.getAccountNumber()).isEqualTo("671");
            assertThat(line.getDebit()).isEqualByComparingTo("12000");
        }).anySatisfy(line -> {
            assertThat(line.getAccountNumber()).isEqualTo("162");
            assertThat(line.getDebit()).isEqualByComparingTo(first.getPrincipal());
        });

        LoanService.LoanView prepaid = loans.prepay(loanCode, new LoanService.Prepayment(LocalDate.of(2026, 3, 20), new BigDecimal("300000"),
                new BigDecimal("3000"), "REDUCE_TERM"));
        assertThat(prepaid.loan().getScheduleVersion()).isEqualTo(2);
        assertThat(prepaid.schedule().size()).isLessThan(12);
        assertThat(prepaid.schedule().get(prepaid.schedule().size() - 1).getClosingBalance()).isEqualByComparingTo("0");
        assertThat(loans.history(loanCode).stream().filter(item -> item.getVersion() == 1)).hasSize(12);

        List<LoanService.Accrual> accruals = loans.accrue(LocalDate.of(2026, 3, 31));
        assertThat(accruals).anySatisfy(accrual -> {
            assertThat(accrual.loanCode()).isEqualTo(loanCode);
            assertThat(accrual.interest()).isPositive();
            assertThat(accrual.reversalNumber()).isNotNull();
        });
        assertThat(loans.accrue(LocalDate.of(2026, 3, 31))).noneMatch(accrual -> accrual.loanCode().equals(loanCode));
        assertThat(loans.note(LocalDate.of(2026, 3, 31))).anySatisfy(line -> {
            assertThat(line.code()).isEqualTo(loanCode);
            assertThat(line.withinOneYear().add(line.beyondOneYear())).isEqualByComparingTo(line.outstanding());
        });
        assertThat(new String(loans.pdf(loanCode), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Paie : le journal importe s'equilibre, l'avance retenue est soldee en 421, le paiement solde le 422 ; un fichier faux est refuse")
    void payrollImport() {
        String employee = "agent-" + code.toLowerCase() + "@elleaose.com";
        String matricule = "MAT-" + code;
        payroll.saveEmployee(AccPayrollEmployee.builder().matricule(matricule).fullName("Agent " + code).thirdPartyCode(employee).build());
        // Avance au personnel non justifiee, que la direction a decide de retenir sur salaire.
        entryService.create(new AccountingRequests.Entry("OD", LocalDate.of(2026, 6, 10), null, "Avance " + code, null, null, List.of(
                new AccountingRequests.EntryLine("421", employee, "Avance", new BigDecimal("50000"), null, null, null, null, null, null, null, "DDF-" + code),
                new AccountingRequests.EntryLine("571", null, "Caisse", null, new BigDecimal("50000"), null, null, null, null, null, null, null)), true));

        String wrong = "Matricule;Nom;Rubrique;Montant;Section\n" + matricule + ";Agent;SALAIRE_BASE;300 000;\n" + matricule + ";Agent;NET_A_PAYER;290000;\n";
        PayrollService.ImportView refused = payroll.importFile("2026-05", null, wrong.getBytes(StandardCharsets.UTF_8), "mai.csv");
        assertThat(refused.errors()).anyMatch(error -> error.contains("brut"));
        assertThatThrownBy(() -> payroll.post(refused.payroll().getId())).isInstanceOf(BadRequestException.class);

        String csv = "Matricule;Nom;Rubrique;Montant;Section\n"
                + matricule + ";Agent;SALAIRE_BASE;300 000;\n"
                + matricule + ";Agent;PRIMES;20 000;\n"
                + matricule + ";Agent;CNSS_SALARIALE;12 800;\n"
                + matricule + ";Agent;IRPP;15 000;\n"
                + matricule + ";Agent;CNSS_PATRONALE;64 000;\n"
                + matricule + ";Agent;AVANCES;50 000;\n"
                + matricule + ";Agent;NET_A_PAYER;242 200;\n"
                + matricule + ";Agent;BRUT;320 000;\n";
        PayrollService.ImportView imported = payroll.importFile("2026-06", "STANDARD", csv.getBytes(StandardCharsets.UTF_8), "juin.csv");
        assertThat(imported.errors()).isEmpty();
        assertThat(imported.payroll().getGrossTotal()).isEqualByComparingTo("320000");
        assertThat(imported.employees()).singleElement().satisfies(totals -> assertThat(totals.difference()).isEqualByComparingTo("0"));
        // Un reimport remplace le brouillon.
        PayrollService.ImportView again = payroll.importFile("2026-06", "STANDARD", csv.getBytes(StandardCharsets.UTF_8), "juin.csv");
        assertThat(payroll.view(imported.payroll().getId()).payroll().getStatus()).isEqualTo("REPLACED");

        PayrollService.ImportView posted = payroll.post(again.payroll().getId());
        assertThat(posted.payroll().getStatus()).isEqualTo("POSTED");
        AccEntry entry = entries.findByEntryNumber(posted.payroll().getEntryNumber()).orElseThrow();
        assertThat(entry.getStatus()).isEqualTo(EntryStatus.VALIDATED);
        assertThat(entry.getTotalDebit()).isEqualByComparingTo(entry.getTotalCredit()).isEqualByComparingTo("384000");
        assertThat(lettering.openLines("421", employee)).isEmpty();
        assertThatThrownBy(() -> payroll.importFile("2026-06", null, csv.getBytes(StandardCharsets.UTF_8), "juin.csv"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("comptabilisee");

        PayrollService.ImportView paid = payroll.pay(again.payroll().getId(), new PayrollService.PaymentRequest("5211", "BQ-5211", LocalDate.of(2026, 7, 2), null));
        assertThat(paid.payroll().getPaidTotal()).isEqualByComparingTo("242200");
        assertThat(lettering.openLines("422", employee)).isEmpty();
    }

    @Test
    @DisplayName("Compte courant d'associe : apport, interets au prorata temporis avec part non deductible, plafond, blocage, releve")
    void partnerAccount() {
        String partner = "associe-" + code.toLowerCase();
        AccPartnerAccount account = partners.save(new PartnerAccountService.PartnerRequest("CCA-" + code, "Associe " + code, partner, null, true, null,
                new BigDecimal("5"), new BigDecimal("3"), new BigDecimal("2000000"), LocalDate.of(2026, 6, 1)));
        partners.move(account.getCode(), new PartnerAccountService.Movement("CONTRIBUTION", LocalDate.of(2026, 6, 1), new BigDecimal("1000000"), "5211",
                null, null, null, null, null));
        assertThatThrownBy(() -> partners.move(account.getCode(), new PartnerAccountService.Movement("CONTRIBUTION", LocalDate.of(2026, 6, 2),
                new BigDecimal("1500000"), "5211", null, null, null, null, null))).isInstanceOf(BadRequestException.class).hasMessageContaining("plafond");
        assertThatThrownBy(() -> partners.move(account.getCode(), new PartnerAccountService.Movement("REPAYMENT", LocalDate.of(2026, 6, 3),
                new BigDecimal("100000"), "5211", null, null, null, null, null))).isInstanceOf(BadRequestException.class).hasMessageContaining("bloque");
        AccPartnerInterest interest = partners.interest(account.getCode(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        assertThat(interest.getInterest()).isEqualByComparingTo("4110");
        assertThat(interest.getNonDeductible()).isEqualByComparingTo("1644");
        PartnerAccountService.Statement statement = partners.statement(account.getCode(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        assertThat(statement.closing()).isEqualByComparingTo("1004110");
        assertThat(statement.lines()).hasSize(2);
    }

    @Test
    @DisplayName("Hors bilan : un bail s'inscrit en classe 90 pour ses loyers restant dus, le registre concorde avec les comptes, la levee contre-passe")
    void offBalanceRegister() {
        LocalDate today = LocalDate.now();
        AccCommitment lease = commitments.save(new CommitmentService.CommitmentRequest("BAIL-" + code, "BAIL", null, null, "Proprietaire " + code,
                "Bail des locaux", null, new BigDecimal("100000"), null, today.minusMonths(1), today.plusMonths(12), null, today.plusMonths(9), null, null, null));
        assertThat(lease.getAmount()).isEqualByComparingTo("1200000");
        assertThat(lease.getBookedAmount()).isEqualByComparingTo("1200000");
        AccCommitment guarantee = commitments.save(new CommitmentService.CommitmentRequest("GAR-" + code, "GARANTIE_BANCAIRE", null, null, "Client " + code,
                "Garantie a la place du depot", new BigDecimal("300000"), null, null, today, today.plusYears(2), null, null, null, null, null));
        assertThat(guarantee.getDirection()).isEqualTo("RECEIVED");
        assertThat(commitments.reconciliation().balanced()).isTrue();
        assertThat(commitments.note(today)).anySatisfy(line -> {
            assertThat(line.typeCode()).isEqualTo("BAIL");
            assertThat(line.withinOneYear()).isEqualByComparingTo("1200000");
        });
        commitments.release(guarantee.getCode(), today, "Mainlevee " + code);
        assertThat(commitments.reconciliation().balanced()).isTrue();
        assertThat(commitments.list().stream().filter(item -> item.getCode().equals(guarantee.getCode())).findFirst().orElseThrow().getBookedAmount())
                .isEqualByComparingTo("0");
    }

    private static void as(String user, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }
}
