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

    @Override
    public void createClosure(CreateResourceClosureRequest request) {
        List<String> errors = validateRequest(request);
        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid resource closure request", errors);
        }

        Resource resource = resourceService.getResourceForService(request.getResourceCode().trim());
        ResourceClosure closure = ResourceClosure.builder()
                .resource(resource)
                .startedAt(request.getStartedAt())
                .endedAt(request.getEndedAt())
                .reason(StringUtils.hasText(request.getReason()) ? request.getReason().trim() : null)
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .build();

        ResourceClosure savedClosure = closureRepository.save(closure);
        if (Boolean.TRUE.equals(savedClosure.getActive())) {
            applyClosureToAvailability(savedClosure);
        }
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
        if (active == null) {
            throw new BadRequestException("Closure active flag is required");
        }
        ResourceClosure closure = getClosureForService(id);
        boolean wasActive = Boolean.TRUE.equals(closure.getActive());
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
            throw new BadRequestException("Closure id is required");
        }
        return closureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource closure with id " + id + " not found"));
    }

    private void applyClosureToAvailability(ResourceClosure closure) {
        List<ResourceAvailability> slots = availabilityRepository.lockOverlappingSlots(
                closure.getResource(),
                closure.getStartedAt(),
                closure.getEndedAt()
        );
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
        List<ResourceAvailability> slots = availabilityRepository.lockOverlappingSlots(
                closure.getResource(),
                closure.getStartedAt(),
                closure.getEndedAt()
        );
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
                    && !closureRepository.existsActiveOverlap(closure.getResource(), slot.getStartedAt(), slot.getEndedAt());

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
                .build();
    }

    private List<String> validateRequest(CreateResourceClosureRequest request) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Invalid resource closure request, the request body is required");
            return errors;
        }
        if (!StringUtils.hasText(request.getResourceCode())) {
            errors.add("Invalid resource closure request, the resource code is required");
        }
        if (request.getStartedAt() == null) {
            errors.add("Invalid resource closure request, the start date is required");
        }
        if (request.getEndedAt() == null) {
            errors.add("Invalid resource closure request, the end date is required");
        }
        if (request.getStartedAt() != null && request.getEndedAt() != null && !request.getEndedAt().isAfter(request.getStartedAt())) {
            errors.add("Invalid resource closure request, the end date must be after the start date");
        }
        return errors;
    }
}
