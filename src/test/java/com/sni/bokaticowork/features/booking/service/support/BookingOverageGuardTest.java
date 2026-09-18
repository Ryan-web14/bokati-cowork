package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.subscription.overage.enums.OveragePolicyMode;
import com.sni.bokaticowork.features.subscription.overage.model.SubscriptionOveragePolicy;
import com.sni.bokaticowork.features.subscription.overage.repository.SubscriptionOveragePolicyRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un droit épuisé n'est pas un droit absent.
 *
 * <p>La différence décide entre deux réponses très différentes. « Votre abonnement ne couvre pas
 * cette salle » est un refus définitif. « Vous avez épuisé vos cinq heures » ouvre le dépassement,
 * si le plan l'autorise : la réservation passe et les heures supplémentaires sont facturées.</p>
 *
 * <p>Le module de dépassement existait déjà, avec ses politiques, ses tarifs et sa facturation. Il
 * n'était jamais atteint depuis une réservation, parce que la recherche de droit exigeait un solde
 * positif et refusait avant.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingOverageGuardTest {

    @Mock
    private SubscriptionOveragePolicyRepository overagePolicyRepository;

    @InjectMocks
    private BookingOverageGuard guard;

    @Test
    void allowsOverageWhenThePlanBillsIt() {
        givenPolicy(OveragePolicyMode.BILLABLE);

        assertTrue(guard.allows(planVersion(), "ENT-SALLE"));
    }

    @Test
    void allowsOverageWhenThePlanAbsorbsIt() {
        givenPolicy(OveragePolicyMode.ALLOW_UNBILLED);

        assertTrue(guard.allows(planVersion(), "ENT-SALLE"));
    }

    @Test
    void refusesOverageWhenThePlanBlocksIt() {
        givenPolicy(OveragePolicyMode.BLOCK);

        assertFalse(guard.allows(planVersion(), "ENT-SALLE"));
    }

    /**
     * Sans politique, rien ne dépasse. Laisser consommer sans avoir décidé ce que cela coûte
     * revient à offrir la prestation.
     */
    @Test
    void refusesOverageWhenNoPolicyWasEverDecided() {
        when(overagePolicyRepository.findActivePolicy(anyLong(), anyString())).thenReturn(Optional.empty());

        assertFalse(guard.allows(planVersion(), "ENT-SALLE"));
    }

    @Test
    void doesNotEvenAskWithoutAPlanOrAnEntitlement() {
        assertFalse(guard.allows(null, "ENT-SALLE"));
        assertFalse(guard.allows(planVersion(), null));
        assertFalse(guard.allows(planVersion(), "   "));

        verify(overagePolicyRepository, never()).findActivePolicy(anyLong(), anyString());
    }

    // -------------------------------------------------------------------------------------

    private void givenPolicy(OveragePolicyMode mode) {
        SubscriptionOveragePolicy policy = new SubscriptionOveragePolicy();
        policy.setMode(mode);
        when(overagePolicyRepository.findActivePolicy(anyLong(), anyString())).thenReturn(Optional.of(policy));
    }

    private PlanVersion planVersion() {
        PlanVersion version = new PlanVersion();
        version.setId(1L);
        return version;
    }
}
