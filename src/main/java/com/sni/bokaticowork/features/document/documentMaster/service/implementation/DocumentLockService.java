package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentLockService {

    private static final long DEFAULT_LOCK_HOURS = 1;

    private final DocumentRepository documentRepository;

    public void lock(String documentCode) {
        Document document = findDocument(documentCode);
        Long userId = currentUserId();

        if (document.getLockedBy() != null && !document.getLockedBy().equals(userId)) {
            if (document.getLockExpiresAt() != null && Instant.now().isBefore(document.getLockExpiresAt())) {
                throw new BadRequestException("Document is locked by another user");
            }
        }

        document.setLockedBy(userId);
        document.setLockedAt(Instant.now());
        document.setLockExpiresAt(Instant.now().plus(DEFAULT_LOCK_HOURS, ChronoUnit.HOURS));
        documentRepository.save(document);
    }

    public void unlock(String documentCode) {
        Document document = findDocument(documentCode);
        Long userId = currentUserId();

        if (document.getLockedBy() == null) return;

        boolean isAdmin = isAdmin();
        if (!document.getLockedBy().equals(userId) && !isAdmin) {
            throw new BadRequestException("Only the user who locked the document or an admin can unlock it");
        }

        document.setLockedBy(null);
        document.setLockedAt(null);
        document.setLockExpiresAt(null);
        documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public boolean isLocked(String documentCode) {
        Document document = findDocument(documentCode);
        if (document.getLockedBy() == null) return false;
        if (document.getLockExpiresAt() != null && Instant.now().isAfter(document.getLockExpiresAt())) return false;
        return true;
    }

    private Document findDocument(String code) {
        return documentRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + code));
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) return p.getUser().getId();
        throw new BadRequestException("User not authenticated");
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
