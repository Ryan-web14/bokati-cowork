package com.sni.bokaticowork.features.document.documentMaster.mapper.interfaces;

import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentRequirementRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentRequirementResponse;
import com.sni.bokaticowork.features.document.documentMaster.mapper.decorator.DocumentRequirementMapperDecorator;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentRequirement;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(DocumentRequirementMapperDecorator.class)
public interface DocumentRequirementMapper {
    @Mapping(target = "id", ignore = true)
    DocumentRequirement toEntity(DocumentRequirementRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntity(@MappingTarget DocumentRequirement entity, DocumentRequirementRequest request);

    DocumentRequirementResponse toResponse(DocumentRequirement entity);
}
