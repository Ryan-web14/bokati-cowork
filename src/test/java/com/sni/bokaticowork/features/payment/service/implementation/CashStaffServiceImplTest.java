package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.CashAbsenceRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashCountRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashSealRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashAbsenceResponse;
import com.sni.bokaticowork.features.payment.enums.CashAbsenceStatus;
import com.sni.bokaticowork.features.payment.enums.CashAssignmentQuality;
import com.sni.bokaticowork.features.payment.enums.CashAssignmentRole;
import com.sni.bokaticowork.features.payment.enums.CashRegisterStatus;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.model.CashCount;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashRegisterAssignment;
import com.sni.bokaticowork.features.payment.model.CashSeal;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.CashStaffAbsence;
import com.sni.bokaticowork.features.payment.repository.*;
import com.sni.bokaticowork.features.payment.service.interfaces.CashSessionLifecycleService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashCountRecorder;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import com.sni.bokaticowork.features.payment.service.support.CashReferenceLists;
import com.sni.bokaticowork.features.payment.service.support.CashRegisterAccessGuard;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot C2 : absences et remplacements, depart, scelles. */
@ExtendWith(MockitoExtension.class)
class CashStaffServiceImplTest {

    private static final String CASHIER = "caissier@elleaose.com";
    private static final String MANAGER = "responsable@elleaose.com";

    @Mock private CashStaffAbsenceRepository absenceRepository;
    @Mock private CashRegisterAssignmentRepository assignmentRepository;
    @Mock private CashRegisterRepository registerRepository;
    @Mock private CashRegisterHistoryRepository historyRepository;
    @Mock private CashSessionRepository sessionRepository;
    @Mock private CashVarianceReviewRepository reviewRepository;
    @Mock private CashSealRepository sealRepository;
    @Mock private CashDelegationRepository delegationRepository;
    @Mock private CashCountRecorder countRecorder;
    @Mock private CashRegisterAccessGuard accessGuard;
    @Mock private CashReferenceLists referenceLists;
    @Spy private CashActorResolver actors = new CashActorResolver();
    @Mock private CashEmailNotifier emailNotifier;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private CashSessionLifecycleService lifecycle;

    @InjectMocks private CashStaffServiceImpl service;

