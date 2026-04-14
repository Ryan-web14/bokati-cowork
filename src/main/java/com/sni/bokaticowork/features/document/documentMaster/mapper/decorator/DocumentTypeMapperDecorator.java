package com.sni.bokaticowork.features.document.documentMaster.mapper.decorator;

import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTypeRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentTypeResponse;
import com.sni.bokaticowork.features.document.documentMaster.mapper.interfaces.DocumentTypeMapper;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public abstract class DocumentTypeMapperDecorator implements DocumentTypeMapper {
    @Autowired
    @Qualifier("delegate")
    private DocumentTypeMapper delegate;

    @Override
    public DocumentType toEntity(DocumentTypeRequest request) {
        DocumentType entity = delegate.toEntity(request);
        applyDefaults(entity);
        return entity;
    }

    @Override
    public void updateEntity(DocumentType entity, DocumentTypeRequest request) {
        delegate.updateEntity(entity, request);
        applyDefaults(entity);
    }

    @Override
    public DocumentTypeResponse toResponse(DocumentType entity) {
        DocumentTypeResponse response = delegate.toResponse(entity);
        if (entity != null) {
            response.setDescription(StringUtils.hasText(entity.getHelpText()) ? entity.getHelpText() : entity.getDescription());
        }
        return response;
    }

    private void applyDefaults(DocumentType entity) {
        if (entity.getRequired() == null) entity.setRequired(Boolean.FALSE);
        if (entity.getRequiresExpiryDate() == null) entity.setRequiresExpiryDate(Boolean.FALSE);
        if (entity.getRequiresReview() == null) entity.setRequiresReview(Boolean.FALSE);
        if (entity.getRequiresSignature() == null) entity.setRequiresSignature(Boolean.FALSE);
        if (entity.getMultipleAllowed() == null) entity.setMultipleAllowed(Boolean.FALSE);
        if (entity.getActive() == null) entity.setActive(Boolean.TRUE);
    }
}
