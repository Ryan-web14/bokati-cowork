package com.sni.bokaticowork.features.ressource;

import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import com.sni.bokaticowork.features.ressource.service.implementation.ResourcePricingRuleServiceImpl;
import com.sni.bokaticowork.features.ressource.service.pricing.PricingRuleGovernance;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot R7 (ecart Rn-6) : une regle modifiee est revalidee, et une condition se vide. */
class ResourceR7Test {

    private final ResourcePricingRuleRepository repository = mock(ResourcePricingRuleRepository.class);
    private final ResourcePricingRule rule = ResourcePricingRule.builder().id(7L).price(5000).resource(com.sni.bokaticowork.features.ressource.model.Resource.builder().code("SAL-1").build())
            .startsAt(LocalTime.of(8, 0)).endsAt(LocalTime.of(12, 0)).validUntil(LocalDate.of(2026, 12, 31)).active(true).build();

    private ResourcePricingRuleServiceImpl service() {
        ResourcePricingRuleServiceImpl service = mock(ResourcePricingRuleServiceImpl.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "pricingRuleRepository", repository);
        ReflectionTestUtils.setField(service, "governance", mock(PricingRuleGovernance.class));
        when(repository.findById(7L)).thenReturn(Optional.of(rule));
        when(repository.findByShortId(7L)).thenReturn(Optional.of(rule));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return service;
    }

    @Test
    void aConditionIsCleared() {
        UpdateResourcePricingRuleRequest request = new UpdateResourcePricingRuleRequest();
        request.setClear(List.of("startsAt", "endsAt", "validUntil"));

        service().updatePricingRule(7L, request);

        assertThat(rule.getStartsAt()).isNull();
        assertThat(rule.getEndsAt()).isNull();
        assertThat(rule.getValidUntil()).isNull();
    }

    @Test
    void anIncoherentUpdateIsRefused() {
        UpdateResourcePricingRuleRequest request = new UpdateResourcePricingRuleRequest();
        // Une plage qui passe minuit est admise (22:00 a 06:00) ; une plage vide ne l'est pas.
        request.setEndsAt(LocalTime.of(8, 0));

        assertThatThrownBy(() -> service().updatePricingRule(7L, request)).isInstanceOf(ValidationException.class);
        verify(repository, never()).save(any());
    }
}
