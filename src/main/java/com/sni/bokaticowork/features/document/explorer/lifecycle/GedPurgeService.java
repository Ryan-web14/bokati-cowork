package com.sni.bokaticowork.features.document.explorer.lifecycle;

import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentStorageService;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedPurgeResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * La destruction definitive : vider la corbeille, et detruire ce que la conservation a marque.
 *
 * <p>Detruire, c'est effacer chaque version du fichier du stockage et son texte lu, puis garder la
 * ligne du document comme trace : qui l'a detruit, quand, pourquoi. Le document n'apparait alors
 * plus nulle part. Un document sous gel juridique n'est jamais detruit : il est saute et compte.</p>
 *
 * <p>Chaque document est detruit dans sa propre transaction, le stockage d'abord : si l'effacement
 * echoue, le document reste entier et l'erreur est comptee ; une destruction se rejoue sans
 * risque.</p>
 */
@Slf4j
@Service
public class GedPurgeService {

    @PersistenceContext
    private EntityManager entityManager;

    private final DocumentStorageService storageService;
    private final GedHoldService holdService;
    private final TransactionTemplate transactionTemplate;

    public GedPurgeService(DocumentStorageService storageService, GedHoldService holdService, TransactionTemplate transactionTemplate) {
        this.storageService = storageService;
        this.holdService = holdService;
        this.transactionTemplate = transactionTemplate;
    }

    /** Detruit des documents ; les geles sont sautes. */
    public GedPurgeResult purgeDocuments(List<Long> documentIds, String reason) {
        Long actor = GedHoldService.currentUserId();
        Set<Long> held = transactionTemplate.execute(status -> holdService.heldDocumentIds(documentIds));
        int purged = 0;
        int files = 0;
        int failed = 0;
        for (Long id : documentIds) {
            if (held != null && held.contains(id)) {
                continue;
            }
            try {
                Integer erased = transactionTemplate.execute(status -> purgeOne(id, actor, reason));
                if (erased != null && erased >= 0) {
                    purged++;
                    files += erased;
                }
            } catch (RuntimeException ex) {
                failed++;
                log.error("GED · destruction du document {} en échec : {}", id, ex.getMessage(), ex);
            }
        }
        return new GedPurgeResult(0, 0, purged, files, held == null ? 0 : held.size(), failed);
    }

    /**
     * Detruit un lot de la corbeille : ses fichiers, puis ses dossiers s'il ne reste rien de gele
     * dedans.
     */
    public GedPurgeResult purgeBatch(String batch, String reason) {
        @SuppressWarnings("unchecked")
        List<Number> ids = transactionTemplate.execute(status -> entityManager.createNativeQuery("""
                SELECT id FROM document WHERE trash_batch = :batch AND deleted = TRUE AND purged_at IS NULL
                """).setParameter("batch", batch).getResultList());
        GedPurgeResult documents = purgeDocuments(ids == null ? List.of() : ids.stream().map(Number::longValue).toList(), reason);
        int folders = 0;
        if (documents.heldSkipped() == 0 && documents.failed() == 0) {
            Integer marked = transactionTemplate.execute(status -> entityManager.createNativeQuery("""
                    UPDATE document_folder SET purged_at = now() WHERE trash_batch = :batch AND deleted = TRUE AND purged_at IS NULL
                    """).setParameter("batch", batch).executeUpdate());
            folders = marked == null ? 0 : marked;
        }
        return new GedPurgeResult(1, folders, documents.documents(), documents.filesErased(), documents.heldSkipped(), documents.failed());
    }

    /** Vide la corbeille des lots supprimes depuis plus de {@code olderThanDays} jours (0 : tout). */
    public GedPurgeResult emptyTrash(int olderThanDays, String reason) {
        @SuppressWarnings("unchecked")
        List<String> batches = transactionTemplate.execute(status -> entityManager.createNativeQuery("""
                SELECT batch FROM (
                    SELECT trash_batch AS batch, trashed_at FROM document
                    WHERE trash_batch IS NOT NULL AND deleted = TRUE AND purged_at IS NULL
                    UNION ALL
                    SELECT trash_batch, trashed_at FROM document_folder
                    WHERE trash_batch IS NOT NULL AND deleted = TRUE AND purged_at IS NULL
                ) t
                GROUP BY batch
                HAVING MIN(trashed_at) <= now() - make_interval(days => :days)
                """).setParameter("days", Math.max(0, olderThanDays)).getResultList());
        int count = 0, folders = 0, documents = 0, files = 0, held = 0, failed = 0;
        for (String batch : batches == null ? List.<String>of() : batches) {
            GedPurgeResult result = purgeBatch(batch, reason);
            count++;
            folders += result.folders();
            documents += result.documents();
            files += result.filesErased();
            held += result.heldSkipped();
            failed += result.failed();
        }
        return new GedPurgeResult(count, folders, documents, files, held, failed);
    }

    /** -1 si le document est deja detruit ou absent ; sinon le nombre de fichiers effaces. */
    private int purgeOne(Long id, Long actor, String reason) {
        @SuppressWarnings("unchecked")
        List<Object[]> versions = entityManager.createNativeQuery("""
                SELECT v.storage_provider, v.storage_path FROM document_version v
                JOIN document d ON d.id = v.document_id
                WHERE v.document_id = :id AND d.purged_at IS NULL
                """).setParameter("id", id).getResultList();
        Number alive = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM document WHERE id = :id AND purged_at IS NULL")
                .setParameter("id", id).getSingleResult();
        if (alive.longValue() == 0) {
            return -1;
        }
        List<String> erased = new ArrayList<>();
        for (Object[] version : versions) {
            if (storageService.delete((String) version[0], (String) version[1])) {
                erased.add((String) version[1]);
            }
        }
        // Le fichier d'origine d'un document sans version (anciens enregistrements)
        if (versions.isEmpty()) {
            String url = (String) entityManager.createNativeQuery("SELECT file_url FROM document WHERE id = :id")
                    .setParameter("id", id).getSingleResult();
            try {
                if (storageService.delete("FILESYSTEM", url)) {
                    erased.add(url);
                }
            } catch (RuntimeException ex) {
                // Une adresse qui n'est pas un fichier du stockage : rien a effacer
                log.warn("GED · document {} sans version, adresse {} non effacée : {}", id, url, ex.getMessage());
            }
        }
        entityManager.createNativeQuery("DELETE FROM document_text WHERE document_id = :id").setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("""
                UPDATE document SET purged_at = now(), purged_by = :actor, purge_reason = :reason, deleted = TRUE,
                                    destruction_due_at = NULL, locked_by = NULL, locked_at = NULL, lock_expires_at = NULL
                WHERE id = :id
                """).setParameter("actor", actor).setParameter("reason", abbreviate(reason)).setParameter("id", id).executeUpdate();
        return erased.size();
    }

    private static String abbreviate(String reason) {
        String value = reason == null || reason.isBlank() ? "Destruction" : reason.trim();
        return value.length() <= 300 ? value : value.substring(0, 297) + "...";
    }
}
