package com.sni.bokaticowork.features.document.explorer.service;

import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedFilingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
public class GedFilingWorker {

    private final GedExplorerService explorerService;

    @Value("${app.ged.filing.enabled:true}")
    private boolean enabled;

    @Value("${app.ged.filing.batch-size:1000}")
    private int batchSize;

    @Scheduled(initialDelayString = "${app.ged.filing.initial-delay-ms:120000}",
            fixedDelayString = "${app.ged.filing.fixed-delay-ms:3600000}")
    public void run() {
        if (!enabled) {
            return;
        }
        try {
            GedFilingResult result = explorerService.fileUnfiled(batchSize);
            if (result.filed() > 0 || result.skipped() > 0) {
                log.info("Classement GED · {} rangé(s), {} sans place, {} restant(s)", result.filed(), result.skipped(), result.remaining());
            }
        } catch (Exception ex) {
            log.error("Classement GED en échec · {}", ex.getMessage(), ex);
        }
    }
}
