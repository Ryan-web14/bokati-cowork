package com.sni.bokaticowork.features.company.sharing.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.membership.service.MemberBillingCompanyResolver;
import com.sni.bokaticowork.features.company.sharing.repository.CompanySeatRepository;
import com.sni.bokaticowork.features.subscription.seat.model.SubscriptionSeat;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Quelle place une reservation utilise · organisation demandee, automatique, unique, ou choix.
 */
class CompanySeatLocatorTest {

    private final CompanySeatRepository seats = mock(CompanySeatRepository.class);
    private final CustomerMemberLinkRepository links = mock(CustomerMemberLinkRepository.class);
    private final MemberBillingCompanyResolver billing = mock(MemberBillingCompanyResolver.class);
    private final CompanySeatLocator locator = new CompanySeatLocator(seats, links, billing);

    private SubscriptionSeat acmeSeat;
    private SubscriptionSeat moboteSeat;

    private static SubscriptionSeat seat(String company) {
        Subscription subscription = new Subscription();
        subscription.setSubscriberType(SubscriberType.CUSTOMER);
        subscription.setSubscriberCode(company);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(LocalDate.now().minusDays(3));
        subscription.setCurrentPeriodEnd(LocalDate.now().plusDays(20));
        return SubscriptionSeat.builder().subscription(subscription).memberCode("MBR-1").build();
    }

    @BeforeEach
    void setUp() {
        acmeSeat = seat("CUS-COM-1");
        moboteSeat = seat("CUS-COM-2");
        when(links.findActiveCompanyCodes("MBR-1")).thenReturn(List.of("CUS-COM-1", "CUS-COM-2"));
        when(seats.findActiveCompanySeatsOfMember("MBR-1")).thenReturn(List.of(acmeSeat, moboteSeat));
        when(billing.resolve("MEMBER", "MBR-1", null)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("L'organisation demandee a la reservation")
    void requested() {
        assertThat(locator.usableSeats("MBR-1", "CUS-COM-2")).containsExactly(moboteSeat);
        assertThat(locator.usableSeats("MBR-1", "NONE")).isEmpty();
    }

    @Test
    @DisplayName("Sans demande : l'organisation en mode automatique")
    void automatic() {
        when(billing.resolve("MEMBER", "MBR-1", null)).thenReturn(Optional.of(Customer.builder().customerId("CUS-COM-1").build()));
        assertThat(locator.usableSeats("MBR-1", null)).containsExactly(acmeSeat);
    }

    @Test
    @DisplayName("Deux organisations possibles sans choix : le membre doit choisir")
    void ambiguous() {
        assertThatThrownBy(() -> locator.usableSeats("MBR-1", null))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("billingCompanyCode");
    }

    @Test
    @DisplayName("Lien termine ou abonnement hors periode : la place ne sert pas")
    void inactiveIsIgnored() {
        when(links.findActiveCompanyCodes("MBR-1")).thenReturn(List.of("CUS-COM-1"));
        acmeSeat.getSubscription().setCurrentPeriodEnd(LocalDate.now().minusDays(1));
        assertThat(locator.usableSeats("MBR-1", null)).isEmpty();
    }
}
