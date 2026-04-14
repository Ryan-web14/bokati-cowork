package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTypeRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentTypeResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.mapper.interfaces.DocumentTypeMapper;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTypeRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentTypeServiceImpl implements DocumentTypeService {

    private final DocumentTypeRepository repository;
    private final DocumentTypeMapper mapper;

    @Override
    public DocumentTypeResponse create(DocumentTypeRequest request) {
        validateRequest(request);
        String code = normalizeCode(request.getCode());
        if (repository.existsByCode(code)) {
            throw new ResourceAlreadyExistException("Document type already exists");
        }

        DocumentType entity = mapper.toEntity(request);
        entity.setCode(code);
        repository.save(entity);
        return mapper.toResponse(entity);
    }

    @Override
    public DocumentTypeResponse update(String code, DocumentTypeRequest request) {
        validateRequest(request);
        DocumentType entity = serviceByCode(code);
        mapper.updateEntity(entity, request);
        repository.save(entity);
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentTypeResponse get(String code) {
        return mapper.toResponse(serviceByCode(code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentTypeResponse> list(DocumentOwnerType ownerType, Boolean active) {
        List<DocumentType> items;
        if (ownerType == null && active == null) {
            items = repository.findAllByOrderByNameAsc();
        } else if (ownerType == null) {
            items = Boolean.TRUE.equals(active)
                    ? repository.findAllByActiveTrueOrderByNameAsc()
                    : repository.findAllByOrderByNameAsc().stream()
                    .filter(item -> Boolean.FALSE.equals(item.getActive()))
                    .toList();
        } else if (active == null) {
            items = repository.findAllByOwnerTypeOrderByNameAsc(ownerType);
        } else if (Boolean.TRUE.equals(active)) {
            items = repository.findAllByOwnerTypeAndActiveTrueOrderByNameAsc(ownerType);
        } else {
            items = repository.findAllByOwnerTypeOrderByNameAsc(ownerType).stream()
                    .filter(item -> Boolean.FALSE.equals(item.getActive()))
                    .toList();
        }
        return items.stream().map(mapper::toResponse).toList();
    }

    @Override
    public DocumentTypeResponse activate(String code) {
        DocumentType entity = serviceByCode(code);
        entity.setActive(Boolean.TRUE);
        repository.save(entity);
        return mapper.toResponse(entity);
    }

    @Override
    public DocumentTypeResponse deactivate(String code) {
        DocumentType entity = serviceByCode(code);
        entity.setActive(Boolean.FALSE);
        repository.save(entity);
        return mapper.toResponse(entity);
    }

    @Override
    public void delete(String code) {
        deactivate(code);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentType serviceByCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Document type code is required");
        }
        return repository.findByCode(normalizeCode(code))
                .orElseThrow(() -> new ResourceNotFoundException("Document type not found"));
    }

    private void validateRequest(DocumentTypeRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Document type name is required");
        }
        if (request.getCategory() == null) {
            throw new BadRequestException("Document type category is required");
        }
    }

    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Document type code is required");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

}
