package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.model.PaymentDunningAttempt;
import com.sni.bokaticowork.features.payment.repository.PaymentDunningAttemptRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.DunningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;

@Slf4j
@Component
@RequiredArgsConstructor
public class DunningWorker implements SchedulingConfigurer {

    private static final String DUNNING_DELAY_MS_KEY = "bokati.payment.workers.dunning-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<DunningWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<DunningWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().executeDueAttempts(), triggers.fixedDelay(DUNNING_DELAY_MS_KEY, 60000L));
    }

    private final PaymentDunningAttemptRepository dunningRepo;
    private final DunningService dunningService;

    public void executeDueAttempts() {
        List<PaymentDunningAttempt> due = dunningRepo.findDuePending(Instant.now());
        if (due.isEmpty()) return;

        log.info("Executing {} due dunning attempt(s)", due.size());
        for (PaymentDunningAttempt attempt : due) {
            try {
                dunningService.executeAttempt(attempt);
            } catch (Exception ex) {
                log.error("Unexpected error executing dunning attempt id={} for intent id={}",
                        attempt.getId(), attempt.getPaymentIntentId(), ex);
            }
        }
    }
}
