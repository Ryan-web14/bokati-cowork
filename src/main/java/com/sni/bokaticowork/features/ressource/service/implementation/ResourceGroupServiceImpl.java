package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.validation.ValidationUtils;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceGroupRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceGroupRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceGroupResponse;
import com.sni.bokaticowork.features.ressource.mapper.interfaces.ResourceGroupMapper;
import com.sni.bokaticowork.features.ressource.model.ResourceGroup;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceGroupRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Transactional
@Service
@Slf4j
public class ResourceGroupServiceImpl implements ResourceGroupService {

    private final ResourceGroupRepository groupRepo;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final ResourceGroupMapper groupMapper;

    @Override
    public void createResourceGroup(CreateResourceGroupRequest request) {
        validateForCreate(request);

        log.debug("Creating resource group");

        if (groupRepo.existsByNameIgnoreCase(request.getName().trim())) {
            throw new ResourceAlreadyExistException("A resource group with this name already exists");
        }

        ResourceGroup group = groupMapper.toEntity(request);
        long groupSeq = CodeComposer.extractSeq(sequenceGenerator.next("resource_group", LocalDate.now()));
        group.setCode(CodeComposer.refWithYear("RGP", LocalDate.now(), groupSeq));
        groupRepo.save(group);
    }

    @Override
    public void updateResourceGroup(String code, UpdateResourceGroupRequest request) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Resource group code is required");
        }

        validateForUpdate(request);

        ResourceGroup group = getResourceGroupForService(code.trim());
        String normalizedName = request.getName().trim();

        if (!group.getName().equalsIgnoreCase(normalizedName)
                && groupRepo.existsByNameIgnoreCase(normalizedName)) {
            throw new ResourceAlreadyExistException("A resource group with this name already exists");
        }

        request.setName(normalizedName.toUpperCase());
        request.setDescription(normalizeText(request.getDescription()));

        groupMapper.updateEntity(group, request);
        groupRepo.save(group);
    }

    @Override
    public void deleteResourceGroup(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Resource group code is required");
        }

        ResourceGroup group = getResourceGroupForService(code.trim());
        groupRepo.delete(group);
    }

    @Override
    public void deleteAllResourceGroups() {
        groupRepo.deleteAll();
    }

    @Override
    public ResourceGroupResponse getResourceGroup(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Resource group code is required");
        }

        return groupMapper.toResponse(getResourceGroupForService(code.trim()));
    }

    @Override
    public PaginatedResponse<ResourceGroupResponse> search(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Search query is required");
        }

        List<ResourceGroupResponse> responses = groupRepo.basicSearch(query.trim()).stream()
                .map(groupMapper::toResponse)
                .toList();

        return new PaginatedResponse<>(new PageImpl<>(responses));
    }



    @Override
    public ResourceGroup getResourceGroupForService(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Resource group code is required");
        }

        return groupRepo.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Resource group with code " + code.trim() + " not found"));
    }

    @Override
    public PaginatedResponse<ResourceGroupResponse> list() {
        List<ResourceGroupResponse> responses = groupRepo.findAll().stream()
                .map(groupMapper::toResponse)
                .toList();

        return new PaginatedResponse<>(new PageImpl<>(responses));
    }

    private List<String> validateForCreate(CreateResourceGroupRequest request){
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Invalid resource group request, the request body is required");
        } else {
            if (!StringUtils.hasText(request.getName())
                    || !ValidationUtils.validateString(request.getName().trim())) {
                errors.add("Invalid resource group request, the name is not valid");
            }

            if (StringUtils.hasText(request.getDescription())
                    && !ValidationUtils.validateString(request.getDescription().trim())) {
                errors.add("Invalid resource group request, the description is not valid");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid resource group request", errors);
        }

        return errors;
    }

    private List<String> validateForUpdate(UpdateResourceGroupRequest request){
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Invalid resource group request, the request body is required");
        } else {
            if (!StringUtils.hasText(request.getName())
                    || !ValidationUtils.validateString(request.getName().trim())) {
                errors.add("Invalid resource group request, the name is not valid");
            }

            if (StringUtils.hasText(request.getDescription())
                    && !ValidationUtils.validateString(request.getDescription().trim())) {
                errors.add("Invalid resource group request, the description is not valid");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid resource group request", errors);
        }

        return errors;
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}
