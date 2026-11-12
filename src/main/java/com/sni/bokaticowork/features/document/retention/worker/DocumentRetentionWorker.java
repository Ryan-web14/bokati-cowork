package com.sni.bokaticowork.features.document.retention.worker;

import com.sni.bokaticowork.features.document.retention.service.interfaces.DocumentRetentionService;
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
public class DocumentRetentionWorker implements SchedulingConfigurer {

    private static final String CRON_KEY = "app.document.retention.cron";
    private static final String DEFAULT_CRON = "0 30 2 * * *";
    private static final String ENABLED_KEY = "app.document.retention.enabled";

    private SettingTriggers triggers;
    private ObjectProvider<DocumentRetentionWorker> self;
    private PlatformSettings platformSettings;

    private final DocumentRetentionService retentionService;

    @Value("${app.document.retention.enabled:true}")
    private boolean enabled;

    public void process() {
        if (!enabled()) return;
        int archived = retentionService.applyPolicies();
        if (archived > 0) {
            log.info("Retention worker archived {} document(s)", archived);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<DocumentRetentionWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().process(), triggers.cron(CRON_KEY, DEFAULT_CRON));
    }

    private boolean enabled() {
        return platformSettings == null ? enabled : platformSettings.bool(ENABLED_KEY, enabled);
    }
}
