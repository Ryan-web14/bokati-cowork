package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.features.payment.cash.dto.response.CashPaymentDeclarationResponse;
import com.sni.bokaticowork.features.payment.cash.enums.CashDeclarationStatus;
import com.sni.bokaticowork.features.payment.cash.model.CashPaymentDeclaration;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashRequestRequest;
import com.sni.bokaticowork.features.payment.dto.request.ReviewCashAnomalyRequest;
import com.sni.bokaticowork.features.payment.dto.request.ReviewCashRequestRequest;
import com.sni.bokaticowork.features.payment.enums.CashAnomalyStatus;
import com.sni.bokaticowork.features.payment.enums.CashRequestStatus;
import com.sni.bokaticowork.features.payment.enums.CashRequestType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.model.CashAnomalyFlag;
import com.sni.bokaticowork.features.payment.model.CashRequest;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashAnomalyFlagRepository;
import com.sni.bokaticowork.features.payment.repository.CashRequestRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Ecarts releves dans la doc des caisses, regles le 17/11/2026. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CashGapsTest {

    @Mock CashRequestRepository requestRepository;
    @Mock CashSessionRepository sessionRepository;
    @Mock CashActorResolver actors;
    @Mock com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier emailNotifier;
    @InjectMocks CashRequestServiceImpl requests;

    @Mock CashAnomalyFlagRepository flagRepository;
    @InjectMocks CashAnomalyDetectionServiceImpl anomalies;

    private CashSession session(CashSessionStatus status) {
        return CashSession.builder().sessionNumber("CSS-1").status(status)
                .cashRegister(com.sni.bokaticowork.features.payment.model.CashRegister.builder().registerCode("CSR-1").build()).build();
    }

    @Test
    void aRequestIsNotSubmittedOnASessionThatIsNotOpen() {
        when(sessionRepository.findBySessionNumber("CSS-1")).thenReturn(Optional.of(session(CashSessionStatus.CLOSED)));

        assertThatThrownBy(() -> requests.submit("CSS-1", new CreateCashRequestRequest(CashRequestType.CASH_ADVANCE,
                new BigDecimal("1000"), null, "avance", null, List.of())))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("n'est pas ouverte");
        verify(requestRepository, never()).save(any());
    }

    @Test
    void approvingARequestOfAClosedSessionCancelsIt() {
        CashRequest pending = CashRequest.builder().requestNumber("CRQ-1").status(CashRequestStatus.PENDING)
                .requestType(CashRequestType.CASH_ADVANCE).amount(new BigDecimal("1000")).currency("XAF")
                .requestedBy("caissier@bokati.cg").cashSession(session(CashSessionStatus.CLOSED)).build();
        when(requestRepository.findByRequestNumber("CRQ-1")).thenReturn(Optional.of(pending));
        when(actors.resolve(any())).thenReturn("chef@bokati.cg");

        requests.approve("CRQ-1", new ReviewCashRequestRequest(null, null));

        assertThat(pending.getStatus()).isEqualTo(CashRequestStatus.CANCELLED);
    }

    @Test
    void onlyTheRequesterOrAnApproverCancels() {
        CashRequest pending = CashRequest.builder().requestNumber("CRQ-2").status(CashRequestStatus.PENDING)
                .requestType(CashRequestType.CASH_ADVANCE).amount(new BigDecimal("1000")).currency("XAF")
                .requestedBy("caissier@bokati.cg").cashSession(session(CashSessionStatus.OPEN)).build();
        when(requestRepository.findByRequestNumber("CRQ-2")).thenReturn(Optional.of(pending));
        when(actors.resolve(any())).thenReturn("autre@bokati.cg");
        when(actors.hasAuthority("CASH_MOVEMENT:APPROVE")).thenReturn(false);

        assertThatThrownBy(() -> requests.cancel("CRQ-2", null)).isInstanceOf(ForbiddenException.class);

        when(actors.resolve(any())).thenReturn("caissier@bokati.cg");
        requests.cancel("CRQ-2", new ReviewCashRequestRequest(null, "plus besoin"));
        assertThat(pending.getStatus()).isEqualTo(CashRequestStatus.CANCELLED);
        assertThat(pending.getReviewNote()).isEqualTo("plus besoin");
    }

    @Test
    void anAnomalyReviewRejectsOpenAsATarget() {
        CashAnomalyFlag flag = CashAnomalyFlag.builder().flagNumber("CAF-1").status(CashAnomalyStatus.ACKNOWLEDGED).build();
        when(flagRepository.findByFlagNumber("CAF-1")).thenReturn(Optional.of(flag));

        assertThatThrownBy(() -> anomalies.review("CAF-1", new ReviewCashAnomalyRequest(null, CashAnomalyStatus.OPEN, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Statut cible invalide");
        assertThatThrownBy(() -> anomalies.review("CAF-1", new ReviewCashAnomalyRequest(null, CashAnomalyStatus.ACKNOWLEDGED, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void theConfirmationNoteIsNotACloseReason() {
        CashPaymentDeclaration confirmed = CashPaymentDeclaration.builder().declarationNumber("CPD-1")
                .status(CashDeclarationStatus.CONFIRMED).closeReason("billets comptes deux fois").build();
        CashPaymentDeclaration cancelled = CashPaymentDeclaration.builder().declarationNumber("CPD-2")
                .status(CashDeclarationStatus.CANCELLED).closeReason("client absent").build();

        CashPaymentDeclarationResponse ok = CashPaymentDeclarationResponse.of(confirmed);
        CashPaymentDeclarationResponse ko = CashPaymentDeclarationResponse.of(cancelled);

        assertThat(ok.confirmationNote()).isEqualTo("billets comptes deux fois");
        assertThat(ok.closeReason()).isNull();
        assertThat(ko.closeReason()).isEqualTo("client absent");
        assertThat(ko.confirmationNote()).isNull();
    }
}
