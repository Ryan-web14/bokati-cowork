package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceClosureRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceClosureResponse;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceClosure;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceClosureRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceClosureService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class ResourceClosureServiceImpl implements ResourceClosureService {

    private final ResourceClosureRepository closureRepository;
    private final ResourceAvailabilityRepository availabilityRepository;
    private final ResourceService resourceService;

    private com.sni.bokaticowork.features.ressource.service.support.ResourceImpactGuard impactGuard;

    @org.springframework.beans.factory.annotation.Autowired
    void setImpactGuard(com.sni.bokaticowork.features.ressource.service.support.ResourceImpactGuard impactGuard) {
        this.impactGuard = impactGuard;
    }

    @Override
    public com.sni.bokaticowork.features.ressource.dto.response.ResourceClosureResponse createClosure(CreateResourceClosureRequest request) {
        List<String> errors = validateRequest(request);
        if (!errors.isEmpty()) {
            throw new ValidationException("Demande de fermeture invalide", errors);
        }

        Resource resource = resourceService.getResourceForService(request.getResourceCode().trim());
        // Lot R2 (D4) : reservations de la plage, refus ou annulation explicite.
        if (request.getActive() == null || Boolean.TRUE.equals(request.getActive())) {
            ResourceClosure draft = ResourceClosure.builder().resource(resource).startedAt(request.getStartedAt())
                    .endedAt(request.getEndedAt()).recurrence(closureRecurrence(request.getRecurrence()))
                    .recurrenceUntil(request.getRecurrenceUntil()).build();
            for (LocalDateTime[] occurrence : upcoming(draft)) {
                if (impactGuard != null) impactGuard.check(resource, occurrence[0], occurrence[1], Boolean.TRUE.equals(request.getCancelBookings()),
                        "fermer la ressource", "Fermeture de " + resource.getCode()
                                + (StringUtils.hasText(request.getReason()) ? " : " + request.getReason().trim() : ""));
            }
        }
        ResourceClosure closure = ResourceClosure.builder()
                .resource(resource)
                .startedAt(request.getStartedAt())
                .endedAt(request.getEndedAt())
                .reason(StringUtils.hasText(request.getReason()) ? request.getReason().trim() : null)
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .kind(closureKind(request.getKind()))
                .recurrence(closureRecurrence(request.getRecurrence()))
                .recurrenceUntil(request.getRecurrenceUntil())
                .build();
        assertRecurrenceFits(closure);

        ResourceClosure savedClosure = closureRepository.save(closure);
        if (Boolean.TRUE.equals(savedClosure.getActive())) {
            applyClosureToAvailability(savedClosure);
        }
        return toResponse(savedClosure);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceClosureResponse getClosure(Long id) {
        return toResponse(getClosureForService(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ResourceClosureResponse> list(Pageable pageable) {
        Page<ResourceClosureResponse> page = closureRepository.findAll(pageable).map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ResourceClosureResponse> listByResource(String resourceCode, Pageable pageable) {
        Resource resource = resourceService.getResourceForService(resourceCode);
        Page<ResourceClosureResponse> page = closureRepository.findAllByResource(resource, pageable).map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    public void updateActive(Long id, Boolean active) {
        updateActive(id, active, false);
    }

    @Override
    public void updateActive(Long id, Boolean active, boolean cancelBookings) {
        if (active == null) {
            throw new BadRequestException("L'état actif de la fermeture est requis");
        }
        ResourceClosure closure = getClosureForService(id);
        boolean wasActive = Boolean.TRUE.equals(closure.getActive());
        if (!wasActive && Boolean.TRUE.equals(active)) {
            for (LocalDateTime[] occurrence : upcoming(closure)) {
                if (impactGuard != null) impactGuard.check(closure.getResource(), occurrence[0], occurrence[1], cancelBookings,
                        "fermer la ressource", "Fermeture de " + closure.getResource().getCode()
                                + (StringUtils.hasText(closure.getReason()) ? " : " + closure.getReason() : ""));
            }
        }
        closure.setActive(active);
        ResourceClosure savedClosure = closureRepository.save(closure);

        if (!wasActive && Boolean.TRUE.equals(active)) {
            applyClosureToAvailability(savedClosure);
            return;
        }
        if (wasActive && !Boolean.TRUE.equals(active)) {
            restoreAvailability(savedClosure);
        }
    }

    @Override
    public void deleteClosure(Long id) {
        ResourceClosure closure = getClosureForService(id);
        boolean wasActive = Boolean.TRUE.equals(closure.getActive());
        closureRepository.delete(closure);
        if (wasActive) {
            restoreAvailability(closure);
        }
    }

    private ResourceClosure getClosureForService(Long id) {
        if (id == null) {
            throw new BadRequestException("L'identifiant de la fermeture est requis");
        }
        return closureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fermeture introuvable : " + id));
    }

    /** Lot R5a : occurrences a venir d'une fermeture, jusqu'a 400 jours (horizon des creneaux largement couvert). */
    private static List<LocalDateTime[]> upcoming(ResourceClosure closure) {
        LocalDateTime now = LocalDateTime.now();
        if (closure.getRecurrence() == null || closure.getRecurrence() == com.sni.bokaticowork.features.ressource.model.ResourceClosureRecurrence.NONE) {
            return closure.occurrences(null, null);
        }
        return closure.occurrences(now, now.plusDays(400));
    }

    private void applyClosureToAvailability(ResourceClosure closure) {
        List<ResourceAvailability> slots = new ArrayList<>();
        for (LocalDateTime[] occurrence : upcoming(closure)) {
            slots.addAll(availabilityRepository.lockOverlappingSlots(closure.getResource(), occurrence[0], occurrence[1]));
        }
        if (slots.isEmpty()) {
            return;
        }

        List<ResourceAvailability> changedSlots = new ArrayList<>();
        for (ResourceAvailability slot : slots) {
            if (!Boolean.FALSE.equals(slot.getAvailable())) {
                slot.setAvailable(Boolean.FALSE);
                changedSlots.add(slot);
            }
        }
        if (!changedSlots.isEmpty()) {
            availabilityRepository.saveAll(changedSlots);
        }
    }

    private void restoreAvailability(ResourceClosure closure) {
        List<ResourceAvailability> slots = new ArrayList<>();
        for (LocalDateTime[] occurrence : upcoming(closure)) {
            slots.addAll(availabilityRepository.lockOverlappingSlots(closure.getResource(), occurrence[0], occurrence[1]));
        }
        if (slots.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<ResourceAvailability> changedSlots = new ArrayList<>();
        for (ResourceAvailability slot : slots) {
            boolean shouldBeAvailable = Boolean.TRUE.equals(slot.getActive())
                    && slot.getEndedAt() != null
                    && slot.getEndedAt().isAfter(now)
                    && slot.getRemainingCapacity() != null
                    && slot.getRemainingCapacity() > 0
                    && !closedDuring(closure.getResource(), slot.getStartedAt(), slot.getEndedAt());

            if (!Boolean.valueOf(shouldBeAvailable).equals(slot.getAvailable())) {
                slot.setAvailable(shouldBeAvailable);
                changedSlots.add(slot);
            }
        }
        if (!changedSlots.isEmpty()) {
            availabilityRepository.saveAll(changedSlots);
        }
    }

    private ResourceClosureResponse toResponse(ResourceClosure closure) {
        return ResourceClosureResponse.builder()
                .id(closure.getId())
                .resourceCode(closure.getResource().getCode())
                .startedAt(closure.getStartedAt())
                .endedAt(closure.getEndedAt())
                .active(closure.getActive())
                .reason(closure.getReason())
                .kind(closure.getKind() == null ? null : closure.getKind().name())
                .recurrence(closure.getRecurrence() == null ? null : closure.getRecurrence().name())
                .recurrenceUntil(closure.getRecurrenceUntil())
                .build();
    }

    private static com.sni.bokaticowork.features.ressource.model.ResourceClosureKind closureKind(String value) {
        if (!StringUtils.hasText(value)) {
            return com.sni.bokaticowork.features.ressource.model.ResourceClosureKind.CLOSURE;
        }
        try {
            return com.sni.bokaticowork.features.ressource.model.ResourceClosureKind.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Nature de blocage invalide : CLOSURE, MAINTENANCE, CLEANING, PRIVATE_EVENT ou OTHER");
        }
    }

    private static com.sni.bokaticowork.features.ressource.model.ResourceClosureRecurrence closureRecurrence(String value) {
        if (!StringUtils.hasText(value)) {
            return com.sni.bokaticowork.features.ressource.model.ResourceClosureRecurrence.NONE;
        }
        try {
            return com.sni.bokaticowork.features.ressource.model.ResourceClosureRecurrence.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Répétition invalide : NONE, WEEKLY ou YEARLY");
        }
    }

    /** Une occurrence ne deborde pas sur la suivante : une semaine au plus chaque semaine, un an chaque annee. */
    private static void assertRecurrenceFits(ResourceClosure closure) {
        java.time.Duration length = java.time.Duration.between(closure.getStartedAt(), closure.getEndedAt());
        switch (closure.getRecurrence()) {
            case WEEKLY -> {
                if (length.compareTo(java.time.Duration.ofDays(7)) > 0) {
                    throw new BadRequestException("Une fermeture hebdomadaire dure au plus une semaine");
                }
            }
            case YEARLY -> {
                if (length.compareTo(java.time.Duration.ofDays(365)) > 0) {
                    throw new BadRequestException("Une fermeture annuelle dure au plus un an");
                }
            }
            default -> {
            }
        }
        if (closure.getRecurrenceUntil() != null && closure.getRecurrenceUntil().isBefore(closure.getStartedAt().toLocalDate())) {
            throw new BadRequestException("La fin de la répétition doit suivre la première occurrence");
        }
    }

    private List<String> validateRequest(CreateResourceClosureRequest request) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Demande de fermeture invalide : le corps de la demande est requis");
            return errors;
        }
        if (!StringUtils.hasText(request.getResourceCode())) {
            errors.add("Demande de fermeture invalide : le code de la ressource est requis");
        }
        if (request.getStartedAt() == null) {
            errors.add("Demande de fermeture invalide : la date de début est requise");
        }
        if (request.getEndedAt() == null) {
            errors.add("Demande de fermeture invalide : la date de fin est requise");
        }
        if (request.getStartedAt() != null && request.getEndedAt() != null && !request.getEndedAt().isAfter(request.getStartedAt())) {
            errors.add("Demande de fermeture invalide : la date de fin doit suivre la date de début");
        }
        return errors;
    }

    /** Lot R5a : une fermeture, unique ou repetee, couvre-t-elle une partie de la plage ? */
    private boolean closedDuring(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt) {
        if (closureRepository.existsActiveOverlap(resource, startedAt, endedAt)) {
            return true;
        }
        List<com.sni.bokaticowork.features.ressource.model.ResourceClosure> recurring = closureRepository.findActiveRecurring(resource);
        return recurring != null && recurring.stream().anyMatch(closure -> closure.occursWithin(startedAt, endedAt));
    }
}
