package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un abonnement en erreur ne doit pas arreter la facturation de tous les autres.
 *
 * <p>Le balayage s'ecrivait {@code subscriptions.forEach(this::renewActive)} dans une seule
 * transaction. Un abonnement qui jetait · un montant absent, un plan devenu introuvable · annulait
 * la passe entiere, y compris les renouvellements deja ecrits. Le worker consignait l'echec et
 * recommencait un quart d'heure plus tard sur la meme liste, dans le meme ordre, avec le meme
 * abonnement en tete. Plus rien n'etait facture, pour personne, et cela pouvait durer des mois
 * sans que le symptome ne designe le coupable.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionRenewalIsolationTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionLifecycleOperator self;
    @InjectMocks private SubscriptionLifecycleOperator operator;

    @BeforeEach
    void setUp() {
        // Le balayage delegue chaque abonnement a lui-meme pour lui donner sa transaction · on
        // substitue ce relais, ce qui laisse observer la seule chose changee : la boucle.
        ReflectionTestUtils.setField(operator, "self", self);
    }

    private Subscription subscription(long id, String number) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setSubscriptionNumber(number);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setNextBillingDate(LocalDate.now().minusDays(1));
        return subscription;
    }

    private void due(Subscription... subscriptions) {
        when(subscriptionRepository.findAllByStatusAndNextBillingDateLessThanEqualAndAutoRenewTrue(
                anyString(), any())).thenReturn(List.of(subscriptions));
    }

    @Test
    @DisplayName("Le premier abonnement échoue · les suivants sont facturés quand même")
    void aFailingSubscriptionNeverBlocksTheOthers() {
        Subscription broken = subscription(1L, "SUB-CASSE");
        Subscription second = subscription(2L, "SUB-DEUX");
        Subscription third = subscription(3L, "SUB-TROIS");
        due(broken, second, third);
        doThrow(new NullPointerException("montant recurrent absent")).when(self).renewOne(1L);

        int renewed = operator.renewDueSubscriptions();

        assertThat(renewed).isEqualTo(2);
        verify(self).renewOne(2L);
        verify(self).renewOne(3L);
    }

    @Test
    @DisplayName("Le compte rendu ne compte que ce qui a réellement été facturé")
    void theCountReflectsWhatWasActuallyBilled() {
        due(subscription(1L, "SUB-UN"), subscription(2L, "SUB-DEUX"));
        doThrow(new IllegalStateException("plan introuvable")).when(self).renewOne(1L);
        doThrow(new IllegalStateException("plan introuvable")).when(self).renewOne(2L);

        // Auparavant la methode rendait subscriptions.size() · elle annoncait deux
        // renouvellements alors qu'aucun n'avait abouti.
        assertThat(operator.renewDueSubscriptions()).isZero();
    }

    @Test
    @DisplayName("Tout passe · chaque échéance est traitée une fois")
    void everyDueSubscriptionIsProcessedOnce() {
        due(subscription(1L, "SUB-UN"), subscription(2L, "SUB-DEUX"));

        assertThat(operator.renewDueSubscriptions()).isEqualTo(2);
        verify(self).renewOne(1L);
        verify(self).renewOne(2L);
    }

    @Test
    @DisplayName("Aucune échéance, aucun appel")
    void nothingDueNothingDone() {
        when(subscriptionRepository.findAllByStatusAndNextBillingDateLessThanEqualAndAutoRenewTrue(
                anyString(), any())).thenReturn(List.of());

        assertThat(operator.renewDueSubscriptions()).isZero();
    }
}
