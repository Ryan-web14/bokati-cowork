package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.features.payment.dto.response.CashReportResponse;
import com.sni.bokaticowork.features.payment.enums.*;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.*;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import com.sni.bokaticowork.features.payment.service.support.CashSessionSummarySupport;
import org.junit.jupiter.api.AfterEach;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** Lot C6 : position de tresorerie, brouillard, export CSV. */
@ExtendWith(MockitoExtension.class)
class CashDashboardServiceImplTest {

    @Mock private CashRegisterRepository registerRepository;
    @Mock private CashSessionRepository sessionRepository;
    @Mock private CashMovementRepository movementRepository;
    @Mock private CashTransferOrderRepository transferRepository;
    @Mock private CashVarianceReviewRepository reviewRepository;
    @Mock private CashCountRepository countRepository;
    @Mock private CashHandoverRepository handoverRepository;
    @Mock private CashResponsibilityTransferRepository responsibilityRepository;
    @Mock private CashOperationNatureRepository natureRepository;
    @Mock private CashSessionSummarySupport summarySupport;
    @Spy private CashActorResolver actors = new CashActorResolver();
    @Mock private CashEmailNotifier emailNotifier;

    @InjectMocks private CashDashboardServiceImpl service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loggedInAs(String email, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null,
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    private CashRegister register(boolean blind) {
        return CashRegister.builder().id(1L).registerCode("CSR-1").name("Accueil").registerType(CashRegisterType.COUNTER)
                .status(CashRegisterStatus.ACTIVE).locationCode("SITE-1").blindClose(blind).maxCashAmount(new BigDecimal("100000"))
                .lastCountAt(Instant.now().minus(10, ChronoUnit.DAYS)).lastCountAmount(new BigDecimal("20000")).notCountedAlertDays(7)
                .spotCheckEveryDays(30).createdAt(Instant.now().minus(60, ChronoUnit.DAYS)).build();
    }

    @Test
    @DisplayName("Position : attendu masque sans le droit de decider, alertes de comptage, totaux par site")
    void positionWithBlindClose() {
        CashRegister register = register(true);
        CashSession session = CashSession.builder().id(10L).sessionNumber("CSS-1").cashRegister(register)
                .status(CashSessionStatus.OPEN).openedBy("caissier@elleaose.com").build();
        when(registerRepository.findAll()).thenReturn(List.of(register));
        when(sessionRepository.findBlockingSessionByCashRegisterId(1L)).thenReturn(Optional.of(session));
        when(summarySupport.expectedClosingAmount(session)).thenReturn(new BigDecimal("95000"));
        when(transferRepository.findByStatusOrderBySentAtAsc(CashTransferStatus.IN_TRANSIT)).thenReturn(List.of());
        when(reviewRepository.findByStatusOrderByCreatedAtAsc(CashVarianceStatus.PENDING)).thenReturn(List.of());
        when(movementRepository.findByCashSession_IdAndStatus(10L, CashMovementStatus.PENDING)).thenReturn(List.of());
        when(countRepository.findByRegisterCodeOrderByCreatedAtDesc("CSR-1")).thenReturn(List.of());

        loggedInAs("comptable@elleaose.com");
        var hidden = service.position(null).registers().get(0);
        assertThat(hidden.expectedHidden()).isTrue();
        assertThat(hidden.expectedBalance()).isNull();
        assertThat(hidden.alerts()).anyMatch(alert -> alert.contains("Non comptee depuis 10 jours"))
                .anyMatch(alert -> alert.contains("inopine"));

        loggedInAs("superviseur@elleaose.com", "CASH_VARIANCE:DECIDE");
        var position = service.position(null);
        assertThat(position.registers().get(0).expectedBalance()).isEqualByComparingTo("95000");
        assertThat(position.registers().get(0).occupancyPercent()).isEqualByComparingTo("95.0");
        assertThat(position.sites()).singleElement().satisfies(site -> assertThat(site.openSessions()).isEqualTo(1));
    }

    @Test
    @DisplayName("Brouillard du jour : solde de debut, entrees, sorties, solde de fin ; CSV avec separateur point-virgule")
    void dailyBlotter() {
        CashRegister register = register(false);
        CashSession session = CashSession.builder().id(10L).sessionNumber("CSS-1").cashRegister(register).build();
        when(registerRepository.findByRegisterCode("CSR-1")).thenReturn(Optional.of(register));
        when(movementRepository.findByRegisterBetween(eq("CSR-1"), any(), any())).thenReturn(List.of(
                movement(session, CashMovementType.OPENING_FLOAT, "20000", CashMovementStatus.CONFIRMED),
                movement(session, CashMovementType.PAYMENT, "15000", CashMovementStatus.CONFIRMED),
                movement(session, CashMovementType.CASH_OUT, "4000", CashMovementStatus.CONFIRMED),
                movement(session, CashMovementType.CASH_OUT, "9999", CashMovementStatus.CANCELLED)));

        CashReportResponse report = service.report("daily-blotter", "CSR-1", LocalDate.of(2026, 11, 11), null);

        assertThat(report.totals()).containsEntry("soldeDebut", new BigDecimal("20000"));
        assertThat((BigDecimal) report.totals().get("entrees")).isEqualByComparingTo("15000");
        assertThat((BigDecimal) report.totals().get("sorties")).isEqualByComparingTo("4000");
        assertThat((BigDecimal) report.totals().get("soldeFin")).isEqualByComparingTo("31000");
        String csv = service.toCsv(report);
        assertThat(csv).startsWith("heure;numero;session;type").contains("PAYMENT");
    }

    @Test
    @DisplayName("CSV : une valeur contenant un point-virgule est protegee")
    void csvEscaping() {
        CashReportResponse report = new CashReportResponse("x", "X", Instant.now(), null, Map.of(), List.of("a"),
                List.of(Map.of("a", "un; deux")), Map.of());
        assertThat(service.toCsv(report)).contains("\"un; deux\"");
    }

    private static CashMovement movement(CashSession session, CashMovementType type, String amount, CashMovementStatus status) {
        return CashMovement.builder().movementNumber(type.name()).cashSession(session).movementType(type)
                .amount(new BigDecimal(amount)).status(status).createdAt(Instant.now()).build();
    }
}
