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
        if (Boolean.TRUE.equals(request.getCover())) {
            clearCover(resource, null);
        }
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
    public ResourcePhotoResponse update(String resourceCode, Long id, ResourcePhotoRequest request) {
        validate(request);
        ResourcePhoto photo = get(resourceCode, id);
        if (Boolean.TRUE.equals(request.getCover())) {
            clearCover(photo.getResource(), photo.getId());
        }
        photo.setDocumentCode(request.getDocumentCode().trim());
        photo.setCaption(request.getCaption());
        photo.setCover(Boolean.TRUE.equals(request.getCover()));
        photo.setDisplayOrder(request.getDisplayOrder());
        photo.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        return toResponse(repository.save(photo));
    }

    @Override
    public void delete(String resourceCode, Long id) {
        ResourcePhoto photo = get(resourceCode, id);
        photo.setActive(Boolean.FALSE);
        repository.save(photo);
    }

    /** Lot R0 : la photo doit appartenir a la ressource du chemin (avant : l'identifiant seul suffisait). */
    private ResourcePhoto get(String resourceCode, Long id) {
        if (id == null) {
            throw new BadRequestException("L'identifiant de la photo est requis");
        }
        ResourcePhoto photo = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Photo introuvable : " + id));
        if (photo.getResource() == null || resourceCode == null || !resourceCode.trim().equalsIgnoreCase(photo.getResource().getCode())) {
            throw new ResourceNotFoundException("Photo introuvable : " + id + " pour la ressource " + resourceCode);
        }
        return photo;
    }

    /** Lot R0 : une seule photo de couverture par ressource. */
    private void clearCover(Resource resource, Long keepId) {
        repository.findAllByResourceAndActiveTrueOrderByDisplayOrderAscIdAsc(resource).stream()
                .filter(other -> Boolean.TRUE.equals(other.getCover()) && !other.getId().equals(keepId))
                .forEach(other -> {
                    other.setCover(Boolean.FALSE);
                    repository.save(other);
                });
    }

    private com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository documentRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setDocumentRepository(com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    private void validate(ResourcePhotoRequest request) {
        if (request == null || !StringUtils.hasText(request.getDocumentCode())) {
            throw new BadRequestException("Le code du document de la photo est requis");
        }
        // Lot R0 : le document doit exister dans la GED et etre une image.
        if (documentRepository != null) {
            com.sni.bokaticowork.features.document.documentMaster.model.Document document = documentRepository
                    .findByCode(request.getDocumentCode().trim())
                    .orElseThrow(() -> new BadRequestException("Document introuvable dans la GED : " + request.getDocumentCode().trim()));
            String mime = document.getMimeType();
            if (mime == null || !mime.toLowerCase(java.util.Locale.ROOT).startsWith("image/")) {
                throw new BadRequestException("Le document " + document.getCode() + " n'est pas une image (" + mime + ")");
            }
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
