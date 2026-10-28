package com.sni.bokaticowork.features.payment.mobilemoney;

import com.sni.bokaticowork.core.async.ScheduledJobRuns;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.core.utils.phone.PhoneNumbers;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyAlertRecipients;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyDailyReconciliation;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyOperationsService;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneySupervisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le rapprochement du matin · regle ce qu'une relecture suffit a regler, signale le reste.
 */
class MobileMoneyDailyReconciliationTest {

    private final MobileMoneyOperationsService operations = mock(MobileMoneyOperationsService.class);
    private final MobileMoneySupervisionService supervision = mock(MobileMoneySupervisionService.class);
    private final MobileMoneyAlertRecipients recipients = mock(MobileMoneyAlertRecipients.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final ScheduledJobRuns jobRuns = mock(ScheduledJobRuns.class);
    private final MobileMoneyDailyReconciliation reconciliation =
            new MobileMoneyDailyReconciliation(operations, supervision, recipients, outbox, jobRuns);

    @BeforeEach
    void setUp() {
        when(recipients.recipients()).thenReturn(List.of("tresorerie@elleaose.com"));
        when(jobRuns.claim(anyString(), anyString())).thenReturn(true);
    }

    private static MobileMoneyDepositResponse deposit(String depositId, String status) {
        MobileMoneyDepositResponse deposit = mock(MobileMoneyDepositResponse.class);
        when(deposit.depositId()).thenReturn(depositId);
        when(deposit.status()).thenReturn(status);
        when(deposit.amount()).thenReturn(new BigDecimal("5000"));
        when(deposit.currency()).thenReturn("XAF");
        when(deposit.payer()).thenReturn(new MobileMoneyDepositResponse.PayerResponse("MMO",
                new MobileMoneyDepositResponse.AccountDetailsResponse("242061234567", "MTN_MOMO_COG")));
        return deposit;
    }

    private static MobileMoneyOperationsService.ReconciliationLine line(String depositId, String severity, String action) {
        return new MobileMoneyOperationsService.ReconciliationLine(deposit(depositId, "PROCESSING"), null,
                List.of(new MobileMoneyOperationsService.Discrepancy(severity, "CODE", "ecart", action)));
    }

    @Test
    @DisplayName("Un rappel perdu est regle par relecture, un ecart critique part dans le rapport")
    void settlesWhatARecheckSettlesAndReportsTheRest() {
        var lost = line("dep-lost", "WARNING", "SYNC");
        var amount = line("dep-amount", "CRITICAL", "Verifier le montant");
        when(operations.reconcile(any(), any())).thenReturn(new MobileMoneyOperationsService.Reconciliation(
                Instant.EPOCH, Instant.EPOCH, 12, false, List.of(lost, amount)));
        MobileMoneyDepositResponse settled = deposit("dep-lost", "SUCCEEDED");
        when(supervision.recheck("dep-lost")).thenReturn(settled);

        var report = invoke();

        assertThat(report.autoSettled()).containsExactly("dep-lost");
        assertThat(report.remaining()).extracting(l -> l.deposit().depositId()).containsExactly("dep-amount");
        verify(supervision, never()).recheck("dep-amount");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(outbox).publish(eq("MOBILE_MONEY_DAILY_RECONCILIATION"), eq("PAYMENT"), anyString(), payload.capture());
        assertThat(payload.getValue()).containsEntry("remainingCount", 1).containsEntry("autoSettledCount", 1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) payload.getValue().get("lines");
        // Le rapport se transfere · le numero du payeur n'y figure pas en entier.
        assertThat(lines.get(0).get("phoneNumber")).isEqualTo("+24206*****67");
    }

    @Test
    @DisplayName("Un jour sans ecart ne produit aucun courriel")
    void staysQuietWhenEverythingMatches() {
        when(operations.reconcile(any(), any())).thenReturn(new MobileMoneyOperationsService.Reconciliation(
                Instant.EPOCH, Instant.EPOCH, 30, false, List.of()));

        invoke();

        verify(outbox, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Le second dyno ne refait pas le passage du jour")
    void secondInstanceSkips() {
        when(jobRuns.claim(anyString(), anyString())).thenReturn(false);

        reconciliation.runDaily();

        verify(operations, never()).reconcile(any(), any());
    }

    @Test
    @DisplayName("Le masque garde l'indicatif et les deux derniers chiffres")
    void masksPhoneNumbers() {
        assertThat(PhoneNumbers.mask("242061234567")).isEqualTo("+24206*****67");
        assertThat(PhoneNumbers.mask("+242 06 123 45 67")).isEqualTo("+24206*****67");
        assertThat(PhoneNumbers.mask("061234567")).isEqualTo("06123**67");
        assertThat(PhoneNumbers.mask(null)).isNull();
        assertThat(PhoneNumbers.mask("123")).isEqualTo("***");
    }

    private MobileMoneyDailyReconciliation.Report invoke() {
        return org.springframework.test.util.ReflectionTestUtils.invokeMethod(reconciliation, "reconcile", Instant.now());
    }
}
