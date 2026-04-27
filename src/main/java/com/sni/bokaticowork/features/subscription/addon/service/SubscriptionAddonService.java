package com.sni.bokaticowork.features.subscription.addon.service;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.addon.dto.CreateSubscriptionAddonRequest;
import com.sni.bokaticowork.features.subscription.addon.dto.SubscriptionAddonResponse;
import com.sni.bokaticowork.features.subscription.addon.enums.SubscriptionAddonStatus;
import org.springframework.data.domain.Pageable;

public interface SubscriptionAddonService {

    SubscriptionAddonResponse add(String subscriptionNumber, CreateSubscriptionAddonRequest request);

    SubscriptionAddonResponse cancel(Long addonId);

    PaginatedResponse<SubscriptionAddonResponse> list(String subscriptionNumber, String planCode, SubscriptionAddonStatus status, Pageable pageable);

    int expireEndedAddons();
}
