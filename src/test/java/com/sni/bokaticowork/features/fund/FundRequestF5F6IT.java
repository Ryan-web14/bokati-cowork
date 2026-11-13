package com.sni.bokaticowork.features.fund;

import com.sni.bokaticowork.core.accounting.AccountingEventPublisher;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.fund.dto.FundDtos;
import com.sni.bokaticowork.features.fund.enums.FundRequestStatus;
import com.sni.bokaticowork.features.fund.enums.FundRequestType;
import com.sni.bokaticowork.features.fund.service.FundNeedService;
import com.sni.bokaticowork.features.fund.service.FundNotifier;
import com.sni.bokaticowork.features.fund.service.FundRequestService;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashMovementRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lots F5 et F6 sur le schema de production : un employe sans session cree une demande, deux niveaux
 * la valident, la caissiere la decaisse ; une avance de 100 000 justifiee en 3 lignes pour 85 000 se
 * solde par un retour de 15 000.
 */
@Import({FundRequestService.class, FundNeedService.class, CashActorResolver.class, com.sni.bokaticowork.features.approval.service.ApprovalEngine.class, com.sni.bokaticowork.features.approval.service.ApprovalHierarchy.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class FundRequestF5F6IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    @Autowired private FundRequestService requests;
    @Autowired private FundNeedService needs;
    @Autowired private CashRegisterRepository registers;
    @Autowired private CashSessionRepository sessions;
    @Autowired private CashMovementRepository movements;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private CashRegisterService cashRegisterService;
    @MockitoBean private AccountingEventPublisher accountingEvents;
    @MockitoBean private FundNotifier notifier;
    @MockitoBean private com.sni.bokaticowork.features.approval.service.ApprovalNotifier approvalNotifier;

    private final List<CreateCashMovementRequest> cashMovements = new ArrayList<>();
    private String code;

    @BeforeEach
    void setUp() {
        code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        when(sequences.next(anyString())).thenAnswer(invocation -> invocation.getArgument(0, String.class).toUpperCase() + "-" + code + "-" + SEQ.getAndIncrement());
        when(cashRegisterService.createMovement(anyString(), any())).thenAnswer(invocation -> {
            CreateCashMovementRequest request = invocation.getArgument(1);
            cashMovements.add(request);
            CashMovementResponse response = mock(CashMovementResponse.class);
            when(response.movementNumber()).thenReturn("MVT-" + code + "-" + SEQ.getAndIncrement());
            return response;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Avance de 100 000 : deux niveaux, decaissement, 3 justifications pour 85 000, retour de 15 000, soldee")
    void advanceLifecycle() {
        as("agent@elleaose.com", "FUND_REQUEST_CREATE");
        FundDtos.Request draft = requests.create(new FundDtos.RequestInput(FundRequestType.ADVANCE, "Mission Pointe-Noire", "Exploitation", null,
                null, null, null, null, null, "CSR-" + code, null, LocalDate.now(), "Pointe-Noire", LocalDate.now(), LocalDate.now().plusDays(2),
                false, null, List.of(new FundDtos.RequestLine("AVANCE_A_JUSTIFIER", "Mission", new BigDecimal("100000"), null, null)), null, true));
        assertThat(draft.status()).isEqualTo(FundRequestStatus.IN_APPROVAL);
        assertThat(draft.requiredLevels()).isEqualTo(2);
        assertThatThrownBy(() -> requests.approve(draft.number(), null)).isInstanceOf(ForbiddenException.class);

        as("chef@elleaose.com", "FUND_REQUEST_APPROVE_L1");
        assertThat(requests.approve(draft.number(), null).currentLevel()).isEqualTo(2);
        assertThatThrownBy(() -> requests.approve(draft.number(), null)).isInstanceOf(ForbiddenException.class);
        as("daf@elleaose.com", "FUND_REQUEST_APPROVE_L2");
        FundDtos.Request approved = requests.approve(draft.number(), null);
        assertThat(approved.status()).isEqualTo(FundRequestStatus.APPROVED);
        assertThat(approved.timeline()).hasSize(3);

        CashSession session = session();
        as("caisse@elleaose.com", "FUND_REQUEST_DISBURSE");
        FundDtos.Request disbursed = requests.disburse(draft.number(), new FundDtos.Disburse(session.getSessionNumber(), null, "Agent", null));
        assertThat(disbursed.status()).isEqualTo(FundRequestStatus.TO_JUSTIFY);
        assertThat(disbursed.disbursedAmount()).isEqualByComparingTo("100000");
        assertThat(cashMovements).singleElement().satisfies(movement -> {
            assertThat(movement.natureCode()).isEqualTo("AVANCE_A_JUSTIFIER");
            assertThat(movement.referenceType()).isEqualTo("FUND_REQUEST");
            assertThat(movement.counterpartyCode()).isEqualTo("agent@elleaose.com");
        });

        as("agent@elleaose.com", "FUND_REQUEST_CREATE");
        for (String amount : Arrays.asList("40000", "30000", "15000")) {
            requests.addExpense(draft.number(), new FundDtos.ExpenseInput(LocalDate.now(), "FRAIS_MISSION", "Hotel", "Depense " + amount,
                    new BigDecimal(amount), null, null, "photo-" + amount + ".jpg", false, 1));
        }
        as("compta@elleaose.com", "FUND_JUSTIFICATION_VALIDATE");
        FundDtos.Request justified = requests.get(draft.number());
        for (FundDtos.ExpenseLine line : justified.justification()) {
            justified = requests.validateExpense(draft.number(), line.number());
        }
        assertThat(justified.justifiedAmount()).isEqualByComparingTo("85000");
        assertThat(justified.remainingToJustify()).isEqualByComparingTo("15000");
        verify(accountingEvents, times(3)).publish(eq("EXPENSE_JUSTIFIED"), eq("FUNDING"), anyString(), any(), any());

        as("caisse@elleaose.com", "FUND_REQUEST_DISBURSE");
        FundDtos.Request settled = requests.returnRemainder(draft.number(), new FundDtos.CashBack(session.getSessionNumber(), null, "Agent", null));
        assertThat(settled.status()).isEqualTo(FundRequestStatus.SETTLED);
        assertThat(settled.returnedAmount()).isEqualByComparingTo("15000");
        assertThat(cashMovements.get(1).movementType()).isEqualTo(CashMovementType.CASH_IN);
        assertThat(cashMovements.get(1).natureCode()).isEqualTo("RETOUR_AVANCE");
    }

    @Test
    @DisplayName("Renvoi pour correction, piece exigee, etat de besoin transforme en demande")
    void sendBackAndNeed() {
        as("agent2@elleaose.com", "FUND_REQUEST_CREATE");
        FundDtos.Request request = requests.create(new FundDtos.RequestInput(FundRequestType.DIRECT_EXPENSE, "Fournitures", null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null,
                List.of(new FundDtos.RequestLine("ACHAT_FOURNITURES_BUREAU", "Ramettes", new BigDecimal("20000"), null, null)), null, true));
        as("chef@elleaose.com", "FUND_REQUEST_APPROVE_L1");
        FundDtos.Request back = requests.sendBack(request.number(), new FundDtos.Decision("Joindre le devis", null));
        assertThat(back.status()).isEqualTo(FundRequestStatus.DRAFT);
        assertThatThrownBy(() -> requests.reject(back.number(), new FundDtos.Decision(null, null))).isInstanceOf(BadRequestException.class);

        as("agent2@elleaose.com", "FUND_REQUEST_CREATE");
        FundDtos.Need need = needs.create(new FundDtos.NeedRequest("Cafe des espaces", null, null, null, null, null,
                List.of(new FundDtos.NeedLineRequest("Cafe en grains", new BigDecimal("4"), "kg", new BigDecimal("6000"), "ACHAT_CONSOMMABLES_ESPACES", null)), null));
        assertThat(need.totalAmount()).isEqualByComparingTo("24000");
        needs.submit(need.number());
        as("chef@elleaose.com", "FUND_REQUEST_APPROVE_L1");
        needs.decide(need.number(), true, null);
        as("agent2@elleaose.com", "FUND_REQUEST_CREATE");
        FundDtos.Request fromNeed = needs.transform(need.number(), new FundDtos.TransformNeed(FundRequestType.DIRECT_EXPENSE, null, null));
        assertThat(fromNeed.needStatementNumber()).isEqualTo(need.number());
        assertThat(fromNeed.totalAmount()).isEqualByComparingTo("24000");
        assertThat(needs.get(need.number()).status().name()).isEqualTo("TRANSFORMED");
    }

    @Test
    @DisplayName("Lot B15 : le responsable financier valide au niveau 1 et approuve les deux niveaux, le niveau 1 note en avance")
    void higherLevelApprovesEarly() {
        as("agent3@elleaose.com", "FUND_REQUEST_CREATE");
        FundDtos.Request draft = requests.create(new FundDtos.RequestInput(FundRequestType.DIRECT_EXPENSE, "Climatiseur", null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null,
                List.of(new FundDtos.RequestLine("ACHAT_FOURNITURES_BUREAU", "Climatiseur", new BigDecimal("300000"), null, null)), null, true));
        assertThat(draft.requiredLevels()).isEqualTo(2);
        as("daf@elleaose.com", "FUND_REQUEST_APPROVE_L2");
        assertThat(requests.toApprove()).extracting(FundDtos.Request::number).contains(draft.number());

        FundDtos.Request approved = requests.approve(draft.number(), new FundDtos.Decision("Panne, urgent", null));

        assertThat(approved.status()).isEqualTo(FundRequestStatus.APPROVED);
        assertThat(approved.timeline()).extracting(FundDtos.Approval::level).containsExactly(0, 1, 2);
        assertThat(approved.timeline().get(1).comment()).contains("en avance");
    }

    private CashSession session() {
        CashRegister register = registers.save(CashRegister.builder().registerCode("CSR-" + code).name("Caisse " + code).businessEntityCode("BIZ")
                .deviceCode("DEV-" + code).active(true).cashControlEnabled(true).build());
        CashSession session = sessions.save(CashSession.builder().sessionNumber("CSS-" + code).cashRegister(register).status(CashSessionStatus.OPEN)
                .openedBy("caisse-" + code.toLowerCase() + "@elleaose.com").openingAmount(new BigDecimal("500000")).build());
        movements.save(CashMovement.builder().movementNumber("MVT-OPEN-" + code).cashSession(session).movementType(CashMovementType.OPENING_FLOAT)
                .amount(new BigDecimal("500000")).currency("XAF").status(CashMovementStatus.CONFIRMED).createdBy("caisse").build());
        return session;
    }

    private static void as(String user, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }
}
