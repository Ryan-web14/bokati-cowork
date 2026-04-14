package com.sni.bokaticowork.features.document.kyc.mapper.interfaces;

import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentResponse;
import com.sni.bokaticowork.features.document.kyc.mapper.decorator.KycMapperDecorator;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(KycMapperDecorator.class)
public interface KycMapper {
    @Mapping(target = "complete", ignore = true)
    @Mapping(target = "approved", ignore = true)
    @Mapping(target = "missingDocumentTypeCodes", ignore = true)
    @Mapping(target = "documents", ignore = true)
    KycCaseResponse toResponse(KycCase kycCase);

    KycDocumentResponse toDocumentResponse(KycDocument document);
}
