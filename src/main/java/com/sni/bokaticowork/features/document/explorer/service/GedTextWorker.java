package com.sni.bokaticowork.features.document.explorer.service;

import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedTextRunResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
public class GedTextWorker {

    private final GedTextService textService;

    @Value("${app.ged.text.enabled:true}")
    private boolean enabled;

    @Value("${app.ged.text.batch-size:20}")
    private int batchSize;

    @Scheduled(initialDelayString = "${app.ged.text.initial-delay-ms:180000}",
            fixedDelayString = "${app.ged.text.fixed-delay-ms:60000}")
    public void run() {
        if (!enabled) {
            return;
        }
        try {
            GedTextRunResult result = textService.runPending(batchSize);
            if (result.processed() > 0) {
                log.info("Lecture GED · {} fichier(s) : {} lu(s), {} vide(s), {} non lu(s), {} en échec ; {} en attente",
                        result.processed(), result.done(), result.empty(), result.unsupported(), result.failed(), result.remaining());
            }
        } catch (Exception ex) {
            log.error("Lecture GED en échec · {}", ex.getMessage(), ex);
        }
    }
}
