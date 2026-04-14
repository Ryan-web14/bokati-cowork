package com.sni.bokaticowork.features.document.kyc.mapper.decorator;

import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentResponse;
import com.sni.bokaticowork.features.document.kyc.mapper.interfaces.KycMapper;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class KycMapperDecorator implements KycMapper {
    @Autowired
    @Qualifier("delegate")
    private KycMapper delegate;

    @Override
    public KycDocumentResponse toDocumentResponse(KycDocument document) {
        KycDocumentResponse response = delegate.toDocumentResponse(document);
        if (document != null && document.getDocument() != null) {
            response.setDocumentCode(document.getDocument().getCode());
        }
        return response;
    }
}
