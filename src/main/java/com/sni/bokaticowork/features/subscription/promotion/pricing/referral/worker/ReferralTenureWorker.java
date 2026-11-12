package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.worker;

import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service.ReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;

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
public class ReferralTenureWorker implements SchedulingConfigurer {

    private static final String TENURE_CRON_KEY = "bokati.promotion.referral.tenure-cron";

    private SettingTriggers triggers;
    private ObjectProvider<ReferralTenureWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<ReferralTenureWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().qualifyDueReferrals(), triggers.cron(TENURE_CRON_KEY, "0 30 2 * * *"));
    }

    private static final int BATCH_SIZE = 200;

    private final ReferralService referralService;

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
