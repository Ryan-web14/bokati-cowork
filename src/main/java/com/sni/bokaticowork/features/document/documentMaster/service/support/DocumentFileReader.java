package com.sni.bokaticowork.features.document.documentMaster.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFileResult;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentVersion;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentVersionRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class DocumentFileReader {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentStorageService storageService;

    @Transactional(readOnly = true)
    public DocumentFileResult read(String documentCode) {
        if (!StringUtils.hasText(documentCode)) {
            throw new BadRequestException("Le code du document est requis");
        }
        Document document = documentRepository.findByCode(documentCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + documentCode));
        if (!StringUtils.hasText(document.getFileUrl())) {
            throw new BadRequestException("Aucun fichier enregistré pour le document : " + documentCode);
        }
        DocumentVersion currentVersion = documentVersionRepository.findByDocumentAndCurrentTrue(document).orElse(null);
        String storageProvider = currentVersion == null ? "FILESYSTEM" : currentVersion.getStorageProvider();
        String storagePath = currentVersion == null ? document.getFileUrl() : currentVersion.getStoragePath();
        byte[] content = storageService.read(storageProvider, storagePath);
        String mimeType = StringUtils.hasText(document.getMimeType()) ? document.getMimeType() : "application/octet-stream";
        String fileName = StringUtils.hasText(document.getFileName()) ? document.getFileName() : document.getCode() + ".pdf";
        return new DocumentFileResult(content, mimeType, fileName);
    }
}
