package com.sni.bokaticowork.features.payment.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.CashRegisterAssignmentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashRegisterStatusRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateResponsibilityTransferRequest;
import com.sni.bokaticowork.features.payment.dto.request.UpdateCashRegisterRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashResponsibilityTransferResponse;
import com.sni.bokaticowork.features.payment.enums.CashAssignmentQuality;
import com.sni.bokaticowork.features.payment.enums.CashAssignmentRole;
import com.sni.bokaticowork.features.payment.enums.CashRegisterStatus;
import com.sni.bokaticowork.features.payment.enums.CashRegisterType;
import com.sni.bokaticowork.features.payment.enums.CashResponsibilityTransferStatus;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashRegisterAssignment;
import com.sni.bokaticowork.features.payment.model.CashRegisterHistory;
import com.sni.bokaticowork.features.payment.model.CashResponsibilityTransfer;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashReferenceValueRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterAssignmentRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterDeviceRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterHistoryRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterScheduleRepository;
import com.sni.bokaticowork.features.payment.repository.CashRequestRepository;
import com.sni.bokaticowork.features.payment.repository.CashResponsibilityTransferRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import com.sni.bokaticowork.features.payment.service.support.CashReferenceLists;
import com.sni.bokaticowork.features.payment.service.support.CashRegisterAccessGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot C1 : fiche historisee, cycle de vie, affectations et remise de caisse. */
@ExtendWith(MockitoExtension.class)
class CashRegisterAdminServiceImplTest {

    private static final String MANAGER = "responsable@elleaose.com";

    @Mock private CashRegisterRepository registerRepository;
    @Mock private CashSessionRepository sessionRepository;
    @Mock private CashMovementRepository movementRepository;
    @Mock private CashRequestRepository requestRepository;
    @Mock private CashRegisterAssignmentRepository assignmentRepository;
    @Mock private CashRegisterHistoryRepository historyRepository;
    @Mock private CashRegisterScheduleRepository scheduleRepository;
    @Mock private CashRegisterDeviceRepository deviceRepository;
    @Mock private CashResponsibilityTransferRepository transferRepository;
    @Mock private CashReferenceValueRepository referenceRepository;
    @Mock private CashReferenceLists referenceLists;
    @Mock private CashRegisterAccessGuard accessGuard;
    @Mock private CashEmailNotifier emailNotifier;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Spy private CashActorResolver actors = new CashActorResolver();
    @Spy private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks private CashRegisterAdminServiceImpl service;

    private final List<CashRegisterAssignment> assignments = new ArrayList<>();
    private CashRegister register;

