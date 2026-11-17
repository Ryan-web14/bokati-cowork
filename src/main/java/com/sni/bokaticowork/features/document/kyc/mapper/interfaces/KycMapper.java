package com.sni.bokaticowork.features.document.kyc.mapper.interfaces;

import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseNoteResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentOcrResultResponse;
import com.sni.bokaticowork.features.document.kyc.mapper.decorator.KycMapperDecorator;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycCaseNote;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.model.KycDocumentOcrResult;
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

    /** Lot KG2 : {@code id} est toujours le numero court de la piece (celui des routes). */
    @Mapping(target = "id", source = "shortId")
    @Mapping(target = "qualityWarnings", ignore = true)
    @Mapping(target = "backQualityWarnings", ignore = true)
    KycDocumentResponse toDocumentResponse(KycDocument document);

    KycCaseNoteResponse toNoteResponse(KycCaseNote note);

    KycDocumentOcrResultResponse toOcrResultResponse(KycDocumentOcrResult result);
}
