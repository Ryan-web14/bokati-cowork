package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.core.retry.policy.RetryPolicy;
import com.sni.bokaticowork.core.retry.service.interfaces.RetryExecutor;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.validation.ValidationUtils;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceTypeRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceTypeRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceTypeResponse;
import com.sni.bokaticowork.features.ressource.mapper.interfaces.ResourceTypeMapper;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceTypeRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceTypeService;
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
public class ResourceTypeServiceImpl implements ResourceTypeService {

    private final ResourceTypeRepository typeRepository;
    private final ResourceTypeMapper typeMapper;
    private final ResourceRepository resourceRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final RetryExecutor retryExecutor;
    private final RetryPolicy defaultRetryPolicy;

    @Override
    public void createType(CreateResourceTypeRequest request) {
        List<String> errors = validateForCreate(request);

        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid resource type request", errors);
        }

        String normalizedName = request.getName().trim();

        if (typeRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ResourceAlreadyExistException("A resource type with this name already exists");
        }

        request.setName(normalizedName);
        request.setDescription(normalizeText(request.getDescription()));

        ResourceType type = typeMapper.toEntity(request);
        long typeSeq = CodeComposer.extractSeq(sequenceGenerator.next("resource_type", LocalDate.now()));
        type.setCode(CodeComposer.refWithYear("RTY", LocalDate.now(), typeSeq));

        retryExecutor.execute("create resource type", defaultRetryPolicy, () -> typeRepository.save(type));
    }

    @Override
    public void updateType(String code, UpdateResourceTypeRequest request) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Resource type code is required");
        }

        ResourceType type = getTypeForService(code.trim());
        List<String> errors = validateForUpdate(request, type);

        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid resource type request", errors);
        }

        String normalizedName = request.getName() == null ? type.getName() : request.getName().trim();

        if (!type.getName().equalsIgnoreCase(normalizedName)
                && typeRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ResourceAlreadyExistException("A resource type with this name already exists");
        }

        if (request.getName() != null) {
            request.setName(normalizedName);
        }
        request.setDescription(normalizeText(request.getDescription()));

        typeMapper.updateEntity(type, request);
        retryExecutor.execute("update resource type", defaultRetryPolicy, () -> typeRepository.save(type));
    }

    @Override
    public void deleteType(String code) {
        ResourceType type = getTypeForService(code);

        if (!resourceRepository.findAllByResourceType(type).isEmpty()) {
            throw new ConflictException("resource type", "it is currently assigned to one or more resources");
        }

        retryExecutor.run("delete resource type", defaultRetryPolicy, () -> typeRepository.delete(type));
    }

    @Override
    public void deleteAllTypes() {
        List<ResourceType> types = typeRepository.findAll();

        if (types.isEmpty()) {
            return;
        }

        boolean hasAssignedTypes = types.stream()
                .anyMatch(type -> !resourceRepository.findAllByResourceType(type).isEmpty());

        if (hasAssignedTypes) {
            throw new ConflictException("resource types", "some types are currently assigned to resources");
        }

        retryExecutor.run("delete all resource types", defaultRetryPolicy, () -> typeRepository.deleteAll(types));
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceTypeResponse getType(String code) {
        return typeMapper.toResponse(getTypeForService(code));
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceType getTypeForService(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Resource type code is required");
        }

        return typeRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Resource type with code " + code.trim() + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceType getTypeForService(Long id) {
        if (id == null) {
            throw new BadRequestException("Resource type id is required");
        }

        return typeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource type with id " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ResourceTypeResponse> list(Pageable pageable) {
        Page<ResourceTypeResponse> page = typeRepository.findAll(pageable)
                .map(typeMapper::toResponse);

        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ResourceTypeResponse> search(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Search query is required");
        }

        List<ResourceTypeResponse> responses = typeRepository.basicSearch(query.trim()).stream()
                .map(typeMapper::toResponse)
                .toList();

        return new PaginatedResponse<>(new PageImpl<>(responses));
    }

    private List<String> validateForCreate(CreateResourceTypeRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Invalid resource type request, the request body is required");
        } else {
            validateCommonFields(errors, request.getName(), request.getDescription());
        }

        return errors;
    }

    private List<String> validateForUpdate(UpdateResourceTypeRequest request, ResourceType currentType) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Invalid resource type request, the request body is required");
        } else {
            validateCommonFields(
                    errors,
                    request.getName() != null ? request.getName() : currentType.getName(),
                    request.getDescription() != null ? request.getDescription() : currentType.getDescription()
            );
        }

        return errors;
    }

    private void validateCommonFields(List<String> errors, String name, String description) {
        if (!StringUtils.hasText(name) || !ValidationUtils.validateString(name.trim())) {
            errors.add("Invalid resource type request, the name is not valid");
        }

        if (StringUtils.hasText(description) && !ValidationUtils.validateString(description.trim())) {
            errors.add("Invalid resource type request, the description is not valid");
        }
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}
