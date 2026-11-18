package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.features.ressource.dto.request.ResourceLocationRequests;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceLocationResponses;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.model.ResourceFloor;
import com.sni.bokaticowork.features.ressource.model.ResourceSite;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceFloorRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Lot R5b : sites, etages, placement des ressources et plan en direct. */
@Component
@RequiredArgsConstructor
public class ResourceSites {

    private final ResourceSiteRepository siteRepository;
    private final ResourceFloorRepository floorRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceAvailabilityRepository availabilityRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    // ------------------------------------------------------------------ sites

    @Transactional(readOnly = true)
    public List<ResourceLocationResponses.Site> sites() {
        return siteRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ResourceLocationResponses.Site site(String code) {
        return toResponse(siteEntity(code));
    }

    @Transactional
    public ResourceLocationResponses.Site createSite(ResourceLocationRequests.Site request) {
        if (request == null || !StringUtils.hasText(request.name())) {
            throw new BadRequestException("Le nom du site est requis");
        }
        if (siteRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("Un site porte déjà ce nom");
        }
        ResourceSite site = ResourceSite.builder()
                .code(code("resource_site", "SIT"))
                .name(request.name().trim())
                .build();
        apply(site, request);
        return toResponse(siteRepository.save(site));
    }

    @Transactional
    public ResourceLocationResponses.Site updateSite(String code, ResourceLocationRequests.Site request) {
        ResourceSite site = siteEntity(code);
        if (request != null && StringUtils.hasText(request.name())) {
            site.setName(request.name().trim());
        }
        apply(site, request);
        return toResponse(siteRepository.save(site));
    }

    @Transactional
    public void deleteSite(String code) {
        ResourceSite site = siteEntity(code);
        long resources = siteRepository.countResources(site);
        long floors = siteRepository.countFloors(site);
        if (resources > 0 || floors > 0) {
            throw new ConflictException("Le site " + site.getCode() + " a encore " + resources + " ressource(s) et "
                    + floors + " étage(s) : rattachez-les ailleurs ou désactivez le site");
        }
        siteRepository.delete(site);
    }

    private static void apply(ResourceSite site, ResourceLocationRequests.Site request) {
        if (request == null) {
            return;
        }
        if (request.address() != null) site.setAddress(blankToNull(request.address()));
        if (request.city() != null) site.setCity(blankToNull(request.city()));
        if (request.phone() != null) site.setPhone(blankToNull(request.phone()));
        if (request.openingHoursNote() != null) site.setOpeningHoursNote(blankToNull(request.openingHoursNote()));
        if (request.active() != null) site.setActive(request.active());
        if (StringUtils.hasText(request.timeZone())) {
            try {
                site.setTimeZone(ZoneId.of(request.timeZone().trim()).getId());
            } catch (DateTimeException ex) {
                throw new BadRequestException("Fuseau horaire inconnu : " + request.timeZone().trim() + " (exemple : Africa/Brazzaville)");
            }
        }
    }

    public ResourceSite siteEntity(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Le code du site est requis");
        }
        return siteRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Site introuvable : " + code.trim()));
    }

    private ResourceLocationResponses.Site toResponse(ResourceSite site) {
        return new ResourceLocationResponses.Site(site.getId(), site.getCode(), site.getName(), site.getAddress(),
                site.getCity(), site.getPhone(), site.getTimeZone(), site.getOpeningHoursNote(), site.getActive(),
                siteRepository.countFloors(site), siteRepository.countResources(site));
    }

    // ------------------------------------------------------------------ etages

    @Transactional(readOnly = true)
    public List<ResourceLocationResponses.Floor> floors(String siteCode) {
        return siteRepository.floors(siteEntity(siteCode)).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ResourceLocationResponses.Floor createFloor(String siteCode, ResourceLocationRequests.Floor request) {
        ResourceSite site = siteEntity(siteCode);
        if (request == null || !StringUtils.hasText(request.name())) {
            throw new BadRequestException("Le nom de l'étage est requis");
        }
        ResourceFloor floor = ResourceFloor.builder()
                .code(code("resource_floor", "FLR"))
                .site(site)
                .name(request.name().trim())
                .level(request.level())
                .planDocumentCode(blankToNull(request.planDocumentCode()))
                .active(request.active() == null ? Boolean.TRUE : request.active())
                .build();
        return toResponse(floorRepository.save(floor));
    }

    @Transactional
    public ResourceLocationResponses.Floor updateFloor(String code, ResourceLocationRequests.Floor request) {
        ResourceFloor floor = floorEntity(code);
        if (request != null) {
            if (StringUtils.hasText(request.name())) floor.setName(request.name().trim());
            if (request.level() != null) floor.setLevel(request.level());
            if (request.planDocumentCode() != null) floor.setPlanDocumentCode(blankToNull(request.planDocumentCode()));
            if (request.active() != null) floor.setActive(request.active());
        }
        return toResponse(floorRepository.save(floor));
    }

    @Transactional
    public void deleteFloor(String code) {
        ResourceFloor floor = floorEntity(code);
        long resources = floorRepository.countResources(floor);
        if (resources > 0) {
            throw new ConflictException("L'étage " + floor.getCode() + " porte encore " + resources + " ressource(s) : déplacez-les d'abord");
        }
        floorRepository.delete(floor);
    }

    public ResourceFloor floorEntity(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Le code de l'étage est requis");
        }
        return floorRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Étage introuvable : " + code.trim()));
    }

    private ResourceLocationResponses.Floor toResponse(ResourceFloor floor) {
        return new ResourceLocationResponses.Floor(floor.getId(), floor.getCode(), floor.getSite().getCode(), floor.getName(),
                floor.getLevel(), floor.getPlanDocumentCode(), floor.getActive(), floorRepository.countResources(floor));
    }

