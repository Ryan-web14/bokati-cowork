package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import com.sni.bokaticowork.features.subscription.usage.dto.CreateUsageRecordRequest;
import com.sni.bokaticowork.features.subscription.usage.service.UsageRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le dépassement de droits, de bout en bout.
 *
 * <p>Cinq heures de salle allouées, cinq heures consommées, une sixième réservation. Elle doit
 * passer si le plan l'autorise et produire une facture, ou être bloquée sinon. Trois maillons
 * séparaient ces deux issues, et deux avaient cédé.</p>
 *
 * <p>Le maillon vérifié ici est le dernier et le plus discret : l'enregistrement d'usage portait
 * {@code consumeEntitlement = false} en toutes circonstances. C'est juste en temps normal, où le
 * droit vient d'être décompté. En dépassement, rien n'a été décompté, et ce drapeau faisait sortir
 * le module de dépassement dès sa première ligne : aucune politique consultée, aucune charge, aucune
 * facture. Les heures supplémentaires étaient consommées et offertes.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingEntitlementOverageTest {

    @Mock private EntitlementService entitlementService;
    @Mock private UsageRecordService usageRecordService;
    @Mock private EntitlementDefinitionRepository definitionRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private BookingOverageGuard overageGuard;

    @InjectMocks
    private BookingEntitlementBridge bridge;

    @Captor
    private ArgumentCaptor<CreateUsageRecordRequest> usageCaptor;

    private Booking booking;

    @BeforeEach
    void setUp() {
        booking = Booking.builder()
                .bookingNumber("BKG-0001")
                .resource(Resource.builder().code("RES-SALLE").name("Salle A").build())
                .ownerType(SubscriberType.MEMBER)
                .ownerCode("MEM-1")
                .paymentMode(BookingPaymentMode.SUBSCRIPTION)
                .subscriptionNumber("SUB-0001")
                .entitlementCode("ENT-SALLE")
                .quantity(1)
                .currency("XAF")
                .startedAt(LocalDateTime.of(2026, 9, 18, 9, 0))
                .endedAt(LocalDateTime.of(2026, 9, 18, 10, 0))
                .build();

        when(definitionRepository.findByCodeIgnoreCase("ENT-SALLE")).thenReturn(Optional.of(
                EntitlementDefinition.builder().code("ENT-SALLE").unit(EntitlementUnit.HOUR).build()));

        Subscription subscription = new Subscription();
        PlanVersion planVersion = new PlanVersion();
        planVersion.setId(1L);
        subscription.setPlanVersion(planVersion);
        when(subscriptionRepository.findBySubscriptionNumber("SUB-0001")).thenReturn(Optional.of(subscription));
    }

    // -------------------------------------------------------------------------------------
    // Le plan autorise le dépassement
    // -------------------------------------------------------------------------------------

    @Test
    void letsTheBookingThroughWhenTheBalanceIsGoneAndThePlanAllowsIt() {
        givenBalanceExhausted();
        when(overageGuard.allows(any(), anyString())).thenReturn(true);

        bridge.reserve(booking);
    }

    /**
     * Le drapeau qui décide de tout : c'est lui qui fait entrer l'usage dans le module de
     * dépassement, lequel consulte la politique, crée la charge et émet la facture.
     */
    @Test
    void handsTheConsumptionToTheOverageModuleSoThatItBills() {
        givenBalanceExhausted();
        when(overageGuard.allows(any(), anyString())).thenReturn(true);

        bridge.consume(booking);

        verify(usageRecordService).record(usageCaptor.capture());
        assertTrue(usageCaptor.getValue().consumeEntitlement(),
                "en dépassement, l'usage doit piloter la consommation, sans quoi rien n'est facturé");
    }

    /** En temps normal, le droit vient d'être décompté : le laisser décompter deux fois serait faux. */
    @Test
    void doesNotLetTheUsageConsumeAgainWhenTheBalanceWasSufficient() {
        bridge.consume(booking);

        verify(usageRecordService).record(usageCaptor.capture());
        assertFalse(usageCaptor.getValue().consumeEntitlement());
    }

    // -------------------------------------------------------------------------------------
    // Le plan ne l'autorise pas
    // -------------------------------------------------------------------------------------

    @Test
    void blocksTheBookingWhenThePlanForbidsOverage() {
        givenBalanceExhausted();
        when(overageGuard.allows(any(), anyString())).thenReturn(false);

        assertThrows(ConflictException.class, () -> bridge.reserve(booking));
    }

    @Test
    void blocksTheConsumptionWhenThePlanForbidsOverage() {
        givenBalanceExhausted();
        when(overageGuard.allows(any(), anyString())).thenReturn(false);

        assertThrows(ConflictException.class, () -> bridge.consume(booking));
        verify(usageRecordService, never()).record(any());
    }

    /** Un pass n'a pas de politique de dépassement : son solde est ce qui a été acheté. */
    @Test
    void neverAllowsOverageOnAPass() {
        booking.setSubscriptionNumber(null);
        booking.setPassNumber("PASS-0001");
        booking.setPaymentMode(BookingPaymentMode.PASS);
        givenBalanceExhausted();

        assertThrows(ConflictException.class, () -> bridge.reserve(booking));
    }

    /** Une autre erreur que le solde reste une erreur, quoi que dise la politique. */
    @Test
    void doesNotSwallowAnUnrelatedConflict() {
        doThrow(new ConflictException("entitlement", "grant is suspended"))
                .when(entitlementService).reserve(any());
        when(overageGuard.allows(any(), anyString())).thenReturn(true);

        assertThrows(ConflictException.class, () -> bridge.reserve(booking));
    }

    // -------------------------------------------------------------------------------------

    private void givenBalanceExhausted() {
        doThrow(new ConflictException("entitlement", "insufficient balance"))
                .when(entitlementService).reserve(any());
        doThrow(new ConflictException("entitlement", "insufficient balance"))
                .when(entitlementService).consume(any());
    }
}
