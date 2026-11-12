package com.sni.bokaticowork.features.document.explorer.lifecycle;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import com.sni.bokaticowork.features.document.explorer.access.GedAccessService;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedHold;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedHoldRequest;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Le gel juridique : un contentieux, un controle fiscal, une enquete imposent de garder des
 * documents tels quels.
 *
 * <p>Un gel porte sur un fichier, ou sur un dossier et tout ce qu'il contient, y compris ce qui y
 * sera depose ensuite. Tant qu'il court, ce qu'il couvre ne se met pas a la corbeille, ne se
 * remplace pas, ne s'archive pas, ne se detruit pas, et les regles de conservation l'ignorent. Le
 * lire, le renommer ou le deplacer reste permis. Un gel se leve, il ne s'efface pas : la trace de
 * qui l'a pose et leve reste.</p>
 */
@Service
public class GedHoldService {

    public static final String FOLDER = "FOLDER";
    public static final String DOCUMENT = "DOCUMENT";

    @PersistenceContext
    private EntityManager entityManager;

    // Poser, lever, lister ------------------------------------------------------------------------

    @Transactional
    public GedHold place(GedHoldRequest request) {
        String type = request.targetType() == null ? "" : request.targetType().trim().toUpperCase(Locale.ROOT);
        if (!FOLDER.equals(type) && !DOCUMENT.equals(type)) {
            throw new BadRequestException("Un gel porte sur un dossier (FOLDER) ou un fichier (DOCUMENT).");
        }
        Long targetId = targetId(type, request.targetCode());
        String code = "GEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        entityManager.createNativeQuery("""
                INSERT INTO document_legal_hold (id, code, target_type, target_id, reason, reference, placed_by, placed_at)
                VALUES (:id, :code, :type, :target, :reason, :reference, :by, now())
                """).setParameter("id", newId()).setParameter("code", code).setParameter("type", type)
                .setParameter("target", targetId).setParameter("reason", request.reason().trim())
                .setParameter("reference", blankToNull(request.reference())).setParameter("by", currentUserId())
                .executeUpdate();
        return get(code);
    }

    /** Leve un gel ; le motif est garde avec la trace. */
    @Transactional
    public GedHold release(String code, String reason) {
        GedHold hold = get(code);
        if (!hold.active()) {
            throw new ConflictException("legal-hold", "ce gel est déjà levé");
        }
        entityManager.createNativeQuery("""
                UPDATE document_legal_hold SET released_at = now(), released_by = :by, release_reason = :reason
                WHERE code = :code AND released_at IS NULL
                """).setParameter("by", currentUserId()).setParameter("reason", reason.trim()).setParameter("code", code)
                .executeUpdate();
        return get(code);
    }

    @Transactional(readOnly = true)
    public GedHold get(String code) {
        return holds("WHERE h.code = :code", code).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Gel introuvable : " + code));
    }

    /** Les gels, du plus recent au plus ancien ; {@code activeOnly} : ceux qui courent. */
    @Transactional(readOnly = true)
    public List<GedHold> list(boolean activeOnly) {
        return holds(activeOnly ? "WHERE h.released_at IS NULL" : "", null);
    }

    // Verifications -------------------------------------------------------------------------------

    /** Le document est-il gele, directement ou par un dossier au-dessus de lui ? */
    @Transactional(readOnly = true)
    public boolean isHeld(Document document) {
        return !heldDocumentIds(List.of(document.getId())).isEmpty();
    }

    /**
     * Parmi ces documents (vivants ou a la corbeille), ceux qui sont geles. En une requete : le
     * gel d'un dossier couvre tout document dont le dossier a ce dossier sur son chemin.
     */
    @Transactional(readOnly = true)
    public Set<Long> heldDocumentIds(Collection<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return Set.of();
        }
        @SuppressWarnings("unchecked")
        List<Number> held = entityManager.createNativeQuery("""
                SELECT d.id FROM document d
                WHERE d.id IN (:ids) AND (
                    EXISTS (SELECT 1 FROM document_legal_hold h
                            WHERE h.released_at IS NULL AND h.target_type = 'DOCUMENT' AND h.target_id = d.id)
                    OR EXISTS (SELECT 1 FROM document_legal_hold h
                               JOIN document_folder hf ON hf.id = h.target_id
                               JOIN document_folder df ON df.id = d.folder_id
                               WHERE h.released_at IS NULL AND h.target_type = 'FOLDER' AND df.path LIKE hf.path || '%'))
                """).setParameter("ids", documentIds).getResultList();
        Set<Long> ids = new HashSet<>();
        held.forEach(id -> ids.add(id.longValue()));
        return ids;
    }

    /** Refuse (409) de toucher a un document gele. */
    @Transactional(readOnly = true)
    public void assertNotHeld(Document document, String what) {
        if (isHeld(document)) {
            throw new ConflictException("legal-hold", "« " + document.getTitle() + " » est sous gel juridique : il ne se "
                    + what + " pas tant que le gel n'est pas levé");
        }
    }

    /** Un dossier est-il gele, lui ou un dossier au-dessus de lui ? */
    @Transactional(readOnly = true)
    public boolean isFolderHeld(DocumentFolder folder) {
        List<Long> path = GedAccessService.pathIds(folder.getPath());
        if (path.isEmpty()) {
            return false;
        }
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM document_legal_hold
                WHERE released_at IS NULL AND target_type = 'FOLDER' AND target_id IN (:ids)
                """).setParameter("ids", path).getSingleResult();
        return count.longValue() > 0;
    }

    // Aides --------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<GedHold> holds(String where, String code) {
        var query = entityManager.createNativeQuery("""
                SELECT h.code, h.target_type,
                       CASE WHEN h.target_type = 'FOLDER' THEN f.code ELSE d.code END,
                       CASE WHEN h.target_type = 'FOLDER' THEN f.name ELSE d.title END,
                       h.reason, h.reference, pu.email, h.placed_at, ru.email, h.released_at, h.release_reason
                FROM document_legal_hold h
                LEFT JOIN document_folder f ON h.target_type = 'FOLDER' AND f.id = h.target_id
                LEFT JOIN document d ON h.target_type = 'DOCUMENT' AND d.id = h.target_id
                LEFT JOIN users pu ON pu.id = h.placed_by
                LEFT JOIN users ru ON ru.id = h.released_by
                """ + where + " ORDER BY h.placed_at DESC");
        if (code != null) {
            query.setParameter("code", code.trim());
        }
        List<GedHold> holds = new ArrayList<>();
        for (Object[] row : (List<Object[]>) query.getResultList()) {
            holds.add(new GedHold((String) row[0], (String) row[1], (String) row[2], (String) row[3], (String) row[4],
                    (String) row[5], (String) row[6], instant(row[7]), (String) row[8], instant(row[9]), (String) row[10],
                    row[9] == null));
        }
        return holds;
    }

    private Long targetId(String type, String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Indiquez le code du dossier ou du fichier à geler.");
        }
        String sql = FOLDER.equals(type) ? "SELECT id FROM document_folder WHERE code = :code AND purged_at IS NULL"
                : "SELECT id FROM document WHERE code = :code AND purged_at IS NULL";
        @SuppressWarnings("unchecked")
        List<Number> ids = entityManager.createNativeQuery(sql).setParameter("code", code.trim()).getResultList();
        if (ids.isEmpty()) {
            throw new ResourceNotFoundException((FOLDER.equals(type) ? "Dossier introuvable : " : "Fichier introuvable : ") + code);
        }
        return ids.getFirst().longValue();
    }

    /** Un identifiant pour une ligne ecrite en SQL, du meme ordre que ceux des entites. */
    private static long newId() {
        return (System.currentTimeMillis() << 20) | java.util.concurrent.ThreadLocalRandom.current().nextInt(1 << 20);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser().getId();
        }
        return null;
    }

    static Instant instant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant instant) return instant;
        if (value instanceof java.time.OffsetDateTime offset) return offset.toInstant();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
        return null;
    }
}
