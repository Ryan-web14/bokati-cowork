package com.sni.bokaticowork.features.subscription.change.mapper.decorator;

import com.sni.bokaticowork.features.subscription.change.dto.CreateSubscriptionChangeRequest;
import com.sni.bokaticowork.features.subscription.change.dto.SubscriptionChangeResponse;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeStatus;
import com.sni.bokaticowork.features.subscription.change.mapper.interfaces.SubscriptionChangeMapper;
import com.sni.bokaticowork.features.subscription.change.model.SubscriptionChangeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Component
public abstract class SubscriptionChangeMapperDecorator implements SubscriptionChangeMapper {

    @Autowired
    @Qualifier("delegate")
    private SubscriptionChangeMapper delegate;

    @Override
    public SubscriptionChangeRequest toEntity(CreateSubscriptionChangeRequest request) {
        SubscriptionChangeRequest change = delegate.toEntity(request);
        change.setProrationAmount(request.prorationAmount() == null ? BigDecimal.ZERO : request.prorationAmount());
        change.setStatus(SubscriptionChangeStatus.REQUESTED);
        change.setReason(trim(request.reason()));
        change.setRequestedBy(trim(request.requestedBy()));
        return change;
    }

    @Override
    public SubscriptionChangeResponse toResponse(SubscriptionChangeRequest change) {
        return new SubscriptionChangeResponse(
                change.getChangeNumber(),
                change.getSubscription().getSubscriptionNumber(),
                change.getChangeType(),
                change.getCurrentPlanVersion() == null ? null : change.getCurrentPlanVersion().getId(),
                change.getTargetPlanVersion() == null ? null : change.getTargetPlanVersion().getId(),
                change.getEffectivePolicy(),
                change.getEffectiveDate(),
                change.getProrationAmount(),
                change.getStatus(),
                change.getReason(),
                change.getRequestedBy(),
                change.getApprovedBy(),
                change.getAppliedAt()
        );
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
