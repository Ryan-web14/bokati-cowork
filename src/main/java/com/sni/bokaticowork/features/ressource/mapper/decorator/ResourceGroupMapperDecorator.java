package com.sni.bokaticowork.features.ressource.mapper.decorator;

import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceGroupRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceGroupRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceGroupResponse;
import com.sni.bokaticowork.features.ressource.mapper.interfaces.ResourceGroupMapper;
import com.sni.bokaticowork.features.ressource.model.ResourceGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public abstract class ResourceGroupMapperDecorator implements ResourceGroupMapper {

    @Autowired
    @Qualifier("delegate")
    private ResourceGroupMapper delegate;

    @Override
    public ResourceGroup toEntity(CreateResourceGroupRequest request) {
        ResourceGroup entity = delegate.toEntity(request);
        if (entity.getPortalVisible() == null) {
            entity.setPortalVisible(Boolean.FALSE);
        }
        if (entity.getActive() == null) {
            entity.setActive(Boolean.TRUE);
        }

        entity.setName(request.getName().toUpperCase());
        return entity;
    }

    @Override
    public ResourceGroupResponse toResponse(ResourceGroup resourceGroup) {
        return delegate.toResponse(resourceGroup);
    }

    @Override
    public ResourceGroup updateEntity(ResourceGroup resourceGroup, UpdateResourceGroupRequest request) {
        ResourceGroup updated = delegate.updateEntity(resourceGroup, request);
        updated.setUpdatedAt(Instant.now());
        return updated;
    }
}