    // ------------------------------------------------------------------ placement et plan

    /** Rattache une ressource a un site et a un etage, avec sa position sur le plan. */
    @Transactional
    public void place(String resourceCode, ResourceLocationRequests.Placement request) {
        Resource resource = resourceRepository.findByCode(resourceCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Ressource introuvable : " + resourceCode.trim()));
        ResourceLocationRequests.Placement placement = request == null
                ? new ResourceLocationRequests.Placement(null, null, null, null) : request;
        ResourceSite site = StringUtils.hasText(placement.siteCode()) ? siteEntity(placement.siteCode()) : null;
        ResourceFloor floor = StringUtils.hasText(placement.floorCode()) ? floorEntity(placement.floorCode()) : null;
        if (floor != null) {
            if (site == null) {
                site = floor.getSite();
            } else if (!floor.getSite().getId().equals(site.getId())) {
                throw new BadRequestException("L'étage " + floor.getCode() + " n'appartient pas au site " + site.getCode());
            }
        }
        checkPercent(placement.planX(), "planX");
        checkPercent(placement.planY(), "planY");
        resource.setSite(site);
        resource.setFloor(floor);
        resource.setPlanX(floor == null ? null : placement.planX());
        resource.setPlanY(floor == null ? null : placement.planY());
        if (floor != null && !StringUtils.hasText(resource.getFloorLabel())) {
            resource.setFloorLabel(floor.getName());
        }
        resourceRepository.save(resource);
    }

    private static void checkPercent(BigDecimal value, String field) {
        if (value != null && (value.signum() < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BadRequestException(field + " est un pourcentage du plan, entre 0 et 100");
        }
    }

    /** Plan d'un etage a un instant (maintenant par defaut), avec l'etat de chaque ressource. */
    @Transactional(readOnly = true)
    public ResourceLocationResponses.FloorPlan plan(String floorCode, LocalDateTime at) {
        ResourceFloor floor = floorEntity(floorCode);
        LocalDateTime instant = at == null ? LocalDateTime.now(zone(floor.getSite())) : at;
        List<ResourceLocationResponses.PlacedResource> placed = new ArrayList<>();
        for (Resource resource : floorRepository.resources(floor)) {
            placed.add(state(resource, instant));
        }
        return new ResourceLocationResponses.FloorPlan(floor.getCode(), floor.getName(), floor.getLevel(),
                floor.getSite().getCode(), floor.getSite().getName(), floor.getPlanDocumentCode(), instant, placed);
    }

    ResourceLocationResponses.PlacedResource state(Resource resource, LocalDateTime at) {
        String typeCode = resource.getResourceType() == null ? null : resource.getResourceType().getCode();
        if (!Boolean.TRUE.equals(resource.getActive()) || resource.getStatus() != ResourceStatus.ACTIVE) {
            return placed(resource, typeCode, "OUT_OF_SERVICE", null, null);
        }
        List<ResourceAvailability> slots = availabilityRepository.findOverlapping(resource, at, at.plusMinutes(1));
        ResourceAvailability slot = slots == null || slots.isEmpty() ? null : slots.getFirst();
        if (slot == null) {
            return placed(resource, typeCode, "NO_SLOT", null, null);
        }
        int total = slot.getTotalCapacity() == null ? 0 : slot.getTotalCapacity();
        int remaining = slot.getRemainingCapacity() == null ? 0 : slot.getRemainingCapacity();
        if (!Boolean.TRUE.equals(slot.getActive()) || (remaining == total && !Boolean.TRUE.equals(slot.getAvailable()))) {
            return placed(resource, typeCode, "CLOSED", remaining, null);
        }
        if (remaining >= total) {
            return placed(resource, typeCode, "FREE", remaining, null);
        }
        String state = remaining > 0 && !resource.isWholeResourceBooking() ? "PARTIAL" : "OCCUPIED";
        return placed(resource, typeCode, state, remaining, freeAt(resource, slot.getEndedAt()));
    }

    /** Prochain moment libre le meme jour, a partir de la fin du creneau occupe. */
    private LocalDateTime freeAt(Resource resource, LocalDateTime from) {
        List<ResourceAvailability> rest = availabilityRepository.findOverlapping(resource, from,
                from.toLocalDate().plusDays(1).atStartOfDay());
        if (rest == null) {
            return null;
        }
        return rest.stream()
                .sorted(Comparator.comparing(ResourceAvailability::getStartedAt))
                .filter(slot -> Boolean.TRUE.equals(slot.getActive()) && slot.getTotalCapacity() != null
                        && slot.getRemainingCapacity() != null && slot.getRemainingCapacity() >= slot.getTotalCapacity())
                .map(ResourceAvailability::getStartedAt)
                .findFirst()
                .orElse(null);
    }

    private static ResourceLocationResponses.PlacedResource placed(Resource resource, String typeCode, String state,
                                                                   Integer remaining, LocalDateTime freeAt) {
        return new ResourceLocationResponses.PlacedResource(resource.getCode(), resource.getName(), typeCode,
                resource.getCapacity(), resource.getPlanX(), resource.getPlanY(), state, remaining, freeAt);
    }

    static ZoneId zone(ResourceSite site) {
        try {
            return site == null || site.getTimeZone() == null ? ZoneId.of("Africa/Brazzaville") : ZoneId.of(site.getTimeZone());
        } catch (DateTimeException ex) {
            return ZoneId.of("Africa/Brazzaville");
        }
    }

    private String code(String sequence, String prefix) {
        long seq = CodeComposer.extractSeq(sequenceGenerator.next(sequence, com.sni.bokaticowork.core.utils.time.AppTime.today()));
        return CodeComposer.refWithYear(prefix, com.sni.bokaticowork.core.utils.time.AppTime.today(), seq);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
