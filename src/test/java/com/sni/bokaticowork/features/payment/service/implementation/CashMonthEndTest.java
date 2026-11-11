package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.support.CashClosingReportMailer;
import com.sni.bokaticowork.features.payment.service.support.CashSessionFinalizedEvent;
import com.sni.bokaticowork.features.payment.service.support.CashSessionSummarySupport;
import com.sni.bokaticowork.security.admin.role.repository.RoleUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fin de mois · la session laissee ouverte est close par le systeme et remplacee, rapport a la cle.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CashMonthEndTest {

    @Mock private CashRegisterRepository registerRepository;
    @Mock private CashSessionRepository sessionRepository;
    @Mock private CashMovementRepository movementRepository;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private CashSessionSummarySupport summarySupport;
    @Mock private CashRegisterMapper mapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    /** Hors contexte authentifie, l'acteur est la valeur fournie : les tests gardent leurs acteurs. */
    @org.mockito.Spy
    private com.sni.bokaticowork.features.payment.service.support.CashActorResolver actors =
            new com.sni.bokaticowork.features.payment.service.support.CashActorResolver();

    @InjectMocks private CashRegisterServiceImpl service;

    private CashSession open(CashRegister register) {
        CashSession session = CashSession.builder().id(1L).sessionNumber("CSS-OCT").cashRegister(register)
                .status(CashSessionStatus.OPEN).openedBy("caissier-1").openingAmount(new BigDecimal("10000")).build();
        when(sessionRepository.findBySessionNumber("CSS-OCT")).thenReturn(Optional.of(session));
        when(summarySupport.expectedClosingAmount(session)).thenReturn(new BigDecimal("85000"));
        when(sequenceGenerator.next("cash_session")).thenReturn("CSS-NOV");
        when(sequenceGenerator.next("cash_movement")).thenReturn("CSM-1");
        when(sessionRepository.save(any(CashSession.class))).thenAnswer(call -> call.getArgument(0));
        when(sessionRepository.saveAndFlush(any(CashSession.class))).thenAnswer(call -> call.getArgument(0));
        when(movementRepository.save(any(CashMovement.class))).thenAnswer(call -> call.getArgument(0));
        return session;
    }

    @Test
    @DisplayName("Caisse tenue a la main : close sans comptage, remplacee pour le meme caissier, montant reporte")
    void manualRegisterIsClosedAndRenewed() {
        CashRegister register = CashRegister.builder().id(5L).registerCode("CSR-1").name("Accueil").systemManaged(false).build();
        CashSession session = open(register);

        service.closeForMonthEnd("CSS-OCT");

        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.CLOSED);
        assertThat(session.getClosedBy()).isEqualTo("SYSTEM");
        assertThat(session.getExpectedClosingAmount()).isEqualByComparingTo("85000");
        assertThat(session.getCountedClosingAmount()).isNull();
        assertThat(session.getVarianceAmount()).isNull();

        ArgumentCaptor<CashSession> saved = ArgumentCaptor.forClass(CashSession.class);
        verify(sessionRepository).save(saved.capture());
        CashSession next = saved.getValue();
        assertThat(next.getSessionNumber()).isEqualTo("CSS-NOV");
        assertThat(next.getOpenedBy()).isEqualTo("caissier-1");
        assertThat(next.getOpeningAmount()).isEqualByComparingTo("85000");
        ArgumentCaptor<CashMovement> movement = ArgumentCaptor.forClass(CashMovement.class);
        verify(movementRepository).save(movement.capture());
        assertThat(movement.getValue().getMovementType()).isEqualTo(CashMovementType.OPENING_FLOAT);
        assertThat(movement.getValue().getReason()).contains("CSS-OCT");

        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(events.capture());
        assertThat(events.getAllValues()).anySatisfy(event -> {
            assertThat(event).isInstanceOf(CashSessionFinalizedEvent.class);
            CashSessionFinalizedEvent finalized = (CashSessionFinalizedEvent) event;
            assertThat(finalized.bySystem()).isTrue();
            assertThat(finalized.renewedAs()).isEqualTo("CSS-NOV");
        });
    }

    @Test
    @DisplayName("Rapport : administration, gerance et responsable de la caisse, sans doublon")
    void reportRecipients() {
        RoleUserRepository roles = mock(RoleUserRepository.class);
        when(roles.findEnabledHolderEmails("SUPER_ADMIN")).thenReturn(List.of("Boss@sni-cg.com"));
        when(roles.findEnabledHolderEmails("ADMIN")).thenReturn(List.of("support@sni-cg.com", "boss@sni-cg.com"));
        when(roles.findEnabledHolderEmails("MANAGER")).thenReturn(List.of("gerance@elleaose.com"));
        CashClosingReportMailer mailer = new CashClosingReportMailer(null, null, roles, null, null, true,
                "superviseur@elleaose.com", "https://admin.elleaose.com");

        CashRegister withManager = CashRegister.builder().managerEmail("caisse@elleaose.com").build();
        assertThat(mailer.recipients(withManager))
                .containsExactly("boss@sni-cg.com", "support@sni-cg.com", "gerance@elleaose.com", "caisse@elleaose.com");
        assertThat(mailer.recipients(CashRegister.builder().build())).contains("superviseur@elleaose.com");
    }

    @Test
    @DisplayName("Message du rapport : par le systeme, especes non comptees, session suivante nommee")
    void reportMessage() {
        CashSession session = CashSession.builder().sessionNumber("CSS-OCT").openedBy("caissier-1").closedBy("SYSTEM")
                .cashRegister(CashRegister.builder().name("Accueil").build())
                .expectedClosingAmount(new BigDecimal("85000")).build();

        String message = CashClosingReportMailer.message(session,
                new CashSessionFinalizedEvent("CSS-OCT", true, "fin de mois, session laissée ouverte", "CSS-NOV"));

        assertThat(message).contains("par le système").contains("non comptées").contains("CSS-NOV").contains("85000");
    }
}
