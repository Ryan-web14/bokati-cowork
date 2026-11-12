package com.sni.bokaticowork.features.accounting;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.model.BankAccount;
import com.sni.bokaticowork.features.accounting.model.BankImportTemplate;
import com.sni.bokaticowork.features.accounting.repository.BankAccountRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.EntryNumbering;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.dto.response.PayInvoiceResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
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
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lots F10 et C5 sur le schema de production : releve importe et pointe automatiquement, ligne de
 * frais comptabilisee par regle, rapprochement valide a ecart nul par une autre personne, verrou
 * de periode, virement client transforme en paiement, taux de change.
 */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class, AnalyticAllocator.class,
        AccountingLetteringService.class, EntryNumbering.class, CashActorResolver.class, BankService.class, ExchangeRateService.class,
        VoucherService.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class BankF10IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private static final LocalDate DAY = LocalDate.of(2026, 4, 10);

    @Autowired private BankService banks;
    @Autowired private ExchangeRateService rates;
    @Autowired private AccountingEntryService entries;
    @Autowired private AccountingPeriodService periods;
    @Autowired private BankAccountRepository accountRepository;
    @Autowired private EntityManager entityManager;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier notifier;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private PaymentService paymentService;
    @MockitoBean private SpringTemplateEngine templateEngine;
    @MockitoBean private QrCodeRenderer qrCodeRenderer;

    @BeforeEach
    void setUp() {
        as("compta@elleaose.com", "BANK_ACCOUNT_MANAGE", "BANK_STATEMENT_IMPORT", "BANK_RECONCILIATION_MANAGE", "CASH_EXCHANGE_RATE_MANAGE");
        when(sequences.next(anyString())).thenAnswer(invocation -> "BR-F10-" + SEQ.getAndIncrement());
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.HOLD);
        when(settings.numberingWidth()).thenReturn(6);
        when(settings.text(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Releve importe, pointe automatiquement a plus de 80 %, frais par regle, rapprochement valide a ecart nul")
    void importMatchReconcile() {
        BankAccount bank = account();
        // Comptabilite : remise BRB-1, virement client, deux cheques remis ensemble, un retrait.
        book("Remise BRB-202604-000001", "BRB-202604-000001", "250000", null, "585", DAY);
        book("Virement client", "TXN-VIR-9", "118000", null, "411", DAY.plusDays(1));
        book("Cheque 1", "CHQ-1", "40000", null, "513", DAY.plusDays(2));
        book("Cheque 2", "CHQ-2", "60000", null, "513", DAY.plusDays(2));
        book("Retrait", null, null, "30000", "585", DAY.plusDays(3));
        entityManager.flush();

        String csv = "Date;Libelle;Reference;Montant\n"
                + "10/04/2026;REMISE BRB-202604-000001;;250 000,00\n"
                + "12/04/2026;VIR RECU SARL KINTELE;TXN-VIR-9;118 000,00\n"
                + "13/04/2026;REMISE CHEQUES;;100 000,00\n"
                + "14/04/2026;RETRAIT GUICHET;;-30 000,00\n"
                + "15/04/2026;FRAIS TENUE DE COMPTE;;-5 000,00\n";
        BankService.ImportResult result = banks.importFile(bank.getCode(), csv.getBytes(StandardCharsets.UTF_8), "avril.csv",
                new BigDecimal("1000000"), null, null);
        assertThat(result.lines()).isEqualTo(5);
        assertThat(result.autoMatched()).isEqualTo(4);
        assertThat((double) result.autoMatched() / result.lines()).isGreaterThan(0.8 - 0.0001);

        BankService.ImportResult again = banks.importFile(bank.getCode(), csv.getBytes(StandardCharsets.UTF_8), "avril.csv",
                new BigDecimal("1433000"), null, null);
        assertThat(again.duplicates()).isEqualTo(5);

        BankService.MatchingScreen screen = banks.screen(bank.getCode());
        assertThat(screen.statementLines()).singleElement().satisfies(line -> assertThat(line.label()).contains("FRAIS"));
        BankService.Line fees = banks.post(bank.getCode(), screen.statementLines().get(0).id(), null);
        assertThat(fees.matchingCode()).isNotNull();
        assertThat(fees.postedEntry()).startsWith(bank.getJournalCode());
        entityManager.flush();

        // Le releve part d'un solde d'1 000 000 que la comptabilite doit porter (a-nouveaux).
        book("A-nouveau banque", "AN", "1000000", null, "101", DAY.minusDays(9));
        entityManager.flush();
        BankService.Reconciliation draft = banks.prepare(bank.getCode(), LocalDate.of(2026, 4, 30));
        assertThat(draft.difference()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> banks.validate(draft.id())).isInstanceOf(ForbiddenException.class);
        as("daf@elleaose.com", "BANK_RECONCILIATION_VALIDATE", "BANK_RECONCILIATION_MANAGE");
        BankService.Reconciliation validated = banks.validate(draft.id());
        assertThat(validated.status()).isEqualTo("VALIDATED");
        assertThat(validated.number()).startsWith("RAP-");
        assertThatThrownBy(() -> banks.unmatch(bank.getCode(), fees.matchingCode(), "Erreur")).isInstanceOf(ForbiddenException.class);
        assertThat(banks.missingReconciliations(LocalDate.of(2026, 4, 30))).doesNotContain(bank.getCode());
        assertThat(banks.missingReconciliations(LocalDate.of(2026, 5, 31))).contains(bank.getCode());
    }

    @Test
    @DisplayName("Un virement client inconnu devient un paiement impute a sa facture ; releve incoherent refuse")
    void customerTransferBecomesPayment() {
        BankAccount bank = account();
        PaymentTransactionResponse transaction = mock(PaymentTransactionResponse.class);
        when(transaction.transactionNumber()).thenReturn("TXN-BQ-1");
        when(paymentService.payInvoice(eq("FAC-77"), any())).thenReturn(new PayInvoiceResponse(null, transaction));
        BankService.ImportResult result = banks.importManual(bank.getCode(), new BankService.ManualStatement(DAY, DAY, BigDecimal.ZERO, null,
                List.of(new BankService.ManualLine(DAY, DAY, "VIR M. NGOMA FAC-77", "VIR-123", new BigDecimal("50000")))));
        Long lineId = banks.screen(bank.getCode()).statementLines().stream().filter(line -> line.reference() != null && line.reference().equals("VIR-123"))
                .findFirst().orElseThrow().id();
        assertThat(result.autoMatched()).isZero();
        BankService.Line line = banks.post(bank.getCode(), lineId, new BankService.PostLine(null, null, null, null, "FAC-77"));
        assertThat(line.awaitingPayment()).isEqualTo("TXN-BQ-1");

        assertThatThrownBy(() -> banks.importManual(bank.getCode(), new BankService.ManualStatement(DAY, DAY, BigDecimal.ZERO, new BigDecimal("999"),
                List.of(new BankService.ManualLine(DAY, DAY, "X", null, new BigDecimal("10")))))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("solde final");
    }

    @Test
    @DisplayName("Taux du jour : conversion d'une devise en XAF")
    void exchangeRate() {
        rates.setRate("EUR", new BigDecimal("655.957"), DAY);
        assertThat(rates.rate("EUR", DAY.plusDays(3))).hasValueSatisfying(rate -> assertThat(rate).isEqualByComparingTo("655.957"));
        assertThat(rates.rate("USD", DAY)).isEmpty();
        assertThat(rates.rate("XAF", DAY)).hasValue(BigDecimal.ONE);
    }

    private BankAccount account() {
        String code = "BQ-IT-" + SEQ.getAndIncrement();
        BankImportTemplate template = banks.saveTemplate(BankImportTemplate.builder().code("TPL-" + code).label("Test").delimiter(";").headerRows(1)
                .dateColumn("Date").labelColumn("Libelle").referenceColumn("Reference").amountColumn("Montant").dateFormat("dd/MM/yyyy")
                .decimalSeparator(",").encoding("UTF-8").build());
        return banks.saveAccount(BankAccount.builder().code(code).label("Banque de test " + code).importTemplateCode(template.getCode())
                .openingBalance(new BigDecimal("1000000")).openingDate(DAY).build());
    }

    /** Ecriture validee sur le compte de la banque (debit : entree) contre un autre compte. */
    private void book(String label, String reference, String debit, String credit, String counter, LocalDate date) {
        BankAccount bank = accountRepository.findAll().stream().filter(a -> a.getCode().startsWith("BQ-IT-")).reduce((a, b) -> b).orElseThrow();
        BigDecimal amount = new BigDecimal(debit != null ? debit : credit);
        boolean in = debit != null;
        entries.create(new AccountingRequests.Entry(bank.getJournalCode(), date, null, label, null, null, List.of(
                new AccountingRequests.EntryLine(bank.getAccountNumber(), null, label, in ? amount : null, in ? null : amount, null, null, null, null,
                        null, null, reference),
                new AccountingRequests.EntryLine(counter, "411".equals(counter) ? "CUS-BQ" : null, label, in ? null : amount, in ? amount : null, null,
                        null, null, null, null, null, reference)), true));
    }

    private static void as(String user, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }
}
