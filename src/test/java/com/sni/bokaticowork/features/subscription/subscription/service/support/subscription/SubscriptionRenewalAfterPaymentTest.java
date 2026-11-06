package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractService;
import com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier;
import com.sni.bokaticowork.features.subscription.derivation.service.PlanDerivationService;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionDirectDebitService;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionGraceService;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingScheduleStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPeriodCalculator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionStatusManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un abonnement ne se renouvelle qu'une fois paye · la facture part a l'echeance, la periode, les
 * droits, le statut et le contrat ne suivent qu'au reglement.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionRenewalAfterPaymentTest {

    private static final String INVOICE = "FAC-RENEW-1";

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionBillingSupport billingSupport;
    @Mock private SubscriptionEventWriter eventWriter;
    @Mock private SubscriptionStatusManager statusManager;
    @Mock private EntitlementService entitlementService;
    @Mock private ContractService contractService;
    @Mock private SubscriptionEmailNotifier emailNotifier;
    @Mock private MemberInAppNotifier inAppNotifier;
    @Mock private OutboxService outboxService;
    @Mock private BillingDocumentRepository documents;
    @Mock private BillingDocumentService billing;
    @Mock private PlanDerivationService derivationService;
    @Mock private SubscriptionDirectDebitService directDebit;
    @Mock private SubscriptionGraceService graceService;
    @InjectMocks private SubscriptionLifecycleOperator operator;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(operator, "periodCalculator", new SubscriptionPeriodCalculator());
        ReflectionTestUtils.setField(operator, "self", operator);
        today = LocalDate.now();
        doAnswer(call -> {
            ((Subscription) call.getArgument(0)).setStatus(call.getArgument(1));
            return null;
        }).when(statusManager).changeStatus(any(), any(), anyString(), anyString());
        when(graceService.exit(any(), anyString())).thenAnswer(call -> {
            ((Subscription) call.getArgument(0)).setStatus(SubscriptionStatus.ACTIVE);
            return true;
        });
    }

    private Subscription subscription(LocalDate periodEnd, SubscriptionStatus status) {
        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setSubscriptionNumber("SUB-1");
        subscription.setStatus(status);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setCurrentPeriodStart(periodEnd.minusMonths(1).plusDays(1));
        subscription.setCurrentPeriodEnd(periodEnd);
        subscription.setNextBillingDate(periodEnd.plusDays(1));
        subscription.setSubtotalAmount(BigDecimal.valueOf(50000));
        subscription.setTaxAmount(BigDecimal.ZERO);
        subscription.setContractCode("CTR-1");
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        return subscription;
    }

    private void invoiceIssued(long balanceDue) {
        BillingDocumentResponse invoice = mock(BillingDocumentResponse.class);
        when(invoice.documentNumber()).thenReturn(INVOICE);
        when(invoice.balanceDue()).thenReturn(BigDecimal.valueOf(balanceDue));
        when(billingSupport.createBillableItem(any(), eq("SUBSCRIPTION_RENEWAL"), anyString(), any(), any(), any()))
                .thenReturn(invoice);
    }

    private BillingDocument invoiceWithBalance(long balanceDue) {
        BillingDocument invoice = BillingDocument.builder().documentNumber(INVOICE).currency("XAF")
                .status(balanceDue == 0 ? BillingDocumentStatus.PAID : BillingDocumentStatus.ISSUED)
                .balanceDue(BigDecimal.valueOf(balanceDue)).paidAmount(BigDecimal.valueOf(50000 - balanceDue)).build();
        when(documents.findByDocumentNumber(INVOICE)).thenReturn(Optional.of(invoice));
        return invoice;
    }

    private void pending(Subscription subscription) {
        LocalDate start = subscription.getCurrentPeriodEnd().plusDays(1);
        subscription.setPendingRenewalInvoice(INVOICE);
        subscription.setPendingRenewalStart(start);
        subscription.setPendingRenewalEnd(start.plusMonths(1).minusDays(1));
    }

    @Test
    @DisplayName("A l'echeance : facture de la periode a venir, mais periode, droits et contrat ne bougent pas")
    void renewalInvoicesWithoutMovingAnything() {
        LocalDate periodEnd = today.minusDays(1);
        Subscription subscription = subscription(periodEnd, SubscriptionStatus.ACTIVE);
        invoiceIssued(50000);
        invoiceWithBalance(50000);

        operator.renewOne(1L);

        LocalDate start = periodEnd.plusDays(1);
        verify(billingSupport).createBillableItem(eq(subscription), eq("SUBSCRIPTION_RENEWAL"), anyString(), any(),
                eq(start), eq(start.plusMonths(1).minusDays(1)));
        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(periodEnd);
        assertThat(subscription.getPendingRenewalInvoice()).isEqualTo(INVOICE);
        assertThat(subscription.getPendingRenewalStart()).isEqualTo(start);
        verify(directDebit).collectAfterCommit(1L, INVOICE);
        verify(entitlementService, never()).grantForSubscription(any());
        verify(outboxService, never()).publish(eq("CONTRACT_RENEWAL_AMENDMENT_REQUESTED"), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Sans prelevement et periode finie : l'abonne est prevenu de payer, l'abonnement n'est plus actif")
    void unpaidAtTheEndLeavesActive() {
        Subscription subscription = subscription(today.minusDays(1), SubscriptionStatus.ACTIVE);
        com.sni.bokaticowork.features.client.member.model.Member member = new com.sni.bokaticowork.features.client.member.model.Member();
        member.setEmail("awa@acme.cg");
        subscription.setMember(member);
        pending(subscription);
        invoiceWithBalance(50000);

        operator.awaitPayment(1L);

        verify(graceService).enterQuietly(eq(subscription), anyString());
        ArgumentCaptor<Map<String, Object>> payload = payloadCaptor();
        verify(outboxService).publish(eq("SUBSCRIPTION_RENEWAL_PAYMENT_DUE"), eq("SUBSCRIPTION"), eq("SUB-1"), payload.capture());
        assertThat(payload.getValue()).containsEntry("invoiceNumber", INVOICE).containsEntry("amount", "50000")
                .containsEntry("templateCode", "subscription_renewal_payment_due");
    }

    @Test
    @DisplayName("Prelevement deja tente : son message suffit, pas de second avis")
    void noSecondNoticeAfterAFailedDebit() {
        Subscription subscription = subscription(today.minusDays(1), SubscriptionStatus.ACTIVE);
        pending(subscription);
        invoiceWithBalance(50000);
        when(directDebit.attempted(1L, INVOICE)).thenReturn(true);

        operator.awaitPayment(1L);

        verify(outboxService, never()).publish(eq("SUBSCRIPTION_RENEWAL_PAYMENT_DUE"), anyString(), anyString(), any());
        verify(graceService).enterQuietly(eq(subscription), anyString());
    }

    @Test
    @DisplayName("Renouvele par avance : l'abonnement reste actif jusqu'a la fin de sa periode")
    void earlyRenewalStaysActiveUntilThePeriodEnds() {
        Subscription subscription = subscription(today.plusDays(4), SubscriptionStatus.ACTIVE);
        pending(subscription);
        invoiceWithBalance(50000);

        operator.awaitPayment(1L);

        verify(graceService, never()).enterQuietly(any(), anyString());
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Paye en tolerance : periode reprise du jour du paiement, actif, droits ouverts, avenant demande")
    void paymentInGraceRenews() {
        LocalDate periodEnd = today.minusDays(3);
        Subscription subscription = subscription(periodEnd, SubscriptionStatus.GRACE_PERIOD);
        pending(subscription);
        invoiceWithBalance(0);

        assertThat(operator.applyPaidRenewal(subscription, "Paiement TXN-1")).isTrue();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getCurrentPeriodStart()).isEqualTo(today);
        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(today.plusMonths(1).minusDays(1));
        assertThat(subscription.getNextBillingDate()).isEqualTo(today.plusMonths(1));
        assertThat(subscription.getPendingRenewalInvoice()).isNull();
        verify(entitlementService).grantForSubscription(subscription);
        verify(billingSupport).upsertBillingSchedule(subscription, BillingScheduleStatus.ACTIVE);
        ArgumentCaptor<Map<String, Object>> payload = payloadCaptor();
        verify(outboxService).publish(eq("CONTRACT_RENEWAL_AMENDMENT_REQUESTED"), eq("SUBSCRIPTION"), eq("SUB-1"), payload.capture());
        assertThat(payload.getValue()).containsEntry("contractCode", "CTR-1")
                .containsEntry("previousPeriodEnd", periodEnd.toString())
                .containsEntry("newPeriodStart", today.toString());
    }

    @Test
    @DisplayName("Paye d'avance : la nouvelle periode suit la precedente, sans jour perdu")
    void earlyPaymentFollowsThePreviousPeriod() {
        LocalDate periodEnd = today.plusDays(4);
        Subscription subscription = subscription(periodEnd, SubscriptionStatus.ACTIVE);
        pending(subscription);
        invoiceWithBalance(0);

        assertThat(operator.applyPaidRenewal(subscription, "Paiement TXN-1")).isTrue();

        assertThat(subscription.getCurrentPeriodStart()).isEqualTo(periodEnd.plusDays(1));
    }

    @Test
    @DisplayName("Paye apres la suspension : l'abonnement revient actif")
    void paymentReactivatesSuspended() {
        Subscription subscription = subscription(today.minusDays(6), SubscriptionStatus.SUSPENDED);
        subscription.setSuspensionReason("Échéance impayée");
        pending(subscription);
        invoiceWithBalance(0);

        assertThat(operator.applyPaidRenewal(subscription, "Paiement TXN-1")).isTrue();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getSuspensionReason()).isNull();
    }

    @Test
    @DisplayName("Paiement partiel : rien ne prend effet")
    void partialPaymentRenewsNothing() {
        LocalDate periodEnd = today.minusDays(1);
        Subscription subscription = subscription(periodEnd, SubscriptionStatus.GRACE_PERIOD);
        pending(subscription);
        invoiceWithBalance(20000);

        assertThat(operator.applyPaidRenewal(subscription, "Paiement TXN-1")).isFalse();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.GRACE_PERIOD);
        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(periodEnd);
        assertThat(subscription.getPendingRenewalInvoice()).isEqualTo(INVOICE);
    }

    @Test
    @DisplayName("Un abonnement clos ne revit pas par un paiement")
    void closedSubscriptionDoesNotRevive() {
        Subscription subscription = subscription(today.minusDays(9), SubscriptionStatus.EXPIRED);
        pending(subscription);
        invoiceWithBalance(0);

        assertThat(operator.applyPaidRenewal(subscription, "Paiement TXN-1")).isFalse();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    @DisplayName("Rien a regler : le renouvellement prend effet aussitot")
    void nothingToPayRenewsAtOnce() {
        LocalDate periodEnd = today.minusDays(1);
        Subscription subscription = subscription(periodEnd, SubscriptionStatus.ACTIVE);
        invoiceIssued(0);

        operator.renewOne(1L);

        assertThat(subscription.getCurrentPeriodStart()).isEqualTo(periodEnd.plusDays(1));
        assertThat(subscription.getPendingRenewalInvoice()).isNull();
        verify(entitlementService).grantForSubscription(subscription);
    }

    @Test
    @DisplayName("Renouveler deux fois n'emet pas deux factures")
    void renewalIsIdempotent() {
        Subscription subscription = subscription(today.plusDays(4), SubscriptionStatus.ACTIVE);
        pending(subscription);

        operator.renew(subscription);

        verify(billingSupport, never()).createBillableItem(any(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("L'abonnement prend fin : la facture impayee est annulee, une facture en partie reglee reste")
    void endingDropsTheUnpaidInvoice() {
        Subscription subscription = subscription(today.minusDays(9), SubscriptionStatus.SUSPENDED);
        pending(subscription);
        invoiceWithBalance(50000).setPaidAmount(BigDecimal.ZERO);

        operator.dropPendingRenewal(subscription, "Période échue");

        verify(billing).cancelAndArchive(INVOICE, "Période échue");
        assertThat(subscription.getPendingRenewalInvoice()).isNull();

        Subscription partial = subscription(today.minusDays(9), SubscriptionStatus.SUSPENDED);
        pending(partial);
        org.mockito.Mockito.clearInvocations(billing);
        invoiceWithBalance(20000);

        operator.dropPendingRenewal(partial, "Période échue");

        verify(billing, never()).cancelAndArchive(anyString(), anyString());
    }

    @Test
    @DisplayName("Le balayage rattrape un renouvellement paye que le paiement n'a pas applique")
    void sweepCatchesUpSettledRenewals() {
        Subscription subscription = subscription(today.minusDays(1), SubscriptionStatus.GRACE_PERIOD);
        pending(subscription);
        invoiceWithBalance(0);
        when(subscriptionRepository.findWithPendingRenewal()).thenReturn(java.util.List.of(subscription));

        operator.renewDueSubscriptions();

        assertThat(subscription.getPendingRenewalInvoice()).isNull();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Rattrapage : un contrat echu sur un abonnement qui court recoit un avenant unique jusqu'a la fin de periode")
    void expiredContractIsCaughtUp() {
        LocalDate periodEnd = today.plusDays(40);
        Subscription subscription = subscription(periodEnd, SubscriptionStatus.ACTIVE);
        LocalDate contractEnd = today.minusDays(50);
        com.sni.bokaticowork.features.contract.model.Contract contract = com.sni.bokaticowork.features.contract.model.Contract.builder()
                .contractCode("CTR-1").startDate(contractEnd.minusMonths(1)).endDate(contractEnd)
                .status(com.sni.bokaticowork.features.contract.enums.ContractStatus.EXPIRED).build();
        when(contractService.serviceByCode("CTR-1")).thenReturn(contract);
        when(subscriptionRepository.findIdsWithContractBehindPeriod()).thenReturn(java.util.List.of(1L));

        assertThat(operator.catchUpContractTerms()).isEqualTo(1);

        ArgumentCaptor<Map<String, Object>> payload = payloadCaptor();
        verify(outboxService).publish(eq("CONTRACT_RENEWAL_AMENDMENT_REQUESTED"), eq("SUBSCRIPTION"), eq("SUB-1"), payload.capture());
        assertThat(payload.getValue()).containsEntry("contractCode", "CTR-1")
                .containsEntry("previousPeriodEnd", contractEnd.toString())
                .containsEntry("newPeriodStart", contractEnd.plusDays(1).toString())
                .containsEntry("newPeriodEnd", periodEnd.toString());
        verify(subscriptionRepository, never()).save(subscription);
    }

    @Test
    @DisplayName("Un contrat deja a jour n'appelle aucun avenant")
    void contractUpToDateIsLeftAlone() {
        LocalDate periodEnd = today.plusDays(40);
        subscription(periodEnd, SubscriptionStatus.ACTIVE);
        when(contractService.serviceByCode("CTR-1")).thenReturn(com.sni.bokaticowork.features.contract.model.Contract.builder()
                .contractCode("CTR-1").endDate(periodEnd).build());

        operator.catchUpContractTerm(1L);

        verify(outboxService, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Une extension en cours de periode porte la date de fin du contrat, sans avenant")
    void extensionFollowsOnContract() {
        LocalDate periodStart = today.minusDays(10);
        Subscription subscription = subscription(periodStart.plusMonths(1).minusDays(1), SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(periodStart);

        operator.realignOnLatePayment(subscription, today);

        verify(contractService).extendEndDate(eq("CTR-1"), eq(today.plusMonths(1).minusDays(1)), anyString());
        verify(outboxService, never()).publish(eq("CONTRACT_RENEWAL_AMENDMENT_REQUESTED"), anyString(), anyString(), any());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Map<String, Object>> payloadCaptor() {
        return ArgumentCaptor.forClass((Class) Map.class);
    }
}
