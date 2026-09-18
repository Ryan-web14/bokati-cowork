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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingPaymentContextResolver {

    private final SubscriptionRepository subscriptionRepository;
    private final EntitlementGrantRepository grantRepository;
    private final @Lazy EntitlementService entitlementService;
    private final BookingOverageGuard overageGuard;

    public BookingPaymentContext resolve(BookingPaymentMode paymentMode,
                                         BookingIdentityResolver.ResolvedBookingIdentity identity,
                                         Resource resource) {
        if (paymentMode == null || paymentMode.isPayable()) {
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
            List<Subscription> subscriptions = subscriptionRepository.findAllCurrentActive(ownerType, ownerCode, LocalDate.now());
            if (subscriptions.isEmpty()) {
                throw new ResourceNotFoundException("No active subscription found for booking owner");
            }
            for (Subscription subscription : subscriptions) {
                Optional<EntitlementGrant> grant = usableSubscriptionGrant(ownerType, ownerCode, subscription, resourceTypeCode, resourceGroupCode, now);
                if (grant.isPresent()) {
                    return new BookingPaymentContext(
                            subscription.getSubscriptionNumber(),
                            null,
                            grant.get().getEntitlementDefinition().getCode()
                    );
                }
            }

            // Aucun droit disponible ne veut pas dire aucun droit. Un abonne qui a consomme ses
            // cinq heures de salle y a bien acces · il les a epuisees. Si le plan autorise le
            // depassement, la reservation passe et les heures supplementaires seront facturees.
            for (Subscription subscription : subscriptions) {
                Optional<EntitlementGrant> exhausted = grantRepository.findSubscriptionGrantIgnoringBalance(
                        ownerType, ownerCode, subscription.getId(), resourceTypeCode, resourceGroupCode, now);
                if (exhausted.isEmpty()) {
                    continue;
                }
                String entitlementCode = exhausted.get().getEntitlementDefinition().getCode();
                if (overageGuard.allows(subscription.getPlanVersion(), entitlementCode)) {
                    log.info("Abonnement {} · droit {} epuise, la reservation passe en depassement",
                            subscription.getSubscriptionNumber(), entitlementCode);
                    return new BookingPaymentContext(subscription.getSubscriptionNumber(), null, entitlementCode);
                }
            }

            throw new ResourceNotFoundException(
                    "No active subscription with a matching entitlement found for resource type="
                    + (resourceTypeCode == null ? "none" : resourceTypeCode)
                    + ", group=" + (resourceGroupCode == null ? "none" : resourceGroupCode));
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
