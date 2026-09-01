package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.core.retry.policy.RetryPolicy;
import com.sni.bokaticowork.core.retry.service.interfaces.RetryExecutor;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.validation.ValidationUtils;
import com.sni.bokaticowork.core.validation.model.ValidationError;
import com.sni.bokaticowork.core.validation.rule.ValidationRule;
import com.sni.bokaticowork.core.validation.service.interfaces.ValidationEngine;
import com.sni.bokaticowork.features.ressource.dto.request.ChangeTypeRequest;
import com.sni.bokaticowork.features.ressource.dto.request.LinkAmenityToResourceRequest;
import com.sni.bokaticowork.features.ressource.dto.request.ResourceRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceRequest;
import com.sni.bokaticowork.features.ressource.dto.response.AmenityResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceSummaryResponse;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.mapper.interfaces.ResourceMapper;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAmenityLink;
import com.sni.bokaticowork.features.ressource.model.ResourceGroup;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAmenityLinkRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.repository.specification.criteria.ResourceSearchCriteria;
import com.sni.bokaticowork.features.ressource.repository.specification.specification.ResourceSpecification;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAmenitiesService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceGroupService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourcePolicyService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;


@RequiredArgsConstructor
@Transactional
@Service
@Slf4j
public class ResourceServiceImpl implements ResourceService {
    private final ResourceRepository resourceRepository;
    private final ResourceAmenityLinkRepository resourceAmenityLinkRepository;
    private final ResourceTypeService resourceTypeService;
    private final ResourceGroupService resourceGroupService;
    private final ResourcePolicyService resourcePolicyService;
    private final ResourceAmenitiesService resourceAmenitiesService;
    private final ResourceMapper resourceMapper;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final RetryExecutor retryExecutor;
    private final RetryPolicy defaultRetryPolicy;
    private final ValidationEngine validationEngine;

    @Override
    public void createResource(ResourceRequest request) {
        validateForCreate(request);

        ResourceType type = resourceTypeService.getTypeForService(normalizeCode(request.getTypeId(), "Resource type code is required"));
        ResourceGroup group = resolveGroup(request.getGroupId());
        ResourcePolicy policy = resolvePolicy(request.getPolicyId());

        Resource resource = resourceMapper.toEntity(request);
        long resourceSeq = CodeComposer.extractSeq(sequenceGenerator.next("resource", LocalDate.now()));
        resource.setCode(CodeComposer.withMonth("RES", CodeComposer.abbrev(type.getName()), LocalDate.now(), resourceSeq));
        resource.setResourceType(type);
        resource.setResourceGroup(group);
        resource.setResourcePolicy(policy);
        if (resource.getStatus() == null) {
            resource.setStatus(ResourceStatus.ACTIVE);
        }
        if (resource.getDisplayOrder() == null) {
            resource.setDisplayOrder(0);
        }

        retryExecutor.execute("create resource", defaultRetryPolicy, () -> resourceRepository.save(resource));
    }

    @Override
    public void updateResource(String code, UpdateResourceRequest request) {
        String normalizedCode = normalizeCode(code, "Resource code is required");
        validateForUpdate(request);

        Resource resource = getResourceForService(normalizedCode);
        resourceMapper.updateEntity(resource, request);
        applyUpdateFlags(resource, request.getBookingEnabled(), request.getPortalVisible());

        retryExecutor.execute("update resource", defaultRetryPolicy, () -> resourceRepository.save(resource));
    }

    @Override
    public void deleteResource(String code) {
        Resource resource = getResourceForService(code);
        resource.setDeleted(Boolean.TRUE);
        resource.setActive(Boolean.FALSE);
        resource.setStatus(ResourceStatus.ARCHIVED);
        retryExecutor.execute("delete resource", defaultRetryPolicy, () -> resourceRepository.save(resource));
    }

    @Override
    public void deleteAllResources() {
        List<Resource> resources = resourceRepository.findAll();
        if (resources.isEmpty()) {
            return;
        }

        resources.forEach(resource -> {
            resource.setDeleted(Boolean.TRUE);
            resource.setActive(Boolean.FALSE);
            resource.setStatus(ResourceStatus.ARCHIVED);
        });
        retryExecutor.execute("delete all resources", defaultRetryPolicy, () -> resourceRepository.saveAll(resources));
    }

    @Override
    public ResourceResponse getResource(String code) {
        return resourceMapper.toResponse(getResourceForService(code));
    }

