package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.features.ressource.dto.response.PublicResourceCalendarResponse;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.PublicResourceCalendarService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PublicResourceCalendarServiceImpl implements PublicResourceCalendarService {

    private final ResourceService resourceService;
    private final ResourceAvailabilityRepository availabilityRepository;

    @Override
    public PublicResourceCalendarResponse calendar(String resourceCode, LocalDate fromDate, LocalDate toDate) {
        Resource resource = resourceService.getResourceForService(resourceCode);
        LocalDate start = fromDate == null ? LocalDate.now() : fromDate;
        LocalDate end = toDate == null ? start.plusMonths(1) : toDate;
        List<ResourceAvailability> slots = availabilityRepository.findAllSlotsInRange(
                resource,
                start.atStartOfDay(),
                end.plusDays(1).atStartOfDay()
        );
        Map<LocalDate, List<ResourceAvailability>> byDay = slots.stream()
                .collect(Collectors.groupingBy(slot -> slot.getStartedAt().toLocalDate()));

        List<PublicResourceCalendarResponse.Day> days = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            List<ResourceAvailability> daySlots = byDay.getOrDefault(day, List.of());
            int totalSlots = daySlots.size();
            int availableSlots = (int) daySlots.stream()
                    .filter(slot -> Boolean.TRUE.equals(slot.getActive()))
                    .filter(slot -> Boolean.TRUE.equals(slot.getAvailable()))
                    .filter(slot -> slot.getRemainingCapacity() != null && slot.getRemainingCapacity() > 0)
                    .count();
            int totalCapacity = daySlots.stream().map(ResourceAvailability::getTotalCapacity).filter(v -> v != null).mapToInt(Integer::intValue).sum();
            int remainingCapacity = daySlots.stream().map(ResourceAvailability::getRemainingCapacity).filter(v -> v != null).mapToInt(Integer::intValue).sum();
            days.add(PublicResourceCalendarResponse.Day.builder()
                    .date(day)
                    .totalSlots(totalSlots)
                    .availableSlots(availableSlots)
                    .totalCapacity(totalCapacity)
                    .remainingCapacity(remainingCapacity)
                    .status(status(totalSlots, availableSlots))
                    .build());
        }

        return PublicResourceCalendarResponse.builder()
                .resourceCode(resource.getCode())
                .resourceName(resource.getName())
                .fromDate(start)
                .toDate(end)
                .days(days)
                .build();
    }

    private String status(int totalSlots, int availableSlots) {
        if (totalSlots == 0 || availableSlots == 0) {
            return "FULL";
        }
        if (availableSlots == totalSlots) {
            return "AVAILABLE";
        }
        return "PARTIAL";
    }
}
