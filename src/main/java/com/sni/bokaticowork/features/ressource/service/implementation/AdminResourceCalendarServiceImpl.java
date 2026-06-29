package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.features.ressource.dto.response.AdminResourceCalendarResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceOccupiedSlotResponse;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.AdminResourceCalendarService;
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
public class AdminResourceCalendarServiceImpl implements AdminResourceCalendarService {

    private final ResourceService resourceService;
    private final ResourceAvailabilityRepository availabilityRepository;

    @Override
    public AdminResourceCalendarResponse calendar(String resourceCode, LocalDate fromDate, LocalDate toDate) {
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

        List<AdminResourceCalendarResponse.Day> days = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            days.add(buildDay(day, byDay.getOrDefault(day, List.of())));
        }

        return AdminResourceCalendarResponse.builder()
                .resourceCode(resource.getCode())
                .resourceName(resource.getName())
                .fromDate(start)
                .toDate(end)
                .days(days)
                .build();
    }

    @Override
    public List<ResourceOccupiedSlotResponse> occupiedSlots(String resourceCode, LocalDate fromDate, LocalDate toDate) {
        Resource resource = resourceService.getResourceForService(resourceCode);
        LocalDate start = fromDate == null ? LocalDate.now() : fromDate;
        LocalDate end = toDate == null ? start.plusMonths(1) : toDate;

        List<ResourceAvailability> slots = availabilityRepository.findAllSlotsInRange(
                resource,
                start.atStartOfDay(),
                end.plusDays(1).atStartOfDay()
        );

        return slots.stream()
                .filter(slot -> slot.getTotalCapacity() != null && slot.getRemainingCapacity() != null)
                .filter(slot -> slot.getRemainingCapacity() < slot.getTotalCapacity())
                .map(slot -> {
                    int total = slot.getTotalCapacity();
                    int remaining = slot.getRemainingCapacity();
                    int used = total - remaining;
                    double rate = total > 0 ? (double) used / total * 100.0 : 0.0;
                    return ResourceOccupiedSlotResponse.builder()
                            .id(slot.getId())
                            .resourceCode(resource.getCode())
                            .resourceName(resource.getName())
                            .startedAt(slot.getStartedAt())
                            .endedAt(slot.getEndedAt())
                            .slotDurationMinutes(slot.getSlotDurationMinutes() != null ? slot.getSlotDurationMinutes() : 0)
                            .totalCapacity(total)
                            .remainingCapacity(remaining)
                            .usedCapacity(used)
                            .occupancyRate(Math.round(rate * 100.0) / 100.0)
                            .occupancyLevel(occupancyLevel(rate))
                            .active(Boolean.TRUE.equals(slot.getActive()))
                            .build();
                })
                .toList();
    }

    private AdminResourceCalendarResponse.Day buildDay(LocalDate date, List<ResourceAvailability> daySlots) {
        int totalSlots = daySlots.size();

        int availableSlots = (int) daySlots.stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .filter(s -> Boolean.TRUE.equals(s.getAvailable()))
                .filter(s -> s.getRemainingCapacity() != null && s.getRemainingCapacity() > 0)
                .count();

        int occupiedSlots = (int) daySlots.stream()
                .filter(s -> s.getTotalCapacity() != null && s.getRemainingCapacity() != null)
                .filter(s -> s.getRemainingCapacity() < s.getTotalCapacity())
                .count();

        int totalCapacity = daySlots.stream()
                .filter(s -> s.getTotalCapacity() != null)
                .mapToInt(ResourceAvailability::getTotalCapacity)
                .sum();

        int remainingCapacity = daySlots.stream()
                .filter(s -> s.getRemainingCapacity() != null)
                .mapToInt(ResourceAvailability::getRemainingCapacity)
                .sum();

        int usedCapacity = totalCapacity - remainingCapacity;
        double dayOccupancyRate = totalCapacity > 0 ? (double) usedCapacity / totalCapacity * 100.0 : 0.0;
        dayOccupancyRate = Math.round(dayOccupancyRate * 100.0) / 100.0;

        List<AdminResourceCalendarResponse.Window> windows = daySlots.stream()
                .map(slot -> {
                    int total = slot.getTotalCapacity() != null ? slot.getTotalCapacity() : 0;
                    int remaining = slot.getRemainingCapacity() != null ? slot.getRemainingCapacity() : 0;
                    int used = total - remaining;
                    double rate = total > 0 ? (double) used / total * 100.0 : 0.0;
                    rate = Math.round(rate * 100.0) / 100.0;
                    return AdminResourceCalendarResponse.Window.builder()
                            .id(slot.getId())
                            .startedAt(slot.getStartedAt())
                            .endedAt(slot.getEndedAt())
                            .slotDurationMinutes(slot.getSlotDurationMinutes() != null ? slot.getSlotDurationMinutes() : 0)
                            .totalCapacity(total)
                            .remainingCapacity(remaining)
                            .usedCapacity(used)
                            .occupancyRate(rate)
                            .occupancyLevel(occupancyLevel(rate))
                            .available(Boolean.TRUE.equals(slot.getAvailable()) && remaining > 0)
                            .active(Boolean.TRUE.equals(slot.getActive()))
                            .build();
                })
                .toList();

        return AdminResourceCalendarResponse.Day.builder()
                .date(date)
                .totalSlots(totalSlots)
                .availableSlots(availableSlots)
                .occupiedSlots(occupiedSlots)
                .totalCapacity(totalCapacity)
                .remainingCapacity(remainingCapacity)
                .usedCapacity(usedCapacity)
                .occupancyRate(dayOccupancyRate)
                .occupancyLevel(occupancyLevel(dayOccupancyRate))
                .status(dayStatus(totalSlots, availableSlots))
                .windows(windows)
                .build();
    }

    static String occupancyLevel(double rate) {
        if (rate == 0) return "FREE";
        if (rate < 50) return "PARTIALLY_OCCUPIED";
        if (rate < 85) return "HIGHLY_OCCUPIED";
        return "FULL";
    }

    private static String dayStatus(int totalSlots, int availableSlots) {
        if (totalSlots == 0 || availableSlots == 0) return "FULL";
        if (availableSlots == totalSlots) return "AVAILABLE";
        return "PARTIAL";
    }
}
