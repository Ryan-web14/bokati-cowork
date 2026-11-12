package com.sni.bokaticowork.features.document.explorer.lifecycle;

import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedPurgeResult;
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

/**
 * Vide chaque nuit la corbeille de la GED de ce qui y dort depuis plus de
 * {@code app.ged.trash.retention-days} jours (30 par defaut). Ce qui est sous gel juridique reste.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GedTrashPurgeWorker implements SchedulingConfigurer {

    private static final String CRON_KEY = "app.ged.trash.purge-cron";
    private static final String DEFAULT_CRON = "0 15 3 * * *";
    private static final String ENABLED_KEY = "app.ged.trash.purge-enabled";
    private static final String RETENTION_DAYS_KEY = "app.ged.trash.retention-days";

    private SettingTriggers triggers;
    private ObjectProvider<GedTrashPurgeWorker> self;
    private PlatformSettings platformSettings;

    private final GedPurgeService purgeService;

    @Value("${app.ged.trash.purge-enabled:true}")
    private boolean enabled;

    @Value("${app.ged.trash.retention-days:30}")
    private int retentionDays;

    public void run() {
        if (!enabled()) {
            return;
        }
        int days = retentionDays();
        try {
            GedPurgeResult result = purgeService.emptyTrash(Math.max(1, days),
                    "Corbeille vidée après " + days + " jours");
            if (result.batches() > 0) {
                log.info("Corbeille GED · {} lot(s), {} fichier(s) détruit(s), {} gelé(s) épargné(s), {} échec(s)",
                        result.batches(), result.documents(), result.heldSkipped(), result.failed());
            }
        } catch (Exception ex) {
            log.error("Corbeille GED · purge en échec : {}", ex.getMessage(), ex);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<GedTrashPurgeWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().run(), triggers.cron(CRON_KEY, DEFAULT_CRON));
    }

    private boolean enabled() {
        return platformSettings == null ? enabled : platformSettings.bool(ENABLED_KEY, enabled);
    }

    private int retentionDays() {
        return platformSettings == null ? retentionDays : platformSettings.integer(RETENTION_DAYS_KEY, retentionDays);
    }
}
