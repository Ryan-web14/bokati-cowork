package com.sni.bokaticowork.features.portal.context;

import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Un abonne, c'est un abonnement actif au nom du membre ou de son client.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriberAccessTest {

    private static final LocalDateTime EVENING = LocalDateTime.of(2026, 11, 3, 19, 0);

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private ResourceAvailabilityService availabilityService;
    @Mock private com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository memberRepository;
    @InjectMocks private SubscriberAccess access;

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setMemberId("MBR-1");
        Customer customer = new Customer();
        customer.setCustomerId("CUS-1");
        member.setCustomer(customer);
        // Le code du client se lit par requete · le membre arrive detache du portail.
        when(memberRepository.findCustomerIdByMemberId(member.getMemberId())).thenReturn(java.util.Optional.of("CUS-1"));
        when(subscriptionRepository.findAllCurrentActive(anyString(), anyString(), any())).thenReturn(List.of());
        when(availabilityService.touchesSubscribersOnly(eq("RES-1"), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("Un abonnement actif au nom du membre en fait un abonne")
    void ownSubscriptionCounts() {
        when(subscriptionRepository.findAllCurrentActive(eq("MEMBER"), eq("MBR-1"), any()))
                .thenReturn(List.of(new Subscription()));

        assertThat(access.isSubscriber(member)).isTrue();
    }

    @Test
    @DisplayName("L'abonnement de son client (une entreprise) le couvre aussi")
    void customerSubscriptionCounts() {
        when(subscriptionRepository.findAllCurrentActive(eq("CUSTOMER"), eq("CUS-1"), any()))
                .thenReturn(List.of(new Subscription()));

        assertThat(access.isSubscriber(member)).isTrue();
    }

    @Test
    @DisplayName("Sans abonnement actif, la soiree est refusee")
    void nonSubscriberIsDeniedTheEvening() {
        assertThat(access.isSubscriber(member)).isFalse();
        assertThatThrownBy(() -> access.assertMayBook(member, "RES-1", EVENING, EVENING.plusHours(1)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("réservé aux abonnés");
    }

    @Test
    @DisplayName("Une plage de journee reste ouverte a tous")
    void daytimeStaysOpen() {
        when(availabilityService.touchesSubscribersOnly(eq("RES-1"), any(), any())).thenReturn(false);

        assertThatCode(() -> access.assertMayBook(member, "RES-1", EVENING.minusHours(8), EVENING.minusHours(7)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un abonne reserve la soiree")
    void subscriberMayBookTheEvening() {
        when(subscriptionRepository.findAllCurrentActive(eq("MEMBER"), eq("MBR-1"), any()))
                .thenReturn(List.of(new Subscription()));

        assertThatCode(() -> access.assertMayBook(member, "RES-1", EVENING, EVENING.plusHours(1)))
                .doesNotThrowAnyException();
    }
}
