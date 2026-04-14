package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentRequirementRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentRequirementResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.mapper.interfaces.DocumentRequirementMapper;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentRequirement;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRequirementRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentTypeService;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentRequirementServiceImpl implements DocumentRequirementService {

    private final DocumentRequirementRepository repository;
    private final DocumentTypeService documentTypeService;
    private final DocumentRequirementMapper mapper;

    @Override
    public DocumentRequirementResponse create(DocumentRequirementRequest request) {
        validateAndNormalize(request);
        DocumentRequirement entity = mapper.toEntity(request);
        if (repository.existsByOwnerTypeAndDocumentTypeCodeAndCustomerTypeAndBusinessLegalForm(
                entity.getOwnerType(),
                entity.getDocumentTypeCode(),
                entity.getCustomerType(),
                entity.getBusinessLegalForm()
        )) {
            throw new ResourceAlreadyExistException("Document requirement already exists");
        }
        repository.save(entity);
        return mapper.toResponse(entity);
    }

    @Override
    public DocumentRequirementResponse update(Long id, DocumentRequirementRequest request) {
        DocumentRequirement entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document requirement not found"));
        validateAndNormalize(request);
        mapper.updateEntity(entity, request);
        repository.save(entity);
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentRequirementResponse get(Long id) {
        return mapper.toResponse(serviceById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentRequirementResponse> list(DocumentOwnerType ownerType, Boolean active) {
        List<DocumentRequirement> items;
        if (ownerType == null) {
            items = repository.findAll();
        } else if (active == null) {
            items = repository.findAllByOwnerTypeOrderByDocumentTypeNameAsc(ownerType);
        } else if (Boolean.TRUE.equals(active)) {
            items = repository.findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(ownerType);
        } else {
            items = repository.findAllByOwnerTypeOrderByDocumentTypeNameAsc(ownerType).stream()
                    .filter(item -> Boolean.FALSE.equals(item.getActive()))
                    .toList();
        }
        if (active != null && ownerType == null) {
            items = items.stream()
                    .filter(item -> active.equals(item.getActive()))
                    .toList();
        }
        return items.stream().map(mapper::toResponse).toList();
    }

    @Override
    public DocumentRequirementResponse activate(Long id) {
        DocumentRequirement entity = serviceById(id);
        entity.setActive(Boolean.TRUE);
        repository.save(entity);
        return mapper.toResponse(entity);
    }

    @Override
    public DocumentRequirementResponse deactivate(Long id) {
        DocumentRequirement entity = serviceById(id);
        entity.setActive(Boolean.FALSE);
        repository.save(entity);
        return mapper.toResponse(entity);
    }

    @Override
    public void delete(Long id) {
        deactivate(id);
    }

    private void validateAndNormalize(DocumentRequirementRequest request) {
        if (request.getOwnerType() == null) {
            throw new BadRequestException("Document requirement owner type is required");
        }
        if (!StringUtils.hasText(request.getDocumentTypeCode())) {
            throw new BadRequestException("Document type code is required");
        }
        documentTypeService.serviceByCode(request.getDocumentTypeCode());
        request.setDocumentTypeCode(request.getDocumentTypeCode().trim().toUpperCase(Locale.ROOT));
        request.setDocumentTypeName(StringUtils.hasText(request.getDocumentTypeName()) ? request.getDocumentTypeName().trim() : request.getDocumentTypeCode());
        request.setCustomerType(StringUtils.hasText(request.getCustomerType()) ? request.getCustomerType().trim().toUpperCase(Locale.ROOT) : null);
        request.setBusinessLegalForm(StringUtils.hasText(request.getBusinessLegalForm()) ? request.getBusinessLegalForm().trim().toUpperCase(Locale.ROOT) : null);
    }

    private DocumentRequirement serviceById(Long id) {
        if (id == null) {
            throw new BadRequestException("Document requirement id is required");
        }
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document requirement not found"));
    }

}
