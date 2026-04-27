package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.retry.policy.RetryPolicy;
import com.sni.bokaticowork.core.retry.service.interfaces.RetryExecutor;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.core.utils.validation.ValidationUtils;
import com.sni.bokaticowork.features.ressource.dto.request.CreateAmenityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateAmenityRequest;
import com.sni.bokaticowork.features.ressource.dto.response.AmenityResponse;
import com.sni.bokaticowork.features.ressource.mapper.interfaces.ResourceAmenitiesMapper;
import com.sni.bokaticowork.features.ressource.model.ResourceAmenities;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAmenitiesRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAmenitiesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ResourceAmenitiesServiceImpl implements ResourceAmenitiesService {

    private final ResourceAmenitiesRepository amenitiesRepository;
    private final ResourceAmenitiesMapper amenitiesMapper;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final RetryExecutor retryExecutor;
    private final RetryPolicy defaultRetryPolicy;

    @Override
    public void createAmenity(CreateAmenityRequest request) {
        List<String> errors = validateForCreate(request);

        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid amenity request", errors);
        }

        String normalizedName = request.getName().trim();

        if (amenitiesRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ResourceAlreadyExistException("An amenity with this name already exists");
        }

        request.setName(normalizedName);
        request.setDescription(normalizeText(request.getDescription()));

        ResourceAmenities amenity = amenitiesMapper.toEntity(request);
        LocalDate businessDate = LocalDate.now();
        amenity.setCode(buildAmenityCode(normalizedName, businessDate));

        retryExecutor.execute("create resource amenity", defaultRetryPolicy, () -> amenitiesRepository.save(amenity));
    }

    @Override
    public void updateAmenity(String code, UpdateAmenityRequest request) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Amenity code is required");
        }

        ResourceAmenities amenity = getAmenityForService(code.trim());
        List<String> errors = validateForUpdate(request, amenity);

        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid amenity request", errors);
        }

        String normalizedName = request.getName() == null ? amenity.getName() : request.getName().trim();

        if (!amenity.getName().equalsIgnoreCase(normalizedName)
                && amenitiesRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ResourceAlreadyExistException("An amenity with this name already exists");
        }

        if (request.getName() != null) {
            request.setName(normalizedName);
        }
        request.setDescription(normalizeText(request.getDescription()));

        amenitiesMapper.updateEntity(amenity, request);
        retryExecutor.execute("update resource amenity", defaultRetryPolicy, () -> amenitiesRepository.save(amenity));
    }

    @Override
    public void deleteAmenity(String code) {
        ResourceAmenities amenity = getAmenityForService(code);

        if (Boolean.TRUE.equals(amenitiesRepository.existsLinkedResources(amenity.getId()))) {
            throw new ConflictException("resource amenity", "it is currently linked to one or more resources");
        }

        retryExecutor.run("delete resource amenity", defaultRetryPolicy, () -> amenitiesRepository.delete(amenity));
    }

    @Override
    public void deleteAllAmenities() {
        List<ResourceAmenities> amenities = amenitiesRepository.findAll();

        if (amenities.isEmpty()) {
            return;
        }

        boolean hasLinkedAmenities = amenities.stream()
                .anyMatch(amenity -> Boolean.TRUE.equals(amenitiesRepository.existsLinkedResources(amenity.getId())));

        if (hasLinkedAmenities) {
            throw new ConflictException("resource amenities", "some amenities are currently linked to resources");
        }

        retryExecutor.run("delete all resource amenities", defaultRetryPolicy, () -> amenitiesRepository.deleteAll(amenities));
    }

    @Override
    @Transactional(readOnly = true)
    public AmenityResponse getAmenity(String code) {
        return amenitiesMapper.toResponse(getAmenityForService(code));
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceAmenities getAmenityForService(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Amenity code is required");
        }

        return amenitiesRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Amenity with code " + code.trim() + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceAmenities getAmenityForService(Long id) {
        if (id == null) {
            throw new BadRequestException("Amenity id is required");
        }

        return amenitiesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity with id " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<AmenityResponse> list(Pageable pageable) {
        Page<AmenityResponse> page = amenitiesRepository.findAll(pageable)
                .map(amenitiesMapper::toResponse);

        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<AmenityResponse> search(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Search query is required");
        }

        List<AmenityResponse> responses = amenitiesRepository.basicSearch(query.trim()).stream()
                .map(amenitiesMapper::toResponse)
                .toList();

        return new PaginatedResponse<>(new PageImpl<>(responses));
    }

    private List<String> validateForCreate(CreateAmenityRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Invalid amenity request, the request body is required");
        } else {
            validateCommonFields(errors, request.getName(), request.getDescription());
        }

        return errors;
    }

    private List<String> validateForUpdate(UpdateAmenityRequest request, ResourceAmenities currentAmenity) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Invalid amenity request, the request body is required");
        } else {
            validateCommonFields(
                    errors,
                    request.getName() != null ? request.getName() : currentAmenity.getName(),
                    request.getDescription() != null ? request.getDescription() : currentAmenity.getDescription()
            );
        }

        return errors;
    }

    private void validateCommonFields(List<String> errors, String name, String description) {
        if (!StringUtils.hasText(name) || !ValidationUtils.validateString(name.trim())) {
            errors.add("Invalid amenity request, the name is not valid");
        }

        if (StringUtils.hasText(description) && !ValidationUtils.validateString(description.trim())) {
            errors.add("Invalid amenity request, the description is not valid");
        }
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private String buildAmenityCode(String amenityName, LocalDate businessDate) {
        long amenitySeq = CodeComposer.extractSeq(sequenceGenerator.next("resource_amenity", businessDate));
        return CodeComposer.withMonth("AMN", CodeComposer.abbrev(amenityName), businessDate, amenitySeq);
    }
}
