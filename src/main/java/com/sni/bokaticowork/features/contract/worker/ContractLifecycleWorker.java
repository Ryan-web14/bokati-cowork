package com.sni.bokaticowork.features.contract.worker;

import com.sni.bokaticowork.features.contract.service.interfaces.ContractLifecycleAutomationService;
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
public class ContractLifecycleWorker implements SchedulingConfigurer {

    private static final String ENABLED_KEY = "app.contract.worker.enabled";
    private static final String DELAY_KEY = "app.contract.worker.fixed-delay-ms";
    private static final long DEFAULT_DELAY_MS = 3_600_000L;

    private SettingTriggers triggers;
    private ObjectProvider<ContractLifecycleWorker> self;
    private PlatformSettings platformSettings;

    private final ContractLifecycleAutomationService automationService;

    @Value("${app.contract.worker.enabled:true}")
    private boolean enabled;

    public void process() {
        if (!enabled()) {
            return;
        }
        try {
            int processed = automationService.processScheduledTransitions();
            if (processed > 0) {
                log.info("Processed {} contract lifecycle transition(s)", processed);
            }
        } catch (Exception ex) {
            log.error("ContractLifecycleWorker failed: {}", ex.getMessage(), ex);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<ContractLifecycleWorker> self) {
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
}
