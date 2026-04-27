package com.sni.bokaticowork.features.ressource.mapper.interfaces;

import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceTypeRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceTypeRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceTypeResponse;
import com.sni.bokaticowork.features.ressource.mapper.decorator.ResourceTypeMapperDecorator;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, builder = @Builder(disableBuilder = true))
@DecoratedWith(ResourceTypeMapperDecorator.class)
public interface ResourceTypeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ResourceType toEntity(CreateResourceTypeRequest request);

    ResourceTypeResponse toResponse(ResourceType resourceType);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ResourceType updateEntity(@MappingTarget ResourceType resourceType, UpdateResourceTypeRequest request);
}
