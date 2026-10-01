package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivation;
import com.sni.bokaticowork.features.subscription.derivation.service.PlanDerivationService;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionCommitment;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionQuote;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionQuoteRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPlanResolver;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionCreationOperator;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Le devis ne contourne rien · il anticipe.
 *
 * <p>Protégé ici : un prix négocié est vérifié contre les règles de dérivation dès le devis, un
 * devis périmé ne se convertit pas, et l'acceptation crée l'abonnement, la dérivation et
 * l'engagement dans cet ordre.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionQuoteServiceTest {

    @Mock private SubscriptionQuoteRepository quoteRepository;
    @Mock private SubscriptionPlanResolver planResolver;
    @Mock private SubscriptionOwnerResolver ownerResolver;
    @Mock private SubscriptionPolicyService policyService;
    @Mock private SubscriptionCommitmentService commitmentService;
    @Mock private SubscriptionCreationOperator creationOperator;
    @Mock private PlanDerivationService derivationService;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private OutboxService outboxService;

    @InjectMocks
    private SubscriptionQuoteService service;

    private PlanVersion version;
    private PlanPrice price;

    @BeforeEach
    void setUp() {
        SubscriptionPlan plan = SubscriptionPlan.builder().id(1L).code("FLEX").name("Flex").build();
        version = PlanVersion.builder().id(10L).plan(plan).versionNumber(2).name("Flex").build();
        price = PlanPrice.builder().planVersion(version).billingCycle(BillingCycle.MONTHLY).currency("XAF").amount(new BigDecimal("100000"))
                .setupFee(BigDecimal.ZERO).commitmentMonths(0).trialDays(0).build();
        when(planResolver.resolvePlanVersion(eq("FLEX"), any())).thenReturn(version);
        when(planResolver.resolvePrice(eq(version), any())).thenReturn(price);
        when(ownerResolver.resolve(SubscriberType.MEMBER, "MBR-1")).thenReturn(new SubscriptionOwnerResolver.Owner("MBR-1", null, null, null));
        when(policyService.current()).thenReturn(SubscriptionPolicy.builder().quoteValidityDays(30).build());
        when(sequenceGenerator.next("subscription_quote")).thenReturn("QTE-2026-00001");
        when(quoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(derivationService.simulateAgainst(any(), any(), any())).thenReturn(preview(false, null));
    }

    private static PlanDerivationService.Preview preview(boolean approval, String blocking) {
        return new PlanDerivationService.Preview("QTE-2026-00001", "Flex v2", new BigDecimal("100000"), new BigDecimal("90000"),
                BigDecimal.TEN, new BigDecimal("10000"), "XAF", List.of(), approval, blocking);
    }

    private static SubscriptionQuoteService.Request request(BigDecimal quoted, Integer commitment, LocalDate validUntil) {
        return new SubscriptionQuoteService.Request(SubscriberType.MEMBER, "MBR-1", "FLEX", null, BillingCycle.MONTHLY, quoted, null,
                commitment, null, null, validUntil, null, null);
    }

    @Test
    void cataloguePriceByDefaultAndValidityFromPolicy() {
        SubscriptionQuote quote = service.create(request(null, null, null), "alice");

        assertEquals(0, new BigDecimal("100000").compareTo(quote.getQuotedPrice()));
        assertFalse(quote.negotiated());
        assertEquals(LocalDate.now().plusDays(30), quote.getValidUntil());
        assertEquals(SubscriptionQuote.Status.DRAFT, quote.getStatus());
        verify(derivationService, never()).simulateAgainst(any(), any(), any());
    }

    @Test
    void negotiatedPriceIsCheckedAgainstDerivationRulesAtQuoteTime() {
        when(derivationService.simulateAgainst(any(), any(), any())).thenReturn(preview(false, "Prix plancher"));
        assertThrows(ConflictException.class, () -> service.create(request(new BigDecimal("40000"), null, null), "alice"));

        when(derivationService.simulateAgainst(any(), any(), any())).thenReturn(preview(true, null));
        SubscriptionQuote quote = service.create(request(new BigDecimal("80000"), null, null), "alice");
        assertTrue(quote.negotiated());
        assertTrue(service.outlook(quote).requiresApproval(), "le devis dit d'avance qu'un visa sera nécessaire");
    }

    @Test
    void pastValidityIsRefused() {
        assertThrows(BadRequestException.class, () -> service.create(request(null, null, LocalDate.now().minusDays(1)), "alice"));
    }

    @Test
    void expiredQuoteCannotBeAccepted() {
        SubscriptionQuote quote = service.create(request(null, null, LocalDate.now().plusDays(1)), "alice");
        quote.setValidUntil(LocalDate.now().minusDays(1));
        when(quoteRepository.findByQuoteNumber("QTE-2026-00001")).thenReturn(Optional.of(quote));

        assertThrows(ConflictException.class, () -> service.accept("QTE-2026-00001", "MBR-1"));
        assertEquals(SubscriptionQuote.Status.EXPIRED, quote.getStatus());
        verify(creationOperator, never()).create(any());
    }

    @Test
    void acceptanceCreatesSubscriptionDerivationAndCommitment() {
        SubscriptionQuote quote = service.create(request(new BigDecimal("90000"), 12, null), "alice");
        when(quoteRepository.findByQuoteNumber("QTE-2026-00001")).thenReturn(Optional.of(quote));
        Subscription created = Subscription.builder().id(50L).subscriptionNumber("SUB-50").build();
        when(creationOperator.create(any())).thenReturn(created);
        when(derivationService.create(eq(created), any(), eq("alice"), any()))
                .thenReturn(PlanDerivation.builder().derivationCode("DRV-2026-00007").status(PlanDerivation.Status.ACTIVE).build());

        SubscriptionQuote converted = service.accept("QTE-2026-00001", "MBR-1");

        assertEquals(SubscriptionQuote.Status.CONVERTED, converted.getStatus());
        assertEquals("SUB-50", converted.getConvertedSubscriptionNumber());
        assertEquals("DRV-2026-00007", converted.getDerivationCode());

        ArgumentCaptor<CreateSubscriptionRequest> request = ArgumentCaptor.forClass(CreateSubscriptionRequest.class);
        verify(creationOperator).create(request.capture());
        assertEquals("FLEX", request.getValue().planCode());
        assertEquals("10", request.getValue().planVersionId());
        assertFalse(request.getValue().autoActivate(), "le devis ne paie pas · l'activation attend le paiement");

        ArgumentCaptor<PlanDerivationService.Spec> spec = ArgumentCaptor.forClass(PlanDerivationService.Spec.class);
        verify(derivationService).create(eq(created), spec.capture(), eq("alice"), any());
        assertEquals(0, new BigDecimal("90000").compareTo(spec.getValue().price()));
        assertEquals(PlanDerivation.Reason.NEGOTIATION, spec.getValue().reason());

        verify(commitmentService).set(eq(created), any(), eq(SubscriptionCommitment.Source.QUOTE), eq("MBR-1"));
    }

    @Test
    void cataloguePriceQuoteCreatesNoDerivation() {
        SubscriptionQuote quote = service.create(request(null, null, null), "alice");
        when(quoteRepository.findByQuoteNumber("QTE-2026-00001")).thenReturn(Optional.of(quote));
        when(creationOperator.create(any())).thenReturn(Subscription.builder().id(50L).subscriptionNumber("SUB-50").build());

        service.accept("QTE-2026-00001", "MBR-1");

        verify(derivationService, never()).create(any(Subscription.class), any(), anyString(), any());
        verify(commitmentService, never()).set(any(), any(), any(), anyString());
    }

    @Test
    void onceConvertedTheQuoteIsClosed() {
        SubscriptionQuote quote = service.create(request(null, null, null), "alice");
        quote.setStatus(SubscriptionQuote.Status.CONVERTED);
        when(quoteRepository.findByQuoteNumber("QTE-2026-00001")).thenReturn(Optional.of(quote));

        assertThrows(BadRequestException.class, () -> service.accept("QTE-2026-00001", "MBR-1"));
        assertThrows(BadRequestException.class, () -> service.reject("QTE-2026-00001", "non"));
        assertThrows(BadRequestException.class, () -> service.send("QTE-2026-00001"));
    }
}
