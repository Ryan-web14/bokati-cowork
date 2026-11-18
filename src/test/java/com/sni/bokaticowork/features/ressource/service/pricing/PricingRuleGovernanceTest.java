package com.sni.bokaticowork.features.ressource.service.pricing;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleVersionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lot R1 : doublons refuses, conflits signales. */
class PricingRuleGovernanceTest {

    private final Resource resource = Resource.builder().id(1L).code("RES-1").build();
    private final List<ResourcePricingRule> rules = new ArrayList<>();
    private long ids = 1;

    private PricingRuleGovernance governance() {
        ResourcePricingRuleRepository repository = mock(ResourcePricingRuleRepository.class);
        when(repository.findAllActiveByResourceId(1L)).thenReturn(rules);
        return new PricingRuleGovernance(repository, mock(ResourcePricingRuleVersionRepository.class));
    }

    private ResourcePricingRule rule(int price) {
        ResourcePricingRule rule = ResourcePricingRule.builder().id(ids).shortId(ids++).resource(resource)
                .resourceBookingUnit(ResourceBookingUnit.HOUR).price(price).label("Heure " + price).active(true).build();
        rules.add(rule);
        return rule;
    }

    @Test
    void anIdenticalRuleIsRefused() {
        rule(5000);
        ResourcePricingRule candidate = ResourcePricingRule.builder().resource(resource).resourceBookingUnit(ResourceBookingUnit.HOUR)
                .price(6000).build();

        assertThatThrownBy(() -> governance().rejectDuplicate(candidate)).isInstanceOf(ConflictException.class).hasMessageContaining("double");
    }

    @Test
    void twoBasesOnTheSameSlotsAreAConflict() {
        rule(5000).setDaysOfWeek("1,2,3");
        rule(6000).setDaysOfWeek("3,4,5");

        assertThat(governance().conflicts(1L)).singleElement().satisfies(c -> assertThat(c.reason()).contains("Deux prix de base"));
    }

    @Test
    void disjointConditionsAreNotAConflict() {
        rule(5000).setDaysOfWeek("1,2,3,4,5");
        rule(7000).setDaysOfWeek("6,7");
        ResourcePricingRule morning = rule(4000);
        morning.setDaysOfWeek("1,2,3,4,5");
        morning.setStartsAt(LocalTime.of(8, 0));
        morning.setEndsAt(LocalTime.of(12, 0));
        morning.setPriority(1);

        assertThat(governance().conflicts(1L)).isEmpty();
    }
}
