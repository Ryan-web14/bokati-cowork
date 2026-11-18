package com.sni.bokaticowork.features.ressource;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceGroup;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceGroupRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.service.implementation.ResourceGroupServiceImpl;
import com.sni.bokaticowork.features.ressource.service.support.ResourceBookingImpact;
import com.sni.bokaticowork.features.ressource.service.support.ResourceImpactGuard;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot R2 des ressources : politiques, suppressions, reservations a venir. */
class ResourceR2Test {

    private final Resource resource = Resource.builder().id(1L).code("RES-1").build();

    @Test
    void aDisabledPolicyNoLongerApplies() {
        ResourcePolicy policy = ResourcePolicy.builder().code("POL-1").active(false).build();
        resource.setResourcePolicy(policy);
        assertThat(resource.getEffectivePolicy()).isNull();

        policy.setActive(true);
        assertThat(resource.getEffectivePolicy()).isSameAs(policy);
    }

    @Test
    void cancellationIsAllowedByDefault() {
        assertThat(ResourcePolicy.builder().build().getAllowCancellation()).isTrue();
    }

    private ResourceImpactGuard guard(ResourceBookingImpact impact) {
        ResourceImpactGuard guard = new ResourceImpactGuard();
        ReflectionTestUtils.setField(guard, "impact", impact);
        return guard;
    }

    @Test
    void upcomingBookingsBlockUnlessCancelled() {
        ResourceBookingImpact impact = mock(ResourceBookingImpact.class);
        when(impact.activeBookings(eq(1L), any(), any())).thenReturn(List.of(
                new ResourceBookingImpact.ImpactedBooking("BKG-1", "CONFIRMED", LocalDateTime.now().plusDays(2),
                        LocalDateTime.now().plusDays(2).plusHours(2), "MEMBER", "MBR-1", "Awa")));

        assertThatThrownBy(() -> guard(impact).check(resource, null, null, false, "supprimer la ressource", null))
                .isInstanceOf(ConflictException.class).hasMessageContaining("BKG-1").hasMessageContaining("cancelBookings=true");
        verify(impact, never()).cancelAndNotify(any(), anyString());

        guard(impact).check(resource, null, null, true, "supprimer la ressource", "Ressource retirée");
        verify(impact).cancelAndNotify(List.of("BKG-1"), "Ressource retirée");
    }

    @Test
    void noBookingsNoObstacle() {
        ResourceBookingImpact impact = mock(ResourceBookingImpact.class);
        when(impact.activeBookings(eq(1L), any(), any())).thenReturn(List.of());

        guard(impact).check(resource, null, null, false, "fermer la ressource", null);

        verify(impact, never()).cancelAndNotify(any(), anyString());
    }

    @Test
    void aGroupWithResourcesIsNotDeleted() {
        ResourceGroupRepository groups = mock(ResourceGroupRepository.class);
        ResourceRepository resources = mock(ResourceRepository.class);
        ResourceGroupServiceImpl service = new ResourceGroupServiceImpl(groups, null, null);
        ReflectionTestUtils.setField(service, "resourceRepository", resources);
        ResourceGroup group = ResourceGroup.builder().id(7L).code("GRP-1").build();
        when(groups.findByCode("GRP-1")).thenReturn(Optional.of(group));
        when(resources.countAttachedToGroup(7L)).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteResourceGroup("GRP-1")).isInstanceOf(ConflictException.class).hasMessageContaining("2 ressource");
        verify(groups, never()).delete(any(ResourceGroup.class));
    }
}
