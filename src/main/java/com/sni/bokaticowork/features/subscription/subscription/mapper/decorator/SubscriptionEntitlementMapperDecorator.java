package com.sni.bokaticowork.features.subscription.subscription.mapper.decorator;

import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementGrantResponse;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionEntitlementMapper;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class SubscriptionEntitlementMapperDecorator implements SubscriptionEntitlementMapper {

    @Autowired
    @Qualifier("delegate")
    private SubscriptionEntitlementMapper delegate;

    @Override
    public EntitlementGrantResponse toResponse(EntitlementGrant grant) {
        return new EntitlementGrantResponse(
                grant.getGrantNumber(),
                grant.getEntitlementDefinition().getCode(),
                grant.getEntitlementDefinition().getName(),
                grant.getEntitlementDefinition().getUnit(),
                grant.getOwnerType(),
                grant.getOwnerCode(),
                grant.getQuantityGranted(),
                grant.getQuantityRemaining(),
                grant.getUnlimited(),
                grant.getValidFrom(),
                grant.getValidUntil(),
                grant.getStatus(),
                grant.getSourceType(),
                grant.getSourceId()
        );
    }
}
