package com.sni.bokaticowork.features.payment.mobilemoney;

import com.sni.bokaticowork.features.payment.model.PawapayCallback;
import com.sni.bokaticowork.features.payment.repository.PawapayCallbackRepository;
import com.sni.bokaticowork.features.payment.service.pawaypay.EarlyCallbackRelay;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneySupervisionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un rappel arrive avant que le depot soit valide en base · il est relu des la validation.
 */
class EarlyCallbackRelayTest {

    private final PawapayCallbackRepository callbacks = mock(PawapayCallbackRepository.class);
    private final MobileMoneySupervisionService supervision = mock(MobileMoneySupervisionService.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<MobileMoneySupervisionService> provider = mock(ObjectProvider.class);
    private final EarlyCallbackRelay relay = new EarlyCallbackRelay(callbacks, provider);

    @AfterEach
    void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("Un rappel en attente fait relire le depot, apres la validation et pas avant")
    void rechecksAfterCommitWhenACallbackWaits() {
        when(provider.getObject()).thenReturn(supervision);
        when(callbacks.existsByKindAndReferenceIdAndOutcome(
                PawapayCallback.Kind.DEPOSIT, "dep-1", PawapayCallback.Outcome.DEFERRED)).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();

        relay.afterCommit("dep-1");
        verify(supervision, never()).recheck(anyString());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(supervision).recheck("dep-1");
    }

    @Test
    @DisplayName("Sans rappel en attente, pas d'appel a l'operateur")
    void doesNothingWithoutAWaitingCallback() {
        when(provider.getObject()).thenReturn(supervision);

        relay.afterCommit("dep-1");

        verify(supervision, never()).recheck(anyString());
    }
}
