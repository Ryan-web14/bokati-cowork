package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Lot KG2 : synchronisation d'une piece KYC apres un depot ou une revue de document.
 *
 * <p>Elle part <b>apres validation</b> de la transaction de l'appelant (avant, la tache asynchrone
 * lisait des donnees pas encore validees, ou pas du tout), une seule fois par document et par
 * transaction (avant, un depot la lancait deux ou trois fois). Hors transaction, elle part tout de
 * suite. Le document est relu dans sa propre transaction.</p>
 */
@Slf4j
@Component
public class KycDocumentSyncRunner {

    private static final String PENDING_KEY = KycDocumentSyncRunner.class.getName() + ".pending";

    private final DocumentRepository documentRepository;
    private final KycAutomationService automation;
    private final KycDocumentSyncRunner self;

    public KycDocumentSyncRunner(DocumentRepository documentRepository,
                                 @Lazy KycAutomationService automation,
                                 @Lazy KycDocumentSyncRunner self) {
        this.documentRepository = documentRepository;
        this.automation = automation;
        this.self = self;
    }

    /** Programme la synchronisation du document (apres validation, une fois). */
    @SuppressWarnings("unchecked")
    public void schedule(Long documentId) {
        if (documentId == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            self.run(documentId);
            return;
        }
        Set<Long> pending = (Set<Long>) TransactionSynchronizationManager.getResource(PENDING_KEY);
        if (pending == null) {
            Set<Long> created = new LinkedHashSet<>();
            TransactionSynchronizationManager.bindResource(PENDING_KEY, created);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    TransactionSynchronizationManager.unbindResourceIfPossible(PENDING_KEY);
                    if (status == STATUS_COMMITTED) {
                        created.forEach(self::run);
                    }
                }
            });
            pending = created;
        }
        pending.add(documentId);
    }

    /** Relit le document et met a jour sa piece et le dossier. */
    @Async
    @Transactional
    public void run(Long documentId) {
        documentRepository.findById(documentId).ifPresentOrElse(
                automation::applyDocumentSync,
                () -> log.warn("Synchronisation KYC : document {} introuvable", documentId));
    }
}
