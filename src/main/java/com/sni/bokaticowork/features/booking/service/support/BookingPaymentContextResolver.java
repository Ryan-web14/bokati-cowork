package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class BookingPaymentContextResolver {

    private final SubscriptionRepository subscriptionRepository;
    private final EntitlementGrantRepository grantRepository;

    public BookingPaymentContext resolve(BookingPaymentMode paymentMode,
                                         BookingIdentityResolver.ResolvedBookingIdentity identity,
                                         Resource resource) {
        if (paymentMode == null || paymentMode == BookingPaymentMode.DIRECT) {
            return BookingPaymentContext.direct();
        }
        if (identity.transientOwner()) {
            throw new BadRequestException("Subscription or pass booking requires an existing member, customer or business entity");
        }

        Instant now = Instant.now();
        String ownerType = identity.ownerType().name();
        String ownerCode = identity.ownerCode();
        String resourceTypeCode = resource.getResourceType() == null ? null : resource.getResourceType().getCode();
        String resourceGroupCode = resource.getResourceGroup() == null ? null : resource.getResourceGroup().getCode();

        if (paymentMode == BookingPaymentMode.SUBSCRIPTION) {
            Subscription subscription = subscriptionRepository.findCurrentActive(ownerType, ownerCode, LocalDate.now())
                    .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for booking owner"));
            EntitlementGrant grant = grantRepository.findUsableSubscriptionGrantForResource(
                            ownerType, ownerCode, subscription.getId(), resourceTypeCode, resourceGroupCode, now)
                    .orElseThrow(() -> new ResourceNotFoundException("No usable subscription entitlement found for this resource"));
            return new BookingPaymentContext(
                    subscription.getSubscriptionNumber(),
                    null,
                    grant.getEntitlementDefinition().getCode()
            );
        }

        if (paymentMode == BookingPaymentMode.PASS) {
            EntitlementGrant grant = grantRepository.findUsablePassGrantForResource(
                            ownerType, ownerCode, resourceTypeCode, resourceGroupCode, now)
                    .orElseThrow(() -> new ResourceNotFoundException("No usable pass entitlement found for this resource"));
            return new BookingPaymentContext(
                    null,
                    grant.getPass().getPassNumber(),
                    grant.getEntitlementDefinition().getCode()
            );
        }

        throw new BadRequestException("Unsupported booking payment mode");
    }

    public record BookingPaymentContext(
            String subscriptionNumber,
            String passNumber,
            String entitlementCode
    ) {
        public static BookingPaymentContext direct() {
            return new BookingPaymentContext(null, null, null);
        }
    }
}
