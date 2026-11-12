package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentIntentExpiryWorker implements SchedulingConfigurer {

    private static final String INTENT_EXPIRY_DELAY_MS_KEY = "bokati.payment.workers.intent-expiry-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<PaymentIntentExpiryWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<PaymentIntentExpiryWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().expirePaymentIntents(), triggers.fixedDelay(INTENT_EXPIRY_DELAY_MS_KEY, 300000L));
    }

    private final PaymentIntentRepository paymentIntentRepository;

    public void expirePaymentIntents() {
        int expired = paymentIntentRepository.expirePending(Instant.now());
        if (expired > 0) {
            log.info("Expired {} payment intents", expired);
        }
    }
}
