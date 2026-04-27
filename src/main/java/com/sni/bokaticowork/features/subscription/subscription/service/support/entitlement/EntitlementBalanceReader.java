package com.sni.bokaticowork.features.subscription.subscription.service.support.entitlement;

import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementOperationResponse;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EntitlementBalanceReader {

    private final EntitlementGrantRepository grantRepository;
    private final EntitlementRequestValidator validator;

    public EntitlementOperationResponse check(EntitlementOperationRequest request) {
        List<EntitlementGrant> grants = usableGrants(request);
        BigDecimal available = availableQuantity(grants);
        boolean allowed = hasUnlimitedGrant(grants) || available.compareTo(request.quantity()) >= 0;
        return new EntitlementOperationResponse(
                allowed,
                request.entitlementCode(),
                request.quantity(),
                available,
                allowed ? "Entitlement is available" : "Insufficient entitlement balance"
        );
    }

    public List<EntitlementGrant> usableGrants(EntitlementOperationRequest request) {
        validator.operation(request);
        return grantRepository.findUsableGrants(
                request.ownerType().name(),
                request.ownerCode(),
                request.entitlementCode(),
                Instant.now()
        );
    }

    public BigDecimal availableQuantity(List<EntitlementGrant> grants) {
        return grants.stream()
                .filter(grant -> !Boolean.TRUE.equals(grant.getUnlimited()))
                .map(EntitlementGrant::getQuantityRemaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean hasUnlimitedGrant(List<EntitlementGrant> grants) {
        return grants.stream().anyMatch(grant -> Boolean.TRUE.equals(grant.getUnlimited()));
    }
}
