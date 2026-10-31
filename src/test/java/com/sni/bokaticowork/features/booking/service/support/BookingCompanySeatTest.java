package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.company.sharing.service.CompanySeatLocator;
import com.sni.bokaticowork.features.company.sharing.service.SeatEntitlementGate;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.seat.model.SubscriptionSeat;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementOperationResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import com.sni.bokaticowork.features.subscription.usage.dto.CreateUsageRecordRequest;
import com.sni.bokaticowork.features.subscription.usage.service.UsageRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un collaborateur reserve sur l'abonnement de son organisation · le droit est decompte chez elle.
 */
class BookingCompanySeatTest {

    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final EntitlementGrantRepository grants = mock(EntitlementGrantRepository.class);
    private final CompanySeatLocator locator = mock(CompanySeatLocator.class);
    private final EntitlementService entitlements = mock(EntitlementService.class);
    private final UsageRecordService usages = mock(UsageRecordService.class);
    private final EntitlementDefinitionRepository definitions = mock(EntitlementDefinitionRepository.class);
    private final SeatEntitlementGate gate = mock(SeatEntitlementGate.class);

    private Subscription companySubscription() {
        Subscription subscription = new Subscription();
        subscription.setId(10L);
        subscription.setSubscriptionNumber("SUB-ACME");
        subscription.setSubscriberType(SubscriberType.CUSTOMER);
        subscription.setSubscriberCode("CUS-COM-1");
        return subscription;
    }

    @Test
    @DisplayName("Sans droit propre, la place sur l'abonnement de l'organisation est utilisee")
    void resolverUsesCompanySeat() {
        Subscription subscription = companySubscription();
        SubscriptionSeat seat = SubscriptionSeat.builder().id(99L).subscription(subscription).memberCode("MBR-1").build();
        EntitlementGrant grant = EntitlementGrant.builder()
                .entitlementDefinition(EntitlementDefinition.builder().code("SALLE_H").build()).build();
        when(subscriptions.findAllCurrentActive(eq("MEMBER"), eq("MBR-1"), any(LocalDate.class))).thenReturn(List.of());
        when(locator.usableSeats("MBR-1", null)).thenReturn(List.of(seat));
        when(grants.findUsableSubscriptionGrantForResource(eq("CUSTOMER"), eq("CUS-COM-1"), eq(10L), any(), any(), any()))
                .thenReturn(Optional.of(grant));
        BookingPaymentContextResolver resolver = new BookingPaymentContextResolver(subscriptions, grants, entitlements,
                mock(BookingOverageGuard.class), locator);

        var context = resolver.resolve(BookingPaymentMode.SUBSCRIPTION,
                new BookingIdentityResolver.ResolvedBookingIdentity(SubscriberType.MEMBER, "MBR-1", "Jean", null, null, false),
                Resource.builder().build(), null);

        assertThat(context.subscriptionNumber()).isEqualTo("SUB-ACME");
        assertThat(context.entitlementOwnerType()).isEqualTo(SubscriberType.CUSTOMER);
        assertThat(context.entitlementOwnerCode()).isEqualTo("CUS-COM-1");
        assertThat(context.seatId()).isEqualTo(99L);
    }

    private Booking seatBooking() {
        when(definitions.findByCodeIgnoreCase("SALLE_H"))
                .thenReturn(Optional.of(EntitlementDefinition.builder().code("SALLE_H").unit(EntitlementUnit.HOUR).build()));
        return Booking.builder().bookingNumber("BKG-1").entitlementCode("SALLE_H")
                .ownerType(SubscriberType.MEMBER).ownerCode("MBR-1")
                .entitlementOwnerType(SubscriberType.CUSTOMER).entitlementOwnerCode("CUS-COM-1").subscriptionSeatId(99L)
                .durationMinutes(120).bookingUnit(ResourceBookingUnit.HOUR).quantity(1).currency("XAF")
                .endedAt(LocalDateTime.of(2026, 11, 3, 12, 0))
                .build();
    }

    private BookingEntitlementBridge bridge() {
        return new BookingEntitlementBridge(entitlements, usages, definitions, subscriptions, mock(BookingOverageGuard.class), gate);
    }

    @Test
    @DisplayName("Retenue : la part de la place d'abord, puis le solde de l'organisation")
    void reserveChecksSeatThenCompanyBalance() {
        Booking booking = seatBooking();
        when(entitlements.check(any())).thenReturn(new EntitlementOperationResponse(true, "SALLE_H", null, null, null));

        bridge().reserve(booking);

        InOrder order = inOrder(gate, entitlements);
        order.verify(gate).reserve(eq(99L), eq("SALLE_H"), eq("BOOKING"), eq("BKG-1"), any(BigDecimal.class));
        ArgumentCaptor<EntitlementOperationRequest> request = ArgumentCaptor.forClass(EntitlementOperationRequest.class);
        order.verify(entitlements).reserve(request.capture());
        assertThat(request.getValue().ownerType()).isEqualTo(SubscriberType.CUSTOMER);
        assertThat(request.getValue().ownerCode()).isEqualTo("CUS-COM-1");
    }

    @Test
    @DisplayName("Cloture : l'usage est au nom de l'organisation, le collaborateur en beneficiaire")
    void consumeRecordsBeneficiary() {
        Booking booking = seatBooking();
        when(entitlements.coverable(any())).thenReturn(true);

        bridge().consume(booking);

        verify(gate).consume(eq(99L), eq("SALLE_H"), eq("BOOKING"), eq("BKG-1"), any(BigDecimal.class));
        ArgumentCaptor<CreateUsageRecordRequest> usage = ArgumentCaptor.forClass(CreateUsageRecordRequest.class);
        verify(usages).record(usage.capture());
        assertThat(usage.getValue().ownerCode()).isEqualTo("CUS-COM-1");
        assertThat(usage.getValue().beneficiaryType()).isEqualTo("MEMBER");
        assertThat(usage.getValue().beneficiaryCode()).isEqualTo("MBR-1");
    }

    @Test
    @DisplayName("Annulation : la retenue de la place est rendue")
    void releaseFreesSeat() {
        bridge().release(seatBooking());
        verify(gate).release(99L, "SALLE_H", "BOOKING", "BKG-1");
        verify(entitlements).release(any());
    }
}
