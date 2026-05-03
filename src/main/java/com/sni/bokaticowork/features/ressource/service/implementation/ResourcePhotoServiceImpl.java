package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.ressource.dto.request.ResourcePhotoRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePhotoResponse;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePhoto;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePhotoRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourcePhotoService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ResourcePhotoServiceImpl implements ResourcePhotoService {

    private final ResourcePhotoRepository repository;
    private final ResourceService resourceService;

    @Override
    public ResourcePhotoResponse add(String resourceCode, ResourcePhotoRequest request) {
        validate(request);
        Resource resource = resourceService.getResourceForService(resourceCode);
        ResourcePhoto photo = ResourcePhoto.builder()
                .resource(resource)
                .documentCode(request.getDocumentCode().trim())
                .caption(request.getCaption())
                .cover(Boolean.TRUE.equals(request.getCover()))
                .displayOrder(request.getDisplayOrder())
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .build();
        return toResponse(repository.save(photo));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourcePhotoResponse> list(String resourceCode) {
        Resource resource = resourceService.getResourceForService(resourceCode);
        return repository.findAllByResourceAndActiveTrueOrderByDisplayOrderAscIdAsc(resource).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ResourcePhotoResponse update(Long id, ResourcePhotoRequest request) {
        validate(request);
        ResourcePhoto photo = get(id);
        photo.setDocumentCode(request.getDocumentCode().trim());
        photo.setCaption(request.getCaption());
        photo.setCover(Boolean.TRUE.equals(request.getCover()));
        photo.setDisplayOrder(request.getDisplayOrder());
        photo.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        return toResponse(repository.save(photo));
    }

    @Override
    public void delete(Long id) {
        ResourcePhoto photo = get(id);
        photo.setActive(Boolean.FALSE);
        repository.save(photo);
    }

    private ResourcePhoto get(Long id) {
        if (id == null) {
            throw new BadRequestException("Photo id is required");
        }
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource photo not found: " + id));
    }

    private void validate(ResourcePhotoRequest request) {
        if (request == null || !StringUtils.hasText(request.getDocumentCode())) {
            throw new BadRequestException("Document code is required");
        }
    }

    private ResourcePhotoResponse toResponse(ResourcePhoto photo) {
        return ResourcePhotoResponse.builder()
                .id(photo.getId())
                .resourceCode(photo.getResource().getCode())
                .documentCode(photo.getDocumentCode())
                .caption(photo.getCaption())
                .cover(photo.getCover())
                .displayOrder(photo.getDisplayOrder())
                .active(photo.getActive())
                .build();
    }
}
