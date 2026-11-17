package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.CashAnomalyFlagRepository;
import com.sni.bokaticowork.features.payment.repository.CashCountRepository;
import com.sni.bokaticowork.features.payment.repository.CashHandoverRepository;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.repository.CashVarianceReviewRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Detail des encaissements, chronologie et indicateurs d'une session ; cloture a l'aveugle respectee. */
class CashSessionInsightServiceTest {

    private final CashSessionRepository sessions = mock(CashSessionRepository.class);
    private final PaymentTransactionRepository transactions = mock(PaymentTransactionRepository.class);
    private final CashMovementRepository movements = mock(CashMovementRepository.class);
    private final CashSessionSummarySupport summary = mock(CashSessionSummarySupport.class);
    private final CashActorResolver actors = mock(CashActorResolver.class);
    private final CashSessionInsightService service = new CashSessionInsightService(sessions, transactions, movements,
            mock(CashCountRepository.class), mock(CashVarianceReviewRepository.class), mock(CashAnomalyFlagRepository.class),
            mock(CashHandoverRepository.class), summary, mock(TransactionContextResolver.class), actors);

    private CashSession session;

    private PaymentTransaction txn(String number, PaymentMethod method, PaymentTransactionStatus status, String amount, Instant at) {
        PaymentTransaction t = new PaymentTransaction();
        t.setTransactionNumber(number);
        t.setPaymentMethod(method);
        t.setStatus(status);
        t.setAmount(new BigDecimal(amount));
        t.setCreatedAt(at);
        t.setReceivedBy("caissier@x.com");
        return t;
    }

    @BeforeEach
    void setUp() {
        CashRegister register = new CashRegister();
        register.setBlindClose(true);
        register.setMaxCashAmount(new BigDecimal("1000000"));
        session = CashSession.builder().id(9L).sessionNumber("CS-1").cashRegister(register).status(CashSessionStatus.OPEN)
                .openedBy("caissier@x.com").openedAt(Instant.parse("2026-11-17T07:00:00Z")).openingAmount(new BigDecimal("10000")).build();
        when(sessions.findBySessionNumber("CS-1")).thenReturn(Optional.of(session));
        when(transactions.findAllByCashSessionNumberOrderByCreatedAtAsc("CS-1")).thenReturn(List.of(
                txn("T1", PaymentMethod.CASH, PaymentTransactionStatus.SUCCEEDED, "5000", Instant.parse("2026-11-17T08:10:00Z")),
                txn("T2", PaymentMethod.CASH, PaymentTransactionStatus.SUCCEEDED, "3000", Instant.parse("2026-11-17T08:40:00Z")),
                txn("T3", PaymentMethod.CHEQUE, PaymentTransactionStatus.SUCCEEDED, "20000", Instant.parse("2026-11-17T10:05:00Z")),
                txn("R1", PaymentMethod.CASH, PaymentTransactionStatus.REFUNDED, "1000", Instant.parse("2026-11-17T11:00:00Z"))));
        when(movements.findByCashSession_IdOrderByCreatedAtAsc(anyLong())).thenReturn(List.of());
        when(summary.expectedClosingAmount(any())).thenReturn(new BigDecimal("17000"));
    }

    @Test
    void paymentsAreTotalledByMethodAndLocalHour() {
        var payments = service.payments("CS-1", null, null);

        assertThat(payments.paymentCount()).isEqualTo(3);
        assertThat(payments.paymentTotal()).isEqualByComparingTo("28000");
        assertThat(payments.refundCount()).isEqualTo(1);
        assertThat(payments.byMethod()).extracting(CashSessionInsightService.MethodTotal::paymentMethod).containsExactly("CASH", "CHEQUE");
        // 08:10Z = 09 h a Brazzaville
        assertThat(payments.byHour()).extracting(CashSessionInsightService.HourTotal::hour).containsExactly(9, 11);
    }

    @Test
    void theExpectedBalanceStaysHiddenToACashierUnderBlindClose() {
        when(actors.hasAuthority(anyString())).thenReturn(false);
        var indicators = service.indicators("CS-1");
        assertThat(indicators.expectedHidden()).isTrue();
        assertThat(indicators.expectedBalance()).isNull();
        assertThat(indicators.averageTicket()).isEqualByComparingTo("9333.33");

        when(actors.hasAuthority("CASH_VARIANCE:DECIDE")).thenReturn(true);
        assertThat(service.indicators("CS-1").expectedBalance()).isEqualByComparingTo("17000");
    }

    @Test
    void theTimelineStartsWithTheOpening() {
        assertThat(service.timeline("CS-1")).first().extracting(CashSessionInsightService.TimelineEvent::kind).isEqualTo("OPENED");
    }
}
