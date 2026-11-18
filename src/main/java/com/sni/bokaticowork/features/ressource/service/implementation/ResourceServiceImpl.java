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

    private com.sni.bokaticowork.features.ressource.service.support.ResourceImpactGuard impactGuard;

    @org.springframework.beans.factory.annotation.Autowired
    void setImpactGuard(com.sni.bokaticowork.features.ressource.service.support.ResourceImpactGuard impactGuard) {
        this.impactGuard = impactGuard;
    }

    /** Lot R2 : reservations a venir touchees (apercu avant une suppression, une fermeture ou une maintenance). */
    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.sni.bokaticowork.features.ressource.service.support.ResourceBookingImpact.ImpactedBooking> impactedBookings(
            String code, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return impactGuard == null ? java.util.List.of() : impactGuard.impacted(getResourceForService(code), from, to);
    }

    @Override
    public com.sni.bokaticowork.features.ressource.dto.response.ResourceResponse createResource(ResourceRequest request) {
        validateForCreate(request);

        ResourceType type = resourceTypeService.getTypeForService(normalizeCode(request.getTypeId(), "Le code du type de ressource est requis"));
        ResourceGroup group = resolveGroup(request.getGroupId());
        ResourcePolicy policy = resolvePolicy(request.getPolicyId());

        Resource resource = resourceMapper.toEntity(request);
        long resourceSeq = CodeComposer.extractSeq(sequenceGenerator.next("resource", com.sni.bokaticowork.core.utils.time.AppTime.today()));
        resource.setCode(CodeComposer.withMonth("RES", CodeComposer.abbrev(type.getName()), com.sni.bokaticowork.core.utils.time.AppTime.today(), resourceSeq));
        resource.setResourceType(type);
        resource.setResourceGroup(group);
        resource.setResourcePolicy(policy);
        if (resource.getStatus() == null) {
            resource.setStatus(ResourceStatus.ACTIVE);
        }
        if (resource.getDisplayOrder() == null) {
            resource.setDisplayOrder(0);
        }

        return resourceMapper.toResponse(resourceRepository.save(resource));
    }

    @Override
    public void updateResource(String code, UpdateResourceRequest request) {
        String normalizedCode = normalizeCode(code, "Le code de la ressource est requis");
        validateForUpdate(request);

        Resource resource = getResourceForService(normalizedCode);
        resourceMapper.updateEntity(resource, request);
        applyUpdateFlags(resource, request.getBookingEnabled(), request.getPortalVisible());

        resourceRepository.save(resource);
    }

    @Override
    public void deleteResource(String code) {
        deleteResource(code, false);
    }

    /** Lot R2 (D4) : refus s'il reste des reservations a venir, sauf annulation explicite. */
    @Override
    public void deleteResource(String code, boolean cancelBookings) {
        Resource resource = getResourceForService(code);
        if (impactGuard != null) impactGuard.check(resource, null, null, cancelBookings, "supprimer la ressource", "Ressource " + resource.getCode() + " retirée");
        resource.setDeleted(Boolean.TRUE);
        resource.setActive(Boolean.FALSE);
        resource.setStatus(ResourceStatus.ARCHIVED);
        resourceRepository.save(resource);
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
        resourceRepository.saveAll(resources);
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
        String normalizedCode = normalizeCode(code, "Le code de la ressource est requis");
        return resourceRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ressource introuvable : " + normalizedCode));
    }

    @Override
    public Resource getResourceForService(Long id) {
        if (id == null) {
            throw new BadRequestException("L'identifiant de la ressource est requis");
        }

        return resourceRepository.findById(id)
                .filter(resource -> !Boolean.TRUE.equals(resource.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Ressource introuvable : " + id));
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
        ResourceType type = resourceTypeService.getTypeForService(normalizeCode(typeCode, "Le code du type de ressource est requis"));
        Page<ResourceSummaryResponse> page = resourceRepository.findAllByResourceType(type, pageable)
                .map(resourceMapper::toSummary);
        return new PaginatedResponse<>(page);
    }

    @Override
    public PaginatedResponse<ResourceSummaryResponse> listSummaryByGroup(String groupCode, Pageable pageable) {
        ResourceGroup group = resourceGroupService.getResourceGroupForService(normalizeCode(groupCode, "Le code du groupe de ressources est requis"));
        Page<ResourceSummaryResponse> page = resourceRepository.findAllByResourceGroup(group, pageable)
                .map(resourceMapper::toSummary);
        return new PaginatedResponse<>(page);
    }

    @Override
    public PaginatedResponse<ResourceSummaryResponse> listSummaryByPolicy(String policyCode, Pageable pageable) {
        ResourcePolicy policy = resourcePolicyService.getPolicyForService(normalizeCode(policyCode, "Le code de la politique est requis"));
        Page<ResourceSummaryResponse> page = resourceRepository.findAllByResourcePolicy(policy, pageable)
                .map(resourceMapper::toSummary);
        return new PaginatedResponse<>(page);
    }

    @Override
    public PaginatedResponse<ResourceSummaryResponse> basicSearch(String query, Pageable pageable) {
        List<ResourceSummaryResponse> all = basicSearch(query);
        int from = (int) Math.min(all.size(), pageable.getOffset());
        int to = Math.min(all.size(), from + pageable.getPageSize());
        return new PaginatedResponse<>(new org.springframework.data.domain.PageImpl<>(all.subList(from, to), pageable, all.size()));
    }

    @Override
    public void updateUsageSettings(String code, String usageRules, java.util.List<String> endChecklist) {
        Resource resource = getResourceForService(code);
        resource.setUsageRules(StringUtils.hasText(usageRules) ? usageRules.trim() : null);
        java.util.List<String> points = endChecklist == null ? java.util.List.of()
                : endChecklist.stream().filter(StringUtils::hasText).map(String::trim).toList();
        resource.setEndChecklist(points.isEmpty() ? null : String.join("\n", points));
        resourceRepository.save(resource);
    }

    @Override
    public void updateSalesSettings(String code, Integer includedParticipants, Integer extraParticipantPrice, Integer depositAmount) {
        for (Integer value : new Integer[]{includedParticipants, extraParticipantPrice, depositAmount}) {
            if (value != null && value < 0) {
                throw new BadRequestException("Les réglages de vente sont positifs ou nuls");
            }
        }
        Resource resource = getResourceForService(code);
        resource.setIncludedParticipants(includedParticipants);
        resource.setExtraParticipantPrice(extraParticipantPrice);
        resource.setDepositAmount(depositAmount);
        resourceRepository.save(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ResourceResponse> listDetailed(ResourceSearchCriteria criteria, Pageable pageable) {
        Page<ResourceResponse> page = resourceRepository.findAll(ResourceSpecification.search(criteria), pageable)
                .map(resourceMapper::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    public List<ResourceSummaryResponse> basicSearch(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Le texte de recherche est requis");
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
        String normalizedCode = normalizeCode(code, "Le code de la ressource est requis");
        if (request == null) {
            throw new BadRequestException("La demande de classement de la ressource est requise");
        }

        Resource resource = getResourceForService(normalizedCode);
        ResourceType type = resourceTypeService.getTypeForService(normalizeCode(request.getTypeId(), "Le code du type de ressource est requis"));
        ResourceGroup group = resolveGroup(request.getGroupId());
        ResourcePolicy policy = resolvePolicy(request.getPolicyId());

        resource.setResourceType(type);
        resource.setResourceGroup(group);
        resource.setResourcePolicy(policy);
        resourceRepository.save(resource);
    }

    @Override
    public void updateStatus(String code, ResourceStatus status) {
        updateStatus(code, status, false);
    }

    /** Lot R2 (D4) : maintenance, hors service, inactive ou archivee avec des reservations a venir : refus, sauf annulation explicite. */
    @Override
    public void updateStatus(String code, ResourceStatus status, boolean cancelBookings) {
        if (status == null) {
            throw new BadRequestException("Le statut de la ressource est requis");
        }
        Resource resource = getResourceForService(code);
        if (status != ResourceStatus.ACTIVE && resource.getStatus() == ResourceStatus.ACTIVE) {
            if (impactGuard != null) impactGuard.check(resource, null, null, cancelBookings, "passer en " + status,
                    "Ressource " + resource.getCode() + " indisponible (" + status + ")");
        }
        resource.setStatus(status);
        resourceRepository.save(resource);
    }

    @Override
    public void updateBookingEnabled(String code, Boolean bookingEnabled) {
        if (bookingEnabled == null) {
            throw new BadRequestException("L'indicateur de réservation est requis");
        }
        Resource resource = getResourceForService(code);
        resource.setBookingEnabled(bookingEnabled);
        resourceRepository.save(resource);
    }

    @Override
    public void updatePortalVisible(String code, Boolean portalVisible) {
        if (portalVisible == null) {
            throw new BadRequestException("L'indicateur de visibilité sur le portail est requis");
        }
        Resource resource = getResourceForService(code);
        resource.setPortalVisible(portalVisible);
        resourceRepository.save(resource);
    }

    @Override
    public void updateActive(String code, Boolean active) {
        if (active == null) {
            throw new BadRequestException("L'indicateur actif est requis");
        }
        Resource resource = getResourceForService(code);
        resource.setActive(active);
        resourceRepository.save(resource);
    }

    @Override
    public void updateDisplayOrder(String code, Integer displayOrder) {
        if (displayOrder == null) {
            throw new BadRequestException("L'ordre d'affichage est requis");
        }
        Resource resource = getResourceForService(code);
        resource.setDisplayOrder(displayOrder);
        resourceRepository.save(resource);
    }

    @Override
    public void linkAmenity(LinkAmenityToResourceRequest request) {
        if (request == null || !StringUtils.hasText(request.getResourceCode()) || !StringUtils.hasText(request.getAmenityCode())) {
            throw new BadRequestException("Le code de la ressource et celui de l'équipement sont requis");
        }

        Resource resource = getResourceForService(request.getResourceCode().trim());
        var amenity = resourceAmenitiesService.getAmenityForService(request.getAmenityCode().trim());

        if (resourceAmenityLinkRepository.existsByResourceAndAmenity(resource, amenity)) {
            throw new ResourceAlreadyExistException("Cet équipement est déjà rattaché à la ressource");
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
            throw new ResourceNotFoundException("Cet équipement n'est pas rattaché à la ressource");
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
                "Demande de ressource invalide",
                createRules()
        );
    }

    private void validateForUpdate(UpdateResourceRequest request) {
        validationEngine.validateAndThrow(
                request,
                "Demande de ressource invalide",
                updateRules()
        );
    }

    private List<ValidationRule<ResourceRequest>> createRules() {
        return List.of(
                request -> requireRequestBody(request, "de ressource"),
                request -> requireTextField(request, request == null ? null : request.getTypeId(), "typeId", "Demande de ressource invalide : le type est requis"),
                request -> requireValidTextField(request, request == null ? null : request.getName(), "name", "Demande de ressource invalide : le nom n'est pas valide"),
                request -> optionalDescriptionField(request, request == null ? null : request.getDescription(), "description", "Demande de ressource invalide : la description n'est pas valide"),
                request -> positiveIntegerField(request, request == null ? null : request.getCapacity(), "capacity", "Demande de ressource invalide : la capacité doit être supérieure à zéro"),
                request -> optionalDescriptionField(request, request == null ? null : request.getZone(), "zone", "Demande de ressource invalide : la zone n'est pas valide"),
                request -> optionalDescriptionField(request, request == null ? null : request.getLocationLabel(), "locationLabel", "Demande de ressource invalide : l'emplacement n'est pas valide"),
                request -> validStatusField(request, request == null ? null : request.getStatus())
        );
    }

    private List<ValidationRule<UpdateResourceRequest>> updateRules() {
        return List.of(
                request -> requireRequestBody(request, "de ressource"),
                request -> validateOptionalTextField(request, request == null ? null : request.getName(), "name", "Demande de ressource invalide : le nom n'est pas valide"),
                request -> optionalDescriptionField(request, request == null ? null : request.getDescription(), "description", "Demande de ressource invalide : la description n'est pas valide"),
                request -> positiveIntegerField(request, request == null ? null : request.getCapacity(), "capacity", "Demande de ressource invalide : la capacité doit être supérieure à zéro"),
                request -> optionalDescriptionField(request, request == null ? null : request.getZone(), "zone", "Demande de ressource invalide : la zone n'est pas valide"),
                request -> optionalDescriptionField(request, request == null ? null : request.getLocationLabel(), "locationLabel", "Demande de ressource invalide : l'emplacement n'est pas valide")
        );
    }

    private <T> List<ValidationError> requireRequestBody(T request, String entityName) {
        if (request == null) {
            return List.of(error(null, "required", "Demande " + entityName + " invalide : le corps de la demande est requis"));
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
            return List.of(error("status", "invalid", "Demande de ressource invalide : le statut n'est pas valide"));
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
        ResourcePolicy policy = resourcePolicyService.getPolicyForService(policyCode.trim());
        // Lot R2 : une politique desactivee ne se rattache pas a une ressource.
        if (Boolean.FALSE.equals(policy.getActive())) {
            throw new BadRequestException("La politique " + policy.getCode() + " est désactivée : réactivez-la avant de la rattacher");
        }
        return policy;
    }

    private String normalizeCode(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }
}
