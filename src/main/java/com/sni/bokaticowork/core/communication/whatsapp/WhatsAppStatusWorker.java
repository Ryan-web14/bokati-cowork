package com.sni.bokaticowork.core.communication.whatsapp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Secours du webhook : toutes les 10 minutes, relit chez Infobip le journal des messages WhatsApp
 * encore en attente (sans effet si l'envoi WhatsApp n'est pas active).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppStatusWorker {

    private final WhatsAppService whatsapp;

    @Scheduled(fixedDelay = 600_000L, initialDelay = 180_000L)
    public void run() {
        try {
            int updated = whatsapp.poll(50);
            if (updated > 0) {
                log.info("WhatsApp : {} statut(s) relu(s) chez Infobip", updated);
            }
        } catch (RuntimeException exception) {
            log.warn("Relecture des statuts WhatsApp impossible : {}", exception.getMessage());
        }
    }
}
