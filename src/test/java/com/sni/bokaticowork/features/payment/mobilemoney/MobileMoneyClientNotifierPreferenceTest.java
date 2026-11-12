package com.sni.bokaticowork.features.payment.mobilemoney;

import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.core.settings.userSettings.service.MemberPreferences;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyClientNotifier;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** L'avis de paiement echoue suit la preference du membre ; le reste part toujours. */
class MobileMoneyClientNotifierPreferenceTest {

    private final OutboxService outboxService = mock(OutboxService.class);
    private final TransactionContextResolver contextResolver = mock(TransactionContextResolver.class);
    private final MemberPreferences preferences = mock(MemberPreferences.class);
    private final MobileMoneyClientNotifier notifier = new MobileMoneyClientNotifier(outboxService, contextResolver);

    private PawapayDeposit deposit(String type, String code) {
        return PawapayDeposit.builder()
                .depositId("dep-1")
                .customerType(type)
                .customerCode(code)
                .amount(new BigDecimal("5000"))
                .currency("XAF")
                .build();
    }

    @BeforeEach
    void setUp() {
        notifier.setMemberPreferences(preferences);
        when(contextResolver.resolveParty(anyString(), anyString())).thenAnswer(call ->
                new TransactionContextResolver.PartyView(call.getArgument(0), call.getArgument(1), "Awa",
                        "awa@example.com", null, null, true));
    }

    @Test
    @DisplayName("Le membre a coupe l'avis de paiement echoue : rien ne part")
    void failureCutByMember() {
        when(preferences.allows("MBR-1", EventType.PAYMENT_FAILED, NotificationChannel.EMAIL)).thenReturn(false);

        notifier.depositFailed(deposit("MEMBER", "MBR-1"));

        verify(outboxService, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Par defaut, l'avis de paiement echoue part")
    void failureSentByDefault() {
        when(preferences.allows("MBR-1", EventType.PAYMENT_FAILED, NotificationChannel.EMAIL)).thenReturn(true);

        notifier.depositFailed(deposit("MEMBER", "MBR-1"));

        verify(outboxService).publish(eq("MOBILE_MONEY_DEPOSIT_FAILED"), eq("PAYMENT"), eq("dep-1"), any());
    }

    @Test
    @DisplayName("Le depot en verification part toujours : il dit de ne pas payer deux fois")
    void pendingReviewIsNeverCut() {
        when(preferences.allows(anyString(), any(), any())).thenReturn(false);

        notifier.depositPendingReview(deposit("MEMBER", "MBR-1"));

        verify(outboxService).publish(eq("MOBILE_MONEY_DEPOSIT_PENDING_REVIEW"), eq("PAYMENT"), eq("dep-1"), any());
    }

    @Test
    @DisplayName("Une organisation recoit toujours l'avis d'echec")
    void businessIsNeverCut() {
        when(preferences.allows(anyString(), any(), any())).thenReturn(false);
        when(preferences.allowsEmail(anyString(), any())).thenReturn(false);

        notifier.depositFailed(deposit("BUSINESS_ENTITY", "BIZ-1"));

        verify(outboxService).publish(eq("MOBILE_MONEY_DEPOSIT_FAILED"), eq("PAYMENT"), eq("dep-1"), any());
    }

    @Test
    @DisplayName("Un client personne suit le membre de meme adresse")
    void customerFollowsMemberByEmail() {
        when(preferences.allowsEmail("awa@example.com", EventType.PAYMENT_FAILED)).thenReturn(false);

        notifier.depositFailed(deposit("CUSTOMER", "CUS-1"));

        verify(outboxService, never()).publish(anyString(), anyString(), anyString(), any());
    }
}
