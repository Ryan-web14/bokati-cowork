package com.sni.bokaticowork.features.subscription.subscription.worker;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.subscription.addon.repository.SubscriptionAddonRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.support.ContractGenerationEvent;
import com.sni.bokaticowork.features.subscription.subscription.service.support.ContractGenerationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rattrape les contrats qui n'ont pas ete generes a la souscription.
 *
 * <p>Toutes les minutes, et c'est voulu · un contrat manquant doit se rattraper vite. Mais toutes
 * les causes d'echec ne se rattrapent pas : il manquait en production la <b>fiche de l'entite
 * exploitante</b>, dont l'adresse determine le lieu de signature. Aucun nombre de tentatives ne
 * cree cette fiche. Le worker journalisait pourtant une trace de pile complete par element et par
 * minute, indefiniment · sur Heroku, ou le collecteur ecarte les lignes au-dela de son debit, ce
 * seul defaut suffisait a faire disparaitre les vraies erreurs des journaux.</p>
 *
 * <p>Deux echecs se distinguent donc desormais :</p>
 * <ul>
 *   <li>un prerequis metier absent ({@link BadRequestException}) · une ligne, sans trace, et
 *       repetee seulement si la cause change. Rien n'est perdu : l'element reste a rattraper et le
 *       sera des que le prerequis existe ;</li>
 *   <li>tout le reste · trace complete, comme avant, parce qu'elle sert.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionContractGenerationRepairWorker {

    private final SubscriptionRepository subscriptionRepository;
    private final PassRepository passRepository;
    private final SubscriptionAddonRepository addonRepository;
    private final ContractGenerationProcessor processor;

    /**
     * La derniere cause metier annoncee, par type de source.
     *
     * <p>Elle n'est redite que si elle change · c'est ce qui evite de reimprimer la meme phrase
     * toutes les minutes pendant des jours, sans pour autant la taire quand elle apparait ou
     * quand elle devient autre.</p>
     */
    private final Map<String, String> lastAnnouncedCause = new LinkedHashMap<>();

    @Value("${bokati.subscription.workers.contract-repair-enabled:true}")
    private boolean enabled;

    @Value("${bokati.subscription.workers.contract-repair-batch-size:20}")
    private int batchSize;

    @Value("${bokati.subscription.workers.contract-repair-min-age-seconds:300}")
    private long minAgeSeconds;

    @Scheduled(fixedDelayString = "${bokati.subscription.workers.contract-repair-delay-ms:60000}")
    public void repairMissingContracts() {
        if (!enabled) {
            return;
        }

        Instant createdBefore = Instant.now().minusSeconds(minAgeSeconds);
        int processed = 0;
        processed += repair("SUBSCRIPTION", subscriptionRepository.findIdsMissingContract(createdBefore, batchSize));
        processed += repair("PASS", passRepository.findIdsMissingContract(createdBefore, batchSize));
        processed += repair("ADDON", addonRepository.findIdsMissingContract(createdBefore, batchSize));

        if (processed > 0) {
            log.info("Repaired {} missing subscription contract generation job(s)", processed);
        }
    }

    private int repair(String sourceType, List<Long> ids) {
        int processed = 0;
        int blocked = 0;
        String cause = null;

        for (Long id : ids) {
            try {
                processor.process(new ContractGenerationEvent(sourceType, id));
                processed++;
            } catch (BadRequestException ex) {
                // Un prerequis metier · la trace de pile ne dit rien que le message ne dise, et
                // le meme message reviendra a chaque minute jusqu'a ce que le prerequis existe.
                blocked++;
                cause = ex.getMessage() == null ? "cause non precisee" : ex.getMessage();
            } catch (Exception ex) {
                log.error("Contract repair failed for {} id={}: {}", sourceType, id, ex.getMessage(), ex);
            }
        }

        announce(sourceType, blocked, cause);
        return processed;
    }

    /** Dit la cause une fois, et ne la redit que si elle change. */
    private void announce(String sourceType, int blocked, String cause) {
        if (blocked == 0) {
            if (lastAnnouncedCause.remove(sourceType) != null) {
                log.info("Generation de contrat {} · le prerequis manquant est leve", sourceType);
            }
            return;
        }
        if (cause.equals(lastAnnouncedCause.put(sourceType, cause))) {
            return;
        }
        log.warn("Generation de contrat {} · {} element(s) en attente d'un prerequis · {}",
                sourceType, blocked, cause);
    }
}
