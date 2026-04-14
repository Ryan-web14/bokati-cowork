package com.sni.bokaticowork.features.ressource.mapper.interfaces;

import com.sni.bokaticowork.features.ressource.dto.request.ResourceRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceSummaryResponse;
import com.sni.bokaticowork.features.ressource.mapper.decorator.ResourceMapperDecorator;
import com.sni.bokaticowork.features.ressource.model.Resource;
import org.mapstruct.BeanMapping;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(ResourceMapperDecorator.class)
public interface ResourceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "resourceType", ignore = true)
    @Mapping(target = "resourceGroup", ignore = true)
    @Mapping(target = "resourcePolicy", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Resource toEntity(ResourceRequest request);

    ResourceResponse toResponse(Resource resource);

    ResourceSummaryResponse toSummary(Resource resource);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "resourceType", ignore = true)
    @Mapping(target = "resourceGroup", ignore = true)
    @Mapping(target = "resourcePolicy", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Resource updateEntity(@MappingTarget Resource resource, UpdateResourceRequest request);
}
