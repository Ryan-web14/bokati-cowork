package com.sni.bokaticowork.features.document.documentMaster.mapper.decorator;

import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentVersionResponse;
import com.sni.bokaticowork.features.document.documentMaster.mapper.interfaces.DocumentMapper;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class DocumentMapperDecorator implements DocumentMapper {
    @Autowired
    @Qualifier("delegate")
    private DocumentMapper delegate;

    @Override
    public DocumentResponse toResponse(Document document) {
        DocumentResponse response = delegate.toResponse(document);
        if (document != null && document.getDocumentType() != null) {
            response.setDocumentTypeCode(document.getDocumentType().getCode());
            response.setDocumentTypeName(document.getDocumentType().getName());
        }
        return response;
    }

    @Override
    public DocumentVersionResponse toVersionResponse(DocumentVersion version) {
        return delegate.toVersionResponse(version);
    }
}
