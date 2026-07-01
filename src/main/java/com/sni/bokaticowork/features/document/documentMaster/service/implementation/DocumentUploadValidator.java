package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class DocumentUploadValidator {

    private static final long DEFAULT_MAX_SIZE_BYTES = 10L * 1024 * 1024;

    public void validateForClient(DocumentUploadMetadataRequest req, DocumentType type, MultipartFile file) {
        validateFile(file, type);
        if (Boolean.TRUE.equals(type.getRequiresDocumentNumber())
                && !StringUtils.hasText(req.getDocumentNumber())) {
            throw new BadRequestException(
                    "Le numéro de document est obligatoire pour le type : " + type.getName());
        }
        if (Boolean.TRUE.equals(type.getRequiresIssueDate()) && req.getIssueDate() == null) {
            throw new BadRequestException(
                    "La date d'émission est obligatoire pour le type : " + type.getName());
        }
        if (Boolean.TRUE.equals(type.getRequiresExpiryDate()) && req.getExpiryDate() == null) {
            throw new BadRequestException(
                    "La date d'expiration est obligatoire pour le type : " + type.getName());
        }
    }

    public void validateForAdmin(DocumentUploadMetadataRequest req, DocumentType type) {
        if (Boolean.TRUE.equals(type.getRequiresDocumentNumber())
                && !StringUtils.hasText(req.getDocumentNumber())) {
            log.warn("Admin upload sans documentNumber pour type={}", type.getCode());
        }
        if (Boolean.TRUE.equals(type.getRequiresIssueDate()) && req.getIssueDate() == null) {
            log.warn("Admin upload sans issueDate pour type={}", type.getCode());
        }
        if (Boolean.TRUE.equals(type.getRequiresExpiryDate()) && req.getExpiryDate() == null) {
            log.warn("Admin upload sans expiryDate pour type={}", type.getCode());
        }
    }

    private void validateFile(MultipartFile file, DocumentType type) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier est requis");
        }
        long maxBytes = type.getMaxFileSizeBytes() != null && type.getMaxFileSizeBytes() > 0
                ? type.getMaxFileSizeBytes() : DEFAULT_MAX_SIZE_BYTES;
        if (file.getSize() > maxBytes) {
            throw new BadRequestException(
                    "Taille du fichier (" + (file.getSize() / 1024) + " Ko) dépasse la limite autorisée de "
                            + (maxBytes / 1024 / 1024) + " Mo");
        }
        if (StringUtils.hasText(type.getAllowedMimeTypes())) {
            List<String> allowed = Arrays.stream(type.getAllowedMimeTypes().split(","))
                    .map(String::trim).toList();
            String contentType = file.getContentType() != null
                    ? file.getContentType().toLowerCase() : "";
            if (!allowed.isEmpty() && !allowed.contains(contentType)) {
                throw new BadRequestException(
                        "Type de fichier non autorisé. Acceptés : " + String.join(", ", allowed));
            }
        }
    }
}
