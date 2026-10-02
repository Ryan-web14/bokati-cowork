package com.sni.bokaticowork.features.portal.subscription.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le client renouvelle depuis son espace · dans la fenetre, et pas avant.
 *
 * <p>Un abonnement a renouvellement manuel etait annonce a J-7, J-3 et le jour meme, mais le
 * client n'avait aucun moyen d'agir : il fallait appeler ou passer a l'accueil. Les avis disaient
 * quoi faire sans qu'il soit possible de le faire.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientSubscriptionRenewalTest {

    @Mock private SubscriptionService subscriptionService;
    @InjectMocks private ClientSubscriptionService service;

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setMemberId("MBR-1");
    }

    private SubscriptionResponse subscription(LocalDate periodEnd, SubscriptionStatus status) {
        return new SubscriptionResponse(
                "SUB-1", SubscriberType.MEMBER, "MBR-1", "Joël Bikindou",
                "COWORK", "Bureau partagé", 1, status,
                LocalDate.now().minusMonths(1), periodEnd.minusMonths(1), periodEnd,
                periodEnd.plusDays(1), false, null, "XAF",
                null, null, null, false, null, null, null, null, null);
    }

    private void current(SubscriptionResponse subscription) {
        when(subscriptionService.get("SUB-1")).thenReturn(subscription);
        when(subscriptionService.renew("SUB-1")).thenReturn(subscription);
    }

    @Test
    @DisplayName("Dans les sept jours qui précèdent la fin, le renouvellement passe")
    void renewalIsOpenInsideTheNoticeWindow() {
        current(subscription(LocalDate.now().plusDays(5), SubscriptionStatus.ACTIVE));

        service.renewSubscription(member, "SUB-1");

        verify(subscriptionService).renew("SUB-1");
    }

    @Test
    @DisplayName("Le jour même de la fin, il passe encore")
    void renewalIsStillOpenOnTheLastDay() {
        current(subscription(LocalDate.now(), SubscriptionStatus.ACTIVE));

        service.renewSubscription(member, "SUB-1");

        verify(subscriptionService).renew("SUB-1");
    }

    @Test
    @DisplayName("Trop tôt · on dit à partir de quand, au lieu d'avancer la période sans raison")
    void renewalTooEarlyIsRefusedWithTheOpeningDate() {
        LocalDate periodEnd = LocalDate.now().plusDays(25);
        current(subscription(periodEnd, SubscriptionStatus.ACTIVE));

        assertThatThrownBy(() -> service.renewSubscription(member, "SUB-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(periodEnd.toString())
                .hasMessageContaining(periodEnd.minusDays(7).toString());

        verify(subscriptionService, never()).renew(anyString());
    }

    @Test
    @DisplayName("Un abonnement qui n'est plus actif ne se renouvelle pas depuis l'espace client")
    void anInactiveSubscriptionCannotBeRenewed() {
        current(subscription(LocalDate.now().minusDays(2), SubscriptionStatus.CANCELLED));

        assertThatThrownBy(() -> service.renewSubscription(member, "SUB-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CANCELLED");

        verify(subscriptionService, never()).renew(anyString());
    }

    @Test
    @DisplayName("L'abonnement d'un autre membre n'est pas renouvelable")
    void anotherMembersSubscriptionIsRefused() {
        Member other = new Member();
        other.setMemberId("MBR-9");
        current(subscription(LocalDate.now().plusDays(2), SubscriptionStatus.ACTIVE));

        assertThatThrownBy(() -> service.renewSubscription(other, "SUB-1"))
                .isInstanceOf(RuntimeException.class);

        verify(subscriptionService, never()).renew(anyString());
    }

    @Test
    @DisplayName("Renouveler sans rien à facturer reconduit quand même la période")
    void aRenewalWithNothingToInvoiceStillSucceeds() {
        // findPayableInvoice ne trouve rien · la periode est reconduite, c'est l'essentiel.
        current(subscription(LocalDate.now().plusDays(1), SubscriptionStatus.ACTIVE));

        var response = service.renewSubscription(member, "SUB-1");

        assertThat(response.subscription()).isNotNull();
        assertThat(response.invoice()).isNull();
    }
}
