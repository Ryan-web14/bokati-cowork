package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.worker;

import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service.ReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Qualifie les parrainages dont l'anciennete exigee est atteinte.
 *
 * <p>Les deux autres regles se qualifient sur un evenement · l'inscription ou le premier reglement.
 * L'anciennete, elle, ne produit aucun evenement : personne ne previent qu'un filleul vient
 * d'atteindre trois mois. C'est la seule raison d'etre de ce traitement.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReferralTenureWorker {

    private static final int BATCH_SIZE = 200;

    private final ReferralService referralService;

    @Scheduled(cron = "${bokati.promotion.referral.tenure-cron:0 30 2 * * *}")
    public void qualifyDueReferrals() {
        try {
            int qualified = referralService.qualifyDueByTenure(BATCH_SIZE);
            if (qualified > 0) {
                log.info("Parrainage : {} qualification(s) par anciennete", qualified);
            }
        } catch (Exception ex) {
            log.error("ReferralTenureWorker a echoue : {}", ex.getMessage(), ex);
        }
    }
}
