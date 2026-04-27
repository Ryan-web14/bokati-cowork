package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.subscription.repository.PlanPriceRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionPlanRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionPlanResolverTest {

    private static final String PLAN_CODE = "PLN-MEM-BOT-202604-00000005";

    @Mock
    private PlanVersionRepository planVersionRepository;

    @Mock
    private SubscriptionPlanRepository planRepository;

    @Mock
    private PlanPriceRepository planPriceRepository;

    @InjectMocks
    private SubscriptionPlanResolver resolver;

    @Test
    void shouldResolveActivePlanVersionByStringId() {
        String rawPlanVersionId = "7244378064049836000";
        long parsedPlanVersionId = 7244378064049836000L;
        PlanVersion version = PlanVersion.builder()
                .id(parsedPlanVersionId)
                .status(PlanStatus.ACTIVE)
                .build();

        when(planVersionRepository.findById(parsedPlanVersionId)).thenReturn(Optional.of(version));

        PlanVersion result = resolver.resolvePlanVersion(PLAN_CODE, rawPlanVersionId);

        assertSame(version, result);
    }

    @Test
    void shouldFallbackToLatestActiveVersionForPlanCodeWhenPlanVersionIsInactive() {
        String rawPlanVersionId = "7244378064049836000";
        long parsedPlanVersionId = 7244378064049836000L;
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .id(12L)
                .code(PLAN_CODE)
                .build();
        PlanVersion inactiveVersion = PlanVersion.builder()
                .id(parsedPlanVersionId)
                .plan(plan)
                .status(PlanStatus.DRAFT)
                .build();
        PlanVersion fallbackVersion = PlanVersion.builder()
                .id(33L)
                .plan(plan)
                .status(PlanStatus.ACTIVE)
                .versionNumber(4)
                .build();

        when(planVersionRepository.findById(parsedPlanVersionId)).thenReturn(Optional.of(inactiveVersion));
        when(planRepository.findByCodeIgnoreCase(PLAN_CODE)).thenReturn(Optional.of(plan));
        when(planVersionRepository.findFirstByPlanAndStatusOrderByVersionNumberDesc(plan.getId(), PlanStatus.ACTIVE.name()))
                .thenReturn(Optional.of(fallbackVersion));

        PlanVersion result = resolver.resolvePlanVersion(PLAN_CODE, rawPlanVersionId);

        assertSame(fallbackVersion, result);
    }

    @Test
    void shouldFallbackToLatestActiveVersionForPlanCodeWhenLargePlanVersionIdIsMissing() {
        String rawPlanVersionId = "7244378064049836032";
        long parsedPlanVersionId = 7244378064049836032L;
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .id(12L)
                .code(PLAN_CODE)
                .build();
        PlanVersion fallbackVersion = PlanVersion.builder()
                .id(44L)
                .plan(plan)
                .status(PlanStatus.ACTIVE)
                .versionNumber(5)
                .build();

        when(planVersionRepository.findById(parsedPlanVersionId)).thenReturn(Optional.empty());
        when(planRepository.findByCodeIgnoreCase(PLAN_CODE)).thenReturn(Optional.of(plan));
        when(planVersionRepository.findFirstByPlanAndStatusOrderByVersionNumberDesc(plan.getId(), PlanStatus.ACTIVE.name()))
                .thenReturn(Optional.of(fallbackVersion));

        PlanVersion result = resolver.resolvePlanVersion(PLAN_CODE, rawPlanVersionId);

        assertSame(fallbackVersion, result);
    }

    @Test
    void shouldRejectMalformedPlanVersionId() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> resolver.resolvePlanVersion(PLAN_CODE, "invalid-id"));

        assertEquals("Plan version id must be a valid integer", ex.getMessage());
    }

    @Test
    void shouldExplainPrecisionLossWhenIdLookupAndPlanCodeFallbackBothFail() {
        String rawPlanVersionId = "7244378064049836032";
        long parsedPlanVersionId = 7244378064049836032L;
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .id(12L)
                .code(PLAN_CODE)
                .build();

        when(planVersionRepository.findById(parsedPlanVersionId)).thenReturn(Optional.empty());
        when(planRepository.findByCodeIgnoreCase(PLAN_CODE)).thenReturn(Optional.of(plan));
        when(planVersionRepository.findFirstByPlanAndStatusOrderByVersionNumberDesc(plan.getId(), PlanStatus.ACTIVE.name()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> resolver.resolvePlanVersion(PLAN_CODE, rawPlanVersionId));

        assertTrue(ex.getMessage().contains("No active version found for plan"));
    }
}
