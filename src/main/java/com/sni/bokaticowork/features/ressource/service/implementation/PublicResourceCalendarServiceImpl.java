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

    private com.sni.bokaticowork.features.ressource.service.support.ResourceCalendarDays calendarDays;
    private com.sni.bokaticowork.features.ressource.repository.repo.ResourceClosureRepository closureRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setR5a(com.sni.bokaticowork.features.ressource.service.support.ResourceCalendarDays calendarDays,
                com.sni.bokaticowork.features.ressource.repository.repo.ResourceClosureRepository closureRepository) {
        this.calendarDays = calendarDays;
        this.closureRepository = closureRepository;
    }

    private String dayLabel(Resource resource, LocalDate date) {
        return calendarDays == null ? null : calendarDays.dayLabel(resource, date);
    }

    @Override
    public PublicResourceCalendarResponse calendar(String resourceCode, LocalDate fromDate, LocalDate toDate) {
        return calendar(resourceCode, fromDate, toDate, false);
    }

    @Override
    public PublicResourceCalendarResponse calendar(String resourceCode, LocalDate fromDate, LocalDate toDate,
                                                   boolean includeSubscribersOnly) {
        Resource resource = resourceService.getResourceForService(resourceCode);
        // Lot R0 : seulement une ressource proposee aux clients (visible au portail, active,
        // reservable) ; sinon introuvable, comme une ressource inexistante (pas d'enumeration).
        if (!Boolean.TRUE.equals(resource.getPortalVisible()) || !Boolean.TRUE.equals(resource.getActive())
                || !Boolean.TRUE.equals(resource.getBookingEnabled())
                || resource.getStatus() != com.sni.bokaticowork.features.ressource.enums.ResourceStatus.ACTIVE) {
            throw new com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException("Ressource introuvable : " + resourceCode);
        }
        com.sni.bokaticowork.features.ressource.service.support.ResourceCalendarRange range =
                com.sni.bokaticowork.features.ressource.service.support.ResourceCalendarRange.of(fromDate, toDate);
        LocalDate start = range.start();
        LocalDate end = range.end();
        List<ResourceAvailability> slots = availabilityRepository.findAllSlotsInRange(
                resource,
                start.atStartOfDay(),
                end.plusDays(1).atStartOfDay()
        );
        // Un lecteur sans droit aux soirees ne les voit pas du tout · ni dans les fenetres, ni
        // dans les totaux du jour, sans quoi une journee pleine paraitrait a moitie libre.
        Map<LocalDate, List<ResourceAvailability>> byDay = slots.stream()
                .filter(slot -> includeSubscribersOnly || !Boolean.TRUE.equals(slot.getSubscribersOnly()))
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
            List<PublicResourceCalendarResponse.Window> windows = daySlots.stream()
                    .map(slot -> PublicResourceCalendarResponse.Window.builder()
                            .startedAt(slot.getStartedAt())
                            .endedAt(slot.getEndedAt())
                            .slotDurationMinutes(slot.getSlotDurationMinutes())
                            .totalCapacity(slot.getTotalCapacity())
                            .remainingCapacity(slot.getRemainingCapacity())
                            .available(slot.getAvailable())
                            .active(slot.getActive())
                            .subscribersOnly(Boolean.TRUE.equals(slot.getSubscribersOnly()))
                            .build())
                    .toList();
            days.add(PublicResourceCalendarResponse.Day.builder()
                    .date(day)
                    .totalSlots(totalSlots)
                    .availableSlots(availableSlots)
                    .totalCapacity(totalCapacity)
                    .remainingCapacity(remainingCapacity)
                    .status(status(totalSlots, availableSlots))
                    .windows(windows)
                    .dayLabel(dayLabel(resource, day))
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

    /** CLOSED : aucun creneau ce jour (ferme, hors horaires) ; FULL : des creneaux, tous pris. */
    private String status(int totalSlots, int availableSlots) {
        if (totalSlots == 0) {
            return "CLOSED";
        }
        if (availableSlots == 0) {
            return "FULL";
        }
        if (availableSlots == totalSlots) {
            return "AVAILABLE";
        }
        return "PARTIAL";
    }
}
