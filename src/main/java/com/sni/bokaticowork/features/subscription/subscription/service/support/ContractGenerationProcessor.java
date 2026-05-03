package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.features.subscription.addon.model.SubscriptionAddon;
import com.sni.bokaticowork.features.subscription.addon.repository.SubscriptionAddonRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractGenerationProcessor {

    private final SubscriptionContractSupport contractSupport;
    private final SubscriptionRepository subscriptionRepository;
    private final PassRepository passRepository;
    private final SubscriptionAddonRepository addonRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(ContractGenerationEvent event) {
        switch (event.sourceType()) {
            case "SUBSCRIPTION" -> handleSubscription(event.sourceId());
            case "PASS" -> handlePass(event.sourceId());
            case "ADDON" -> handleAddon(event.sourceId());
            default -> log.warn("Unknown contract source type: {}", event.sourceType());
        }
    }

    private void handleSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        if (subscription == null) {
            log.warn("Subscription {} not found for contract generation", subscriptionId);
            return;
        }
        if (subscription.getContractCode() != null) {
            return;
        }
        String contractCode = contractSupport.createAndSignForSubscription(subscription);
        subscription.setContractCode(contractCode);
        subscriptionRepository.save(subscription);
    }

    private void handlePass(Long passId) {
        Pass pass = passRepository.findById(passId).orElse(null);
        if (pass == null) {
            log.warn("Pass {} not found for contract generation", passId);
            return;
        }
        if (pass.getContractCode() != null) {
            return;
        }
        String contractCode = contractSupport.createAndSignForPass(pass);
        pass.setContractCode(contractCode);
        passRepository.save(pass);
    }

    private void handleAddon(Long addonId) {
        SubscriptionAddon addon = addonRepository.findById(addonId).orElse(null);
        if (addon == null) {
            log.warn("Addon {} not found for contract generation", addonId);
            return;
        }
        if (addon.getContractCode() != null) {
            return;
        }
        String contractCode = contractSupport.createAndSignForAddon(addon);
        addon.setContractCode(contractCode);
        addonRepository.save(addon);
    }
}
