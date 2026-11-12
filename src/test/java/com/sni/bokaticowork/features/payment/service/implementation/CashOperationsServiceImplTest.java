package com.sni.bokaticowork.features.payment.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.CashCountRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashExchangeRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashReasonRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashTransferReceiveRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashTransferRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashTransferResponse;
import com.sni.bokaticowork.features.payment.enums.*;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.*;
import com.sni.bokaticowork.features.payment.repository.*;
import com.sni.bokaticowork.features.payment.service.support.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** Lot C3 : transferts a deux temps, contre-passation, visas, echange, petite caisse. */
@ExtendWith(MockitoExtension.class)
class CashOperationsServiceImplTest {

    private static final String MAIN_CASHIER = "principale@elleaose.com";
    private static final String COUNTER_CASHIER = "guichet@elleaose.com";

    @Mock private CashOperationNatureRepository natureRepository;
    @Mock private CashMovementRepository movementRepository;
    @Mock private CashSessionRepository sessionRepository;
    @Mock private CashRegisterRepository registerRepository;
    @Mock private CashTransferOrderRepository transferRepository;
    @Mock private CashVarianceReviewRepository reviewRepository;
    @Mock private CashMovementWriter writer;
    @Mock private CashCountRecorder countRecorder;
    @Mock private CashReferenceLists referenceLists;
    @Spy private CashActorResolver actors = new CashActorResolver();
    @Mock private CashEmailNotifier emailNotifier;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private CashRegisterMapper mapper;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private CashOperationsServiceImpl service;