    @Override
    public Resource saveResource(Resource resource) {
        return resourceRepository.save(resource);
    }

    @Override
    public Resource getResourceForService(String code) {
        String normalizedCode = normalizeCode(code, "Resource code is required");
        return resourceRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Resource with code " + normalizedCode + " not found"));
    }

    @Override
    public Resource getResourceForService(Long id) {
        if (id == null) {
            throw new BadRequestException("Resource id is required");
        }

        return resourceRepository.findById(id)
                .filter(resource -> !Boolean.TRUE.equals(resource.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Resource with id " + id + " not found"));
    }

    @Override
    public PaginatedResponse<ResourceResponse> list(Pageable pageable) {
        Page<ResourceResponse> page = resourceRepository.findAll(pageable)
                .map(resourceMapper::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    public PaginatedResponse<ResourceSummaryResponse> listSummary(Pageable pageable) {
        Page<ResourceSummaryResponse> page = resourceRepository.findAll(pageable)
                .map(resourceMapper::toSummary);
        return new PaginatedResponse<>(page);
    }

    @Override
    public PaginatedResponse<ResourceSummaryResponse> listSummaryByType(String typeCode, Pageable pageable) {
        ResourceType type = resourceTypeService.getTypeForService(normalizeCode(typeCode, "Resource type code is required"));
        Page<ResourceSummaryResponse> page = resourceRepository.findAllByResourceType(type, pageable)
                .map(resourceMapper::toSummary);
        return new PaginatedResponse<>(page);
    }

    @Override
    public PaginatedResponse<ResourceSummaryResponse> listSummaryByGroup(String groupCode, Pageable pageable) {
        ResourceGroup group = resourceGroupService.getResourceGroupForService(normalizeCode(groupCode, "Resource group code is required"));
        Page<ResourceSummaryResponse> page = resourceRepository.findAllByResourceGroup(group, pageable)
                .map(resourceMapper::toSummary);
        return new PaginatedResponse<>(page);
    }

    @Override
    public PaginatedResponse<ResourceSummaryResponse> listSummaryByPolicy(String policyCode, Pageable pageable) {
        ResourcePolicy policy = resourcePolicyService.getPolicyForService(normalizeCode(policyCode, "Resource policy code is required"));
        Page<ResourceSummaryResponse> page = resourceRepository.findAllByResourcePolicy(policy, pageable)
                .map(resourceMapper::toSummary);
        return new PaginatedResponse<>(page);
    }

    @Override
    public List<ResourceSummaryResponse> basicSearch(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Search query is required");
        }

        return resourceRepository.basicSearch(query.trim()).stream()
                .map(resourceMapper::toSummary)
                .toList();
    }

    @Override
    public PaginatedResponse<ResourceSummaryResponse> search(ResourceSearchCriteria criteria, Pageable pageable) {
        Specification<Resource> specification = ResourceSpecification.search(criteria);
        Page<ResourceSummaryResponse> page = resourceRepository.findAll(specification, pageable)
                .map(resourceMapper::toSummary);
        return new PaginatedResponse<>(page);
    }

    @Override
    public void changeClassification(String code, ChangeTypeRequest request) {
        String normalizedCode = normalizeCode(code, "Resource code is required");
        if (request == null) {
            throw new BadRequestException("Resource classification request is required");
        }

        Resource resource = getResourceForService(normalizedCode);
        ResourceType type = resourceTypeService.getTypeForService(normalizeCode(request.getTypeId(), "Resource type code is required"));
        ResourceGroup group = resolveGroup(request.getGroupId());
        ResourcePolicy policy = resolvePolicy(request.getPolicyId());

        resource.setResourceType(type);
        resource.setResourceGroup(group);
        resource.setResourcePolicy(policy);
        retryExecutor.execute("change resource classification", defaultRetryPolicy, () -> resourceRepository.save(resource));
    }

    @Override
    public void updateStatus(String code, ResourceStatus status) {
        if (status == null) {
            throw new BadRequestException("Resource status is required");
        }
        Resource resource = getResourceForService(code);
        resource.setStatus(status);
        retryExecutor.execute("update resource status", defaultRetryPolicy, () -> resourceRepository.save(resource));
    }

    @Override
    public void updateBookingEnabled(String code, Boolean bookingEnabled) {
        if (bookingEnabled == null) {
            throw new BadRequestException("Booking enabled flag is required");
        }
        Resource resource = getResourceForService(code);
        resource.setBookingEnabled(bookingEnabled);
        retryExecutor.execute("update resource booking flag", defaultRetryPolicy, () -> resourceRepository.save(resource));
    }

    @Override
    public void updatePortalVisible(String code, Boolean portalVisible) {
        if (portalVisible == null) {
            throw new BadRequestException("Portal visible flag is required");
        }
        Resource resource = getResourceForService(code);
        resource.setPortalVisible(portalVisible);
        retryExecutor.execute("update resource portal visibility", defaultRetryPolicy, () -> resourceRepository.save(resource));
    }

    @Override
    public void updateActive(String code, Boolean active) {
        if (active == null) {
            throw new BadRequestException("Active flag is required");
        }
        Resource resource = getResourceForService(code);
        resource.setActive(active);
        retryExecutor.execute("update resource active flag", defaultRetryPolicy, () -> resourceRepository.save(resource));
    }

    @Override
    public void updateDisplayOrder(String code, Integer displayOrder) {
        if (displayOrder == null) {
            throw new BadRequestException("Display order is required");
        }
        Resource resource = getResourceForService(code);
        resource.setDisplayOrder(displayOrder);
        retryExecutor.execute("update resource display order", defaultRetryPolicy, () -> resourceRepository.save(resource));
    }

    @Override
    public void linkAmenity(LinkAmenityToResourceRequest request) {
        if (request == null || !StringUtils.hasText(request.getResourceCode()) || !StringUtils.hasText(request.getAmenityCode())) {
            throw new BadRequestException("Resource code and amenity code are required");
        }

        Resource resource = getResourceForService(request.getResourceCode().trim());
        var amenity = resourceAmenitiesService.getAmenityForService(request.getAmenityCode().trim());

        if (resourceAmenityLinkRepository.existsByResourceAndAmenity(resource, amenity)) {
            throw new ResourceAlreadyExistException("This amenity is already linked to the resource");
        }

        ResourceAmenityLink link = ResourceAmenityLink.builder()
                .resource(resource)
                .amenity(amenity)
                .quantity(request.getQuantity())
                .optional(Boolean.TRUE.equals(request.getOptional()))
                .extraPrice(request.getExtraPrice())
                .build();
        resourceAmenityLinkRepository.save(link);
    }

    @Override
    public void unlinkAmenity(String resourceCode, String amenityCode) {
        Resource resource = getResourceForService(resourceCode);
        var amenity = resourceAmenitiesService.getAmenityForService(amenityCode);

        if (!resourceAmenityLinkRepository.existsByResourceAndAmenity(resource, amenity)) {
            throw new ResourceNotFoundException("Amenity link not found for this resource");
        }

        resourceAmenityLinkRepository.deleteByResourceAndAmenity(resource, amenity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponse> listAmenities(String resourceCode) {
        Resource resource = getResourceForService(resourceCode);
        return resourceAmenityLinkRepository.findAllByResource(resource).stream()
                .map(link -> AmenityResponse.builder()
                        .code(link.getAmenity().getCode())
                        .name(link.getAmenity().getName())
                        .description(link.getAmenity().getDescription())
                        .active(link.getAmenity().getActive())
                        .quantity(link.getQuantity())
                        .optional(link.getOptional())
                        .extraPrice(link.getExtraPrice())
                        .build())
                .toList();
    }

    private AmenityResponse toAmenityResponse(ResourceAmenityLink link) {
        var amenity = link.getAmenity();
        return AmenityResponse.builder()
                .code(amenity.getCode())
                .name(amenity.getName())
                .description(amenity.getDescription())
                .active(amenity.getActive())
                .quantity(link.getQuantity())
                .optional(link.getOptional())
                .extraPrice(link.getExtraPrice())
                .build();
    }

    private void validateForCreate(ResourceRequest request) {
        validationEngine.validateAndThrow(
                request,
                "Invalid resource request",
                createRules()
        );
    }

    private void validateForUpdate(UpdateResourceRequest request) {
        validationEngine.validateAndThrow(
                request,
                "Invalid resource request",
                updateRules()
        );
    }

    private List<ValidationRule<ResourceRequest>> createRules() {
        return List.of(
                request -> requireRequestBody(request, "resource"),
                request -> requireTextField(request, request == null ? null : request.getTypeId(), "typeId", "Invalid resource request, the type is required"),
                request -> requireValidTextField(request, request == null ? null : request.getName(), "name", "Invalid resource request, the name is not valid"),
                request -> optionalDescriptionField(request, request == null ? null : request.getDescription(), "description", "Invalid resource request, the description is not valid"),
                request -> positiveIntegerField(request, request == null ? null : request.getCapacity(), "capacity", "Invalid resource request, the capacity must be greater than zero"),
                request -> optionalDescriptionField(request, request == null ? null : request.getZone(), "zone", "Invalid resource request, the zone is not valid"),
                request -> optionalDescriptionField(request, request == null ? null : request.getLocationLabel(), "locationLabel", "Invalid resource request, the location label is not valid"),
                request -> validStatusField(request, request == null ? null : request.getStatus())
        );
    }

    private List<ValidationRule<UpdateResourceRequest>> updateRules() {
        return List.of(
                request -> requireRequestBody(request, "resource"),
                request -> validateOptionalTextField(request, request == null ? null : request.getName(), "name", "Invalid resource request, the name is not valid"),
                request -> optionalDescriptionField(request, request == null ? null : request.getDescription(), "description", "Invalid resource request, the description is not valid"),
                request -> positiveIntegerField(request, request == null ? null : request.getCapacity(), "capacity", "Invalid resource request, the capacity must be greater than zero"),
                request -> optionalDescriptionField(request, request == null ? null : request.getZone(), "zone", "Invalid resource request, the zone is not valid"),
                request -> optionalDescriptionField(request, request == null ? null : request.getLocationLabel(), "locationLabel", "Invalid resource request, the location label is not valid")
        );
    }

    private <T> List<ValidationError> requireRequestBody(T request, String entityName) {
        if (request == null) {
            return List.of(error(null, "required", "Invalid " + entityName + " request, the request body is required"));
        }
        return List.of();
    }

    private <T> List<ValidationError> requireTextField(T request, String value, String field, String message) {
        if (request == null) {
            return List.of();
        }
        if (!StringUtils.hasText(value)) {
            return List.of(error(field, "required", message));
        }
        return List.of();
    }

    private <T> List<ValidationError> requireValidTextField(T request, String value, String field, String message) {
        if (request == null) {
            return List.of();
        }
        if (!StringUtils.hasText(value) || !ValidationUtils.validateString(value.trim())) {
            return List.of(error(field, "invalid", message));
        }
        return List.of();
    }

    private <T> List<ValidationError> validateOptionalTextField(T request, String value, String field, String message) {
        if (request == null || value == null) {
            return List.of();
        }
        if (!StringUtils.hasText(value) || !ValidationUtils.validateString(value.trim())) {
            return List.of(error(field, "invalid", message));
        }
        return List.of();
    }

    private <T> List<ValidationError> optionalDescriptionField(T request, String value, String field, String message) {
        if (request == null || !StringUtils.hasText(value)) {
            return List.of();
        }
        if (!ValidationUtils.validateDescription(value.trim())) {
            return List.of(error(field, "invalid", message));
        }
        return List.of();
    }

    private <T> List<ValidationError> positiveIntegerField(T request, Integer value, String field, String message) {
        if (request == null || value == null) {
            return List.of();
        }
        if (value < 1) {
            return List.of(error(field, "invalid", message));
        }
        return List.of();
    }

    private List<ValidationError> validStatusField(ResourceRequest request, String status) {
        if (request == null || !StringUtils.hasText(status)) {
            return List.of();
        }
        if (!isValidStatus(status)) {
            return List.of(error("status", "invalid", "Invalid resource request, the status is not valid"));
        }
        return List.of();
    }

    private boolean isValidStatus(String status) {
        try {
            ResourceStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private ValidationError error(String field, String code, String message) {
        return ValidationError.builder()
                .field(field)
                .code(code)
                .message(message)
                .build();
    }

    private void applyUpdateFlags(Resource resource, Boolean bookingEnabled, Boolean portalVisible) {
        if (bookingEnabled != null) {
            resource.setBookingEnabled(bookingEnabled);
        }
        if (portalVisible != null) {
            resource.setPortalVisible(portalVisible);
        }
    }

    private ResourceGroup resolveGroup(String groupCode) {
        if (!StringUtils.hasText(groupCode)) {
            return null;
        }
        return resourceGroupService.getResourceGroupForService(groupCode.trim());
    }

    private ResourcePolicy resolvePolicy(String policyCode) {
        if (!StringUtils.hasText(policyCode)) {
            return null;
        }
        return resourcePolicyService.getPolicyForService(policyCode.trim());
    }

    private String normalizeCode(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }
}
