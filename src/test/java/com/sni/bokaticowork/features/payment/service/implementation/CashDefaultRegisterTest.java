package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.OpenCashSessionRequest;
import com.sni.bokaticowork.features.payment.enums.CashMonthEndPolicy;
import com.sni.bokaticowork.features.payment.enums.CashRegisterStatus;
import com.sni.bokaticowork.features.payment.enums.CashRegisterType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashSessionLifecycleService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashRegisterAccessGuard;
import com.sni.bokaticowork.features.payment.service.support.CashSessionSummarySupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Caisse par defaut creee par le systeme, et ouverture de session par un administrateur sans validation. */
@ExtendWith(MockitoExtension.class)
class CashDefaultRegisterTest {

    @Mock private CashRegisterRepository registerRepository;
    @Mock private CashSessionRepository sessionRepository;
    @Mock private CashMovementRepository movementRepository;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private CashSessionSummarySupport summarySupport;
    @Mock private CashRegisterMapper mapper;
    @Spy private CashActorResolver actors = new CashActorResolver();
    @Mock private CashRegisterAccessGuard accessGuard;
    @Mock private CashRegisterAdminServiceImpl registerAdmin;
    @Mock private CashSessionLifecycleService lifecycle;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks private CashRegisterServiceImpl service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void loggedInAs(String email, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    @DisplayName("Sans caisse par defaut : le systeme la cree (tous moyens, report en fin de mois) et lui ouvre une session")
    void createsDefaultRegisterAndSystemSession() {
        when(registerRepository.findFirstByDefaultRegisterTrue()).thenReturn(Optional.empty());
        when(registerRepository.findByRegisterCode(CashRegisterServiceImpl.DEFAULT_REGISTER_CODE)).thenReturn(Optional.empty());
        when(registerRepository.save(any(CashRegister.class))).thenAnswer(invocation -> {
            CashRegister register = invocation.getArgument(0);
            register.setId(7L);
            return register;
        });
        when(sessionRepository.findBlockingSessionByCashRegisterId(7L)).thenReturn(Optional.empty());
        when(sequenceGenerator.next("cash_session")).thenReturn("CSS-DEF-1");
        when(sessionRepository.save(any(CashSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String sessionNumber = service.defaultSessionNumber();

        assertThat(sessionNumber).isEqualTo("CSS-DEF-1");
        ArgumentCaptor<CashRegister> register = ArgumentCaptor.forClass(CashRegister.class);
        verify(registerRepository).save(register.capture());
        assertThat(register.getValue().getRegisterCode()).isEqualTo("CSR-DEFAULT");
        assertThat(register.getValue().getDefaultRegister()).isTrue();
        assertThat(register.getValue().getSystemManaged()).isFalse();
        assertThat(register.getValue().getRestrictedToMethod()).isNull();
        assertThat(register.getValue().getRegisterType()).isEqualTo(CashRegisterType.MAIN);
        assertThat(register.getValue().getMonthEndPolicy()).isEqualTo(CashMonthEndPolicy.CARRY_OVER);
        ArgumentCaptor<CashSession> session = ArgumentCaptor.forClass(CashSession.class);
        verify(sessionRepository).save(session.capture());
        assertThat(session.getValue().getStatus()).isEqualTo(CashSessionStatus.OPEN);
        assertThat(session.getValue().getOpenedBy()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("La session ouverte de la caisse par defaut est reprise, sans en ouvrir une autre")
    void reusesOpenDefaultSession() {
        CashRegister register = defaultRegister();
        when(registerRepository.findFirstByDefaultRegisterTrue()).thenReturn(Optional.of(register));
        when(sessionRepository.findBlockingSessionByCashRegisterId(7L)).thenReturn(Optional.of(CashSession.builder()
                .sessionNumber("CSS-DEF-2").cashRegister(register).status(CashSessionStatus.OPEN).openedBy("SYSTEM").build()));

        assertThat(service.defaultSessionNumber()).isEqualTo("CSS-DEF-2");
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Session de la caisse par defaut suspendue : le paiement sans session est refuse avec la marche a suivre")
    void suspendedDefaultSessionIsRefused() {
        CashRegister register = defaultRegister();
        when(registerRepository.findFirstByDefaultRegisterTrue()).thenReturn(Optional.of(register));
        when(sessionRepository.findBlockingSessionByCashRegisterId(7L)).thenReturn(Optional.of(CashSession.builder()
                .sessionNumber("CSS-DEF-3").cashRegister(register).status(CashSessionStatus.SUSPENDED).openedBy("SYSTEM").build()));

        assertThatThrownBy(() -> service.defaultSessionNumber())
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("indiquez la session");
    }

    @Test
    @DisplayName("Fin de mois : la session de la caisse par defaut est close et rouverte par le systeme avec le solde reporte")
    void monthEndCarriesOverDefaultSession() {
        CashRegister register = defaultRegister();
        CashSession open = CashSession.builder().id(70L).sessionNumber("CSS-DEF-4").cashRegister(register)
                .status(CashSessionStatus.OPEN).openedBy("SYSTEM").openingAmount(BigDecimal.ZERO)
                .openedAt(Instant.parse("2026-10-03T08:00:00Z")).build();
        when(sessionRepository.findBySessionNumber("CSS-DEF-4")).thenReturn(Optional.of(open));
        when(summarySupport.expectedClosingAmount(open)).thenReturn(new BigDecimal("125000"));
        when(sessionRepository.saveAndFlush(any(CashSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.save(any(CashSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sequenceGenerator.next(anyString())).thenAnswer(invocation -> invocation.getArgument(0) + "-1");
        lenient().when(movementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.closeForMonthEnd("CSS-DEF-4");

        assertThat(open.getStatus()).isEqualTo(CashSessionStatus.CLOSED);
        ArgumentCaptor<CashSession> next = ArgumentCaptor.forClass(CashSession.class);
        verify(sessionRepository).save(next.capture());
        assertThat(next.getValue().getOpenedBy()).isEqualTo("SYSTEM");
        assertThat(next.getValue().getOpeningAmount()).isEqualByComparingTo("125000");
        verify(lifecycle, never()).suspendBySystem(anyString(), anyString());
    }

    @Test
    @DisplayName("Un administrateur ouvre sans validation : controles leves, session ouverte malgre l'ecart, trace de l'administrateur")
    void adminOpensWithoutValidation() {
        loggedInAs("admin@elleaose.com", "ROLE_SUPER_ADMIN");
        CashRegister register = counter();
        when(registerRepository.findByRegisterCode("CSR-1")).thenReturn(Optional.of(register));
        when(accessGuard.assertCanOpen(eq(register), eq("admin@elleaose.com"), isNull(), isNull(), isNull(), eq(true)))
                .thenReturn(new CashRegisterAccessGuard.OpeningClearance(null, "Ouverture hors horaires par un administrateur", null));
        when(sessionRepository.findBlockingSessionByCashRegisterId(anyLong())).thenReturn(Optional.empty());
        when(sessionRepository.findBlockingSessionByOpenedBy("admin@elleaose.com")).thenReturn(Optional.empty());
        when(sequenceGenerator.next(anyString())).thenAnswer(invocation -> invocation.getArgument(0) + "-1");
        when(sessionRepository.save(any(CashSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(movementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.openSession(new OpenCashSessionRequest("CSR-1", null, new BigDecimal("12000")));

        ArgumentCaptor<CashSession> session = ArgumentCaptor.forClass(CashSession.class);
        verify(sessionRepository).save(session.capture());
        assertThat(session.getValue().getStatus()).isEqualTo(CashSessionStatus.OPEN);
        assertThat(session.getValue().getOpeningChecksWaivedBy()).isEqualTo("admin@elleaose.com");
        assertThat(session.getValue().getOpeningVariance()).isEqualByComparingTo("2000");
        verify(lifecycle).recordAdminOpeningVariance(eq(session.getValue()), isNull());
    }

    @Test
    @DisplayName("Un caissier ordinaire garde les controles : ecart d'ouverture a confirmer")
    void cashierKeepsChecks() {
        loggedInAs("caissier@elleaose.com");
        CashRegister register = counter();
        register.setCountLinesRequired(false);
        when(registerRepository.findByRegisterCode("CSR-1")).thenReturn(Optional.of(register));
        when(accessGuard.assertCanOpen(eq(register), eq("caissier@elleaose.com"), isNull(), isNull(), isNull(), eq(false)))
                .thenReturn(new CashRegisterAccessGuard.OpeningClearance(null, null, null));
        when(sessionRepository.findBlockingSessionByCashRegisterId(anyLong())).thenReturn(Optional.empty());
        when(sessionRepository.findBlockingSessionByOpenedBy("caissier@elleaose.com")).thenReturn(Optional.empty());
        when(sequenceGenerator.next(anyString())).thenAnswer(invocation -> invocation.getArgument(0) + "-1");
        when(sessionRepository.save(any(CashSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(movementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.openSession(new OpenCashSessionRequest("CSR-1", null, new BigDecimal("12000")));

        ArgumentCaptor<CashSession> session = ArgumentCaptor.forClass(CashSession.class);
        verify(sessionRepository).save(session.capture());
        assertThat(session.getValue().getStatus()).isEqualTo(CashSessionStatus.OPENING);
        assertThat(session.getValue().getOpeningChecksWaivedBy()).isNull();
        verify(lifecycle, never()).recordAdminOpeningVariance(any(), any());
    }

    private static CashRegister defaultRegister() {
        return CashRegister.builder().id(7L).registerCode("CSR-DEFAULT").name("Caisse par défaut").active(true)
                .status(CashRegisterStatus.ACTIVE).registerType(CashRegisterType.MAIN).systemManaged(false).defaultRegister(true)
                .monthEndPolicy(CashMonthEndPolicy.CARRY_OVER).build();
    }

    private static CashRegister counter() {
        return CashRegister.builder().id(1L).registerCode("CSR-1").name("Accueil").active(true)
                .status(CashRegisterStatus.ACTIVE).registerType(CashRegisterType.COUNTER).systemManaged(false)
                .countLinesRequired(true).lastCountAt(Instant.parse("2026-11-20T18:00:00Z"))
                .lastCountAmount(new BigDecimal("10000")).build();
    }
}
