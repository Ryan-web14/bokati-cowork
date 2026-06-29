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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionClosed(CashSessionClosedEvent event) {
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
