package com.sni.bokaticowork.features.ressource.mapper.decorator;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.ressource.dto.request.ResourceRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceSummaryResponse;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.mapper.interfaces.ResourceMapper;
import com.sni.bokaticowork.features.ressource.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Locale;

@Component
public abstract class ResourceMapperDecorator implements ResourceMapper {

    @Autowired
    @Qualifier("delegate")
    private ResourceMapper delegate;

    @Override
    public Resource toEntity(ResourceRequest request) {
        Resource entity = delegate.toEntity(request);
        if (StringUtils.hasText(request.getStatus())) {
            entity.setStatus(parseStatus(request.getStatus()));
        }
        if (entity.getBookingEnabled() == null) {
            entity.setBookingEnabled(Boolean.TRUE);
        }
        if (entity.getPortalVisible() == null) {
            entity.setPortalVisible(Boolean.TRUE);
        }
        if (entity.getActive() == null) {
            entity.setActive(Boolean.TRUE);
        }
        if (entity.getDeleted() == null) {
            entity.setDeleted(Boolean.FALSE);
        }
        if (entity.getCapacity() == null) {
            entity.setCapacity(1);
        }
        return entity;
    }

    @Override
    public ResourceResponse toResponse(Resource resource) {
        return ResourceResponse.builder()
                .code(resource.getCode())
                .typeCode(resource.getResourceType() == null ? null : resource.getResourceType().getCode())
                .groupCode(resource.getResourceGroup() == null ? null : resource.getResourceGroup().getCode())
                .policyCode(resource.getResourcePolicy() == null ? null : resource.getResourcePolicy().getCode())
                .name(resource.getName())
                .description(resource.getDescription())
                .capacity(resource.getCapacity())
                .zone(resource.getZone())
                .locationLabel(resource.getLocationLabel())
                .status(resource.getStatus() == null ? null : resource.getStatus().name())
                .bookingEnabled(resource.getBookingEnabled())
                .portalVisible(resource.getPortalVisible())
                .displayOrder(resource.getDisplayOrder())
                .active(resource.getActive())
                .slotDurationMinutes(resource.getSlotDurationMinutes())
                .build();
    }

    @Override
    public ResourceSummaryResponse toSummary(Resource resource) {
        return ResourceSummaryResponse.builder()
                .code(resource.getCode())
                .typeCode(resource.getResourceType() == null ? null : resource.getResourceType().getCode())
                .name(resource.getName())
                .status(resource.getStatus() == null ? null : resource.getStatus().name())
                .bookingEnabled(resource.getBookingEnabled())
                .portalVisible(resource.getPortalVisible())
                .active(resource.getActive())
                .build();
    }

    @Override
    public Resource updateEntity(Resource resource, UpdateResourceRequest request) {
        Resource updated = delegate.updateEntity(resource, request);
        updated.setUpdatedAt(Instant.now());
        return updated;
    }

    private ResourceStatus parseStatus(String value) {
        try {
            return ResourceStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid resource status: " + value, ex);
        }
    }
}
