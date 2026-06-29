package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.CreateShareLinkRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.ShareLinkResponse;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentShareLink;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentShareLinkRepository;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentShareService {

    private final DocumentShareLinkRepository shareRepository;
    private final DocumentRepository documentRepository;
    private final DocumentFolderRepository folderRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.api-base-url:}")
    private String apiBaseUrl;

    public ShareLinkResponse shareDocument(String documentCode, CreateShareLinkRequest request) {
        Document document = documentRepository.findByCode(documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        return createLink(document, null, request);
    }

    public ShareLinkResponse shareFolder(String folderCode, CreateShareLinkRequest request) {
        DocumentFolder folder = folderRepository.findByCode(folderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        return createLink(null, folder, request);
    }

    @Transactional(readOnly = true)
    public ShareLinkResponse access(String token) {
        DocumentShareLink link = shareRepository.findByTokenAndActiveTrue(token)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found or expired"));
        validateLink(link);
        return toResponse(link);
    }

    public void recordAccess(String token) {
        DocumentShareLink link = shareRepository.findByTokenAndActiveTrue(token)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));
        validateLink(link);
        link.setAccessCount(link.getAccessCount() + 1);
        link.setLastAccessedAt(Instant.now());
        shareRepository.save(link);
    }

    public void revoke(String token) {
        DocumentShareLink link = shareRepository.findByTokenAndActiveTrue(token)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));
        link.setActive(false);
        shareRepository.save(link);
    }

    @Transactional(readOnly = true)
    public List<ShareLinkResponse> listByDocument(String documentCode) {
        Document document = documentRepository.findByCode(documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        return shareRepository.findAllByDocumentAndActiveTrueOrderByCreatedAtDesc(document)
                .stream().map(this::toResponse).toList();
    }

    private ShareLinkResponse createLink(Document document, DocumentFolder folder, CreateShareLinkRequest request) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String passwordHash = StringUtils.hasText(request.getPassword())
                ? passwordEncoder.encode(request.getPassword()) : null;

        DocumentShareLink link = DocumentShareLink.builder()
                .token(token)
                .document(document)
                .folder(folder)
                .createdBy(currentUserId())
                .expiresAt(Instant.now().plus(request.getExpiresInHours(), ChronoUnit.HOURS))
                .passwordHash(passwordHash)
                .allowDownload(request.getAllowDownload() != null ? request.getAllowDownload() : Boolean.TRUE)
                .maxAccessCount(request.getMaxAccessCount())
                .build();
        shareRepository.save(link);
        return toResponse(link);
    }

    private void validateLink(DocumentShareLink link) {
        if (Instant.now().isAfter(link.getExpiresAt())) {
            throw new BadRequestException("This share link has expired");
        }
        if (link.getMaxAccessCount() != null && link.getAccessCount() >= link.getMaxAccessCount()) {
            throw new BadRequestException("This share link has reached its maximum access count");
        }
    }

    private ShareLinkResponse toResponse(DocumentShareLink link) {
        String baseUrl = StringUtils.hasText(apiBaseUrl) ? apiBaseUrl.replaceAll("/+$", "") : "";
        return ShareLinkResponse.builder()
                .token(link.getToken())
                .shareUrl(baseUrl + "/sni/api/v1/shares/" + link.getToken())
                .documentCode(link.getDocument() != null ? link.getDocument().getCode() : null)
                .folderCode(link.getFolder() != null ? link.getFolder().getCode() : null)
                .allowDownload(link.getAllowDownload())
                .passwordProtected(link.getPasswordHash() != null)
                .maxAccessCount(link.getMaxAccessCount())
                .accessCount(link.getAccessCount())
                .expiresAt(link.getExpiresAt())
                .createdAt(link.getCreatedAt())
                .createdBy(link.getCreatedBy())
                .createdByEmail(userRepository.findById(link.getCreatedBy()).map(u -> u.getEmail()).orElse(null))
                .active(link.getActive())
                .build();
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) return p.getUser().getId();
        return null;
    }
}
