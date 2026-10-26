package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.features.ressource.dto.response.PublicResourceCalendarResponse;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Le calendrier public ne montre pas les soirees des abonnes, ni dans ses fenetres ni dans ses totaux.
 */
@ExtendWith(MockitoExtension.class)
class PublicResourceCalendarEveningTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 3);

    @Mock private ResourceService resourceService;
    @Mock private ResourceAvailabilityRepository availabilityRepository;
    @InjectMocks private PublicResourceCalendarServiceImpl service;

    private final Resource resource = Resource.builder().code("RES-1").name("Open space").capacity(10).build();

    @BeforeEach
    void setUp() {
        when(resourceService.getResourceForService("RES-1")).thenReturn(resource);
        when(availabilityRepository.findAllSlotsInRange(eq(resource), any(), any())).thenReturn(List.of(
                slot(DAY.atTime(10, 0), false),
                slot(DAY.atTime(19, 0), true)));
    }

    private ResourceAvailability slot(LocalDateTime start, boolean subscribersOnly) {
        return ResourceAvailability.builder().resource(resource).startedAt(start).endedAt(start.plusMinutes(30))
                .slotDurationMinutes(30).totalCapacity(10).remainingCapacity(10).available(true).active(true)
                .subscribersOnly(subscribersOnly).build();
    }

    @Test
    @DisplayName("Le public ne voit que la journee")
    void publicSeesDaytimeOnly() {
        PublicResourceCalendarResponse.Day day = service.calendar("RES-1", DAY, DAY).days().getFirst();

        assertThat(day.windows()).hasSize(1);
        assertThat(day.totalSlots()).isEqualTo(1);
        assertThat(day.windows().getFirst().subscribersOnly()).isFalse();
    }

    @Test
    @DisplayName("Un abonne voit aussi la soiree, signalee comme telle")
    void subscriberSeesTheEvening() {
        PublicResourceCalendarResponse.Day day = service.calendar("RES-1", DAY, DAY, true).days().getFirst();

        assertThat(day.windows()).hasSize(2);
        assertThat(day.windows().getLast().subscribersOnly()).isTrue();
    }
}
