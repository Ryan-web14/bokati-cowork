package com.sni.bokaticowork.features.subscription.addon.mapper.decorator;

import com.sni.bokaticowork.features.subscription.addon.dto.CreateSubscriptionAddonRequest;
import com.sni.bokaticowork.features.subscription.addon.dto.SubscriptionAddonResponse;
import com.sni.bokaticowork.features.subscription.addon.enums.SubscriptionAddonStatus;
import com.sni.bokaticowork.features.subscription.addon.mapper.interfaces.SubscriptionAddonMapper;
import com.sni.bokaticowork.features.subscription.addon.model.SubscriptionAddon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Component
public abstract class SubscriptionAddonMapperDecorator implements SubscriptionAddonMapper {

    @Autowired
    @Qualifier("delegate")
    private SubscriptionAddonMapper delegate;

    @Override
    public SubscriptionAddon toEntity(CreateSubscriptionAddonRequest request) {
        SubscriptionAddon addon = delegate.toEntity(request);
        addon.setQuantity(request.quantity() == null ? 1 : request.quantity());
        addon.setStartsAt(request.startsAt() == null ? LocalDate.now() : request.startsAt());
        addon.setEndsAt(request.endsAt());
        addon.setStatus(SubscriptionAddonStatus.ACTIVE);
        addon.setMetadataJson(trim(request.metadataJson()));
        return addon;
    }

    @Override
    public SubscriptionAddonResponse toResponse(SubscriptionAddon addon) {
        return new SubscriptionAddonResponse(
                addon.getId(),
                addon.getSubscription().getSubscriptionNumber(),
                addon.getPlanVersion().getId(),
                addon.getPlanVersion().getPlan().getCode(),
                addon.getPlanVersion().getName(),
                addon.getQuantity(),
                addon.getUnitPrice(),
                addon.getCurrency(),
                addon.getStatus(),
                addon.getStartsAt(),
                addon.getEndsAt(),
                addon.getContractCode(),
                addon.getMetadataJson()
        );
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
