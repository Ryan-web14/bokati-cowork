package com.sni.bokaticowork.features.billing.worker;

import com.sni.bokaticowork.features.billing.service.interfaces.PaymentScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentScheduleOverdueWorker implements SchedulingConfigurer {

    private static final String DELAY_KEY = "bokati.billing.workers.schedule-overdue-delay-ms";
    private static final long DEFAULT_DELAY_MS = 3_600_000L;

    private SettingTriggers triggers;
    private ObjectProvider<PaymentScheduleOverdueWorker> self;

    private final PaymentScheduleService paymentScheduleService;

    public void markOverdueInstallments() {
        int marked = paymentScheduleService.markOverdueInstallments();
        if (marked > 0) {
            log.info("Marked {} payment schedule installments as overdue", marked);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<PaymentScheduleOverdueWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().markOverdueInstallments(), triggers.fixedDelay(DELAY_KEY, DEFAULT_DELAY_MS));
    }
}
