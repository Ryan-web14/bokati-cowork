package com.sni.bokaticowork.features.ressource;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.ressource.dto.request.ReleaseResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceClosureRepository;
import com.sni.bokaticowork.features.ressource.service.implementation.ResourceAvailabilityServiceImpl;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import com.sni.bokaticowork.features.ressource.service.support.ResourceBookingImpact;
import com.sni.bokaticowork.features.ressource.service.support.ResourceImpactGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot R3 des ressources : gestion des creneaux et operations manuelles. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceR3Test {

    @Mock ResourceAvailabilityRepository availabilityRepository;
    @Mock ResourceClosureRepository closureRepository;
    @Mock ResourceService resourceService;
    @InjectMocks ResourceAvailabilityServiceImpl service;

    private final LocalDateTime start = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0).withSecond(0).withNano(0);

    private ResourceAvailability slot(long id, int total, int remaining) {
        return ResourceAvailability.builder().id(id).startedAt(start).endedAt(start.plusMinutes(30))
                .totalCapacity(total).remainingCapacity(remaining).active(true).available(remaining > 0).build();
    }

    @Test
    void aBookedSlotIsNotDeleted() {
        when(availabilityRepository.findById(1L)).thenReturn(Optional.of(slot(1, 4, 2)));
        assertThatThrownBy(() -> service.deleteSlot(1L)).isInstanceOf(ConflictException.class).hasMessageContaining("réservé");
        verify(availabilityRepository, never()).delete(any());
    }

    @Test
    void bulkDeletionKeepsBookedSlots() {
        Resource resource = Resource.builder().id(1L).code("RES-1").build();
        when(resourceService.getResourceForService("RES-1")).thenReturn(resource);
        ResourceAvailability free = slot(1, 4, 4);
        ResourceAvailability booked = slot(2, 4, 1);
        when(availabilityRepository.lockAllSlotsInRange(any(), any(), any())).thenReturn(List.of(free, booked));

        Map<String, Integer> result = service.deleteFutureSlots("RES-1", null, null);

        assertThat(result).containsEntry("deleted", 1).containsEntry("kept", 1);
        verify(availabilityRepository).deleteAll(List.of(free));
    }

    private ReleaseResourceAvailabilityRequest release(String reason) {
        ReleaseResourceAvailabilityRequest request = new ReleaseResourceAvailabilityRequest("SAL-1", start, start.plusMinutes(30), 1);
        request.setReason(reason);
        return request;
    }

    @Test
    void aManualOperationNeedsAReason() {
        assertThatThrownBy(() -> service.releaseManually(release(" "))).isInstanceOf(BadRequestException.class).hasMessageContaining("motif");
    }

    @Test
    void aManualReleaseNeverFreesARoomInUse() {
        Resource room = Resource.builder().id(1L).code("SAL-1")
                .resourceType(ResourceType.builder().wholeResourceBooking(true).build()).build();
        when(resourceService.getResourceForService("SAL-1")).thenReturn(room);
        ResourceBookingImpact impact = mock(ResourceBookingImpact.class);
        when(impact.activeBookings(any(), any(), any())).thenReturn(List.of(new ResourceBookingImpact.ImpactedBooking(
                "BKG-1", "CONFIRMED", start, start.plusMinutes(30), "MEMBER", "MBR-1", "Awa", 1)));
        ResourceImpactGuard guard = new ResourceImpactGuard();
        ReflectionTestUtils.setField(guard, "impact", impact);
        ReflectionTestUtils.setField(service, "impactGuard", guard);

        assertThatThrownBy(() -> service.releaseManually(release("rattrapage"))).isInstanceOf(ConflictException.class)
                .hasMessageContaining("BKG-1");
    }
}
