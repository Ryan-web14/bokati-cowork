package com.sni.bokaticowork.features.ressource;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.ressource.dto.request.ResourceLocationRequests;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityWindowResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceLocationResponses;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePriceQuoteResponse;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.model.ResourceFloor;
import com.sni.bokaticowork.features.ressource.model.ResourceSite;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAmenityLinkRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceFloorRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceSiteRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourcePricingRuleService;
import com.sni.bokaticowork.features.ressource.service.support.ResourceFinder;
import com.sni.bokaticowork.features.ressource.service.support.ResourceIcal;
import com.sni.bokaticowork.features.ressource.service.support.ResourceSites;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lot R5b des ressources : sites, plan en direct, recherche d'une salle, iCalendar. */
class ResourceR5bTest {

    private final LocalDateTime start = LocalDateTime.of(2026, 12, 2, 10, 0);

    @Test
    void anInvitationIsInUtcAndFolded() {
        String ics = ResourceIcal.invitation("BKG-1", "Salle Baobab", "Salle Baobab, 1er étage, Siège, avenue de la Paix, Brazzaville",
                start, start.plusHours(2), ZoneId.of("Africa/Brazzaville"), "Réservation BKG-1");

        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n");
        assertThat(ics).contains("DTSTART:20261202T090000Z\r\n", "DTEND:20261202T110000Z\r\n", "UID:booking-BKG-1@bokati");
        assertThat(ics).contains("LOCATION:Salle Baobab\\, 1er étage");
        for (String line : ics.split("\r\n")) {
            assertThat(line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75);
        }
    }

    private ResourceSites sites(ResourceAvailabilityRepository availability, ResourceSiteRepository siteRepository,
                                ResourceFloorRepository floorRepository, ResourceRepository resources) {
        return new ResourceSites(siteRepository, floorRepository, resources, availability, null);
    }

    private ResourceAvailability slot(int total, int remaining) {
        return ResourceAvailability.builder().startedAt(start).endedAt(start.plusMinutes(30)).totalCapacity(total)
                .remainingCapacity(remaining).active(true).available(remaining > 0).build();
    }

    private Resource room() {
        return Resource.builder().id(1L).code("SAL-1").name("Baobab").active(true).bookingEnabled(true)
                .status(ResourceStatus.ACTIVE).capacity(8)
                .resourceType(ResourceType.builder().code("MEET").wholeResourceBooking(true).build()).build();
    }

    @Test
    void theLivePlanShowsFreeOccupiedAndUnopened() {
        ResourceAvailabilityRepository availability = mock(ResourceAvailabilityRepository.class);
        ResourceFloorRepository floors = mock(ResourceFloorRepository.class);
        ResourceSite site = ResourceSite.builder().id(1L).code("SIT-1").name("Siège").build();
        ResourceFloor floor = ResourceFloor.builder().id(2L).code("FLR-1").name("1er étage").site(site).build();
        Resource free = room();
        Resource busy = room();
        busy.setCode("SAL-2");
        Resource closed = room();
        closed.setCode("SAL-3");
        when(floors.findByCodeIgnoreCase("FLR-1")).thenReturn(Optional.of(floor));
        when(floors.resources(floor)).thenReturn(List.of(free, busy, closed));
        when(availability.findOverlapping(eq(free), any(), any())).thenReturn(List.of(slot(1, 1)));
        when(availability.findOverlapping(eq(busy), any(), any())).thenReturn(List.of(slot(1, 0)));
        when(availability.findOverlapping(eq(closed), any(), any())).thenReturn(List.of());

        ResourceLocationResponses.FloorPlan plan = sites(availability, mock(ResourceSiteRepository.class), floors,
                mock(ResourceRepository.class)).plan("FLR-1", start);

        assertThat(plan.resources()).extracting(ResourceLocationResponses.PlacedResource::state)
                .containsExactly("FREE", "OCCUPIED", "NO_SLOT");
        assertThat(plan.siteName()).isEqualTo("Siège");
    }

