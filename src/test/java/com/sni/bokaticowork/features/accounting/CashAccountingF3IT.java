package com.sni.bokaticowork.features.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccEntryLine;
import com.sni.bokaticowork.features.accounting.repository.AccEntryLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccEntryRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingChartService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingEntryService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingLetteringService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingPeriodService;
import com.sni.bokaticowork.features.accounting.service.implementation.CashAccountingService;
import com.sni.bokaticowork.features.accounting.service.implementation.ExchangeRateService;
import com.sni.bokaticowork.features.accounting.service.implementation.PostingEngine;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.BasicPostingResolvers;
import com.sni.bokaticowork.features.accounting.service.support.EntryNumbering;
import com.sni.bokaticowork.features.accounting.service.support.PostingTemplate;
import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.CashVarianceDecision;
import com.sni.bokaticowork.features.payment.enums.CashVarianceKind;
import com.sni.bokaticowork.features.payment.enums.CashVarianceStatus;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.CashVarianceReview;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.repository.CashVarianceReviewRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Lot F3 sur le schema de production : chaque fait de caisse produit sa piece, le solde comptable suit la caisse. */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class, com.sni.bokaticowork.features.accounting.service.implementation.AnalyticAllocator.class, AccountingLetteringService.class,
        EntryNumbering.class, CashActorResolver.class, PostingEngine.class, PostingTemplate.class, BasicPostingResolvers.class,
        CashAccountingService.class, ExchangeRateService.class, CashAccountingF3IT.Json.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CashAccountingF3IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    @TestConfiguration
    static class Json {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired private CashAccountingService cash;
    @Autowired private PostingEngine engine;
    @Autowired private CashRegisterRepository registers;
    @Autowired private CashSessionRepository sessions;
    @Autowired private CashMovementRepository movements;
    @Autowired private CashVarianceReviewRepository reviews;
    @Autowired private AccEntryRepository entries;
    @Autowired private AccEntryLineRepository lines;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier notifier;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private PlatformSettings platformSettings;

    private String code;

    @BeforeEach
    void setUp() {
        when(sequences.next(anyString())).thenAnswer(invocation -> "BR-F3-" + SEQ.getAndIncrement() + "-" + UUID.randomUUID().toString().substring(0, 6));
        when(settings.enabled()).thenReturn(true);
        when(settings.familyEnabled(anyString())).thenReturn(true);
        when(settings.startDate()).thenReturn(java.time.LocalDate.of(2026, 1, 1));
        when(settings.postingBatchSize()).thenReturn(200);
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.HOLD);
        when(settings.strictChronology()).thenReturn(true);
        when(settings.numberingWidth()).thenReturn(6);
        when(settings.text(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        code = "F3-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Test
    @DisplayName("Fonds initial, sortie avec TVA, entree, ajustement : une piece chacun ; le paiement client n'est pas ecrit ici ; soldes egaux")
    void movementsArePosted() {
        CashSession session = session(code);
        CashMovement opening = movement(session, CashMovementType.OPENING_FLOAT, "50000", null, null, null);
        CashMovement out = movement(session, CashMovementType.CASH_OUT, "11800", null, "ACHAT_FOURNITURES_BUREAU", "1800");
        CashMovement in = movement(session, CashMovementType.CASH_IN, "5000", null, "PRODUIT_DIVERS", null);
        CashMovement adjustment = movement(session, CashMovementType.ADJUSTMENT, "300", CashFlowDirection.OUT, null, null);

        run();

        CashRegister register = registers.findByRegisterCode("CSR-" + code).orElseThrow();
        assertThat(register.getTreasuryAccountNumber()).startsWith("571");
        assertThat(register.getDefaultJournalCode()).isEqualTo("CA-" + register.getTreasuryAccountNumber());
        for (CashMovement movement : List.of(opening, out, in, adjustment)) {
            CashMovement reloaded = movements.findById(movement.getId()).orElseThrow();
            assertThat(reloaded.getAccountingStatus()).as(reloaded.getMovementType() + " " + reloaded.getAccountingNote()).isEqualTo("POSTED");
            assertThat(reloaded.getAccountingEntryNumber()).startsWith(register.getDefaultJournalCode() + "-2026-");
        }
        List<AccEntryLine> outLines = lines(out);
        assertThat(outLines).extracting(AccEntryLine::getAccountNumber).containsExactly("6055", "4452", register.getTreasuryAccountNumber());
        assertThat(outLines.get(0).getDebit()).isEqualByComparingTo("10000");

        CashAccountingService.RegisterReconciliation row = cash.reconciliation().stream()
                .filter(line -> line.registerCode().equals(register.getRegisterCode())).findFirst().orElseThrow();
        assertThat(row.accountingBalance()).isEqualByComparingTo("42900");
        assertThat(row.difference()).isEqualByComparingTo("0");
        assertThat(row.movementsNotPosted()).isZero();

        CashMovement payment = movement(session, CashMovementType.PAYMENT, "2000", null, null, null);
        run();
        assertThat(movements.findById(payment.getId()).orElseThrow().getAccountingStatus()).isEqualTo("NOT_APPLICABLE");
    }

    @Test
    @DisplayName("Une contre-passation de caisse contre-passe la piece du mouvement d'origine")
    void reversal() {
        CashSession session = session(code);
        movement(session, CashMovementType.OPENING_FLOAT, "10000", null, null, null);
        CashMovement out = movement(session, CashMovementType.CASH_OUT, "2000", null, "TRANSPORT_COURSE", null);
        run();
        String original = movements.findById(out.getId()).orElseThrow().getAccountingEntryNumber();

        out.setStatus(CashMovementStatus.REVERSED);
        out.setAccountingStatus("POSTED");
        movements.save(movements.findById(out.getId()).map(m -> {
            m.setStatus(CashMovementStatus.REVERSED);
            return m;
        }).orElseThrow());
        CashMovement reversal = movements.save(CashMovement.builder().movementNumber("MVT-" + code + "-R").cashSession(session)
                .movementType(CashMovementType.ADJUSTMENT).direction(CashFlowDirection.IN).amount(new BigDecimal("2000")).currency("XAF")
                .flowCategory("REVERSAL").referenceType("CASH_MOVEMENT").referenceCode(out.getMovementNumber())
                .reversalReason("Erreur de saisie").status(CashMovementStatus.CONFIRMED).createdBy("chef@elleaose.com").build());
        run();

        assertThat(entries.findByEntryNumber(original).orElseThrow().getStatus()).isEqualTo(EntryStatus.REVERSED);
        CashMovement posted = movements.findById(reversal.getId()).orElseThrow();
        assertThat(posted.getAccountingStatus()).isEqualTo("POSTED");
        AccEntry reverseEntry = entries.findByEntryNumber(posted.getAccountingEntryNumber()).orElseThrow();
        assertThat(reverseEntry.getReversalOf()).isEqualTo(original);
    }

    @Test
    @DisplayName("Ecart de cloture constate en 471, puis impute en charge ; la session suivante n'ecrit pas son fonds")
    void varianceRecordedThenDecided() {
        CashSession first = session(code);
        movement(first, CashMovementType.OPENING_FLOAT, "10000", null, null, null);
        CashVarianceReview review = reviews.save(CashVarianceReview.builder().reviewNumber("ECR-" + code).registerCode("CSR-" + code)
                .sessionNumber(first.getSessionNumber()).kind(CashVarianceKind.CLOSING).amount(new BigDecimal("-500"))
                .cashier("caissier@elleaose.com").status(CashVarianceStatus.PENDING).build());
        run();
        CashVarianceReview recorded = reviews.findById(review.getId()).orElseThrow();
        assertThat(recorded.getAccountingStatus()).isEqualTo("POSTED");
        assertThat(lines(entries.findByEntryNumber(recorded.getEntryNumber()).orElseThrow()))
                .anySatisfy(line -> {
                    assertThat(line.getAccountNumber()).isEqualTo("471");
                    assertThat(line.getDebit()).isEqualByComparingTo("500");
                });

        recorded.setStatus(CashVarianceStatus.DECIDED);
        recorded.setDecision(CashVarianceDecision.EXPENSE);
        recorded.setDecidedAt(Instant.now());
        recorded.setDecidedBy("chef@elleaose.com");
        reviews.save(recorded);
        run();
        CashVarianceReview decided = reviews.findById(review.getId()).orElseThrow();
        assertThat(decided.getDecisionAccountingStatus()).isEqualTo("POSTED");
        // OD ne valide pas automatiquement : l'imputation attend en brouillard.
        AccEntry decision = entries.findByEntryNumber(decided.getDecisionEntryNumber())
                .or(() -> entries.findByProvisionalNumber(decided.getDecisionEntryNumber())).orElseThrow();
        assertThat(lines(decision))
                .extracting(AccEntryLine::getAccountNumber).containsExactly("658", "471");

        first.setStatus(CashSessionStatus.CLOSED);
        sessions.save(first);
        CashSession second = sessions.save(CashSession.builder().sessionNumber("CSS-" + code + "-2").cashRegister(first.getCashRegister())
                .status(CashSessionStatus.OPEN).openedBy(cashier()).openingAmount(new BigDecimal("9500")).build());
        CashMovement carried = movement(second, CashMovementType.OPENING_FLOAT, "9500", null, null, null);
        run();
        assertThat(movements.findById(carried.getId()).orElseThrow().getAccountingStatus()).isEqualTo("IGNORED");
    }

    private String cashier() {
        return "caissier-" + code.toLowerCase() + "@elleaose.com";
    }

    @Test
    @DisplayName("Lot F10 : especes en devise entrees a 650, remises a 660 : la caisse sort a 65 000, la remise vaut 66 000, gain de change 1 000")
    void foreignCashExchangeGain() {
        CashSession session = session(code);
        CashMovement in = movements.save(CashMovement.builder().movementNumber("MVT-" + code + "-EUR1").cashSession(session)
                .movementType(CashMovementType.CASH_IN).amount(new BigDecimal("100")).currency("EUR").exchangeRate(new BigDecimal("650"))
                .reason("Achat de devises").status(CashMovementStatus.CONFIRMED).createdBy("caissier@elleaose.com").build());
        CashMovement deposit = movements.save(CashMovement.builder().movementNumber("MVT-" + code + "-EUR2").cashSession(session)
                .movementType(CashMovementType.SAFE_DEPOSIT).amount(new BigDecimal("100")).currency("EUR").exchangeRate(new BigDecimal("660"))
                .reason("Remise EUR").status(CashMovementStatus.CONFIRMED).createdBy("caissier@elleaose.com").build());
        run();
        assertThat(movements.findById(in.getId()).orElseThrow().getAccountingStatus()).isEqualTo("POSTED");
        List<AccEntryLine> remise = lines(deposit);
        assertThat(remise).anySatisfy(line -> {
            assertThat(line.getAccountNumber()).isEqualTo("585");
            assertThat(line.getDebit()).isEqualByComparingTo("66000");
        });
        assertThat(remise).anySatisfy(line -> {
            assertThat(line.getAccountNumber()).startsWith("571");
            assertThat(line.getCredit()).isEqualByComparingTo("65000");
        });
        assertThat(remise).anySatisfy(line -> {
            assertThat(line.getAccountNumber()).isEqualTo("776");
            assertThat(line.getCredit()).isEqualByComparingTo("1000");
        });
    }

    private void run() {
        cash.feed();
        engine.processPending();
        cash.feed();
        engine.processPending();
    }

    private List<AccEntryLine> lines(CashMovement movement) {
        String number = movements.findById(movement.getId()).orElseThrow().getAccountingEntryNumber();
        return lines(entries.findByEntryNumber(number).orElseThrow());
    }

    private List<AccEntryLine> lines(AccEntry entry) {
        return lines.findByEntryIdOrderByLineNoAsc(entry.getId());
    }

    private CashSession session(String code) {
        CashRegister register = registers.save(CashRegister.builder().registerCode("CSR-" + code).name("Caisse " + code)
                .businessEntityCode("BIZ-" + code).deviceCode("DEV-" + code).active(true).cashControlEnabled(true).build());
        return sessions.save(CashSession.builder().sessionNumber("CSS-" + code).cashRegister(register).status(CashSessionStatus.OPEN)
                .openedBy(cashier()).openingAmount(new BigDecimal("10000")).build());
    }

    private CashMovement movement(CashSession session, CashMovementType type, String amount, CashFlowDirection direction, String nature,
                                  String vat) {
        return movements.save(CashMovement.builder().movementNumber("MVT-" + code + "-" + SEQ.getAndIncrement()).cashSession(session)
                .movementType(type).amount(new BigDecimal(amount)).currency("XAF").direction(direction).natureCode(nature)
                .vatAmount(vat == null ? null : new BigDecimal(vat)).reason(type.name()).status(CashMovementStatus.CONFIRMED)
                .createdBy("caissier@elleaose.com").build());
    }
}
