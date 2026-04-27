package com.sni.bokaticowork.features.subscription.subscription.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementGrantResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementOperationResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.EntitlementGrantSearchCriteria;
import org.springframework.data.domain.Pageable;

public interface EntitlementService {

    void grantForSubscription(Subscription subscription);

    void grantForPass(Pass pass);

    EntitlementOperationResponse check(EntitlementOperationRequest request);

    EntitlementOperationResponse reserve(EntitlementOperationRequest request);

    EntitlementOperationResponse consume(EntitlementOperationRequest request);

    EntitlementOperationResponse release(EntitlementOperationRequest request);

    EntitlementOperationResponse refund(EntitlementOperationRequest request);

    PaginatedResponse<EntitlementGrantResponse> list(EntitlementGrantSearchCriteria criteria, Pageable pageable);

    java.util.List<EntitlementGrantResponse> balances(SubscriberType ownerType, String ownerCode);

    int expireGrants();

    int expireReservations();
}