    private CashRegister main;
    private CashRegister counter;
    private CashSession mainSession;
    private CashSession counterSession;
    private final List<CashMovement> written = new ArrayList<>();
    private final List<CashTransferOrder> orders = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        main = CashRegister.builder().id(1L).registerCode("CSR-MAIN").name("Principale").registerType(CashRegisterType.MAIN)
                .status(CashRegisterStatus.ACTIVE).locationCode("SITE-1").build();
        counter = CashRegister.builder().id(2L).registerCode("CSR-G1").name("Guichet 1").registerType(CashRegisterType.COUNTER)
                .status(CashRegisterStatus.ACTIVE).locationCode("SITE-1").parentRegisterCode("CSR-MAIN").build();
        mainSession = CashSession.builder().id(10L).sessionNumber("CSS-MAIN").cashRegister(main).status(CashSessionStatus.OPEN)
                .openedBy(MAIN_CASHIER).build();
        counterSession = CashSession.builder().id(20L).sessionNumber("CSS-G1").cashRegister(counter).status(CashSessionStatus.OPEN)
                .openedBy(COUNTER_CASHIER).build();
        lenient().when(registerRepository.findByRegisterCode("CSR-MAIN")).thenReturn(Optional.of(main));
        lenient().when(registerRepository.findByRegisterCode("CSR-G1")).thenReturn(Optional.of(counter));
        lenient().when(sessionRepository.findBlockingSessionByOpenedBy(MAIN_CASHIER)).thenReturn(Optional.of(mainSession));
        lenient().when(sessionRepository.findBlockingSessionByOpenedBy(COUNTER_CASHIER)).thenReturn(Optional.of(counterSession));
        lenient().when(sequenceGenerator.next(anyString())).thenAnswer(invocation -> invocation.getArgument(0) + "-" + ids.incrementAndGet());
        lenient().when(writer.write(any())).thenAnswer(invocation -> {
            CashMovementWriter.Spec spec = invocation.getArgument(0);
            CashMovement movement = CashMovement.builder().id(ids.incrementAndGet()).movementNumber("CSM-" + ids.get())
                    .cashSession(spec.session()).movementType(spec.type()).amount(spec.amount()).direction(spec.direction())
                    .status(spec.status() == null ? CashMovementStatus.CONFIRMED : spec.status()).createdBy(spec.createdBy())
                    .transferOrderNumber(spec.transferOrderNumber()).relatedMovementId(spec.relatedMovementId()).build();
            written.add(movement);
            return movement;
        });
        lenient().when(transferRepository.save(any(CashTransferOrder.class))).thenAnswer(invocation -> {
            CashTransferOrder order = invocation.getArgument(0);
            if (!orders.contains(order)) {
                orders.add(order);
            }
            return order;
        });
        lenient().when(transferRepository.findByTransferNumber(anyString())).thenAnswer(invocation -> orders.stream()
                .filter(order -> order.getTransferNumber().equals(invocation.getArgument(0))).findFirst());
        lenient().when(movementRepository.findByMovementNumber(anyString())).thenAnswer(invocation -> written.stream()
                .filter(movement -> movement.getMovementNumber().equals(invocation.getArgument(0))).findFirst());
        lenient().when(countRecorder.totals(any(), any())).thenAnswer(invocation -> {
            CashCountRequest request = invocation.getArgument(1);
            BigDecimal total = request.totalAmount() != null ? request.totalAmount()
                    : request.lines().stream().map(line -> line.value().multiply(BigDecimal.valueOf(line.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CashCountRecorder.Totals("XAF", total, BigDecimal.ZERO, request.lines() != null, List.of());
        });
        lenient().when(countRecorder.record(any(), any(), any(), any(), anyString(), any(), any(), any(), any())).thenAnswer(invocation -> {
            CashCountRequest request = invocation.getArgument(3);
            return CashCount.builder().countNumber("PVB-" + ids.incrementAndGet()).total(request.totalAmount()).build();
        });
        lenient().when(reviewRepository.save(any(CashVarianceReview.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loggedInAs(String email, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null,
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    private static CashCountRequest total(String amount) {
        return new CashCountRequest(null, null, new BigDecimal(amount), null, null, null, null, null, null);
    }

    @Test
    @DisplayName("Transfert de 200 000 envoye, recu pour 195 000 : contestation et revue")
    void disputedTransfer() {
        loggedInAs(MAIN_CASHIER);
        CashTransferResponse sent = service.sendTransfer(new CashTransferRequest("CSR-MAIN", "CSR-G1", new BigDecimal("200000"), null, null, null));
        assertThat(sent.status()).isEqualTo(CashTransferStatus.IN_TRANSIT);
        assertThat(sent.kind()).isEqualTo(CashTransferKind.FEED);
        assertThat(written).singleElement().satisfies(out -> assertThat(out.getMovementType()).isEqualTo(CashMovementType.TRANSFER_OUT));

        loggedInAs(COUNTER_CASHIER);
        assertThatThrownBy(() -> service.receiveTransfer(sent.transferNumber(), new CashTransferReceiveRequest(total("195000"), null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("ecart");
        CashTransferResponse received = service.receiveTransfer(sent.transferNumber(),
                new CashTransferReceiveRequest(total("195000"), "Une liasse de 5 000 manquante"));

        assertThat(received.status()).isEqualTo(CashTransferStatus.DISPUTED);
        assertThat(received.difference()).isEqualByComparingTo("-5000");
        assertThat(received.reviewNumber()).isNotNull();
        assertThat(written).anySatisfy(in -> {
            assertThat(in.getMovementType()).isEqualTo(CashMovementType.TRANSFER_IN);
            assertThat(in.getAmount()).isEqualByComparingTo("195000");
        });
    }

    @Test
    @DisplayName("L'emetteur ne receptionne pas son propre transfert")
    void senderCannotReceive() {
        loggedInAs(MAIN_CASHIER);
        CashTransferResponse sent = service.sendTransfer(new CashTransferRequest("CSR-MAIN", "CSR-G1", new BigDecimal("1000"), null, null, null));

        assertThatThrownBy(() -> service.receiveTransfer(sent.transferNumber(), new CashTransferReceiveRequest(total("1000"), null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Un transfert hors de l'arbre des caisses est refuse")
    void circuitIsEnforced() {
        CashRegister other = CashRegister.builder().id(3L).registerCode("CSR-OTHER").name("Autre site")
                .registerType(CashRegisterType.COUNTER).status(CashRegisterStatus.ACTIVE).locationCode("SITE-2").build();
        when(registerRepository.findByRegisterCode("CSR-OTHER")).thenReturn(Optional.of(other));
        loggedInAs(COUNTER_CASHIER);

        assertThatThrownBy(() -> service.sendTransfer(new CashTransferRequest("CSR-G1", "CSR-OTHER", new BigDecimal("1000"), null, null, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Circuit");
    }

    @Test
    @DisplayName("Annuler un transfert en transit contre-passe sa sortie")
    void cancelledTransferIsReversed() {
        loggedInAs(MAIN_CASHIER);
        assertThatThrownBy(() -> service.sendTransfer(new CashTransferRequest("CSR-MAIN", null, new BigDecimal("3000"), null, null, null)))
                .as("sans destination ni caisse mere").isInstanceOf(BadRequestException.class);
        CashTransferResponse sent = service.sendTransfer(new CashTransferRequest("CSR-MAIN", "CSR-G1", new BigDecimal("3000"), null, null, null));
        CashMovement out = written.get(0);

        CashTransferResponse cancelled = service.cancelTransfer(sent.transferNumber(), new CashReasonRequest(null, "Porteur indisponible", null));

        assertThat(cancelled.status()).isEqualTo(CashTransferStatus.CANCELLED);
        assertThat(out.getStatus()).isEqualTo(CashMovementStatus.REVERSED);
        CashMovement back = written.get(1);
        assertThat(back.getDirection()).isEqualTo(CashFlowDirection.IN);
        assertThat(CashFlowRules.signedAmount(out).add(CashFlowRules.signedAmount(back))).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Contre-passation dans le delai par le caissier : le solde revient a sa valeur d'avant")
    void reversalRestoresTheBalance() {
        loggedInAs(COUNTER_CASHIER);
        CashMovement original = CashMovement.builder().id(500L).movementNumber("CSM-500").cashSession(counterSession)
                .movementType(CashMovementType.CASH_OUT).amount(new BigDecimal("5000")).status(CashMovementStatus.CONFIRMED)
                .createdBy(COUNTER_CASHIER).createdAt(Instant.now().minus(5, ChronoUnit.MINUTES)).build();
        written.add(original);
        when(referenceLists.describe(eq(CashOperationsServiceImpl.REVERSAL_REASON), anyString(), any())).thenReturn("Erreur de saisie");

        service.reverseMovement("CSM-500", new CashReasonRequest("ENTRY_ERROR", null, null));

        CashMovement reversal = written.get(written.size() - 1);
        assertThat(original.getStatus()).isEqualTo(CashMovementStatus.REVERSED);
        assertThat(reversal.getMovementType()).isEqualTo(CashMovementType.ADJUSTMENT);
        assertThat(reversal.getDirection()).isEqualTo(CashFlowDirection.IN);
        assertThat(CashFlowRules.signedAmount(original).add(CashFlowRules.signedAmount(reversal))).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Hors du delai, le caissier seul ne contre-passe pas ; un paiement ne se contre-passe pas ici")
    void reversalLimits() {
        loggedInAs(COUNTER_CASHIER);
        written.add(CashMovement.builder().id(501L).movementNumber("CSM-501").cashSession(counterSession)
                .movementType(CashMovementType.CASH_OUT).amount(new BigDecimal("5000")).status(CashMovementStatus.CONFIRMED)
                .createdBy(COUNTER_CASHIER).createdAt(Instant.now().minus(2, ChronoUnit.HOURS)).build());
        written.add(CashMovement.builder().id(502L).movementNumber("CSM-502").cashSession(counterSession)
                .movementType(CashMovementType.PAYMENT).amount(new BigDecimal("5000")).status(CashMovementStatus.CONFIRMED)
                .createdBy(COUNTER_CASHIER).createdAt(Instant.now()).build());

        assertThatThrownBy(() -> service.reverseMovement("CSM-501", new CashReasonRequest("ENTRY_ERROR", null, null)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.reverseMovement("CSM-502", new CashReasonRequest("ENTRY_ERROR", null, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("transaction de paiement");
    }

    @Test
    @DisplayName("Le second visa vient d'une autre personne habilitee")
    void secondVisa() {
        written.add(CashMovement.builder().id(600L).movementNumber("CSM-600").cashSession(counterSession)
                .movementType(CashMovementType.CASH_OUT).amount(new BigDecimal("80000")).status(CashMovementStatus.PENDING)
                .createdBy(COUNTER_CASHIER).build());

        loggedInAs(COUNTER_CASHIER, "CASH_MOVEMENT:APPROVE");
        assertThatThrownBy(() -> service.approveMovement("CSM-600")).isInstanceOf(ForbiddenException.class);

        loggedInAs("superviseur@elleaose.com", "CASH_MOVEMENT:APPROVE");
        service.approveMovement("CSM-600");
        assertThat(written.get(0).getStatus()).isEqualTo(CashMovementStatus.CONFIRMED);
        assertThat(written.get(0).getApprovedBy()).isEqualTo("superviseur@elleaose.com");
    }

    @Test
    @DisplayName("Un echange donne et recoit le meme montant")
    void exchangeMustBalance() {
        loggedInAs(COUNTER_CASHIER);
        when(sessionRepository.findBySessionNumber("CSS-G1")).thenReturn(Optional.of(counterSession));
        var tenThousand = new CashCountRequest.Line(new BigDecimal("10000"), CashDenominationKind.BILL, 1, 0);
        var fiveThousand = new CashCountRequest.Line(new BigDecimal("5000"), CashDenominationKind.BILL, 1, 0);

        assertThatThrownBy(() -> service.exchange("CSS-G1", new CashExchangeRequest(List.of(tenThousand), List.of(fiveThousand), null)))
                .isInstanceOf(BadRequestException.class);

        service.exchange("CSS-G1", new CashExchangeRequest(List.of(tenThousand),
                List.of(new CashCountRequest.Line(new BigDecimal("5000"), CashDenominationKind.BILL, 2, 0)), null));
        assertThat(written).singleElement().satisfies(movement -> {
            assertThat(movement.getMovementType()).isEqualTo(CashMovementType.EXCHANGE);
            assertThat(CashFlowRules.signedAmount(movement)).isEqualByComparingTo("0");
        });
    }

    @Test
    @DisplayName("Petite caisse : la realimentation proposee est le total des depenses depuis la derniere")
    void pettyCashRefill() {
        CashRegister petty = CashRegister.builder().id(4L).registerCode("CSR-PC").name("Petite caisse").registerType(CashRegisterType.PETTY_CASH)
                .status(CashRegisterStatus.ACTIVE).locationCode("SITE-1").parentRegisterCode("CSR-MAIN")
                .createdAt(Instant.parse("2026-11-01T00:00:00Z")).build();
        when(registerRepository.findByRegisterCode("CSR-PC")).thenReturn(Optional.of(petty));
        when(transferRepository.findIncoming(eq("CSR-PC"), eq(CashTransferKind.PETTY_REFILL), any())).thenReturn(List.of());
        when(movementRepository.findByRegisterTypeSince(eq("CSR-PC"), eq(CashMovementType.CASH_OUT), any(), any())).thenReturn(List.of(
                CashMovement.builder().amount(new BigDecimal("12000")).build(),
                CashMovement.builder().amount(new BigDecimal("8500")).build()));

        var proposal = service.pettyRefillProposal("CSR-PC");

        assertThat(proposal.amount()).isEqualByComparingTo("20500");
        assertThat(proposal.expenseCount()).isEqualTo(2);
        assertThat(proposal.parentRegisterCode()).isEqualTo("CSR-MAIN");
    }
}
