package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.payment.service.interfaces.CashAnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CashSessionAnomalyDetectionListener {

    private final CashAnomalyDetectionService anomalyDetectionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionClosed(CashSessionClosedEvent event) {
        try {
            anomalyDetectionService.analyzeSession(event.sessionNumber());
        } catch (Exception ex) {
            log.warn("Cash anomaly detection failed for session {}: {}", event.sessionNumber(), ex.getMessage());
        }
    }
}
