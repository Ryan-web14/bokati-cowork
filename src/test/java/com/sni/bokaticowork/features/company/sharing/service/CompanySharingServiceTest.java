package com.sni.bokaticowork.features.company.sharing.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.model.CustomerMemberLink;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.sharing.dto.SharingDtos;
import com.sni.bokaticowork.features.company.sharing.model.EntitlementSharing;
import com.sni.bokaticowork.features.company.sharing.repository.CompanySeatRepository;
import com.sni.bokaticowork.features.company.sharing.repository.EntitlementSharingRepository;
import com.sni.bokaticowork.features.company.sharing.repository.SeatAllowanceRepository;
import com.sni.bokaticowork.features.company.sharing.repository.SeatUsageRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.seat.enums.SeatStatus;
import com.sni.bokaticowork.features.subscription.seat.model.SubscriptionSeat;
import com.sni.bokaticowork.features.subscription.seat.repository.SubscriptionSeatRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Attribuer, ajuster et liberer les places d'un abonnement d'organisation.
 */
class CompanySharingServiceTest {

    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final CustomerMemberLinkRepository links = mock(CustomerMemberLinkRepository.class);
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final SubscriptionSeatRepository legacySeats = mock(SubscriptionSeatRepository.class);
    private final CompanySeatRepository seats = mock(CompanySeatRepository.class);
    private final EntitlementSharingRepository rules = mock(EntitlementSharingRepository.class);
    private final SeatAllowanceRepository allowances = mock(SeatAllowanceRepository.class);
    private final SeatUsageRepository usages = mock(SeatUsageRepository.class);
    private final EntitlementGrantRepository grants = mock(EntitlementGrantRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final SeatEntitlementGate gate = new SeatEntitlementGate(seats, rules, allowances, usages, grants);
    private final CompanySharingService service = new CompanySharingService(customers, members, links, subscriptions,
            legacySeats, seats, rules, allowances, usages, grants, bookings, gate);

    private Customer acme;
    private Subscription subscription;
    private Member jean;

    @BeforeEach
    void setUp() {
        acme = Customer.builder().id(1L).customerId("CUS-COM-1").companyName("SARL Acme").type(CustomerType.COMPANY).build();
        subscription = new Subscription();
        subscription.setId(10L);
        subscription.setSubscriptionNumber("SUB-ACME");
        subscription.setSubscriberType(SubscriberType.CUSTOMER);
        subscription.setSubscriberCode("CUS-COM-1");
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptions.findBySubscriptionNumber("SUB-ACME")).thenReturn(Optional.of(subscription));
        when(seats.lockSubscription(10L)).thenReturn(Optional.of(subscription));
        jean = new Member();
        jean.setMemberId("MBR-1");
        jean.setFirstname("Jean");
        jean.setLastname("Mbemba");
        when(members.findByMemberIdAndDeletedFalse("MBR-1")).thenReturn(Optional.of(jean));
        when(links.findByCustomerAndMemberAndStatus(acme, jean, MemberLinkStatus.ACTIVE))
                .thenReturn(Optional.of(CustomerMemberLink.builder().customer(acme).member(jean).build()));
        when(legacySeats.findActiveSeat(10L, "MBR-1")).thenReturn(Optional.empty());
        when(seats.save(any(SubscriptionSeat.class))).thenAnswer(call -> {
            SubscriptionSeat seat = call.getArgument(0);
            seat.setId(99L);
            return seat;
        });
        EntitlementGrant grant = EntitlementGrant.builder()
                .entitlementDefinition(EntitlementDefinition.builder().code("SALLE_H").name("Salle").build())
                .quantityGranted(new BigDecimal("20")).quantityRemaining(new BigDecimal("20")).build();
        when(grants.findAllBySubscription(10L)).thenReturn(List.of(grant));
        when(usages.sumSeatQuota(any(), anyString(), any())).thenReturn(BigDecimal.ZERO);
        when(usages.sumSeatTotal(any(), anyString(), any())).thenReturn(BigDecimal.ZERO);
        when(usages.sumPool(any(), anyString(), any())).thenReturn(BigDecimal.ZERO);
    }

    private static SharingDtos.AssignSeatRequest assign(String quota) {
        return new SharingDtos.AssignSeatRequest("MBR-1", quota == null ? null
                : List.of(new SharingDtos.AllowanceRequest("salle_h", new BigDecimal(quota), null)));
    }

    @Test
    @DisplayName("Un membre rattache recoit une place")
    void assignsSeat() {
        service.assignSeat(acme, "SUB-ACME", assign(null), "admin");
        verify(seats).save(any(SubscriptionSeat.class));
    }

