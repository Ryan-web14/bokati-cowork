package com.sni.bokaticowork.features.ressource.mapper.interfaces;

import com.sni.bokaticowork.features.ressource.dto.request.CreateResourcePolicyRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourcePolicyRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePolicyResponse;
import com.sni.bokaticowork.features.ressource.mapper.decorator.ResourcePolicyMapperDecorator;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, builder = @Builder(disableBuilder = true))
@DecoratedWith(ResourcePolicyMapperDecorator.class)
public interface ResourcePolicyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ResourcePolicy toEntity(CreateResourcePolicyRequest request);

    ResourcePolicyResponse toResponse(ResourcePolicy resourcePolicy);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ResourcePolicy updateEntity(@MappingTarget ResourcePolicy resourcePolicy, UpdateResourcePolicyRequest request);
}
