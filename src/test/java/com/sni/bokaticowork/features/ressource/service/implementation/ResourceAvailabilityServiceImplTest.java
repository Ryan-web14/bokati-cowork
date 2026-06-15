package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.ReserveResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceClosureRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceAvailabilityServiceImplTest {

    private static final String RESOURCE_CODE = "RES-000001";

    @Mock
    private ResourceAvailabilityRepository availabilityRepository;

    @Mock
    private ResourceClosureRepository closureRepository;

    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private ResourceAvailabilityServiceImpl service;

    @Test
    void shouldCreateMultiDayAvailabilityInsideWorkingHoursWithoutApplyingBookingDurationPolicy() {
        Resource resource = bookableResourceWithPolicy(60);
        LocalDate startDate = LocalDate.now().plusDays(8);
        LocalDateTime startedAt = startDate.atTime(15, 0);
        LocalDateTime endedAt = startDate.plusDays(3).atTime(15, 0);

        when(resourceService.getResourceForService(RESOURCE_CODE)).thenReturn(resource);

        service.createAvailability(CreateResourceAvailabilityRequest.builder()
                .resourceCode(RESOURCE_CODE)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .capacity(10)
                .active(true)
                .build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<ResourceAvailability>> slotsCaptor = ArgumentCaptor.forClass((Class<Iterable<ResourceAvailability>>) (Class<?>) Iterable.class);
        verify(availabilityRepository).saveAll(slotsCaptor.capture());

        List<ResourceAvailability> slots = StreamSupport.stream(slotsCaptor.getValue().spliterator(), false).toList();
        assertEquals(72, slots.size());
        assertEquals(startedAt, slots.getFirst().getStartedAt());
        assertEquals(endedAt, slots.getLast().getEndedAt());
        assertFalse(slots.stream().anyMatch(slot -> slot.getStartedAt().toLocalTime().isBefore(LocalTime.of(8, 0))));
        assertFalse(slots.stream().anyMatch(slot -> slot.getEndedAt().toLocalTime().isAfter(LocalTime.of(20, 0))));
    }

    @Test
    void shouldRejectAvailabilityCreatedOutsideWorkingHours() {
        Resource resource = bookableResourceWithPolicy(60);
        LocalDateTime startedAt = LocalDate.now().plusDays(1).atTime(7, 0);
        LocalDateTime endedAt = LocalDate.now().plusDays(1).atTime(9, 0);

        when(resourceService.getResourceForService(RESOURCE_CODE)).thenReturn(resource);

        ConflictException exception = assertThrows(ConflictException.class, () -> service.createAvailability(
                CreateResourceAvailabilityRequest.builder()
                        .resourceCode(RESOURCE_CODE)
                        .startedAt(startedAt)
                        .endedAt(endedAt)
                        .capacity(10)
                        .active(true)
                        .build()
        ));

        assertEquals("Could not process resource availability: availability must be created within working hours from 08:00 to 20:00", exception.getMessage());
        verify(availabilityRepository, never()).saveAll(any());
    }

    @Test
    void shouldKeepBookingDurationPolicyForReservation() {
        Resource resource = bookableResourceWithPolicy(60);
        LocalDateTime startedAt = LocalDate.now().plusDays(8).atTime(8, 0);
        LocalDateTime endedAt = startedAt.plusMinutes(120);

        when(resourceService.getResourceForService(RESOURCE_CODE)).thenReturn(resource);

        ConflictException exception = assertThrows(ConflictException.class, () -> service.reserve(
                ReserveResourceAvailabilityRequest.builder()
                        .resourceCode(RESOURCE_CODE)
                        .startedAt(startedAt)
                        .endedAt(endedAt)
                        .quantity(1)
                        .build()
        ));

        assertEquals("Could not process resource availability: the requested duration exceeds the policy maximum", exception.getMessage());
    }

    @Test
    void shouldRejectAvailabilityCreatedMoreThanOneMonthInAdvance() {
        Resource resource = bookableResourceWithPolicy(60);
        LocalDateTime startedAt = LocalDate.now().plusMonths(1).plusDays(1).atTime(8, 0);
        LocalDateTime endedAt = startedAt.plusHours(2);

        when(resourceService.getResourceForService(RESOURCE_CODE)).thenReturn(resource);

        ConflictException exception = assertThrows(ConflictException.class, () -> service.createAvailability(
                CreateResourceAvailabilityRequest.builder()
                        .resourceCode(RESOURCE_CODE)
                        .startedAt(startedAt)
                        .endedAt(endedAt)
                        .capacity(10)
                        .active(true)
                        .build()
        ));

        assertEquals("Could not process resource availability: availability can only be created up to one month in advance", exception.getMessage());
        verify(availabilityRepository, never()).saveAll(any());
    }

    @Test
    void shouldGroupAvailabilityByResource() {
        Resource resourceOne = bookableResourceWithPolicy(60);
        resourceOne.setName("Meeting Room A");

        Resource resourceTwo = bookableResourceWithPolicy(60);
        resourceTwo.setCode("RES-000002");
        resourceTwo.setName("Training Room");

        ResourceAvailability firstSlot = ResourceAvailability.builder()
                .id(1L)
                .resource(resourceOne)
                .startedAt(LocalDate.now().plusDays(1).atTime(8, 0))
                .endedAt(LocalDate.now().plusDays(1).atTime(8, 30))
                .slotDurationMinutes(30)
                .totalCapacity(10)
                .remainingCapacity(10)
                .available(true)
                .active(true)
                .build();
        ResourceAvailability secondSlot = ResourceAvailability.builder()
                .id(2L)
                .resource(resourceOne)
                .startedAt(LocalDate.now().plusDays(1).atTime(8, 30))
                .endedAt(LocalDate.now().plusDays(1).atTime(9, 0))
                .slotDurationMinutes(30)
                .totalCapacity(10)
                .remainingCapacity(8)
                .available(true)
                .active(true)
                .build();
        ResourceAvailability thirdSlot = ResourceAvailability.builder()
                .id(3L)
                .resource(resourceTwo)
                .startedAt(LocalDate.now().plusDays(1).atTime(8, 0))
                .endedAt(LocalDate.now().plusDays(1).atTime(8, 30))
                .slotDurationMinutes(30)
                .totalCapacity(20)
                .remainingCapacity(20)
                .available(true)
                .active(true)
                .build();

        when(availabilityRepository.findExpiredSlots(any(LocalDateTime.class), any(Limit.class))).thenReturn(List.of());
        when(availabilityRepository.findFutureForGroupedView(any(LocalDateTime.class))).thenReturn(List.of(firstSlot, secondSlot, thirdSlot));

        var grouped = service.listGroupedByResource();

        assertEquals(2, grouped.size());
        assertEquals("RES-000001", grouped.getFirst().getResourceCode());
        assertEquals("Meeting Room A", grouped.getFirst().getResourceName());
        assertEquals(2, grouped.getFirst().getAvailabilities().size());
        assertEquals(1L, grouped.getFirst().getAvailabilities().getFirst().getId());
        assertEquals("RES-000002", grouped.get(1).getResourceCode());
        assertEquals(1, grouped.get(1).getAvailabilities().size());
    }

    @Test
    void shouldReturnFullRemainingWindowsWhenSearchParametersAreMissing() {
        Resource resource = bookableResourceWithPolicy(60);
        LocalDateTime slotOneStart = LocalDateTime.now().plusHours(2).withMinute(0).withSecond(0).withNano(0);
        ResourceAvailability firstSlot = ResourceAvailability.builder()
                .resource(resource)
                .startedAt(slotOneStart)
                .endedAt(slotOneStart.plusMinutes(30))
                .slotDurationMinutes(30)
                .totalCapacity(4)
                .remainingCapacity(3)
                .available(true)
                .active(true)
                .build();
        ResourceAvailability secondSlot = ResourceAvailability.builder()
                .resource(resource)
                .startedAt(slotOneStart.plusMinutes(30))
                .endedAt(slotOneStart.plusMinutes(60))
                .slotDurationMinutes(30)
                .totalCapacity(4)
                .remainingCapacity(2)
                .available(true)
                .active(true)
                .build();
        LocalDateTime slotThreeStart = slotOneStart.plusHours(3);
        ResourceAvailability thirdSlot = ResourceAvailability.builder()
                .resource(resource)
                .startedAt(slotThreeStart)
                .endedAt(slotThreeStart.plusMinutes(30))
                .slotDurationMinutes(30)
                .totalCapacity(4)
                .remainingCapacity(1)
                .available(true)
                .active(true)
                .build();

        when(resourceService.getResourceForService(RESOURCE_CODE)).thenReturn(resource);
        when(availabilityRepository.findExpiredSlots(any(LocalDateTime.class), any(Limit.class))).thenReturn(List.of());
        when(availabilityRepository.findFutureActiveSlots(eq(resource), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(firstSlot, secondSlot, thirdSlot));

        var windows = service.findRemainingWindows(RESOURCE_CODE, null, null, null, 1);

        assertEquals(2, windows.size());
        assertEquals(slotOneStart, windows.getFirst().getStartedAt());
        assertEquals(slotOneStart.plusMinutes(60), windows.getFirst().getEndedAt());
        assertEquals(60, windows.getFirst().getDurationMinutes());
        assertEquals(2, windows.getFirst().getRemainingCapacity());
        assertEquals(2, windows.getFirst().getSlotCount());
        assertEquals(slotThreeStart, windows.get(1).getStartedAt());
        assertEquals(slotThreeStart.plusMinutes(30), windows.get(1).getEndedAt());
    }

    @Test
    void shouldExpirePastAvailableSlotsAndExcludeThemFromList() {
        LocalDateTime now = LocalDateTime.now();
        Resource pastResource = bookableResourceWithPolicy(60);
        Resource futureResource = bookableResourceWithPolicy(60);
        ResourceAvailability expiredSlot = ResourceAvailability.builder()
                .resource(pastResource)
                .startedAt(now.minusHours(2))
                .endedAt(now.minusHours(1))
                .slotDurationMinutes(30)
                .totalCapacity(4)
                .remainingCapacity(2)
                .available(true)
                .active(true)
                .build();
        ResourceAvailability futureSlot = ResourceAvailability.builder()
                .id(99L)
                .resource(futureResource)
                .startedAt(now.plusHours(2))
                .endedAt(now.plusHours(3))
                .slotDurationMinutes(30)
                .totalCapacity(4)
                .remainingCapacity(4)
                .available(true)
                .active(true)
                .build();

        when(availabilityRepository.findExpiredSlots(any(LocalDateTime.class), any(Limit.class))).thenReturn(List.of(expiredSlot));
        when(availabilityRepository.findAllByEndedAtAfter(any(LocalDateTime.class), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(futureSlot)));

        var page = service.list(PageRequest.of(0, 20));

        verify(availabilityRepository).saveAll(List.of(expiredSlot));
        assertEquals(false, expiredSlot.getAvailable());
        assertEquals(false, expiredSlot.getActive());
        assertEquals(1, page.getData().size());
        assertEquals(99L, page.getData().getFirst().getId());
    }

    private Resource bookableResourceWithPolicy(int maxBookingDurationMinutes) {
        ResourcePolicy policy = ResourcePolicy.builder()
                .minBookingDurationMinutes(30)
                .maxBookingDurationMinutes(maxBookingDurationMinutes)
                .minBookingNoticeMinutes(60)
                .cancellationNoticeMinutes(60)
                .build();

        Resource resource = Resource.builder()
                .code(RESOURCE_CODE)
                .capacity(10)
                .bookingEnabled(true)
                .active(true)
                .status(ResourceStatus.ACTIVE)
                .resourcePolicy(policy)
                .build();
        assertSame(policy, resource.getResourcePolicy());
        return resource;
    }
}
