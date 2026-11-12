package com.sni.bokaticowork.features.document.documentMaster.worker;

import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentLifecycleAutomationService;
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
public class DocumentLifecycleWorker implements SchedulingConfigurer {

    private static final String ENABLED_KEY = "app.document.worker.enabled";
    private static final String DELAY_KEY = "app.document.worker.fixed-delay-ms";
    private static final long DEFAULT_DELAY_MS = 3_600_000L;

    private SettingTriggers triggers;
    private ObjectProvider<DocumentLifecycleWorker> self;
    private PlatformSettings platformSettings;

    private final DocumentLifecycleAutomationService automationService;

    @Value("${app.document.worker.enabled:true}")
    private boolean enabled;

    public void process() {
        if (!enabled()) {
            return;
        }
        try {
            int processed = automationService.expireDocuments();
            if (processed > 0) {
                log.info("Expired {} document(s) automatically", processed);
            }
            int reminded = automationService.notifyPreExpiry();
            if (reminded > 0) {
                log.info("Sent {} document pre-expiry reminder(s)", reminded);
            }
            int autoApproved = automationService.autoApproveDocuments();
            if (autoApproved > 0) {
                log.info("Auto-approved {} document(s)", autoApproved);
            }
        } catch (Exception ex) {
            log.error("DocumentLifecycleWorker failed: {}", ex.getMessage(), ex);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<DocumentLifecycleWorker> self) {
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