    private final List<CashRegisterAssignment> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        lenient().when(sequenceGenerator.next(anyString())).thenReturn("ABS-1");
        lenient().when(referenceLists.describe(eq(CashStaffServiceImpl.ABSENCE_REASON), anyString(), any())).thenReturn("Conge annuel");
        lenient().when(absenceRepository.save(any(CashStaffAbsence.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(assignmentRepository.save(any(CashRegisterAssignment.class))).thenAnswer(invocation -> {
            created.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        lenient().when(assignmentRepository.findLiveByUser(CASHIER)).thenReturn(List.of(CashRegisterAssignment.builder()
                .registerCode("CSR-1").userEmail(CASHIER).role(CashAssignmentRole.CASHIER).quality(CashAssignmentQuality.HOLDER)
                .validFrom(Instant.parse("2026-01-01T00:00:00Z")).build()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loggedInAs(String email, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null,
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    @DisplayName("Conge declare par le responsable avec remplacant : interim cree aux bonnes dates")
    void plannedLeaveCreatesTheInterim() {
        loggedInAs(MANAGER, "CASH_STAFF:MANAGE");
        LocalDate start = LocalDate.now(CashRegisterAccessGuard.ZONE).plusDays(10);
        LocalDate end = start.plusDays(14);

        CashAbsenceResponse response = service.declareAbsence(new CashAbsenceRequest(CASHIER, "ANNUAL_LEAVE", null, start, end,
                "releve@elleaose.com", null, false, false, null));

        assertThat(response.status()).isEqualTo(CashAbsenceStatus.APPROVED);
        assertThat(response.registerCodes()).containsExactly("CSR-1");
        assertThat(created).singleElement().satisfies(interim -> {
            assertThat(interim.getUserEmail()).isEqualTo("releve@elleaose.com");
            assertThat(interim.getQuality()).isEqualTo(CashAssignmentQuality.INTERIM);
            assertThat(interim.getValidFrom()).isEqualTo(start.atStartOfDay(CashRegisterAccessGuard.ZONE).toInstant());
            assertThat(interim.getValidTo()).isEqualTo(end.plusDays(1).atStartOfDay(CashRegisterAccessGuard.ZONE).toInstant());
        });
    }

    @Test
    @DisplayName("Declaree par le caissier lui-meme : a valider, et il ne la valide pas")
    void selfDeclaredAbsenceNeedsApproval() {
        loggedInAs(CASHIER);
        LocalDate start = LocalDate.now(CashRegisterAccessGuard.ZONE).plusDays(10);
        CashAbsenceResponse response = service.declareAbsence(new CashAbsenceRequest(null, "ANNUAL_LEAVE", null, start, start.plusDays(3),
                "releve@elleaose.com", null, false, false, null));
        assertThat(response.status()).isEqualTo(CashAbsenceStatus.PLANNED);

        when(absenceRepository.findByAbsenceNumber("ABS-1")).thenReturn(Optional.of(CashStaffAbsence.builder().absenceNumber("ABS-1")
                .userEmail(CASHIER).kindCode("ANNUAL_LEAVE").startsOn(start).status(CashAbsenceStatus.PLANNED).declaredBy(CASHIER).build()));
        assertThatThrownBy(() -> service.approveAbsence("ABS-1")).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Absence qui commence aujourd'hui : la session laissee ouverte est suspendue")
    void absenceStartingTodaySuspendsTheSession() {
        loggedInAs(MANAGER, "CASH_STAFF:MANAGE");
        when(sessionRepository.findBlockingSessionByOpenedBy(CASHIER)).thenReturn(Optional.of(
                CashSession.builder().sessionNumber("CSS-9").status(CashSessionStatus.OPEN).openedBy(CASHIER).build()));

        service.declareAbsence(new CashAbsenceRequest(CASHIER, "SICKNESS", null, LocalDate.now(CashRegisterAccessGuard.ZONE), null,
                "releve@elleaose.com", null, true, false, null));

        verify(lifecycle).suspendBySystem(eq("CSS-9"), anyString());
    }

    @Test
    @DisplayName("Depart bloque par une session non close, sauf forcage motive")
    void departureBlockedByPendingItems() {
        loggedInAs(MANAGER, "CASH_STAFF:MANAGE");
        when(sessionRepository.findBlockingSessionByOpenedBy(CASHIER)).thenReturn(Optional.of(
                CashSession.builder().sessionNumber("CSS-9").status(CashSessionStatus.CLOSING_REVIEW).openedBy(CASHIER).build()));
        LocalDate start = LocalDate.now(CashRegisterAccessGuard.ZONE).plusDays(5);

        assertThatThrownBy(() -> service.declareAbsence(new CashAbsenceRequest(CASHIER, "DEPARTURE", null, start, null,
                null, null, false, false, null))).isInstanceOf(BadRequestException.class).hasMessageContaining("CSS-9");

        CashAbsenceResponse forced = service.declareAbsence(new CashAbsenceRequest(CASHIER, "DEPARTURE", null, start, null,
                null, null, false, true, "Ecart impute sur le solde de tout compte"));
        assertThat(forced.status()).isEqualTo(CashAbsenceStatus.APPROVED);
        assertThat(forced.warnings()).anyMatch(warning -> warning.contains("titulaire"));
    }

    @Test
    @DisplayName("Scelles : temoin different, caisse suspendue ; a l'ouverture, ecart signale et caisse reactivee")
    void sealAndUnseal() {
        loggedInAs(MANAGER);
        CashRegister register = CashRegister.builder().id(1L).registerCode("CSR-1").name("Accueil").status(CashRegisterStatus.ACTIVE)
                .active(true).build();
        when(registerRepository.findByRegisterCode("CSR-1")).thenReturn(Optional.of(register));
        when(sessionRepository.findBlockingSessionByCashRegisterId(1L)).thenReturn(Optional.empty());
        List<CashSeal> seals = new ArrayList<>();
        when(sealRepository.findByRegisterCodeOrderByCreatedAtDesc("CSR-1")).thenAnswer(invocation -> List.copyOf(seals));
        when(sealRepository.save(any(CashSeal.class))).thenAnswer(invocation -> {
            CashSeal seal = invocation.getArgument(0);
            if (!seals.contains(seal)) {
                seals.add(seal);
            }
            return seal;
        });
        when(countRecorder.record(any(), any(), any(), any(), anyString(), any(), any(), any(), any())).thenAnswer(invocation -> {
            CashCountRequest request = invocation.getArgument(3);
            return CashCount.builder().countNumber("PVB-" + seals.size()).total(request.totalAmount()).build();
        });
        CashCountRequest sealed = new CashCountRequest(null, null, new BigDecimal("80000"), null, null, null, null, null, null);

        assertThatThrownBy(() -> service.seal("CSR-1", new CashSealRequest("ENV-1", "Coffre", MANAGER, sealed, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("temoin");
        service.seal("CSR-1", new CashSealRequest("ENV-1", "Coffre", "temoin@elleaose.com", sealed, null));
        assertThat(register.getStatus()).isEqualTo(CashRegisterStatus.SUSPENDED);

        service.unseal("CSR-1", new CashSealRequest(null, null, "temoin@elleaose.com",
                new CashCountRequest(null, null, new BigDecimal("79000"), null, null, null, null, null, null), null));
        assertThat(register.getStatus()).isEqualTo(CashRegisterStatus.ACTIVE);
        assertThat(register.getLastCountAmount()).isEqualByComparingTo("79000");
        verify(reviewRepository).save(any());
    }
}
