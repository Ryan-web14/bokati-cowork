package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.CreateDocumentSignatureRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.SignDocumentRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentSignatureResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSignatureStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentSignature;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentSignatureRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentSignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class DocumentSignatureServiceImpl implements DocumentSignatureService {

    private final DocumentRepository documentRepository;
    private final DocumentSignatureRepository signatureRepository;
    private final OutboxService outboxService;

    @Override
    public DocumentSignatureResponse requestSignature(String documentCode, CreateDocumentSignatureRequest request) {
        Document document = document(documentCode);
        DocumentSignature signature = DocumentSignature.builder()
                .document(document)
                .signerType(request.signerType().trim().toUpperCase())
                .signerId(request.signerId())
                .signerName(request.signerName().trim())
                .signerEmail(trim(request.signerEmail()))
                .signatureStatus(DocumentSignatureStatus.PENDING)
                .build();
        DocumentSignature saved = signatureRepository.save(signature);
        publishSignatureEvent("DOCUMENT_SIGNATURE_REQUESTED", saved);
        return toResponse(saved);
    }

    @Override
    public DocumentSignatureResponse sign(String documentCode, Long signatureId, SignDocumentRequest request, String ipAddress, String userAgent) {
        if (request == null || !Boolean.TRUE.equals(request.accepted())) {
            throw new BadRequestException("Signature acceptance is required");
        }
        DocumentSignature signature = signatureRepository.findById(signatureId)
                .filter(item -> item.getDocument().getCode().equals(documentCode))
                .orElseThrow(() -> new ResourceNotFoundException("Document signature request not found"));
        if (signature.getSignatureStatus() != DocumentSignatureStatus.PENDING) {
            throw new BadRequestException("Document signature is not pending");
        }
        signature.setSignatureStatus(DocumentSignatureStatus.SIGNED);
        signature.setSignedAt(Instant.now());
        signature.setIpAddress(trim(ipAddress));
        signature.setUserAgent(trim(userAgent));
        signature.setSignatureData(trim(request.signatureData()));
        DocumentSignature saved = signatureRepository.save(signature);
        publishSignatureEvent("DOCUMENT_SIGNED", saved);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentSignatureResponse> list(String documentCode) {
        return signatureRepository.findAllByDocument_CodeOrderByIdAsc(documentCode).stream()
                .map(this::toResponse)
                .toList();
    }

    private Document document(String documentCode) {
        if (!StringUtils.hasText(documentCode)) {
            throw new BadRequestException("Document code is required");
        }
        return documentRepository.findByCode(documentCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    private DocumentSignatureResponse toResponse(DocumentSignature signature) {
        return new DocumentSignatureResponse(
                signature.getId(),
                signature.getDocument().getCode(),
                signature.getSignerType(),
                signature.getSignerId(),
                signature.getSignerName(),
                signature.getSignerEmail(),
                signature.getSignatureStatus(),
                signature.getSignedAt(),
                signature.getIpAddress(),
                signature.getUserAgent()
        );
    }

    private void publishSignatureEvent(String eventType, DocumentSignature signature) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("documentCode", signature.getDocument().getCode());
        payload.put("signerName", signature.getSignerName());
        payload.put("signerEmail", signature.getSignerEmail());
        payload.put("signatureStatus", signature.getSignatureStatus().name());
        payload.put("ownerType", signature.getDocument().getOwnerType() != null ? signature.getDocument().getOwnerType().name() : null);
        payload.put("ownerId", signature.getDocument().getOwnerId());
        outboxService.publish(eventType, "DOCUMENT_SIGNATURE", signature.getDocument().getCode(), payload);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
