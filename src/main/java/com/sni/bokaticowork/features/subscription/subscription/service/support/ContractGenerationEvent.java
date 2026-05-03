package com.sni.bokaticowork.features.subscription.subscription.service.support;

public record ContractGenerationEvent(
        String sourceType,
        Long sourceId
) {
    public static ContractGenerationEvent forSubscription(Long subscriptionId) {
        return new ContractGenerationEvent("SUBSCRIPTION", subscriptionId);
    }

    public static ContractGenerationEvent forPass(Long passId) {
        return new ContractGenerationEvent("PASS", passId);
    }

    public static ContractGenerationEvent forAddon(Long addonId) {
        return new ContractGenerationEvent("ADDON", addonId);
    }
}