    @Test
    void aFloorOfAnotherSiteIsRefused() {
        ResourceSiteRepository siteRepository = mock(ResourceSiteRepository.class);
        ResourceFloorRepository floors = mock(ResourceFloorRepository.class);
        ResourceRepository resources = mock(ResourceRepository.class);
        ResourceSite a = ResourceSite.builder().id(1L).code("SIT-A").name("A").build();
        ResourceSite b = ResourceSite.builder().id(2L).code("SIT-B").name("B").build();
        when(resources.findByCode("SAL-1")).thenReturn(Optional.of(room()));
        when(siteRepository.findByCodeIgnoreCase("SIT-A")).thenReturn(Optional.of(a));
        when(floors.findByCodeIgnoreCase("FLR-B")).thenReturn(Optional.of(ResourceFloor.builder().id(3L).code("FLR-B").site(b).build()));

        assertThatThrownBy(() -> sites(mock(ResourceAvailabilityRepository.class), siteRepository, floors, resources)
                .place("SAL-1", new ResourceLocationRequests.Placement("SIT-A", "FLR-B", BigDecimal.TEN, BigDecimal.TEN)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("n'appartient pas");
        assertThatThrownBy(() -> sites(mock(ResourceAvailabilityRepository.class), siteRepository, floors, resources)
                .place("SAL-1", new ResourceLocationRequests.Placement("SIT-A", null, BigDecimal.valueOf(120), null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("pourcentage");
    }

    @Test
    void findingARoomGivesMatchesThenExplainedAlternatives() {
        ResourceRepository resources = mock(ResourceRepository.class);
        ResourceAmenityLinkRepository links = mock(ResourceAmenityLinkRepository.class);
        ResourceAvailabilityService availability = mock(ResourceAvailabilityService.class);
        ResourcePricingRuleService pricing = mock(ResourcePricingRuleService.class);
        Resource fits = room();
        Resource taken = room();
        taken.setCode("SAL-2");
        Resource small = room();
        small.setCode("SAL-3");
        small.setCapacity(2);
        when(resources.findAll()).thenReturn(List.of(fits, taken, small));
        when(links.findAllByResource(any())).thenReturn(List.of());
        ResourceAvailabilityWindowResponse exact = ResourceAvailabilityWindowResponse.builder()
                .startedAt(start).endedAt(start.plusHours(1)).build();
        ResourceAvailabilityWindowResponse later = ResourceAvailabilityWindowResponse.builder()
                .startedAt(start.plusHours(3)).endedAt(start.plusHours(4)).build();
        when(availability.findRemainingWindows(eq("SAL-1"), any(), any(), anyInt(), anyInt(), anyBoolean())).thenReturn(List.of(exact));
        when(availability.findRemainingWindows(eq("SAL-2"), eq(start), any(), anyInt(), anyInt(), anyBoolean())).thenReturn(List.of());
        when(availability.findRemainingWindows(eq("SAL-2"), eq(start.toLocalDate().atStartOfDay()), any(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(List.of(later));
        when(pricing.quote(any(), isNull(), any(), any(), any(), any(), any(), isNull())).thenReturn(new ResourcePriceQuoteResponse(
                "SAL-1", "HOUR", null, null, null, null, null, null, null, null, null, null, BigDecimal.valueOf(15000), "XAF", null, null));
        ResourceFinder finder = new ResourceFinder(resources, links, availability, pricing);

        ResourceLocationResponses.FindResult result = finder.find(new ResourceLocationRequests.Find(start, start.plusHours(1), 6,
                null, null, null, null, null, null));

        assertThat(result.matches()).extracting(ResourceLocationResponses.Option::resourceCode).containsExactly("SAL-1");
        assertThat(result.matches().getFirst().price()).isEqualByComparingTo("15000");
        assertThat(result.alternatives()).extracting(ResourceLocationResponses.Option::resourceCode).containsExactly("SAL-2");
        assertThat(result.alternatives().getFirst().notes()).contains("Autre horaire le même jour");
        assertThat(result.alternatives().getFirst().startedAt()).isEqualTo(start.plusHours(3));
    }
}
