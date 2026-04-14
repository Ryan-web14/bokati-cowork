package com.sni.bokaticowork.features.document.documentMaster.mapper.decorator;

import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentRequirementRequest;
import com.sni.bokaticowork.features.document.documentMaster.mapper.interfaces.DocumentRequirementMapper;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class DocumentRequirementMapperDecorator implements DocumentRequirementMapper {
    @Autowired
    @Qualifier("delegate")
    private DocumentRequirementMapper delegate;

    @Override
    public DocumentRequirement toEntity(DocumentRequirementRequest request) {
        DocumentRequirement entity = delegate.toEntity(request);
        applyDefaults(entity);
        return entity;
    }

    @Override
    public void updateEntity(DocumentRequirement entity, DocumentRequirementRequest request) {
        delegate.updateEntity(entity, request);
        applyDefaults(entity);
    }

    private void applyDefaults(DocumentRequirement entity) {
        if (entity.getRequired() == null) entity.setRequired(Boolean.TRUE);
        if (entity.getActive() == null) entity.setActive(Boolean.TRUE);
    }
}
