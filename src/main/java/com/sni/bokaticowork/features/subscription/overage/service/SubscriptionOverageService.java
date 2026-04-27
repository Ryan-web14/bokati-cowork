package com.sni.bokaticowork.features.subscription.overage.service;

import com.sni.bokaticowork.features.subscription.overage.dto.CreateOveragePolicyRequest;
import com.sni.bokaticowork.features.subscription.overage.dto.OverageBillingResult;
import com.sni.bokaticowork.features.subscription.overage.dto.OverageChargeResponse;
import com.sni.bokaticowork.features.subscription.overage.dto.OveragePolicyResponse;
import com.sni.bokaticowork.features.subscription.overage.model.SubscriptionOverageCharge;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.usage.dto.CreateUsageRecordRequest;
import com.sni.bokaticowork.features.subscription.usage.model.UsageRecord;

import java.util.List;

public interface SubscriptionOverageService {

    OveragePolicyResponse createPolicy(CreateOveragePolicyRequest request);

    List<OveragePolicyResponse> listPolicies(Long planVersionId, String entitlementCode);

    List<OverageChargeResponse> listCharges(String subscriptionNumber, SubscriberType ownerType, String ownerCode, String entitlementCode);

    OverageBillingResult processUsage(CreateUsageRecordRequest request);

    SubscriptionOverageCharge createCharge(UsageRecord usage, OverageBillingResult result);
}
