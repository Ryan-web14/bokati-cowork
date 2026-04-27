package com.sni.bokaticowork.features.subscription.subscription.service.support.entitlement;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EntitlementRequestValidator {

    public void operation(EntitlementOperationRequest request) {
        if (request == null) {
            throw new BadRequestException("Entitlement operation request is required");
        }
        if (request.ownerType() == null || !StringUtils.hasText(request.ownerCode())) {
            throw new BadRequestException("Owner type and owner code are required");
        }
        if (!StringUtils.hasText(request.entitlementCode())) {
            throw new BadRequestException("Entitlement code is required");
        }
        if (request.quantity() == null || request.quantity().signum() <= 0) {
            throw new BadRequestException("Entitlement quantity must be positive");
        }
    }

    public void reference(EntitlementOperationRequest request) {
        operation(request);
        if (!StringUtils.hasText(request.referenceType()) || !StringUtils.hasText(request.referenceId())) {
            throw new BadRequestException("Reference type and reference id are required for entitlement reservation operations");
        }
    }
}
