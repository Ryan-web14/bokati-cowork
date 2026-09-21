package com.sni.bokaticowork.features.billing.dunning.service;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.billing.dunning.model.DunningNotice;
import com.sni.bokaticowork.features.billing.dunning.model.DunningPolicy;
import com.sni.bokaticowork.features.billing.dunning.model.DunningStep;
import com.sni.bokaticowork.features.billing.dunning.repository.DunningNoticeRepository;
import com.sni.bokaticowork.features.billing.dunning.repository.DunningPolicyRepository;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionGraceService;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionLifecycleOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Chaque palier, une fois · au bon jour, pour le bon segment.
 *
 * <p>Protégé ici : un palier n'est exécuté que si son jour est atteint et s'il ne l'a pas déjà été,
 * la politique du segment prime sur la politique par défaut, la suspension ne touche qu'un
 * abonnement encore ouvert, et la remise à une personne est écrite même quand elle ne peut pas
 * partir.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DunningRunnerTest {

    @Mock private DunningPolicyRepository policyRepository;
    @Mock private DunningNoticeRepository noticeRepository;
    @Mock private BillingDocumentRepository documentRepository;
    @Mock private BillableItemRepository billableItemRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private OutboxService outboxService;
    @Mock private MemberInAppNotifier inAppNotifier;
    @Mock private SubscriptionGraceService graceService;
    @Mock private SubscriptionLifecycleOperator lifecycleOperator;

    @InjectMocks
    private DunningRunner runner;

    private DunningPolicy defaultPolicy;
    private DunningPolicy businessPolicy;
    private BillingDocument invoice;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(runner, "handoverEmail", "");
        ReflectionTestUtils.setField(runner, "batchSize", 100);
        defaultPolicy = DunningPolicy.builder().id(1L).policyCode("DNP-DEFAULT").segment(DunningPolicy.Segment.DEFAULT).tone(DunningPolicy.Tone.STANDARD).build();
        defaultPolicy.getSteps().addAll(List.of(
                step(11L, defaultPolicy, 1, 3, DunningStep.Action.REMINDER),
                step(12L, defaultPolicy, 2, 10, DunningStep.Action.REMINDER),
                step(13L, defaultPolicy, 3, 30, DunningStep.Action.SUSPEND),
                step(14L, defaultPolicy, 4, 45, DunningStep.Action.HANDOVER)));
        businessPolicy = DunningPolicy.builder().id(2L).policyCode("DNP-BUSINESS").segment(DunningPolicy.Segment.BUSINESS_ENTITY).tone(DunningPolicy.Tone.SOFT).build();
        businessPolicy.getSteps().add(step(21L, businessPolicy, 1, 7, DunningStep.Action.REMINDER));
        when(policyRepository.findFirstBySegmentAndActiveTrue(DunningPolicy.Segment.DEFAULT)).thenReturn(Optional.of(defaultPolicy));
        when(policyRepository.findFirstBySegmentAndActiveTrue(DunningPolicy.Segment.BUSINESS_ENTITY)).thenReturn(Optional.of(businessPolicy));
        when(policyRepository.findFirstBySegmentAndActiveTrue(DunningPolicy.Segment.MEMBER)).thenReturn(Optional.empty());
        when(noticeRepository.existsByDocumentNumberAndStep_Id(anyString(), any())).thenReturn(false);
        when(noticeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        invoice = new BillingDocument();
        invoice.setId(500L);
        invoice.setDocumentNumber("INV-1");
        invoice.setCustomerType("MEMBER");
        invoice.setCustomerCode("MBR-1");
        invoice.setCustomerName("Alice");
        invoice.setCustomerEmail("alice@example.com");
        invoice.setCurrency("XAF");
        invoice.setBalanceDue(new BigDecimal("30000"));
        invoice.setTotalAmount(new BigDecimal("30000"));
        invoice.setDueDate(LocalDate.now().minusDays(12));

        subscription = Subscription.builder().id(1L).subscriptionNumber("SUB-1").status(SubscriptionStatus.ACTIVE).build();
        when(billableItemRepository.findByInvoiceId(500L)).thenReturn(List.of(
                BillableItem.builder().sourceType("SUBSCRIPTION_RENEWAL").sourceId("SUB-1").build()));
        when(subscriptionRepository.findBySubscriptionNumber("SUB-1")).thenReturn(Optional.of(subscription));
    }

    private static DunningStep step(long id, DunningPolicy policy, int order, int days, DunningStep.Action action) {
        return DunningStep.builder().id(id).policy(policy).stepOrder(order).daysAfterDue(days).action(action)
                .channel(action == DunningStep.Action.HANDOVER ? DunningStep.Channel.STAFF : DunningStep.Channel.EMAIL)
                .subjectTemplate("Palier " + order + " · {documentNumber}").messageTemplate("{balanceDue} {currency} depuis {daysOverdue} jours").build();
    }

    @Test
    void onlyReachedAndNotYetExecutedStepsAreDue() {
        List<DunningStep> due = runner.dueSteps(invoice);
        assertEquals(List.of(11L, 12L), due.stream().map(DunningStep::getId).toList(), "12 jours de retard · deux rappels, pas la suspension");

        when(noticeRepository.existsByDocumentNumberAndStep_Id("INV-1", 11L)).thenReturn(true);
        assertEquals(List.of(12L), runner.dueSteps(invoice).stream().map(DunningStep::getId).toList());
    }

    @Test
    void segmentPolicyWinsOverDefaultAndMemberFallsBackToDefault() {
        invoice.setCustomerType("BUSINESS_ENTITY");
        assertEquals(List.of(21L), runner.dueSteps(invoice).stream().map(DunningStep::getId).toList());

        invoice.setCustomerType("MEMBER");
        assertEquals("DNP-DEFAULT", runner.policyFor("MEMBER").orElseThrow().getPolicyCode());
        assertEquals("DNP-DEFAULT", runner.policyFor("something-else").orElseThrow().getPolicyCode());
    }

    @Test
    void reminderIsPublishedWithRenderedTemplatesAndTone() {
        DunningNotice notice = runner.execute(invoice, defaultPolicy.getSteps().getFirst());

        assertEquals(DunningNotice.Outcome.SENT, notice.getOutcome());
        assertEquals("SUB-1", notice.getSubscriptionNumber());
        assertEquals(12, notice.getDaysOverdue());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(outboxService).publish(eq("BILLING_DUNNING_REMINDER"), eq("BILLING_DOCUMENT"), eq("INV-1"), payload.capture());
        assertEquals("alice@example.com", payload.getValue().get("recipientEmail"));
        assertEquals("Palier 1 · INV-1", payload.getValue().get("subject"));
        assertEquals("30000 XAF depuis 12 jours", payload.getValue().get("message"));
        assertEquals("STANDARD", payload.getValue().get("tone"));
        verify(inAppNotifier).notify(eq("BILLING_DUNNING_REMINDER"), eq("BILLING_DOCUMENT"), eq("INV-1"), eq("alice@example.com"), eq("Alice"),
                eq("MBR-1"), eq("Palier 1 · INV-1"), any());
    }

    @Test
    void withoutContactTheReminderIsWrittenAsSkipped() {
        invoice.setCustomerEmail(null);
        invoice.setCustomerType("CUSTOMER");
        subscription.setMember(null);

        DunningNotice notice = runner.execute(invoice, defaultPolicy.getSteps().getFirst());

        assertEquals(DunningNotice.Outcome.SKIPPED, notice.getOutcome());
        verify(outboxService, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    void suspendTouchesOnlyAnOpenSubscription() {
        DunningStep suspend = defaultPolicy.getSteps().get(2);

        DunningNotice notice = runner.execute(invoice, suspend);
        assertEquals(DunningNotice.Outcome.SENT, notice.getOutcome());
        verify(lifecycleOperator).suspend(eq(subscription), any());

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        notice = runner.execute(invoice, suspend);
        assertEquals(DunningNotice.Outcome.SKIPPED, notice.getOutcome());
        assertTrue(notice.getDetail().contains("CANCELLED"));
        verify(lifecycleOperator, times(1)).suspend(any(), any());
    }

    @Test
    void invoiceWithoutSubscriptionSkipsSubscriptionActions() {
        when(billableItemRepository.findByInvoiceId(500L)).thenReturn(List.of());

        DunningNotice notice = runner.execute(invoice, defaultPolicy.getSteps().get(2));

        assertEquals(DunningNotice.Outcome.SKIPPED, notice.getOutcome());
        assertNull(notice.getSubscriptionNumber());
        verify(lifecycleOperator, never()).suspend(any(), any());
    }

    @Test
    void handoverNeedsAnAddressAndIsWrittenEitherWay() {
        DunningStep handover = defaultPolicy.getSteps().get(3);

        DunningNotice notice = runner.execute(invoice, handover);
        assertEquals(DunningNotice.Outcome.SKIPPED, notice.getOutcome());
        assertTrue(notice.getDetail().contains("handover-email"));

        ReflectionTestUtils.setField(runner, "handoverEmail", "compta@example.com");
        notice = runner.execute(invoice, handover);
        assertEquals(DunningNotice.Outcome.SENT, notice.getOutcome());
        verify(outboxService).publish(eq("BILLING_DUNNING_HANDOVER"), eq("BILLING_DOCUMENT"), eq("INV-1"), any());
    }

    @Test
    void failureOfAnActionIsWrittenNotThrown() {
        doThrow(new IllegalStateException("plus de contrat")).when(lifecycleOperator).suspend(any(), any());

        DunningNotice notice = runner.execute(invoice, defaultPolicy.getSteps().get(2));

        assertEquals(DunningNotice.Outcome.FAILED, notice.getOutcome());
        assertEquals("plus de contrat", notice.getDetail());
    }

    @Test
    void nothingRunsWithoutAnActivePolicy() {
        when(policyRepository.existsByActiveTrue()).thenReturn(false);
        assertTrue(runner.overdueDocuments().isEmpty());
        verify(documentRepository, never()).findOverdueWithBalance(anyInt());
    }
}
