package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionStatusManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La fin d'un abonnement s'annonce avant d'arriver, puis elle arrive.
 *
 * <p>J-7, J-3, le jour meme · et le lendemain l'abonnement est clos. Le balayage passe toutes les
 * heures : chaque avis ne doit partir qu'une fois, et un abonnement vu tardivement ne doit pas
 * recevoir la serie entiere d'un coup.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionEndNoticeServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionStatusManager statusManager;
    @Mock private SubscriptionEventWriter eventWriter;
    @Mock private OutboxService outboxService;

    private SubscriptionEndNoticeService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionEndNoticeService(subscriptionRepository, statusManager, eventWriter,
                outboxService, null);
        ReflectionTestUtils.setField(service, "self", service);
    }

    private Subscription ending(LocalDate periodEnd, Integer noticeStage, boolean autoRenew) {
        Member member = new Member();
        member.setEmail("joel@example.com");
        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setSubscriptionNumber("SUB-MEM-COW-MON-202609-00000015");
        subscription.setCurrentPeriodEnd(periodEnd);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setAutoRenew(autoRenew);
        subscription.setEndNoticeStage(noticeStage);
        subscription.setMember(member);
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.findEndingSubscriptions(any())).thenReturn(List.of(subscription));
        return subscription;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> published(String eventType) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).publish(eq(eventType), eq("SUBSCRIPTION"), anyString(), captor.capture());
        return (Map<String, Object>) captor.getValue();
    }

    // -----------------------------------------------------------------------------------------
    // Le calendrier
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("J-7 · le client est prévenu une semaine avant")
    void sevenDaysBefore() {
        Subscription subscription = ending(LocalDate.now().plusDays(7), null, false);

        assertThat(service.sweep().noticed()).isEqualTo(1);

        Map<String, Object> payload = published("SUBSCRIPTION_ENDING_SOON");
        assertThat(payload).containsEntry("daysLeft", "7")
                .containsEntry("stage", "7")
                .containsEntry("manualRenewal", "true")
                .containsEntry("recipientEmail", "joel@example.com")
                .containsEntry("templateCode", "subscription_ending_soon");
        assertThat(subscription.getEndNoticeStage()).isEqualTo(7);
    }

    @Test
    @DisplayName("J-3 · le rappel")
    void threeDaysBefore() {
        Subscription subscription = ending(LocalDate.now().plusDays(3), 7, false);

        assertThat(service.sweep().noticed()).isEqualTo(1);

        assertThat(published("SUBSCRIPTION_ENDING_SOON")).containsEntry("daysLeft", "3").containsEntry("stage", "3");
        assertThat(subscription.getEndNoticeStage()).isEqualTo(3);
    }

    @Test
    @DisplayName("Le jour même · il prend fin à minuit")
    void onTheLastDay() {
        Subscription subscription = ending(LocalDate.now(), 3, false);

        assertThat(service.sweep().noticed()).isEqualTo(1);

        assertThat(published("SUBSCRIPTION_ENDS_TODAY")).containsEntry("daysLeft", "0")
                .containsEntry("endsTonight", "true");
        assertThat(subscription.getEndNoticeStage()).isZero();
        verify(statusManager, never()).changeStatus(any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Le lendemain · l'abonnement est annulé")
    void theDayAfter() {
        Subscription subscription = ending(LocalDate.now().minusDays(1), 0, false);

        assertThat(service.sweep().cancelled()).isEqualTo(1);

        verify(statusManager).changeStatus(eq(subscription), eq(SubscriptionStatus.CANCELLED), anyString(), eq("SYSTEM"));
        assertThat(subscription.getCancelledAt()).isNotNull();
        assertThat(subscription.getCancellationReason()).contains("arrivé à son terme");
        assertThat(published("SUBSCRIPTION_ENDED")).containsEntry("templateCode", "subscription_ended");
    }

    // -----------------------------------------------------------------------------------------
    // Ce qui ne doit pas arriver
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("Le balayage passe toutes les heures · un avis ne part qu'une fois")
    void aNoticeIsSentOnlyOnce() {
        ending(LocalDate.now().plusDays(7), 7, false);

        assertThat(service.sweep().noticed()).isZero();
        verify(outboxService, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Un abonnement vu à trois jours de sa fin reçoit le rappel, pas la série entière")
    void aLateSubscriptionGetsOnlyTheUrgentNotice() {
        // Jamais prevenu, et deja a trois jours · c'est l'avis de J-3 qui part, pas celui de J-7.
        ending(LocalDate.now().plusDays(2), null, false);

        assertThat(service.sweep().noticed()).isEqualTo(1);
        assertThat(published("SUBSCRIPTION_ENDING_SOON")).containsEntry("stage", "3");
    }

    @Test
    @DisplayName("Une fin encore lointaine ne déclenche rien")
    void nothingIsSaidTooEarly() {
        ending(LocalDate.now().plusDays(20), null, false);

        assertThat(service.sweep().noticed()).isZero();
        verify(outboxService, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Un abonnement déjà clos ne se referme pas")
    void anAlreadyClosedSubscriptionIsLeftAlone() {
        Subscription subscription = ending(LocalDate.now().minusDays(5), 0, false);
        subscription.setStatus(SubscriptionStatus.CANCELLED);

        assertThat(service.sweep().cancelled()).isZero();
        verify(statusManager, never()).changeStatus(any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Sans adresse joignable, l'avis ne part pas · la clôture suit son cours")
    void aSubscriberWithoutAnEmailIsStillClosed() {
        Subscription subscription = ending(LocalDate.now().minusDays(1), 0, false);
        subscription.setMember(null);

        assertThat(service.sweep().cancelled()).isEqualTo(1);
        verify(statusManager).changeStatus(eq(subscription), eq(SubscriptionStatus.CANCELLED), anyString(), eq("SYSTEM"));
        verify(outboxService, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Un abonnement en erreur ne retient pas les autres")
    void oneFailureNeverStopsTheSweep() {
        Subscription broken = ending(LocalDate.now().minusDays(1), 0, false);
        Subscription healthy = new Subscription();
        healthy.setId(2L);
        healthy.setSubscriptionNumber("SUB-SAIN");
        healthy.setCurrentPeriodEnd(LocalDate.now().minusDays(1));
        healthy.setStatus(SubscriptionStatus.ACTIVE);
        healthy.setAutoRenew(false);
        when(subscriptionRepository.findById(2L)).thenReturn(Optional.of(healthy));
        when(subscriptionRepository.findEndingSubscriptions(any())).thenReturn(List.of(broken, healthy));
        org.mockito.Mockito.doThrow(new IllegalStateException("plan introuvable"))
                .when(statusManager).changeStatus(eq(broken), any(), anyString(), anyString());

        assertThat(service.sweep().cancelled()).isEqualTo(1);
        verify(statusManager).changeStatus(eq(healthy), eq(SubscriptionStatus.CANCELLED), anyString(), eq("SYSTEM"));
    }

    // -----------------------------------------------------------------------------------------
    // La règle des jalons
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("Le jalon retenu est toujours le plus urgent qui s'applique")
    void theStageIsAlwaysTheMostUrgentOne() {
        assertThat(SubscriptionEndNoticeService.stageFor(10)).isNull();
        assertThat(SubscriptionEndNoticeService.stageFor(7)).isEqualTo(7);
        assertThat(SubscriptionEndNoticeService.stageFor(4)).isEqualTo(7);
        assertThat(SubscriptionEndNoticeService.stageFor(3)).isEqualTo(3);
        assertThat(SubscriptionEndNoticeService.stageFor(1)).isEqualTo(3);
        assertThat(SubscriptionEndNoticeService.stageFor(0)).isZero();
    }
}
