package com.sni.bokaticowork.features.ressource.mapper.interfaces;

import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceGroupRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourceGroupRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceGroupResponse;
import com.sni.bokaticowork.features.ressource.mapper.decorator.ResourceGroupMapperDecorator;
import com.sni.bokaticowork.features.ressource.model.ResourceGroup;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, builder = @Builder(disableBuilder = true))
@DecoratedWith(ResourceGroupMapperDecorator.class)
public interface ResourceGroupMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ResourceGroup toEntity(CreateResourceGroupRequest request);

    ResourceGroupResponse toResponse(ResourceGroup resourceGroup);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ResourceGroup updateEntity(@MappingTarget ResourceGroup resourceGroup, UpdateResourceGroupRequest request);
}
