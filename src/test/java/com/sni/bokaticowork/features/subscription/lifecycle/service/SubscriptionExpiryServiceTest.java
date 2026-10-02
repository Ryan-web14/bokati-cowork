package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Une periode echue que rien n'a fait avancer finit par fermer l'abonnement.
 *
 * <p>Constate en production : un abonnement dont la periode s'achevait le 7 septembre etait encore
 * {@code ACTIVE} le 1er octobre, droits ouverts, sans aucune facture emise. Trois chemins menaient
 * la, aucun ne se refermait · le renouvellement ne prend que la reconduction automatique, la
 * cloture de fin de periode que ceux qui l'avaient demandee, et la tolerance que ceux qui portent
 * une facture de renouvellement impayee.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionExpiryServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionPolicyService policyService;
    @Mock private SubscriptionStatusManager statusManager;
    @Mock private SubscriptionEventWriter eventWriter;
    @Mock private SubscriptionGraceService graceService;

    private SubscriptionExpiryService service;

    @BeforeEach
    void setUp() {
        SubscriptionPolicy policy = new SubscriptionPolicy();
        policy.setGracePeriodDays(7);
        policy.setSuspensionAfterGraceDays(14);
        when(policyService.current()).thenReturn(policy);
        when(graceService.enter(any(), anyString())).thenReturn(true);

        // Le service s'appelle lui-meme pour donner sa transaction a chaque abonnement · en test
        // on lui passe l'instance reelle, le comportement observe reste le sien.
        service = new SubscriptionExpiryService(subscriptionRepository, policyService, statusManager,
                eventWriter, graceService, null);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "self", service);
    }

    private Subscription subscription(long id, String number, LocalDate periodEnd, SubscriptionStatus status) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setSubscriptionNumber(number);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscription.setStatus(status);
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));
        return subscription;
    }

    private void ended(Subscription... subscriptions) {
        when(subscriptionRepository.findEndedPeriodsStillOpen(any())).thenReturn(List.of(subscriptions));
    }

    @Test
    @DisplayName("Le cas de production · période échue de 24 jours, l'abonnement se ferme")
    void theSubscriptionFromSeptemberSeventhIsClosed() {
        // 7 + 14 = 21 jours de latitude · 24 jours de retard les depassent.
        Subscription stuck = subscription(1L, "SUB-MEM-COW-MON-202609-00000015",
                LocalDate.now().minusDays(24), SubscriptionStatus.ACTIVE);
        ended(stuck);

        SubscriptionExpiryService.Sweep sweep = service.sweep();

        assertThat(sweep.expired()).isEqualTo(1);
        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(statusManager).changeStatus(eq(stuck), eq(SubscriptionStatus.EXPIRED), reason.capture(), eq("SYSTEM"));
        assertThat(reason.getValue()).contains("Période échue").contains("24 jours");
        verify(subscriptionRepository).save(stuck);
    }

    @Test
    @DisplayName("Avant de fermer, on prévient · la tolérance est l'état prévu pour ça")
    void theSubscriberIsWarnedBeforeTheSubscriptionCloses() {
        // 10 jours de retard · au-dela de la tolerance de 7, en deca des 21 de la fermeture.
        Subscription late = subscription(1L, "SUB-1", LocalDate.now().minusDays(10), SubscriptionStatus.ACTIVE);
        ended(late);

        SubscriptionExpiryService.Sweep sweep = service.sweep();

        assertThat(sweep.warned()).isEqualTo(1);
        assertThat(sweep.expired()).isZero();
        verify(graceService).enter(eq(late), anyString());
        verify(statusManager, never()).changeStatus(any(), eq(SubscriptionStatus.EXPIRED), anyString(), anyString());
    }

    @Test
    @DisplayName("Un retard dans la tolérance ne déclenche rien · le client a le temps de régler")
    void aFreshDelayIsLeftAlone() {
        ended(subscription(1L, "SUB-1", LocalDate.now().minusDays(3), SubscriptionStatus.ACTIVE));

        SubscriptionExpiryService.Sweep sweep = service.sweep();

        assertThat(sweep.warned()).isZero();
        assertThat(sweep.expired()).isZero();
        verify(graceService, never()).enter(any(), anyString());
    }

    @Test
    @DisplayName("Un abonnement déjà en tolérance n'y entre pas deux fois · il attend sa fermeture")
    void aSubscriptionAlreadyInGraceIsNotWarnedAgain() {
        ended(subscription(1L, "SUB-1", LocalDate.now().minusDays(10), SubscriptionStatus.GRACE_PERIOD));

        SubscriptionExpiryService.Sweep sweep = service.sweep();

        assertThat(sweep.warned()).isZero();
        verify(graceService, never()).enter(any(), anyString());
    }

    @Test
    @DisplayName("Une période relancée entre la lecture et l'écriture ne se ferme pas")
    void aPeriodRenewedMeanwhileIsLeftAlone() {
        Subscription renewed = subscription(1L, "SUB-1", LocalDate.now().minusDays(24), SubscriptionStatus.ACTIVE);
        ended(renewed);
        // Le renouvellement a passe entre-temps · la periode est repartie vers l'avant.
        renewed.setCurrentPeriodEnd(LocalDate.now().plusDays(20));

        SubscriptionExpiryService.Sweep sweep = service.sweep();

        assertThat(sweep.expired()).isZero();
        verify(statusManager, never()).changeStatus(any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Un abonnement déjà clos ne se referme pas")
    void anAlreadyClosedSubscriptionIsLeftAlone() {
        ended(subscription(1L, "SUB-1", LocalDate.now().minusDays(60), SubscriptionStatus.CANCELLED));

        assertThat(service.sweep().expired()).isZero();
        verify(statusManager, never()).changeStatus(any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Un abonnement en erreur ne retient pas les autres · c'est ce qui avait figé la facturation")
    void oneFailureNeverStopsTheSweep() {
        Subscription broken = subscription(1L, "SUB-CASSE", LocalDate.now().minusDays(30), SubscriptionStatus.ACTIVE);
        Subscription healthy = subscription(2L, "SUB-SAIN", LocalDate.now().minusDays(30), SubscriptionStatus.ACTIVE);
        ended(broken, healthy);
        org.mockito.Mockito.doThrow(new IllegalStateException("plan introuvable"))
                .when(statusManager).changeStatus(eq(broken), any(), anyString(), anyString());

        SubscriptionExpiryService.Sweep sweep = service.sweep();

        assertThat(sweep.expired()).isEqualTo(1);
        verify(statusManager).changeStatus(eq(healthy), eq(SubscriptionStatus.EXPIRED), anyString(), eq("SYSTEM"));
    }

    @Test
    @DisplayName("Aucune période échue, aucune écriture")
    void nothingEndedNothingDone() {
        when(subscriptionRepository.findEndedPeriodsStillOpen(any())).thenReturn(List.of());

        assertThat(service.sweep()).isEqualTo(new SubscriptionExpiryService.Sweep(0, 0));
        verify(subscriptionRepository, never()).findById(anyLong());
    }
}
