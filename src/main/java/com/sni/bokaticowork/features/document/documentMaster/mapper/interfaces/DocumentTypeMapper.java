package com.sni.bokaticowork.features.document.documentMaster.mapper.interfaces;

import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTypeRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentTypeResponse;
import com.sni.bokaticowork.features.document.documentMaster.mapper.decorator.DocumentTypeMapperDecorator;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(DocumentTypeMapperDecorator.class)
public interface DocumentTypeMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    DocumentType toEntity(DocumentTypeRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    void updateEntity(@MappingTarget DocumentType entity, DocumentTypeRequest request);

    DocumentTypeResponse toResponse(DocumentType entity);
}
