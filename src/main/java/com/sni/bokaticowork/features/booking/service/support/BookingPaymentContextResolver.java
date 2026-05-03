package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookingPaymentContextResolver {

    private final SubscriptionRepository subscriptionRepository;
    private final EntitlementGrantRepository grantRepository;
    private final @Lazy EntitlementService entitlementService;

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
            EntitlementGrant grant = usableSubscriptionGrant(ownerType, ownerCode, subscription, resourceTypeCode, resourceGroupCode, now)
                    .orElseThrow(() -> new ResourceNotFoundException(noSubscriptionGrantMessage(subscription, resourceTypeCode, resourceGroupCode)));
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

    private Optional<EntitlementGrant> usableSubscriptionGrant(String ownerType,
                                                              String ownerCode,
                                                              Subscription subscription,
                                                              String resourceTypeCode,
                                                              String resourceGroupCode,
                                                              Instant now) {
        Optional<EntitlementGrant> grant = grantRepository.findUsableSubscriptionGrantForResource(
                ownerType, ownerCode, subscription.getId(), resourceTypeCode, resourceGroupCode, now);
        if (grant.isPresent()) {
            return grant;
        }

        entitlementService.grantForSubscription(subscription);
        return grantRepository.findUsableSubscriptionGrantForResource(
                ownerType, ownerCode, subscription.getId(), resourceTypeCode, resourceGroupCode, Instant.now());
    }

    private String noSubscriptionGrantMessage(Subscription subscription, String resourceTypeCode, String resourceGroupCode) {
        return "No usable subscription entitlement found for subscription "
                + subscription.getSubscriptionNumber()
                + " and resource type=" + (resourceTypeCode == null ? "none" : resourceTypeCode)
                + ", group=" + (resourceGroupCode == null ? "none" : resourceGroupCode);
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
