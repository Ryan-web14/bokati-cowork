package com.sni.bokaticowork.features.ressource.mapper.interfaces;

import com.sni.bokaticowork.features.ressource.dto.request.CreateAmenityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateAmenityRequest;
import com.sni.bokaticowork.features.ressource.dto.response.AmenityResponse;
import com.sni.bokaticowork.features.ressource.mapper.decorator.ResourceAmenitiesMapperDecorator;
import com.sni.bokaticowork.features.ressource.model.ResourceAmenities;
import org.mapstruct.BeanMapping;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(ResourceAmenitiesMapperDecorator.class)
public interface ResourceAmenitiesMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    ResourceAmenities toEntity(CreateAmenityRequest request);

    AmenityResponse toResponse(ResourceAmenities amenity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    ResourceAmenities updateEntity(@MappingTarget ResourceAmenities amenity, UpdateAmenityRequest request);
}
