package com.sni.bokaticowork.features.ressource.mapper.decorator;

import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceTypeRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceTypeRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceTypeResponse;
import com.sni.bokaticowork.features.ressource.mapper.interfaces.ResourceTypeMapper;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public abstract class ResourceTypeMapperDecorator implements ResourceTypeMapper {

    @Autowired
    @Qualifier("delegate")
    private ResourceTypeMapper delegate;

    @Override
    public ResourceType toEntity(CreateResourceTypeRequest request) {
        ResourceType entity = delegate.toEntity(request);
        if (entity.getActive() == null) {
            entity.setActive(Boolean.TRUE);
        }
        return entity;
    }

    @Override
    public ResourceTypeResponse toResponse(ResourceType resourceType) {
        return delegate.toResponse(resourceType);
    }

    @Override
    public ResourceType updateEntity(ResourceType resourceType, UpdateResourceTypeRequest request) {
        ResourceType updated = delegate.updateEntity(resourceType, request);
        updated.setUpdatedAt(Instant.now());
        return updated;
    }
}
