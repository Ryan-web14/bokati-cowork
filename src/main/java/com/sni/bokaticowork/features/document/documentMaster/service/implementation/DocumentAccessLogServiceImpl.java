package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentAccessLogResponse;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentAccessLog;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentAccessLogRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentAccessLogServiceImpl {

    private final DocumentAccessLogRepository accessLogRepository;
    private final DocumentRepository documentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String documentCode, String action, String ipAddress, String userAgent) {
        documentRepository.findByCode(documentCode).ifPresent(doc ->
                accessLogRepository.save(DocumentAccessLog.builder()
                        .document(doc)
                        .userId(currentUserId())
                        .action(action)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .build())
        );
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<DocumentAccessLogResponse> getByDocument(String documentCode, Pageable pageable) {
        Document document = documentRepository.findByCode(documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentCode));
        return new PaginatedResponse<>(
                accessLogRepository.findAllByDocumentOrderByAccessedAtDesc(document, pageable)
                        .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<DocumentAccessLogResponse> getByUser(Long userId, Pageable pageable) {
        return new PaginatedResponse<>(
                accessLogRepository.findAllByUserIdOrderByAccessedAtDesc(userId, pageable)
                        .map(this::toResponse));
    }

    private DocumentAccessLogResponse toResponse(DocumentAccessLog log) {
        return DocumentAccessLogResponse.builder()
                .id(log.getId())
                .documentCode(log.getDocument() != null ? log.getDocument().getCode() : null)
                .userId(log.getUserId())
                .action(log.getAction())
                .accessedAt(log.getAccessedAt())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .build();
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser().getId();
        }
        return 0L;
    }
}
