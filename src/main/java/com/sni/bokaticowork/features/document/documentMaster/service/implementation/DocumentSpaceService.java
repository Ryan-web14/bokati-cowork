package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTagAssignRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.SpaceDocumentUploadRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTypeRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentMetadataService;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentSpaceService {

    private final DocumentService documentService;
    private final DocumentTagService tagService;
    private final DocumentMetadataService metadataService;
    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentFolderRepository folderRepository;
    private final DocumentRepository documentRepository;

    private static final Map<DocumentSpace, Set<DocumentCategory>> SPACE_CATEGORIES = Map.of(
            DocumentSpace.KYC_SPACE, Set.of(DocumentCategory.KYC),
            DocumentSpace.CONTRACT_SPACE, Set.of(DocumentCategory.LEGAL),
            DocumentSpace.FINANCIAL_SPACE, Set.of(DocumentCategory.FINANCIAL),
            DocumentSpace.ASSET_SPACE, Set.of(DocumentCategory.ASSET),
            DocumentSpace.ADMINISTRATIVE, Set.of(DocumentCategory.SYSTEM, DocumentCategory.OTHER),
            DocumentSpace.GENERIC, Set.of(DocumentCategory.values())
    );

    private static final Map<DocumentSpace, List<String>> SPACE_MIME_TYPES = Map.of(
            DocumentSpace.FINANCIAL_SPACE, List.of("application/pdf", "image/jpeg", "image/png",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/csv"),
            DocumentSpace.CONTRACT_SPACE, List.of("application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "image/jpeg", "image/png"),
            DocumentSpace.ASSET_SPACE, List.of("application/pdf", "image/jpeg", "image/png", "image/webp", "image/tiff")
    );

    @Transactional
    public DocumentResponse uploadToSpace(DocumentSpace space, SpaceDocumentUploadRequest request, MultipartFile file) {
        validateDocumentTypeForSpace(space, request.getDocumentTypeCode());
        validateMimeTypeForSpace(space, file);

        DocumentUploadMetadataRequest metadata = new DocumentUploadMetadataRequest();
        metadata.setOwnerType(request.getOwnerType());
        metadata.setOwnerCode(request.getOwnerCode());
        metadata.setDocumentTypeCode(request.getDocumentTypeCode());
        metadata.setTitle(request.getTitle());
        metadata.setDescription(request.getDescription());
        metadata.setDocumentNumber(request.getDocumentNumber());
        metadata.setIssueDate(request.getIssueDate());
        metadata.setExpiryDate(request.getExpiryDate());

        DocumentResponse response = documentService.upload(metadata, file);

        Document document = documentRepository.findByCode(response.getCode())
                .orElseThrow(() -> new BadRequestException("Document not found after upload"));
        document.setSpace(space);
        if (request.getReferenceCode() != null) {
            document.setSpaceReferenceCode(request.getReferenceCode().trim());
        }
        if (request.getFolderCode() != null) {
            DocumentFolder folder = folderRepository.findByCode(request.getFolderCode().trim())
                    .orElseThrow(() -> new BadRequestException("Folder not found: " + request.getFolderCode()));
            document.setFolder(folder);
        }
        documentRepository.save(document);

        if (request.getTagCodes() != null && !request.getTagCodes().isEmpty()) {
            DocumentTagAssignRequest tagReq = new DocumentTagAssignRequest();
            tagReq.setTagCodes(request.getTagCodes());
            tagService.assignTags(response.getCode(), tagReq);
        }

        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            metadataService.setMetadata(response.getCode(), request.getMetadata());
        }

        return documentService.getByCode(response.getCode());
    }

    private void validateDocumentTypeForSpace(DocumentSpace space, String documentTypeCode) {
        if (space == DocumentSpace.GENERIC) return;

        DocumentType docType = documentTypeRepository.findByCode(documentTypeCode.trim())
                .orElseThrow(() -> new BadRequestException("Unknown document type: " + documentTypeCode));

        Set<DocumentCategory> allowedCategories = SPACE_CATEGORIES.getOrDefault(space, Set.of());
        if (!allowedCategories.contains(docType.getCategory())) {
            throw new BadRequestException(
                    "Document type " + documentTypeCode + " (category: " + docType.getCategory()
                            + ") is not allowed in space " + space);
        }
    }

    private void validateMimeTypeForSpace(DocumentSpace space, MultipartFile file) {
        List<String> allowed = SPACE_MIME_TYPES.get(space);
        if (allowed == null) return;

        String contentType = file.getContentType();
        if (contentType == null || !allowed.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "File type " + contentType + " is not allowed in space " + space
                            + ". Accepted: " + String.join(", ", allowed));
        }
    }
}
