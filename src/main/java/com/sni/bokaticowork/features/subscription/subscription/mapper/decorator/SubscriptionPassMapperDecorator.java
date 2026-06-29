package com.sni.bokaticowork.features.subscription.subscription.mapper.decorator;

import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PassResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementGrantStatus;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionEntitlementMapper;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionPassMapper;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class SubscriptionPassMapperDecorator implements SubscriptionPassMapper {

    @Autowired
    @Qualifier("delegate")
    private SubscriptionPassMapper delegate;

    @Autowired
    private EntitlementGrantRepository entitlementGrantRepository;

    @Autowired
    private SubscriptionEntitlementMapper entitlementMapper;

    @Override
    public PassResponse toResponse(Pass pass) {
        PassPlanVersion pv = pass.getPassVersion();
        return new PassResponse(
                pass.getPassNumber(),
                pass.getPassType(),
                pass.getOwnerType(),
                pass.getOwnerCode(),
                pass.getSubscription() == null ? null : pass.getSubscription().getSubscriptionNumber(),
                pass.getStatus(),
                pass.getName(),
                pass.getDescription(),
                pass.getValidFrom(),
                pass.getValidUntil(),
                pass.getTransferable(),
                pass.getShareable(),
                pass.getMaxUses(),
                pass.getUsedCount(),
                pass.getContractCode(),
                pv != null && pv.getPlan() != null ? pv.getPlan().getCode() : null,
                pv != null && pv.getPlan() != null ? pv.getPlan().getName() : null,
                pv != null ? pv.getVersionNumber() : null,
                pass.getCurrency(),
                pass.getSubtotalAmount(),
                pass.getTaxAmount(),
                pass.getTotalAmount(),
                pass.getAutoRenew(),
                pass.getNextRenewalDate(),
                pass.getRenewalCount(),
                entitlementGrantRepository.findAllByOwnerTypeAndOwnerCodeAndStatus(
                                pass.getOwnerType().name(),
                                pass.getOwnerCode(),
                                EntitlementGrantStatus.ACTIVE.name()
                        ).stream()
                        .filter(grant -> grant.getPass() != null && grant.getPass().getId().equals(pass.getId()))
                        .map(entitlementMapper::toResponse)
                        .toList(),
                pass.getCreatedAt(),
                pass.getUpdatedAt()
        );
    }
}
