package com.sni.bokaticowork.features.document.documentMaster.mapper.interfaces;

import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentVersionResponse;
import com.sni.bokaticowork.features.document.documentMaster.mapper.decorator.DocumentMapperDecorator;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentVersion;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(DocumentMapperDecorator.class)
public interface DocumentMapper {
    DocumentResponse toResponse(Document document);
    DocumentVersionResponse toVersionResponse(DocumentVersion version);
}
