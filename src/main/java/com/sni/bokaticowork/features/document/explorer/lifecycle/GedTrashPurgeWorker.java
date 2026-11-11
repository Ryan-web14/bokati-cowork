package com.sni.bokaticowork.features.document.explorer.lifecycle;

import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedPurgeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vide chaque nuit la corbeille de la GED de ce qui y dort depuis plus de
 * {@code app.ged.trash.retention-days} jours (30 par defaut). Ce qui est sous gel juridique reste.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GedTrashPurgeWorker {

    private final GedPurgeService purgeService;

    @Value("${app.ged.trash.purge-enabled:true}")
    private boolean enabled;

    @Value("${app.ged.trash.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${app.ged.trash.purge-cron:0 15 3 * * *}")
    public void run() {
        if (!enabled) {
            return;
        }
        try {
            GedPurgeResult result = purgeService.emptyTrash(Math.max(1, retentionDays),
                    "Corbeille vidée après " + retentionDays + " jours");
            if (result.batches() > 0) {
                log.info("Corbeille GED · {} lot(s), {} fichier(s) détruit(s), {} gelé(s) épargné(s), {} échec(s)",
                        result.batches(), result.documents(), result.heldSkipped(), result.failed());
            }
        } catch (Exception ex) {
            log.error("Corbeille GED · purge en échec : {}", ex.getMessage(), ex);
        }
    }
}
