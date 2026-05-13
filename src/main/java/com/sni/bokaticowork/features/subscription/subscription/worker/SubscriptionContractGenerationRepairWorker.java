package com.sni.bokaticowork.features.subscription.subscription.worker;

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
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionContractGenerationRepairWorker {

    private final SubscriptionRepository subscriptionRepository;
    private final PassRepository passRepository;
    private final SubscriptionAddonRepository addonRepository;
    private final ContractGenerationProcessor processor;

    @Value("${bokati.subscription.workers.contract-repair-enabled:true}")
    private boolean enabled;

    @Value("${bokati.subscription.workers.contract-repair-batch-size:20}")
    private int batchSize;

    @Value("${bokati.subscription.workers.contract-repair-min-age-seconds:120}")
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
        for (Long id : ids) {
            try {
                processor.process(new ContractGenerationEvent(sourceType, id));
                processed++;
            } catch (Exception ex) {
                log.error("Contract repair failed for {} id={}: {}", sourceType, id, ex.getMessage(), ex);
            }
        }
        return processed;
    }
}
