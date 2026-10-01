package com.sni.bokaticowork.features.portal.subscription.service;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.subscription.change.dto.CreateSubscriptionChangeRequest;
import com.sni.bokaticowork.features.subscription.change.dto.SubscriptionChangeResponse;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeEffectivePolicy;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeStatus;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeType;
import com.sni.bokaticowork.features.subscription.change.service.SubscriptionChangeRequestService;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionDebitMandate;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionQuote;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionTermination;
import com.sni.bokaticowork.features.subscription.lifecycle.service.*;
import com.sni.bokaticowork.features.subscription.repository.PlanPriceRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPlanResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Le membre n'agit que sur ce qui est à lui · et par les mêmes règles que le guichet.
 *
 * <p>Protégé ici : un abonnement d'un autre n'existe pas, un changement de plan est appliqué tout
 * de suite ou approuvé pour la prochaine échéance, le gel respecte le préavis de la politique, le
 * préavis de résiliation part avec le canal PORTAL et le membre comme demandeur.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientSubscriptionSelfServiceTest {

    @Mock private SubscriptionService subscriptionService;
    @Mock private SubscriptionChangeRequestService changeService;
    @Mock private SubscriptionPlanResolver planResolver;
    @Mock private PlanPriceRepository planPriceRepository;
    @Mock private PlanChangeProrationService prorationService;
    @Mock private SubscriptionFreezeService freezeService;
    @Mock private SubscriptionPolicyService policyService;
    @Mock private SubscriptionTerminationService terminationService;
    @Mock private SubscriptionCommitmentService commitmentService;
    @Mock private SubscriptionDirectDebitService directDebitService;
    @Mock private SubscriptionQuoteService quoteService;

    @InjectMocks
    private ClientSubscriptionSelfService service;

    private Member member;
    private Subscription subscription;
    private PlanVersion target;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setMemberId("MBR-1");
        PlanVersion current = PlanVersion.builder().id(10L).plan(SubscriptionPlan.builder().code("FLEX").build()).name("Flex").build();
        subscription = Subscription.builder().id(1L).subscriptionNumber("SUB-1").subscriberType(SubscriberType.MEMBER).subscriberCode("MBR-1")
                .status(SubscriptionStatus.ACTIVE).planVersion(current).billingCycle(BillingCycle.MONTHLY)
                .subtotalAmount(new BigDecimal("30000")).nextBillingDate(LocalDate.now().plusDays(10)).build();
        target = PlanVersion.builder().id(20L).plan(SubscriptionPlan.builder().code("PRO").build()).name("Pro").build();
        when(subscriptionService.getForService("SUB-1")).thenReturn(subscription);
        when(planResolver.resolvePlanVersion("PRO", null)).thenReturn(target);
        when(planPriceRepository.findAllByPlanVersion(20L)).thenReturn(List.of(
                PlanPrice.builder().billingCycle(BillingCycle.MONTHLY).amount(new BigDecimal("60000")).build()));
        when(policyService.current()).thenReturn(SubscriptionPolicy.builder().freezeNoticeDays(0).build());
        SubscriptionChangeResponse requested = response("CHG-1", SubscriptionChangeStatus.REQUESTED);
        when(changeService.request(eq("SUB-1"), any())).thenReturn(requested);
        when(changeService.approve("CHG-1", "MBR-1")).thenReturn(response("CHG-1", SubscriptionChangeStatus.APPROVED));
        when(changeService.apply("CHG-1")).thenReturn(response("CHG-1", SubscriptionChangeStatus.APPLIED));
    }

    private static SubscriptionChangeResponse response(String number, SubscriptionChangeStatus status) {
        return new SubscriptionChangeResponse(number, "SUB-1", SubscriptionChangeType.UPGRADE, 10L, 20L, SubscriptionChangeEffectivePolicy.IMMEDIATE,
                LocalDate.now(), BigDecimal.ZERO, null, false, null, status, null, "MBR-1", null, null);
    }

    @Test
    void someoneElsesSubscriptionDoesNotExist() {
        subscription.setSubscriberCode("MBR-2");
        assertThrows(ResourceNotFoundException.class, () -> service.freezeTerms(member, "SUB-1"));
        assertThrows(ResourceNotFoundException.class, () -> service.changePlan(member, "SUB-1", "PRO", false, null));
        verify(changeService, never()).request(anyString(), any());
    }

    @Test
    void immediateChangeIsAppliedAndScheduledChangeIsApprovedForTheWorker() {
        SubscriptionChangeResponse now = service.changePlan(member, "SUB-1", "PRO", false, null);
        assertEquals(SubscriptionChangeStatus.APPLIED, now.status());

        ArgumentCaptor<CreateSubscriptionChangeRequest> request = ArgumentCaptor.forClass(CreateSubscriptionChangeRequest.class);
        verify(changeService).request(eq("SUB-1"), request.capture());
        assertEquals(SubscriptionChangeType.UPGRADE, request.getValue().changeType());
        assertEquals(SubscriptionChangeEffectivePolicy.IMMEDIATE, request.getValue().effectivePolicy());
        assertEquals("MBR-1", request.getValue().requestedBy());

        SubscriptionChangeResponse later = service.changePlan(member, "SUB-1", "PRO", true, "moins cher");
        assertEquals(SubscriptionChangeStatus.APPROVED, later.status());
        verify(changeService, times(1)).apply("CHG-1");
    }

    @Test
    void changeIsRefusedDuringNoticeOrWithoutPriceForTheCycle() {
        subscription.setStatus(SubscriptionStatus.PENDING_TERMINATION);
        assertThrows(ConflictException.class, () -> service.changePlan(member, "SUB-1", "PRO", false, null));

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setBillingCycle(BillingCycle.YEARLY);
        assertThrows(ConflictException.class, () -> service.changePlan(member, "SUB-1", "PRO", false, null));
    }

    @Test
    void freezeFromThePortalRespectsThePolicyNotice() {
        when(freezeService.allowance(subscription)).thenReturn(new SubscriptionFreezeService.Allowance(0, 2, 60, BigDecimal.ZERO, true));
        assertTrue(service.freezeTerms(member, "SUB-1").canFreezeNow());

        when(policyService.current()).thenReturn(SubscriptionPolicy.builder().freezeNoticeDays(5).build());
        assertFalse(service.freezeTerms(member, "SUB-1").canFreezeNow());
        assertThrows(ConflictException.class, () -> service.freeze(member, "SUB-1", LocalDate.now().plusDays(10), "vacances"));
        verify(subscriptionService, never()).pause(anyString(), any());
    }

    @Test
    void terminationRequestCarriesThePortalChannelAndTheMember() {
        when(terminationService.request(eq("SUB-1"), any(), eq("MBR-1")))
                .thenReturn(SubscriptionTermination.builder().terminationCode("TRM-1").subscription(subscription).build());

        service.requestTermination(member, "SUB-1", null, "je déménage", null);

        ArgumentCaptor<SubscriptionTerminationService.Request> request = ArgumentCaptor.forClass(SubscriptionTerminationService.Request.class);
        verify(terminationService).request(eq("SUB-1"), request.capture(), eq("MBR-1"));
        assertEquals(SubscriptionTermination.Channel.PORTAL, request.getValue().channel());
        assertEquals(SubscriptionTermination.ReasonCategory.OTHER, request.getValue().reasonCategory());
        assertNull(request.getValue().noticePeriodDays(), "le membre ne choisit pas son préavis");
    }

    @Test
    void mandateFromThePortalIsAConsentOfTheMember() {
        when(directDebitService.give(eq("SUB-1"), any(), eq("MBR-1"))).thenReturn(SubscriptionDebitMandate.builder().mandateCode("MDT-1").build());

        service.giveMandate(member, "SUB-1", "WAL-1", null);

        ArgumentCaptor<SubscriptionDirectDebitService.Consent> consent = ArgumentCaptor.forClass(SubscriptionDirectDebitService.Consent.class);
        verify(directDebitService).give(eq("SUB-1"), consent.capture(), eq("MBR-1"));
        assertEquals(SubscriptionDebitMandate.Channel.PORTAL, consent.getValue().channel());
        assertEquals("portal:MBR-1", consent.getValue().reference());
    }

    @Test
    void quotesOfOthersAreInvisible() {
        SubscriptionQuote mine = SubscriptionQuote.builder().quoteNumber("QTE-1").subscriberType(SubscriberType.MEMBER).subscriberCode("MBR-1").build();
        SubscriptionQuote theirs = SubscriptionQuote.builder().quoteNumber("QTE-2").subscriberType(SubscriberType.MEMBER).subscriberCode("MBR-2").build();
        when(quoteService.get("QTE-1")).thenReturn(mine);
        when(quoteService.get("QTE-2")).thenReturn(theirs);
        when(quoteService.accept("QTE-1", "MBR-1")).thenReturn(mine);

        assertSame(mine, service.acceptQuote(member, "QTE-1"));
        assertThrows(ResourceNotFoundException.class, () -> service.acceptQuote(member, "QTE-2"));
        verify(quoteService, never()).accept(eq("QTE-2"), anyString());
    }
}
