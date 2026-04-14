package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceClosureRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceClosureResponse;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceClosure;
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

@Service
@Transactional
@RequiredArgsConstructor
public class ResourceClosureServiceImpl implements ResourceClosureService {

    private final ResourceClosureRepository closureRepository;
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

        closureRepository.save(closure);
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
        closure.setActive(active);
        closureRepository.save(closure);
    }

    @Override
    public void deleteClosure(Long id) {
        closureRepository.delete(getClosureForService(id));
    }

    private ResourceClosure getClosureForService(Long id) {
        if (id == null) {
            throw new BadRequestException("Closure id is required");
        }
        return closureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource closure with id " + id + " not found"));
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
