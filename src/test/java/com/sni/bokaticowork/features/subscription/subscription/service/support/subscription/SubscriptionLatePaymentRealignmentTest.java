package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPeriodCalculator;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * La duree payee doit etre la duree recue.
 *
 * <p>Le renouvellement avance la periode des qu'il facture : elle commence le lendemain de la
 * periode precedente, avant tout reglement. Un client qui payait dix jours plus tard se retrouvait
 * avec une periode commencee dix jours plus tot · il payait un mois et en recevait vingt jours.
 * Personne ne le lui avait annonce, et personne ne le voyait.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionLatePaymentRealignmentTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionEventWriter eventWriter;
    @InjectMocks private SubscriptionLifecycleOperator operator;

    @BeforeEach
    void setUp() {
        // Le calcul de periode est une regle, pas une dependance a simuler.
        ReflectionTestUtils.setField(operator, "periodCalculator", new SubscriptionPeriodCalculator());
    }

    private Subscription monthly(LocalDate periodStart, LocalDate periodEnd) {
        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setSubscriptionNumber("SUB-1");
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setCurrentPeriodStart(periodStart);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscription.setNextBillingDate(periodEnd.plusDays(1));
        subscription.setEndNoticeStage(3);
        return subscription;
    }

    @Test
    @DisplayName("Payé dix jours en retard · la période repart du jour du paiement")
    void aLatePaymentRestartsThePeriodOnThePaymentDay() {
        // Echeance au 1er septembre, periode du 1er au 30 · le client paie le 11.
        Subscription subscription = monthly(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        boolean moved = operator.realignOnLatePayment(subscription, LocalDate.of(2026, 9, 11));

        assertThat(moved).isTrue();
        assertThat(subscription.getCurrentPeriodStart()).isEqualTo(LocalDate.of(2026, 9, 11));
        // Un mois plein a partir du 11 · il recoit ce qu'il a paye.
        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 10, 11));
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    @DisplayName("Payé le jour de l'échéance · rien ne bouge")
    void anOnTimePaymentChangesNothing() {
        Subscription subscription = monthly(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        boolean moved = operator.realignOnLatePayment(subscription, LocalDate.of(2026, 9, 1));

        assertThat(moved).isFalse();
        assertThat(subscription.getCurrentPeriodStart()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(LocalDate.of(2026, 9, 30));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Payé avant l'échéance · rien ne bouge non plus")
    void anEarlyPaymentChangesNothing() {
        Subscription subscription = monthly(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(operator.realignOnLatePayment(subscription, LocalDate.of(2026, 8, 28))).isFalse();
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un retard d'un seul jour décale d'un seul jour")
    void oneDayLateShiftsByOneDay() {
        Subscription subscription = monthly(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        operator.realignOnLatePayment(subscription, LocalDate.of(2026, 9, 2));

        assertThat(subscription.getCurrentPeriodStart()).isEqualTo(LocalDate.of(2026, 9, 2));
        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    @DisplayName("La période repart · les avis de fin déjà envoyés ne la concernent plus")
    void theEndNoticesAreResetWithTheNewPeriod() {
        Subscription subscription = monthly(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        operator.realignOnLatePayment(subscription, LocalDate.of(2026, 9, 15));

        assertThat(subscription.getEndNoticeStage()).isNull();
    }

    @Test
    @DisplayName("Un cycle annuel se recale sur l'année, pas sur le mois")
    void theCycleDecidesTheNewLength() {
        Subscription subscription = monthly(LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31));
        subscription.setBillingCycle(BillingCycle.YEARLY);

        operator.realignOnLatePayment(subscription, LocalDate.of(2026, 9, 20));

        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(LocalDate.of(2027, 9, 19));
    }

    @Test
    @DisplayName("Sans période en cours, il n'y a rien à recaler")
    void nothingToRealignWithoutAPeriod() {
        Subscription subscription = monthly(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        subscription.setCurrentPeriodStart(null);

        assertThat(operator.realignOnLatePayment(subscription, LocalDate.of(2026, 9, 15))).isFalse();
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Une prestation ponctuelle ne se recale pas · elle n'a pas de période suivante")
    void aOneTimeCycleIsLeftAlone() {
        Subscription subscription = monthly(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1));
        subscription.setBillingCycle(BillingCycle.ONE_TIME);

        // periodEnd vaut le jour du paiement, nextBillingDate est nul · le recalage est inoffensif
        // mais la periode ne doit pas s'etendre.
        operator.realignOnLatePayment(subscription, LocalDate.of(2026, 9, 15));

        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(subscription.getNextBillingDate()).isNull();
    }
}
