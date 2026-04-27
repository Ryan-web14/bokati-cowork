package com.sni.bokaticowork.features.subscription.subscription.service.support.catalog;

import com.sni.bokaticowork.features.subscription.subscription.dto.request.UpdatePlanRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanType;
import com.sni.bokaticowork.features.subscription.subscription.enums.TargetAudience;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
import com.sni.bokaticowork.features.subscription.repository.PlanBenefitRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanPriceRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionPlanRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionCodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanCatalogOperatorTest {

    @Mock
    private SubscriptionPlanRepository planRepository;

    @Mock
    private PlanVersionRepository planVersionRepository;

    @Mock
    private PlanPriceRepository planPriceRepository;

    @Mock
    private PlanBenefitRepository planBenefitRepository;

    @Mock
    private PlanEntitlementRepository planEntitlementRepository;

    @Mock
    private SubscriptionCodeFactory codeFactory;

    @Mock
    private PlanComponentBuilder componentBuilder;

    @InjectMocks
    private PlanCatalogOperator operator;

    @Test
    void shouldUpdatePlanMetadataWithoutChangingCodeOrStatus() {
        SubscriptionPlan existingPlan = SubscriptionPlan.builder()
                .id(10L)
                .code("PLN-00004")
                .name("Coworking Essentiel")
                .description("Initial")
                .planType(PlanType.COWORKING_ACCESS)
                .targetAudience(TargetAudience.INDIVIDUAL)
                .status(PlanStatus.ACTIVE)
                .visible(false)
                .sortOrder(1)
                .build();

        UpdatePlanRequest request = new UpdatePlanRequest(
                "  Coworking Essentiel Plus  ",
                "  Updated description  ",
                PlanType.COMPANY_PLAN,
                TargetAudience.BOTH,
                true,
                5
        );

        when(planRepository.findByCodeIgnoreCase("PLN-00004")).thenReturn(Optional.of(existingPlan));
        when(planRepository.save(existingPlan)).thenReturn(existingPlan);

        SubscriptionPlan result = operator.updatePlan("PLN-00004", request);

        ArgumentCaptor<SubscriptionPlan> planCaptor = ArgumentCaptor.forClass(SubscriptionPlan.class);
        verify(planRepository).save(planCaptor.capture());

        SubscriptionPlan savedPlan = planCaptor.getValue();
        assertEquals("PLN-00004", savedPlan.getCode());
        assertEquals(PlanStatus.ACTIVE, savedPlan.getStatus());
        assertEquals("Coworking Essentiel Plus", savedPlan.getName());
        assertEquals("Updated description", savedPlan.getDescription());
        assertEquals(PlanType.COMPANY_PLAN, savedPlan.getPlanType());
        assertEquals(TargetAudience.BOTH, savedPlan.getTargetAudience());
        assertEquals(true, savedPlan.getVisible());
        assertEquals(5, savedPlan.getSortOrder());
        assertSame(existingPlan, result);
    }

    @Test
    void shouldSoftDeletePlanWithoutVersions() {
        SubscriptionPlan existingPlan = SubscriptionPlan.builder()
                .id(10L)
                .code("PLN-00004")
                .status(PlanStatus.DRAFT)
                .visible(true)
                .build();

        when(planRepository.findByCodeIgnoreCase("PLN-00004")).thenReturn(Optional.of(existingPlan));
        when(planVersionRepository.findAllByPlanOrderByVersionNumberDesc(10L)).thenReturn(List.of());

        operator.deletePlan("PLN-00004");

        verify(planRepository).delete(existingPlan);
        verify(planRepository, never()).save(existingPlan);
    }

    @Test
    void shouldArchiveAndHidePlanWhenVersionsAlreadyExist() {
        SubscriptionPlan existingPlan = SubscriptionPlan.builder()
                .id(10L)
                .code("PLN-00004")
                .status(PlanStatus.ACTIVE)
                .visible(true)
                .build();
        PlanVersion existingVersion = PlanVersion.builder()
                .id(100L)
                .plan(existingPlan)
                .versionNumber(1)
                .name("Version 1")
                .status(PlanStatus.ACTIVE)
                .build();

        when(planRepository.findByCodeIgnoreCase("PLN-00004")).thenReturn(Optional.of(existingPlan));
        when(planVersionRepository.findAllByPlanOrderByVersionNumberDesc(10L)).thenReturn(List.of(existingVersion));
        when(planRepository.save(existingPlan)).thenReturn(existingPlan);

        operator.deletePlan("PLN-00004");

        verify(planRepository, never()).delete(existingPlan);
        verify(planRepository).save(existingPlan);
        assertEquals(PlanStatus.ARCHIVED, existingPlan.getStatus());
        assertEquals(false, existingPlan.getVisible());
    }
}
