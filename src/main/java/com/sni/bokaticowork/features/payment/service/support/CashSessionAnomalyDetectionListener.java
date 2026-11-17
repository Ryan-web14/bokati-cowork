package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier;
import com.sni.bokaticowork.features.payment.service.interfaces.CashAnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CashSessionAnomalyDetectionListener {

    private final CashAnomalyDetectionService anomalyDetectionService;
    private final AdminInAppNotifier adminInAppNotifier;
    private com.sni.bokaticowork.features.payment.service.interfaces.CashRequestService cashRequestService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setCashRequestService(com.sni.bokaticowork.features.payment.service.interfaces.CashRequestService cashRequestService) {
        this.cashRequestService = cashRequestService;
    }

    /**
     * Apres validation de la cloture, la transaction d'origine est terminee : sans transaction
     * neuve, les signalements crees ici n'etaient jamais enregistres alors que l'alerte partait.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void onSessionClosed(CashSessionClosedEvent event) {
        try {
            if (cashRequestService != null) {
                int cancelled = cashRequestService.cancelPendingOfSession(event.sessionNumber());
                if (cancelled > 0) {
                    log.info("Session {} close : {} demande(s) en attente annulee(s)", event.sessionNumber(), cancelled);
                }
            }
        } catch (Exception ex) {
            log.warn("Annulation des demandes en attente impossible pour la session {} : {}", event.sessionNumber(), ex.getMessage());
        }
        try {
            var anomalies = anomalyDetectionService.analyzeSession(event.sessionNumber());
            if (anomalies != null && !anomalies.isEmpty()) {
                adminInAppNotifier.broadcastAlert(
                        "CASH_ANOMALY", "PAYMENT",
                        "Anomalie caisse détectée",
                        "Anomalie détectée à la clôture de la session " + event.sessionNumber(),
                        "CRITICAL", null
                );
            }
        } catch (Exception ex) {
            log.warn("Cash anomaly detection failed for session {}: {}", event.sessionNumber(), ex.getMessage());
        }
    }
}
