package com.sni.bokaticowork.features.ressource.mapper.decorator;

import com.sni.bokaticowork.features.ressource.dto.request.CreateAmenityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateAmenityRequest;
import com.sni.bokaticowork.features.ressource.dto.response.AmenityResponse;
import com.sni.bokaticowork.features.ressource.mapper.interfaces.ResourceAmenitiesMapper;
import com.sni.bokaticowork.features.ressource.model.ResourceAmenities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class ResourceAmenitiesMapperDecorator implements ResourceAmenitiesMapper {

    @Autowired
    @Qualifier("delegate")
    private ResourceAmenitiesMapper delegate;

    @Override
    public ResourceAmenities toEntity(CreateAmenityRequest request) {
        ResourceAmenities entity = delegate.toEntity(request);
        if (entity.getActive() == null) {
            entity.setActive(Boolean.TRUE);
        }
        return entity;
    }

    @Override
    public AmenityResponse toResponse(ResourceAmenities amenity) {
        return delegate.toResponse(amenity);
    }

    @Override
    public ResourceAmenities updateEntity(ResourceAmenities amenity, UpdateAmenityRequest request) {
        return delegate.updateEntity(amenity, request);
    }
}
