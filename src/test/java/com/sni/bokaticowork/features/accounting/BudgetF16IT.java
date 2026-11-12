package com.sni.bokaticowork.features.accounting;

import com.sni.bokaticowork.core.accounting.AccountingEventPublisher;
import com.sni.bokaticowork.core.accounting.BudgetControl;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.accounting.model.AccBudgetCommitment;
import com.sni.bokaticowork.features.accounting.model.AccBudgetLine;
import com.sni.bokaticowork.features.accounting.repository.AccBudgetCommitmentRepository;
import com.sni.bokaticowork.features.accounting.repository.AccBudgetLineRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.BudgetEngine;
import com.sni.bokaticowork.features.accounting.service.implementation.BudgetService;
import com.sni.bokaticowork.features.fund.dto.FundDtos;
import com.sni.bokaticowork.features.fund.enums.FundRequestStatus;
import com.sni.bokaticowork.features.fund.enums.FundRequestType;
import com.sni.bokaticowork.features.fund.service.FundNotifier;
import com.sni.bokaticowork.features.fund.service.FundRequestService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Lot F16 sur le schema de production : une demande sur une ligne bloquante sans disponible part
 * en validation budgetaire ; un engagement de 100 000 realise pour 85 000 libere 15 000.
 */
@Import({BudgetEngine.class, BudgetService.class, FundRequestService.class, CashActorResolver.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class BudgetF16IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    @Autowired private BudgetService budgets;
    @Autowired private BudgetEngine engine;
    @Autowired private FundRequestService requests;
    @Autowired private AccBudgetLineRepository lineRepository;
    @Autowired private AccBudgetCommitmentRepository commitmentRepository;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private CashRegisterService cashRegisterService;
    @MockitoBean private AccountingEventPublisher accountingEvents;
    @MockitoBean private FundNotifier notifier;

    private Long budgetId;

    @BeforeEach
    void setUp() {
        when(sequences.next(anyString())).thenAnswer(invocation -> invocation.getArgument(0, String.class).toUpperCase() + "-F16-" + SEQ.getAndIncrement());
        as("daf@elleaose.com", "BUDGET_PREPARE", "BUDGET_CONSOLIDATE", "BUDGET_APPROVE", "BUDGET_ITEM_MANAGE");
        String year = String.valueOf(LocalDate.now().getYear());
        BudgetService.Budget budget = budgets.create(new BudgetService.CreateBudget(year, "Budget de test", null, "EMPTY", null));
        budgetId = budget.id();
        budgets.upsertLine(budgetId, new BudgetService.LineInput("MISSIONS", null, new BigDecimal("50000"), null, null, "BLOCKING", BigDecimal.ZERO, null));
        budgets.upsertLine(budgetId, new BudgetService.LineInput("ACCUEIL", null, new BigDecimal("200000"), null, null, "ALERT", null, null));
        budgets.transition(budgetId, "submit", null);
        budgets.transition(budgetId, "consolidate", null);
        assertThat(budgets.transition(budgetId, "approve", null).status()).isEqualTo("ACTIVE");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Depassement d'une ligne bloquante : validation budgetaire en plus, par un detenteur de BUDGET:OVERRIDE")
    void blockingLine() {
        as("agent@elleaose.com", "FUND_REQUEST_CREATE");
        FundDtos.Request request = requests.create(input("FRAIS_MISSION", "80000", "Mission imprevue"));
        assertThat(request.status()).isEqualTo(FundRequestStatus.IN_APPROVAL);
        assertThat(request.requiredLevels()).isEqualTo(3);
        assertThat(request.insight().budget().blocked()).isTrue();
        assertThat(request.insight().budgetApproval()).isEqualTo("OVERRIDE");

        as("chef@elleaose.com", "FUND_REQUEST_APPROVE_L1");
        requests.approve(request.number(), null);
        as("daf2@elleaose.com", "FUND_REQUEST_APPROVE_L2");
        requests.approve(request.number(), null);
        assertThatThrownBy(() -> requests.approve(request.number(), null)).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("BUDGET:OVERRIDE");
        as("dg@elleaose.com", "BUDGET_OVERRIDE");
        assertThat(requests.approve(request.number(), new FundDtos.Decision("Mission indispensable", null)).status()).isEqualTo(FundRequestStatus.APPROVED);

        AccBudgetLine missions = line("MISSIONS");
        BudgetEngine.Amounts amounts = engine.amounts(missions);
        assertThat(amounts.committed()).isEqualByComparingTo("80000");
        assertThat(amounts.available()).isEqualByComparingTo("-30000");
    }

    @Test
    @DisplayName("Un engagement de 100 000 realise pour 85 000 libere 15 000")
    void commitmentSettledForLess() {
        as("agent@elleaose.com", "FUND_REQUEST_CREATE");
        FundDtos.Request request = requests.create(input("ACHAT_FOURNITURES_BUREAU", "100000", "Rentree"));
        as("chef@elleaose.com", "FUND_REQUEST_APPROVE_L1");
        requests.approve(request.number(), null);
        as("daf2@elleaose.com", "FUND_REQUEST_APPROVE_L2");
        requests.approve(request.number(), null);

        AccBudgetLine accueil = line("ACCUEIL");
        assertThat(engine.amounts(accueil).committed()).isEqualByComparingTo("100000");
        assertThat(engine.amounts(accueil).available()).isEqualByComparingTo("100000");

        engine.settle("FUND_REQUEST", request.number(), new BigDecimal("85000"));
        AccBudgetCommitment commitment = commitmentRepository.findBySourceTypeAndReference("FUND_REQUEST", request.number()).get(0);
        assertThat(commitment.getStatus()).isEqualTo("SETTLED");
        assertThat(commitment.getSettledAmount()).isEqualByComparingTo("85000");
        assertThat(commitment.getAmount().subtract(commitment.getSettledAmount())).isEqualByComparingTo("15000");
        assertThat(engine.amounts(accueil).committed()).isEqualByComparingTo("0");

        BudgetControl.Impact impact = engine.check(List.of(new BudgetControl.Spend("ACHAT_FOURNITURES_BUREAU", null, null, new BigDecimal("1000"))), LocalDate.now());
        assertThat(impact.lines()).singleElement().satisfies(line -> assertThat(line.status()).isEqualTo(BudgetControl.Status.GREEN));
    }

    private AccBudgetLine line(String item) {
        return lineRepository.findByBudgetIdOrderByItemCodeAscSectionCodeAsc(budgetId).stream()
                .filter(line -> line.getItemCode().equals(item)).findFirst().orElseThrow();
    }

    private static FundDtos.RequestInput input(String nature, String amount, String reason) {
        return new FundDtos.RequestInput(FundRequestType.DIRECT_EXPENSE, "Depense " + nature, null, null, null, null, null, null, null, null, null,
                null, null, null, null, true, reason, List.of(new FundDtos.RequestLine(nature, "Ligne", new BigDecimal(amount), null, null)), null, true,
                null, "URGENCE");
    }

    private static void as(String user, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }
}
