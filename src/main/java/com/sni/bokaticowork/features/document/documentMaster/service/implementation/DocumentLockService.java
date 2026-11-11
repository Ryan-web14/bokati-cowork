package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Le verrou d'un document : on le pose pour le modifier sans etre derange.
 *
 * <p>Tant qu'il court, seul celui qui l'a pose remplace le fichier, restaure une version, le
 * renomme, le deplace, l'archive ou le met a la corbeille ({@link #assertEditable}). Il expire seul
 * : un verrou oublie ne bloque pas le document indefiniment. Le lire reste permis a tous.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentLockService {

    public static final Duration DEFAULT_DURATION = Duration.ofHours(1);
    public static final Duration MAX_DURATION = Duration.ofHours(24);
    private static final DateTimeFormatter UNTIL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH'h'mm").withZone(ZoneId.of("Africa/Brazzaville"));

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public void lock(String documentCode) {
        lock(findDocument(documentCode), DEFAULT_DURATION);
    }

    /**
     * Pose ou prolonge le verrou de la personne connectee. Refuse si quelqu'un d'autre le tient
     * encore.
     */
    public Document lock(Document document, Duration duration) {
        Long userId = currentUserId();
        if (userId == null) {
            throw new BadRequestException("Un verrou se pose par une personne connectée.");
        }
        if (heldByOther(document, userId)) {
            throw lockedConflict(document);
        }
        Duration length = duration == null || duration.isNegative() || duration.isZero() ? DEFAULT_DURATION
                : duration.compareTo(MAX_DURATION) > 0 ? MAX_DURATION : duration;
        Instant now = Instant.now();
        if (!userId.equals(document.getLockedBy()) || !active(document)) {
            document.setLockedAt(now);
        }
        document.setLockedBy(userId);
        document.setLockExpiresAt(now.plus(length));
        return documentRepository.save(document);
    }

    public void unlock(String documentCode) {
        unlock(findDocument(documentCode), isAdmin());
    }

    /**
     * Leve le verrou. Celui qui l'a pose le peut toujours ; un autre seulement avec {@code force}
     * (administrateur, ou gestionnaire du dossier restreint).
     */
    public Document unlock(Document document, boolean force) {
        if (document.getLockedBy() == null) {
            return document;
        }
        Long userId = currentUserId();
        if (active(document) && !document.getLockedBy().equals(userId) && !force) {
            throw new ConflictException("document-lock", "seul " + lockerName(document)
                    + " ou un administrateur peut lever ce verrou");
        }
        document.setLockedBy(null);
        document.setLockedAt(null);
        document.setLockExpiresAt(null);
        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public boolean isLocked(String documentCode) {
        return active(findDocument(documentCode));
    }

    /** Refuse la modification d'un document verrouille par quelqu'un d'autre (409). */
    @Transactional(readOnly = true)
    public void assertEditable(Document document) {
        if (heldByOther(document, currentUserId())) {
            throw lockedConflict(document);
        }
    }

    /** Le verrou court-il encore ? */
    public static boolean active(Document document) {
        return document.getLockedBy() != null
                && (document.getLockExpiresAt() == null || Instant.now().isBefore(document.getLockExpiresAt()));
    }

    /** Le nom de celui qui tient le verrou, pour les messages et les listes. */
    @Transactional(readOnly = true)
    public String lockerName(Document document) {
        if (document.getLockedBy() == null) {
            return null;
        }
        return userRepository.findById(document.getLockedBy()).map(user -> user.getEmail()).orElse("un autre utilisateur");
    }

    private static boolean heldByOther(Document document, Long userId) {
        return active(document) && !document.getLockedBy().equals(userId);
    }

    private ConflictException lockedConflict(Document document) {
        String until = document.getLockExpiresAt() == null ? "" : " jusqu'au " + UNTIL.format(document.getLockExpiresAt());
        return new ConflictException("document-lock", "« " + document.getTitle() + " » est verrouillé par "
                + lockerName(document) + until);
    }

    private Document findDocument(String code) {
        return documentRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + code));
    }

    private static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) return p.getUser().getId();
        return null;
    }

    private static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
