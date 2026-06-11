package com.sni.bokaticowork.features.subscription.subscription.mapper.decorator;

import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionHistoryResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionResponse;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionLifecycleMapper;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionStatusHistory;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class SubscriptionLifecycleMapperDecorator implements SubscriptionLifecycleMapper {

    @Autowired
    @Qualifier("delegate")
    private SubscriptionLifecycleMapper delegate;

    @Override
    public SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getSubscriptionNumber(),
                subscription.getSubscriberType(),
                subscription.getSubscriberCode(),
                resolveSubscriberName(subscription),
                subscription.getPlanVersion().getPlan().getCode(),
                subscription.getPlanVersion().getName(),
                subscription.getPlanVersion().getVersionNumber(),
                subscription.getStatus(),
                subscription.getStartDate(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.getNextBillingDate(),
                subscription.getAutoRenew(),
                subscription.getBillingCycle(),
                subscription.getCurrency(),
                subscription.getSubtotalAmount(),
                subscription.getTaxAmount(),
                subscription.getTotalAmount(),
                subscription.getCancelAtPeriodEnd(),
                subscription.getPausedAt(),
                subscription.getPauseUntil(),
                subscription.getContractCode(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }

    @Override
    public SubscriptionHistoryResponse toHistoryResponse(SubscriptionStatusHistory history) {
        return new SubscriptionHistoryResponse(history.getFromStatus(), history.getToStatus(), history.getReason(), history.getChangedBy(), history.getChangedAt());
    }

    private String resolveSubscriberName(Subscription subscription) {
        try {
            if (subscription.getMember() != null) {
                String displayName = normalize(subscription.getMember().getDisplayName());
                if (displayName != null) {
                    return displayName;
                }
            }
            if (subscription.getCustomer() != null) {
                String companyName = normalize(subscription.getCustomer().getCompanyName());
                if (companyName != null) {
                    return companyName;
                }
                String fullName = normalize(join(subscription.getCustomer().getFirstname(), subscription.getCustomer().getLastname()));
                if (fullName != null) {
                    return fullName;
                }
            }
            if (subscription.getBusinessEntity() != null) {
                String businessName = normalize(subscription.getBusinessEntity().getName());
                if (businessName != null) {
                    return businessName;
                }
            }
        } catch (EntityNotFoundException ignored) {
            return subscription.getSubscriberCode();
        }
        return subscription.getSubscriberCode();
    }

    private String join(String first, String last) {
        String firstname = first == null ? "" : first.trim();
        String lastname = last == null ? "" : last.trim();
        return (firstname + " " + lastname).trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
