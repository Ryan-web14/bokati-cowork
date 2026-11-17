package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Lot KG2 : la synchronisation part apres validation, une fois par document ; jamais si la transaction est annulee. */
class KycDocumentSyncRunnerKg2Test {

    private final KycDocumentSyncRunner self = mock(KycDocumentSyncRunner.class);
    private final KycDocumentSyncRunner runner = new KycDocumentSyncRunner(mock(DocumentRepository.class), mock(KycAutomationService.class), self);

    @AfterEach
    void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void complete(int status) {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(status);
        }
    }

    @Test
    void itRunsOnceAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        runner.schedule(5L);
        runner.schedule(5L);
        runner.schedule(6L);
        verify(self, never()).run(5L);

        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(self, times(1)).run(5L);
        verify(self, times(1)).run(6L);
    }

    @Test
    void nothingRunsAfterARollback() {
        TransactionSynchronizationManager.initSynchronization();
        runner.schedule(5L);

        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(self, never()).run(5L);
    }

    @Test
    void withoutTransactionItRunsAtOnce() {
        runner.schedule(5L);
        verify(self).run(5L);
    }
}