    @BeforeEach
    void setUp() {
        loggedInAs(MANAGER);
        register = CashRegister.builder().id(1L).registerCode("CSR-1").name("Accueil")
                .registerType(CashRegisterType.COUNTER).status(CashRegisterStatus.ACTIVE).active(true)
                .maxOutPerOperation(new BigDecimal("50000.0000")).build();
        lenient().when(registerRepository.findByRegisterCode("CSR-1")).thenReturn(Optional.of(register));
        lenient().when(assignmentRepository.findByRegisterCodeOrderByQualityAscBackupRankAscValidFromAsc("CSR-1"))
                .thenAnswer(invocation -> List.copyOf(assignments));
        lenient().when(assignmentRepository.save(any(CashRegisterAssignment.class))).thenAnswer(invocation -> {
            CashRegisterAssignment assignment = invocation.getArgument(0);
            if (!assignments.contains(assignment)) {
                assignments.add(assignment);
            }
            return assignment;
        });
        lenient().when(assignmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(scheduleRepository.findByRegisterCodeOrderByDayOfWeekAscOpensAtAsc("CSR-1")).thenReturn(List.of());
        lenient().when(deviceRepository.findByRegisterCodeOrderByFirstSeenAtAsc("CSR-1")).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loggedInAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }

    @Test
    @DisplayName("Chaque modification de la fiche est historisee : champ, ancienne et nouvelle valeur, auteur")
    void updatesAreHistorised() {
        service.update("CSR-1", new UpdateCashRegisterRequest("Accueil rez-de-chaussee", null, null, null, null, null,
                null, null, null, new BigDecimal("75000"), null, null, null, null, null, null, null, "Nouvelle grille"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CashRegisterHistory>> saved = ArgumentCaptor.forClass(List.class);
        verify(historyRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(CashRegisterHistory::getField, CashRegisterHistory::getOldValue,
                        CashRegisterHistory::getNewValue, CashRegisterHistory::getChangedBy)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("name", "Accueil", "Accueil rez-de-chaussee", MANAGER),
                        org.assertj.core.groups.Tuple.tuple("maxOutPerOperation", "50000", "75000", MANAGER));
        assertThat(register.getUpdatedBy()).isEqualTo(MANAGER);
    }

    @Test
    @DisplayName("Une valeur identique ne produit pas d'historique")
    void unchangedValuesAreNotHistorised() {
        service.update("CSR-1", new UpdateCashRegisterRequest("Accueil", null, null, null, null, null,
                null, null, null, new BigDecimal("50000"), null, null, null, null, null, null, null, null));

        verify(historyRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Deux titulaires en meme temps sur un guichet sont refuses")
    void onlyOneHolderAtATime() {
        assertThatThrownBy(() -> service.replaceAssignments("CSR-1", List.of(
                new CashRegisterAssignmentRequest("a@elleaose.com", CashAssignmentRole.CASHIER, CashAssignmentQuality.HOLDER, null, null, null, null),
                new CashRegisterAssignmentRequest("b@elleaose.com", CashAssignmentRole.CASHIER, CashAssignmentQuality.HOLDER, null, null, null, null))))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("titulaire");
    }

    @Test
    @DisplayName("Un interim sans date de fin est refuse")
    void interimNeedsAnEnd() {
        assertThatThrownBy(() -> service.replaceAssignments("CSR-1", List.of(
                new CashRegisterAssignmentRequest("a@elleaose.com", CashAssignmentRole.CASHIER, CashAssignmentQuality.INTERIM, null, null, null, null))))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("date de fin");
    }

    @Test
    @DisplayName("Une affectation retiree de la liste est revoquee, pas supprimee")
    void removedAssignmentIsRevoked() {
        CashRegisterAssignment old = CashRegisterAssignment.builder().registerCode("CSR-1").userEmail("ancien@elleaose.com")
                .role(CashAssignmentRole.CASHIER).quality(CashAssignmentQuality.BACKUP)
                .validFrom(Instant.now().minus(10, ChronoUnit.DAYS)).build();
        assignments.add(old);

        service.replaceAssignments("CSR-1", List.of(
                new CashRegisterAssignmentRequest("nouveau@elleaose.com", CashAssignmentRole.CASHIER, CashAssignmentQuality.HOLDER, null, null, null, null)));

        assertThat(old.getRevokedAt()).isNotNull();
        assertThat(old.getRevokedBy()).isEqualTo(MANAGER);
        verify(assignmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Une caisse avec une session en cours ne se suspend pas")
    void cannotSuspendWithOpenSession() {
        when(sessionRepository.findBlockingSessionByCashRegisterId(1L)).thenReturn(Optional.of(
                CashSession.builder().sessionNumber("CSS-9").status(CashSessionStatus.OPEN).build()));

        assertThatThrownBy(() -> service.suspend("CSR-1", new CashRegisterStatusRequest("CONTROL", null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("CSS-9");
    }

    @Test
    @DisplayName("Une caisse ne se ferme que si son dernier comptage vaut zero")
    void closingNeedsAZeroCount() {
        when(sessionRepository.findBlockingSessionByCashRegisterId(1L)).thenReturn(Optional.empty());
        register.setLastCountAmount(new BigDecimal("12000"));

        assertThatThrownBy(() -> service.close("CSR-1", null))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("zero");

        register.setLastCountAmount(BigDecimal.ZERO);
        service.close("CSR-1", null);
        assertThat(register.getStatus()).isEqualTo(CashRegisterStatus.CLOSED);
        assertThat(register.getActive()).isFalse();
        assertThat(register.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("Remise de caisse refusee tant qu'une session est ouverte")
    void transferRefusedWithOpenSession() {
        when(referenceLists.describe(eq(CashReferenceLists.RESPONSIBILITY_TRANSFER_REASON), anyString(), any())).thenReturn("Conge");
        when(sessionRepository.findByRegisterCodeAndStatuses(eq("CSR-1"), anyList())).thenReturn(List.of(
                CashSession.builder().sessionNumber("CSS-7").status(CashSessionStatus.OPEN).openedBy("titulaire@elleaose.com").build()));

        assertThatThrownBy(() -> service.createResponsibilityTransfer("CSR-1", transfer(true, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("CSS-7");
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Remise definitive : l'ancien titulaire s'arrete a la date d'effet, le nouveau commence, PV a signer")
    void permanentTransferSwitchesHolders() {
        CashRegisterAssignment holder = CashRegisterAssignment.builder().registerCode("CSR-1").userEmail("titulaire@elleaose.com")
                .role(CashAssignmentRole.CASHIER).quality(CashAssignmentQuality.HOLDER)
                .validFrom(Instant.now().minus(30, ChronoUnit.DAYS)).build();
        assignments.add(holder);
        when(referenceLists.describe(eq(CashReferenceLists.RESPONSIBILITY_TRANSFER_REASON), anyString(), any())).thenReturn("Mutation");
        when(sessionRepository.findByRegisterCodeAndStatuses(eq("CSR-1"), anyList())).thenReturn(List.of());
        when(sequenceGenerator.next("cash_responsibility_transfer")).thenReturn("PRC-202611-000001");
        when(transferRepository.save(any(CashResponsibilityTransfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashResponsibilityTransferResponse response = service.createResponsibilityTransfer("CSR-1", transfer(true, null));

        assertThat(holder.getValidTo()).isNotNull();
        assertThat(assignments).anySatisfy(created -> {
            assertThat(created.getUserEmail()).isEqualTo("nouveau@elleaose.com");
            assertThat(created.getQuality()).isEqualTo(CashAssignmentQuality.HOLDER);
            assertThat(created.getSourceReference()).isEqualTo("PRC-202611-000001");
        });
        assertThat(response.fromUser()).isEqualTo("titulaire@elleaose.com");
        assertThat(response.status()).isEqualTo(CashResponsibilityTransferStatus.PENDING_SIGNATURES);
    }

    @Test
    @DisplayName("Titulaire sortant absent : un temoin est exige, different du superviseur et de l'entrant")
    void absentOutgoingHolderNeedsAWitness() {
        assignments.add(CashRegisterAssignment.builder().registerCode("CSR-1").userEmail("titulaire@elleaose.com")
                .role(CashAssignmentRole.CASHIER).quality(CashAssignmentQuality.HOLDER)
                .validFrom(Instant.now().minus(30, ChronoUnit.DAYS)).build());

        assertThatThrownBy(() -> service.createResponsibilityTransfer("CSR-1", transfer(true, true)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("temoin");
    }

    @Test
    @DisplayName("Le PV est signe par le sortant et l'entrant, et par personne d'autre")
    void transferSignatures() {
        CashResponsibilityTransfer transfer = CashResponsibilityTransfer.builder().transferNumber("PRC-1").registerCode("CSR-1")
                .fromUser("titulaire@elleaose.com").toUser("nouveau@elleaose.com").reasonCode("TRANSFER").permanent(true)
                .effectiveAt(Instant.now()).status(CashResponsibilityTransferStatus.PENDING_SIGNATURES).createdBy(MANAGER).build();
        when(transferRepository.findByTransferNumber("PRC-1")).thenReturn(Optional.of(transfer));
        lenient().when(transferRepository.save(any(CashResponsibilityTransfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        loggedInAs("intrus@elleaose.com");
        assertThatThrownBy(() -> service.signResponsibilityTransfer("PRC-1")).isInstanceOf(ForbiddenException.class);

        loggedInAs("titulaire@elleaose.com");
        service.signResponsibilityTransfer("PRC-1");
        loggedInAs("nouveau@elleaose.com");
        CashResponsibilityTransferResponse signed = service.signResponsibilityTransfer("PRC-1");

        assertThat(signed.status()).isEqualTo(CashResponsibilityTransferStatus.SIGNED);
        assertThat(signed.outgoingSignedBy()).isEqualTo("titulaire@elleaose.com");
        assertThat(signed.incomingSignedBy()).isEqualTo("nouveau@elleaose.com");
    }

    private CreateResponsibilityTransferRequest transfer(boolean permanent, Boolean outgoingAbsent) {
        return new CreateResponsibilityTransferRequest("nouveau@elleaose.com", "TRANSFER", null, permanent, null,
                permanent ? null : Instant.now().plus(15, ChronoUnit.DAYS), new BigDecimal("40000"), null, outgoingAbsent, false);
    }
}
