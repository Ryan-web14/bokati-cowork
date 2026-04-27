package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateEntitlementDefinitionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePlanRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.ConsumptionMode;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementResetPolicy;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementType;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.TargetAudience;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionCodeFactoryTest {

    @Mock
    private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private SubscriptionCodeFactory codeFactory;

    @Test
    void shouldGenerateDynamicPlanCode() {
        LocalDate today = LocalDate.now();
        CreatePlanRequest request = new CreatePlanRequest(
                "Coworking Essential",
                null,
                PlanType.COWORKING_ACCESS,
                TargetAudience.INDIVIDUAL,
                true,
                1
        );

        when(sequenceGenerator.next("subscription_plan", today)).thenReturn("PLN-00000012");

        String code = codeFactory.nextPlanCode(request);

        assertEquals("PLN-COW-IND-" + today.getYear() + String.format("%02d", today.getMonthValue()) + "-00000012", code);
    }

    @Test
    void shouldGenerateDynamicEntitlementCode() {
        LocalDate today = LocalDate.now();
        CreateEntitlementDefinitionRequest request = new CreateEntitlementDefinitionRequest(
                "Meeting Room Hours",
                null,
                EntitlementType.TIME,
                EntitlementUnit.HOUR,
                ConsumptionMode.RESERVE_THEN_CONSUME,
                EntitlementResetPolicy.PER_BILLING_CYCLE,
                true,
                false,
                "MEETING_ROOM",
                null,
                null,
                true
        );

        when(sequenceGenerator.next("entitlement_definition", today)).thenReturn("EDF-00000027");

        String code = codeFactory.nextEntitlementDefinitionCode(request);

        assertEquals("ENT-TIM-HOU-RES-PER-MEE-" + today.getYear() + String.format("%02d", today.getMonthValue()) + "-00000027", code);
    }

    @Test
    void shouldGenerateDynamicSubscriptionNumber() {
        LocalDate businessDate = LocalDate.of(2026, 7, 15);
        PlanVersion version = PlanVersion.builder()
                .plan(SubscriptionPlan.builder()
                        .planType(PlanType.PRIVATE_OFFICE)
                        .build())
                .build();

        when(sequenceGenerator.next("subscription", businessDate)).thenReturn("SUB-00000099");

        String code = codeFactory.nextSubscriptionNumber(
                SubscriberType.BUSINESS_ENTITY,
                version,
                BillingCycle.YEARLY,
                businessDate
        );

        assertEquals("SUB-BUS-PRI-YEA-202607-00000099", code);
    }
}
