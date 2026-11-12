package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.dto.request.CreateReconciliationBatchRequest;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationWorker implements SchedulingConfigurer {

    private static final String RECONCILIATION_CRON_KEY = "bokati.payment.workers.reconciliation-cron";

    private SettingTriggers triggers;
    private ObjectProvider<PaymentReconciliationWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<PaymentReconciliationWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().reconcileDaily(), triggers.cron(RECONCILIATION_CRON_KEY, "0 30 2 * * *"));
    }

    private final PaymentReconciliationService reconciliationService;

    public void reconcileDaily() {
        var batch = reconciliationService.create(new CreateReconciliationBatchRequest("AUTO", "SYSTEM"));
        reconciliationService.complete(batch.batchNumber());
        log.info("Payment reconciliation batch completed: {}", batch.batchNumber());
    }
}
