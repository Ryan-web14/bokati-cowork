package com.sni.bokaticowork.features.subscription.subscription.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.PauseSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.SubscriptionStatusChangeRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.BillingScheduleResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementGrantResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionHistoryResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.SubscriptionSearchCriteria;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SubscriptionService {

    SubscriptionResponse create(CreateSubscriptionRequest request);

    SubscriptionResponse activate(String subscriptionNumber, SubscriptionStatusChangeRequest request);

    SubscriptionResponse suspend(String subscriptionNumber, SubscriptionStatusChangeRequest request);

    SubscriptionResponse pause(String subscriptionNumber, PauseSubscriptionRequest request);

    SubscriptionResponse resume(String subscriptionNumber, SubscriptionStatusChangeRequest request);

    SubscriptionResponse cancel(String subscriptionNumber, SubscriptionStatusChangeRequest request);

    SubscriptionResponse renew(String subscriptionNumber);

    Subscription getForService(String subscriptionNumber);

    SubscriptionResponse get(String subscriptionNumber);

    SubscriptionResponse current(SubscriberType subscriberType, String subscriberCode);

    PaginatedResponse<SubscriptionResponse> list(SubscriptionSearchCriteria criteria, Pageable pageable);

    List<EntitlementGrantResponse> listEntitlements(String subscriptionNumber);

    List<SubscriptionHistoryResponse> history(String subscriptionNumber);

    BillingScheduleResponse billingSchedule(String subscriptionNumber);

    int renewDueSubscriptions();

    int cancelEndedSubscriptions();

    int repairActiveSubscriptionsWithoutGrants();
}
