package com.sni.bokaticowork.features.ressource.mapper.decorator;

import  com.sni.bokaticowork.features.ressource.dto.request.CreateResourcePolicyRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourcePolicyRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePolicyResponse;
import com.sni.bokaticowork.features.ressource.mapper.interfaces.ResourcePolicyMapper;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public abstract class ResourcePolicyMapperDecorator implements ResourcePolicyMapper {

    @Autowired
    @Qualifier("delegate")
    private ResourcePolicyMapper delegate;

    @Override
    public ResourcePolicy toEntity(CreateResourcePolicyRequest request) {
        ResourcePolicy entity = delegate.toEntity(request);
        if (entity.getCancellationNoticeMinutes() == null) {
            entity.setCancellationNoticeMinutes(1);
        }
        if (entity.getAllowCancellation() == null) {
            entity.setAllowCancellation(Boolean.FALSE);
        }
        if (entity.getActive() == null) {
            entity.setActive(Boolean.TRUE);
        }
        return entity;
    }

    @Override
    public ResourcePolicyResponse toResponse(ResourcePolicy resourcePolicy) {
        return delegate.toResponse(resourcePolicy);
    }

    @Override
    public ResourcePolicy updateEntity(ResourcePolicy resourcePolicy, UpdateResourcePolicyRequest request) {
        ResourcePolicy updated = delegate.updateEntity(resourcePolicy, request);
        updated.setUpdatedAt(Instant.now());
        return updated;
    }
}
