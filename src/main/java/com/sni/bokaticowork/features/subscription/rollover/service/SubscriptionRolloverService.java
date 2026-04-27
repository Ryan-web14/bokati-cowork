package com.sni.bokaticowork.features.subscription.rollover.service;

import com.sni.bokaticowork.features.subscription.rollover.dto.RolloverRecordResponse;

import java.util.List;

public interface SubscriptionRolloverService {

    int applyDueRollovers();

    List<RolloverRecordResponse> list(String subscriptionNumber, String entitlementCode);
}
