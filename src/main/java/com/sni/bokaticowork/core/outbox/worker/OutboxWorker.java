package com.sni.bokaticowork.core.outbox.worker;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker implements SchedulingConfigurer {

    private static final String ENABLED_KEY = "app.outbox.worker.enabled";
    private static final String BATCH_SIZE_KEY = "app.outbox.worker.batch-size";
    private static final String DELAY_KEY = "app.outbox.worker.fixed-delay-ms";
    private static final long DEFAULT_DELAY_MS = 2_000L;

    private SettingTriggers triggers;
    private ObjectProvider<OutboxWorker> self;
    private PlatformSettings platformSettings;

    private final OutboxService outboxService;

    @Value("${app.outbox.worker.enabled:true}")
    private boolean enabled;

    @Value("${app.outbox.worker.batch-size:25}")
    private int batchSize;

    public void process() {
        if (!enabled()) {
            return;
        }
        try {
            int processed = outboxService.processPending(batchSize());
            if (processed > 0) {
                log.info("Processed {} outbox event(s)", processed);
            }
        } catch (Exception ex) {
            log.error("OutboxWorker failed: {}", ex.getMessage(), ex);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<OutboxWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().process(), triggers.fixedDelay(DELAY_KEY, DEFAULT_DELAY_MS));
    }

    private boolean enabled() {
        return platformSettings == null ? enabled : platformSettings.bool(ENABLED_KEY, enabled);
    }

    private int batchSize() {
        return platformSettings == null ? batchSize : platformSettings.integer(BATCH_SIZE_KEY, batchSize);
    }
}
