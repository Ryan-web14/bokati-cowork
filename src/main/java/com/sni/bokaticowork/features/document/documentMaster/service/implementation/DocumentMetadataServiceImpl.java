package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentMetadata;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentMetadataRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentMetadataServiceImpl implements DocumentMetadataService {

    private final DocumentMetadataRepository metadataRepository;
    private final DocumentRepository documentRepository;

    @Override
    public Map<String, String> setMetadata(String documentCode, Map<String, String> metadata) {
        Document document = serviceDocument(documentCode);
        metadata.forEach((key, value) -> {
            if (!StringUtils.hasText(key)) return;
            String trimmedKey = key.trim();
            DocumentMetadata entry = metadataRepository
                    .findByDocumentAndMetaKey(document, trimmedKey)
                    .orElseGet(() -> DocumentMetadata.builder().document(document).metaKey(trimmedKey).build());
            entry.setMetaValue(value);
            metadataRepository.save(entry);
        });
        return getMetadata(documentCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getMetadata(String documentCode) {
        Document document = serviceDocument(documentCode);
        Map<String, String> result = new LinkedHashMap<>();
        metadataRepository.findAllByDocument(document)
                .forEach(m -> result.put(m.getMetaKey(), m.getMetaValue()));
        return result;
    }

    @Override
    public void deleteKey(String documentCode, String key) {
        Document document = serviceDocument(documentCode);
        metadataRepository.findByDocumentAndMetaKey(document, key.trim())
                .ifPresent(metadataRepository::delete);
    }

    private Document serviceDocument(String code) {
        return documentRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + code));
    }
}
