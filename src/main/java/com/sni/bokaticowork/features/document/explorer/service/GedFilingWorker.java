package com.sni.bokaticowork.features.document.explorer.service;

import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedFilingResult;
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
 * Reprise periodique du classement : range les documents restes sans dossier.
 *
 * <p>Au premier passage, elle range l'existant par lots. Ensuite elle rattrape les rares depots
 * dont le classement a echoue. Plusieurs instances peuvent tourner en meme temps : ranger un
 * document deja range ne fait rien.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GedFilingWorker implements SchedulingConfigurer {

    private static final String ENABLED_KEY = "app.ged.filing.enabled";
    private static final String BATCH_SIZE_KEY = "app.ged.filing.batch-size";
    private static final String DELAY_KEY = "app.ged.filing.fixed-delay-ms";
    private static final long DEFAULT_DELAY_MS = 3_600_000L;
    /** Lu une seule fois, au demarrage. */
    private static final String INITIAL_DELAY_KEY = "app.ged.filing.initial-delay-ms";
    private static final long DEFAULT_INITIAL_DELAY_MS = 120_000L;

    private SettingTriggers triggers;
    private ObjectProvider<GedFilingWorker> self;
    private PlatformSettings platformSettings;

    private final GedExplorerService explorerService;

    @Value("${app.ged.filing.enabled:true}")
    private boolean enabled;

    @Value("${app.ged.filing.batch-size:1000}")
    private int batchSize;

    public void run() {
        if (!enabled()) {
            return;
        }
        try {
            GedFilingResult result = explorerService.fileUnfiled(batchSize());
            if (result.filed() > 0 || result.skipped() > 0) {
                log.info("Classement GED · {} rangé(s), {} sans place, {} restant(s)", result.filed(), result.skipped(), result.remaining());
            }
        } catch (Exception ex) {
            log.error("Classement GED en échec · {}", ex.getMessage(), ex);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<GedFilingWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        long initialDelayMs = platformSettings == null
                ? DEFAULT_INITIAL_DELAY_MS : platformSettings.number(INITIAL_DELAY_KEY, DEFAULT_INITIAL_DELAY_MS);
        registrar.addTriggerTask(() -> self.getObject().run(), triggers.fixedDelay(DELAY_KEY, DEFAULT_DELAY_MS, initialDelayMs));
    }

    private boolean enabled() {
        return platformSettings == null ? enabled : platformSettings.bool(ENABLED_KEY, enabled);
    }

    private int batchSize() {
        return platformSettings == null ? batchSize : platformSettings.integer(BATCH_SIZE_KEY, batchSize);
    }
}