    @Test
    @DisplayName("Un membre non rattache, ou deja place, est refuse")
    void refusesUnlinkedOrDuplicate() {
        when(links.findByCustomerAndMemberAndStatus(acme, jean, MemberLinkStatus.ACTIVE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.assignSeat(acme, "SUB-ACME", assign(null), "admin"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("rattaché");
        verify(seats, never()).save(any(SubscriptionSeat.class));
    }

    @Test
    @DisplayName("Toutes les places occupees : refus")
    void maxSeatsHolds() {
        subscription.setMaxSeats(3);
        when(seats.countActive(10L)).thenReturn(3L);
        assertThatThrownBy(() -> service.assignSeat(acme, "SUB-ACME", assign(null), "admin"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("occupées");
    }

    @Test
    @DisplayName("Les parts distribuees ne depassent pas ce que l'organisation a recu")
    void quotasMustFit() {
        when(rules.findBySubscription_IdAndEntitlementCodeIgnoreCase(10L, "SALLE_H")).thenReturn(Optional.of(
                EntitlementSharing.builder().subscription(subscription).entitlementCode("SALLE_H")
                        .mode(EntitlementSharing.Mode.QUOTA).build()));
        SubscriptionSeat placed = SubscriptionSeat.builder().id(99L).subscription(subscription).memberCode("MBR-1")
                .status(SeatStatus.ACTIVE).build();
        when(seats.findActiveBySubscription(10L)).thenReturn(List.of(placed));
        when(allowances.findAllBySeat_IdIn(any())).thenAnswer(call -> List.of(
                com.sni.bokaticowork.features.company.sharing.model.SeatAllowance.builder()
                        .seat(placed).entitlementCode("SALLE_H").quota(new BigDecimal("25")).build()));

        assertThatThrownBy(() -> service.assignSeat(acme, "SUB-ACME", assign("25"), "admin"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("dépassent");
    }

    @Test
    @DisplayName("Liberer une place signale ses reservations a venir sans les annuler")
    void releaseWarnsAboutFutureBookings() {
        SubscriptionSeat placed = SubscriptionSeat.builder().id(99L).shortId(7L).subscription(subscription).memberCode("MBR-1")
                .status(SeatStatus.ACTIVE).build();
        when(seats.findByShortId(7L)).thenReturn(Optional.of(placed));
        when(bookings.findFutureBookingNumbersOnSeats(eq(List.of(99L)), any())).thenReturn(List.of("BKG-9"));

        SharingDtos.ReleaseResult result = service.releaseSeat(acme, "SUB-ACME", 7L, "Départ");

        assertThat(placed.getStatus()).isEqualTo(SeatStatus.REMOVED);
        assertThat(placed.getRemovedReason()).isEqualTo("Départ");
        assertThat(result.futureBookings()).containsExactly("BKG-9");
        assertThat(result.warning()).contains("ne sont pas annulées");
    }

    @Test
    @DisplayName("L'abonnement d'une autre organisation n'est pas visible")
    void foreignSubscriptionIsHidden() {
        subscription.setSubscriberCode("CUS-COM-2");
        assertThatThrownBy(() -> service.view(acme, "SUB-ACME"))
                .isInstanceOf(com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Le gerant regle ses places sous le plafond vendu, pas au-dessus")
    void ownerSeatsStayUnderCeiling() {
        subscription.setSeatCeiling(10);
        subscription.setMaxSeats(10);
        when(seats.countActive(10L)).thenReturn(2L);

        service.setMaxSeatsByOwner(acme, "SUB-ACME", 4);
        assertThat(subscription.getMaxSeats()).isEqualTo(4);

        assertThatThrownBy(() -> service.setMaxSeatsByOwner(acme, "SUB-ACME", 12))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("administration");
        assertThatThrownBy(() -> service.setMaxSeatsByOwner(acme, "SUB-ACME", 1))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("occupées");

        service.setMaxSeatsByOwner(acme, "SUB-ACME", null);
        assertThat(subscription.getMaxSeats()).isEqualTo(10);
    }

    @Test
    @DisplayName("L'administration fixe ensemble le plafond vendu et le nombre de places")
    void adminSetsCeiling() {
        service.setMaxSeats(acme, "SUB-ACME", 6);
        assertThat(subscription.getSeatCeiling()).isEqualTo(6);
        assertThat(subscription.getMaxSeats()).isEqualTo(6);
    }
}
