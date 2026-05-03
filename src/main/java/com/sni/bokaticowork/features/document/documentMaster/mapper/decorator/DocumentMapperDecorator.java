package com.sni.bokaticowork.features.document.documentMaster.mapper.decorator;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentVersionResponse;
import com.sni.bokaticowork.features.document.documentMaster.mapper.interfaces.DocumentMapper;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public abstract class DocumentMapperDecorator implements DocumentMapper {
    @Autowired
    @Qualifier("delegate")
    private DocumentMapper delegate;

    @Value("${app.api-base-url:}")
    private String apiBaseUrl;

    @Override
    public DocumentResponse toResponse(Document document) {
        DocumentResponse response = delegate.toResponse(document);
        if (document == null || response == null) {
            return response;
        }
        if (document.getDocumentType() != null) {
            response.setDocumentTypeCode(document.getDocumentType().getCode());
            response.setDocumentTypeName(document.getDocumentType().getName());
        }
        if (StringUtils.hasText(document.getCode()) && StringUtils.hasText(document.getFileUrl())) {
            String previewUrl = documentPreviewUrl(document.getCode());
            response.setPreviewUrl(previewUrl);
            response.setDownloadUrl(documentDownloadUrl(document.getCode()));
            response.setFileUrl(previewUrl);
        }
        return response;
    }

    @Override
    public DocumentVersionResponse toVersionResponse(DocumentVersion version) {
        DocumentVersionResponse response = delegate.toVersionResponse(version);
        if (version == null || response == null) {
            return response;
        }
        response.setStoragePath(null);
        if (version.getDocument() != null && StringUtils.hasText(version.getDocument().getCode())) {
            response.setPreviewUrl(documentPreviewUrl(version.getDocument().getCode()));
            response.setDownloadUrl(documentDownloadUrl(version.getDocument().getCode()));
        }
        return response;
    }

    private String documentPreviewUrl(String documentCode) {
        return apiUrl(ApiPath.V1 + "/documents/" + documentCode + "/preview");
    }

    private String documentDownloadUrl(String documentCode) {
        return apiUrl(ApiPath.V1 + "/documents/" + documentCode + "/download");
    }

    private String apiUrl(String path) {
        if (!StringUtils.hasText(apiBaseUrl)) {
            return path;
        }
        return apiBaseUrl.replaceAll("/+$", "") + path;
    }
}
