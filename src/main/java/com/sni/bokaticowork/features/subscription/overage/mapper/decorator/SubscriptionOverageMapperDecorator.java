package com.sni.bokaticowork.features.subscription.overage.mapper.decorator;

import com.sni.bokaticowork.features.subscription.overage.dto.CreateOveragePolicyRequest;
import com.sni.bokaticowork.features.subscription.overage.dto.OverageChargeResponse;
import com.sni.bokaticowork.features.subscription.overage.dto.OveragePolicyResponse;
import com.sni.bokaticowork.features.subscription.overage.enums.OveragePolicyMode;
import com.sni.bokaticowork.features.subscription.overage.mapper.interfaces.SubscriptionOverageMapper;
import com.sni.bokaticowork.features.subscription.overage.model.SubscriptionOverageCharge;
import com.sni.bokaticowork.features.subscription.overage.model.SubscriptionOveragePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public abstract class SubscriptionOverageMapperDecorator implements SubscriptionOverageMapper {

    @Autowired
    @Qualifier("delegate")
    private SubscriptionOverageMapper delegate;

    @Override
    public SubscriptionOveragePolicy toEntity(CreateOveragePolicyRequest request) {
        SubscriptionOveragePolicy policy = delegate.toEntity(request);
        policy.setCurrency(StringUtils.hasText(request.currency()) ? request.currency().trim().toUpperCase(Locale.ROOT) : null);
        policy.setFreeQuantity(request.freeQuantity() == null ? BigDecimal.ZERO : request.freeQuantity());
        policy.setMetadataJson(trim(request.metadataJson()));
        policy.setActive(request.active() == null || request.active());
        if (policy.getMode() == OveragePolicyMode.BILLABLE && policy.getUnitPrice() == null) {
            policy.setUnitPrice(BigDecimal.ZERO);
        }
        return policy;
    }

    @Override
    public OveragePolicyResponse toResponse(SubscriptionOveragePolicy policy) {
        return new OveragePolicyResponse(
                policy.getId(),
                policy.getPlanVersion().getId(),
                policy.getPlanVersion().getPlan().getCode(),
                policy.getEntitlementDefinition().getCode(),
                policy.getMode(),
                policy.getUnitPrice(),
                policy.getCurrency(),
                policy.getFreeQuantity(),
                policy.getMetadataJson(),
                policy.getActive()
        );
    }

    @Override
    public OverageChargeResponse toResponse(SubscriptionOverageCharge charge) {
        return new OverageChargeResponse(
                charge.getChargeNumber(),
                charge.getSubscription() == null ? null : charge.getSubscription().getSubscriptionNumber(),
                charge.getUsageRecord().getUsageNumber(),
                charge.getBillableItem() == null ? null : charge.getBillableItem().getBillableNumber(),
                charge.getOwnerType(),
                charge.getOwnerCode(),
                charge.getEntitlementCode(),
                charge.getOverageQuantity(),
                charge.getUnitPrice(),
                charge.getAmount(),
                charge.getCurrency(),
                charge.getCreatedAt()
        );
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
