package com.sni.bokaticowork.features.subscription.change.service;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.change.dto.CreateSubscriptionChangeRequest;
import com.sni.bokaticowork.features.subscription.change.dto.SubscriptionChangeResponse;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeStatus;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeType;
import org.springframework.data.domain.Pageable;

public interface SubscriptionChangeRequestService {

    SubscriptionChangeResponse request(String subscriptionNumber, CreateSubscriptionChangeRequest request);

    SubscriptionChangeResponse approve(String changeNumber, String approvedBy);

    SubscriptionChangeResponse apply(String changeNumber);

    PaginatedResponse<SubscriptionChangeResponse> list(String subscriptionNumber, SubscriptionChangeType changeType, SubscriptionChangeStatus status, Pageable pageable);

    int applyDueChanges();
}
