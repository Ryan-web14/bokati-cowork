package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceClosureRequest;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.model.ResourceClosure;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceClosureRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceClosureServiceImplTest {

    private static final String RESOURCE_CODE = "RES-BUR-202604-00000002";

    @Mock
    private ResourceClosureRepository closureRepository;

    @Mock
    private ResourceAvailabilityRepository availabilityRepository;

    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private ResourceClosureServiceImpl service;

    @Test
    void shouldMakeOverlappingAvailabilityUnavailableWhenClosureIsCreated() {
        Resource resource = Resource.builder().code(RESOURCE_CODE).build();
        LocalDateTime startedAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endedAt = startedAt.plusHours(2);
        ResourceAvailability firstSlot = slot(resource, startedAt, startedAt.plusMinutes(30), true, true, 2);
        ResourceAvailability secondSlot = slot(resource, startedAt.plusMinutes(30), startedAt.plusMinutes(60), true, true, 1);

        when(resourceService.getResourceForService(RESOURCE_CODE)).thenReturn(resource);
        when(closureRepository.save(any(ResourceClosure.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(availabilityRepository.lockOverlappingSlots(resource, startedAt, endedAt)).thenReturn(List.of(firstSlot, secondSlot));

        service.createClosure(CreateResourceClosureRequest.builder()
                .resourceCode(RESOURCE_CODE)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .active(true)
                .build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<ResourceAvailability>> captor = ArgumentCaptor.forClass((Class<Iterable<ResourceAvailability>>) (Class<?>) Iterable.class);
        verify(availabilityRepository).saveAll(captor.capture());
        List<ResourceAvailability> changedSlots = java.util.stream.StreamSupport.stream(captor.getValue().spliterator(), false).toList();

        assertEquals(2, changedSlots.size());
        assertFalse(firstSlot.getAvailable());
        assertFalse(secondSlot.getAvailable());
    }

    @Test
    void shouldRestoreAvailabilityWhenClosureIsDeactivated() {
        Resource resource = Resource.builder().code(RESOURCE_CODE).build();
        LocalDateTime startedAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endedAt = startedAt.plusHours(2);
        ResourceClosure closure = ResourceClosure.builder()
                .id(10L)
                .resource(resource)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .active(true)
                .build();
        ResourceAvailability reopenableSlot = slot(resource, startedAt, startedAt.plusMinutes(30), true, false, 2);
        ResourceAvailability reservedSlot = slot(resource, startedAt.plusMinutes(30), startedAt.plusMinutes(60), true, false, 0);

        when(closureRepository.findById(10L)).thenReturn(Optional.of(closure));
        when(closureRepository.save(any(ResourceClosure.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(availabilityRepository.lockOverlappingSlots(resource, startedAt, endedAt)).thenReturn(List.of(reopenableSlot, reservedSlot));
        when(closureRepository.existsActiveOverlap(resource, reopenableSlot.getStartedAt(), reopenableSlot.getEndedAt())).thenReturn(false);

        service.updateActive(10L, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<ResourceAvailability>> captor = ArgumentCaptor.forClass((Class<Iterable<ResourceAvailability>>) (Class<?>) Iterable.class);
        verify(availabilityRepository).saveAll(captor.capture());
        List<ResourceAvailability> changedSlots = java.util.stream.StreamSupport.stream(captor.getValue().spliterator(), false).toList();

        assertEquals(1, changedSlots.size());
        assertTrue(reopenableSlot.getAvailable());
        assertFalse(reservedSlot.getAvailable());
    }

    @Test
    void shouldKeepAvailabilityBlockedWhenAnotherClosureStillOverlaps() {
        Resource resource = Resource.builder().code(RESOURCE_CODE).build();
        LocalDateTime startedAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endedAt = startedAt.plusHours(2);
        ResourceClosure closure = ResourceClosure.builder()
                .id(11L)
                .resource(resource)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .active(true)
                .build();
        ResourceAvailability blockedSlot = slot(resource, startedAt, startedAt.plusMinutes(30), true, false, 2);

        when(closureRepository.findById(11L)).thenReturn(Optional.of(closure));
        when(closureRepository.save(any(ResourceClosure.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(availabilityRepository.lockOverlappingSlots(resource, startedAt, endedAt)).thenReturn(List.of(blockedSlot));
        when(closureRepository.existsActiveOverlap(resource, blockedSlot.getStartedAt(), blockedSlot.getEndedAt())).thenReturn(true);

        service.updateActive(11L, false);

        verify(availabilityRepository, never()).saveAll(any());
        assertFalse(blockedSlot.getAvailable());
    }

    private ResourceAvailability slot(Resource resource,
                                      LocalDateTime startedAt,
                                      LocalDateTime endedAt,
                                      boolean active,
                                      boolean available,
                                      int remainingCapacity) {
        return ResourceAvailability.builder()
                .resource(resource)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .slotDurationMinutes(30)
                .totalCapacity(2)
                .remainingCapacity(remainingCapacity)
                .active(active)
                .available(available)
                .build();
    }
}
