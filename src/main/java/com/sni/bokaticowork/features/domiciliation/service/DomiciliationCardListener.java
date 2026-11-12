package com.sni.bokaticowork.features.domiciliation.service;

import com.sni.bokaticowork.features.company.service.support.CompanyLegalCardChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Une modification de la fiche d'une organisation se reporte sur ses domiciliations en cours, dans
 * la meme transaction · l'attestation suivante l'imprime.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomiciliationCardListener {

    private final DomiciliationService domiciliationService;

    @EventListener
    public void onCardChanged(CompanyLegalCardChangedEvent event) {
        int refreshed = domiciliationService.refreshCompany(event.customerId());
        if (refreshed > 0) {
            log.info("Fiche de {} modifiee · {} domiciliation(s) en cours mise(s) a jour", event.customerId(), refreshed);
        }
    }
}
