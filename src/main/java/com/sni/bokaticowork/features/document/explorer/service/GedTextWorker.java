package com.sni.bokaticowork.features.document.explorer.service;

import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedTextRunResult;
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
 * Lit le texte des fichiers de la GED pour la recherche : nouveaux depots, nouvelles versions,
 * echecs a retenter, et au premier passage tout l'existant, par petits lots.
 *
 * <p>Les lots sont petits parce que l'OCR est lent (quelques secondes par page) : le travailleur
 * ne doit pas occuper le serveur d'un bloc. Deux instances qui liraient le meme fichier en meme
 * temps ecriraient le meme texte.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GedTextWorker implements SchedulingConfigurer {

    private static final String ENABLED_KEY = "app.ged.text.enabled";
    private static final String BATCH_SIZE_KEY = "app.ged.text.batch-size";
    private static final String DELAY_KEY = "app.ged.text.fixed-delay-ms";
    private static final long DEFAULT_DELAY_MS = 60_000L;
    /** Lu une seule fois, au demarrage. */
    private static final String INITIAL_DELAY_KEY = "app.ged.text.initial-delay-ms";
    private static final long DEFAULT_INITIAL_DELAY_MS = 180_000L;

    private SettingTriggers triggers;
    private ObjectProvider<GedTextWorker> self;
    private PlatformSettings platformSettings;

    private final GedTextService textService;

    @Value("${app.ged.text.enabled:true}")
    private boolean enabled;

    @Value("${app.ged.text.batch-size:20}")
    private int batchSize;

    public void run() {
        if (!enabled()) {
            return;
        }
        try {
            GedTextRunResult result = textService.runPending(batchSize());
            if (result.processed() > 0) {
                log.info("Lecture GED · {} fichier(s) : {} lu(s), {} vide(s), {} non lu(s), {} en échec ; {} en attente",
                        result.processed(), result.done(), result.empty(), result.unsupported(), result.failed(), result.remaining());
            }
        } catch (Exception ex) {
            log.error("Lecture GED en échec · {}", ex.getMessage(), ex);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<GedTextWorker> self) {
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
