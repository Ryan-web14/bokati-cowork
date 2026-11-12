package com.sni.bokaticowork.features.document.explorer.lifecycle;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import com.sni.bokaticowork.features.document.explorer.access.GedAccessService;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedUsage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * L'espace occupe par la GED, et ses limites.
 *
 * <p>Ce qu'occupe un dossier, c'est la place de toutes les versions de ses fichiers, sous-dossiers
 * compris, corbeille comprise : un fichier supprime occupe le stockage tant qu'il n'est pas
 * detruit. Un dossier peut porter un quota, qui vaut pour tout son contenu ; la GED entiere peut
 * en porter un aussi ({@code app.ged.quota.total-bytes}, 0 : pas de limite). Un depot qui
 * depasserait l'un d'eux est refuse (409) en disant lequel.</p>
 */
@Service
public class GedQuotaService {

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.ged.quota.total-bytes:0}")
    private long totalQuotaBytes;

    /** Ce qu'occupe un dossier (ou toute la GED si {@code folder} est vide), et sa limite. */
    @Transactional(readOnly = true)
    public GedUsage usage(DocumentFolder folder) {
        long globalUsed = usedBytes(null);
        long globalQuota = Math.max(0, totalQuotaBytes);
        if (folder == null) {
            return new GedUsage(null, null, globalUsed, files(null), globalQuota == 0 ? null : globalQuota,
                    globalQuota == 0 ? null : Math.max(0, globalQuota - globalUsed), null, globalUsed, globalQuota == 0 ? null : globalQuota);
        }
        long used = usedBytes(folder.getPath());
        Long quota = folder.getQuotaBytes();
        Limit limit = tightest(folder, 0);
        return new GedUsage(folder.getCode(), folder.getName(), used, files(folder.getPath()), quota,
                limit == null ? null : Math.max(0, limit.quota() - limit.used()),
                limit == null ? null : limit.folderCode(), globalUsed, globalQuota == 0 ? null : globalQuota);
    }

    /** Fixe ou retire ({@code null} ou 0) le quota d'un dossier. */
    @Transactional
    public void setQuota(DocumentFolder folder, Long quotaBytes) {
        if (quotaBytes != null && quotaBytes < 0) {
            throw new BadRequestException("Un quota est un nombre d'octets positif.");
        }
        entityManager.createNativeQuery("UPDATE document_folder SET quota_bytes = :quota WHERE id = :id")
                .setParameter("quota", quotaBytes == null || quotaBytes == 0 ? null : quotaBytes)
                .setParameter("id", folder.getId()).executeUpdate();
        folder.setQuotaBytes(quotaBytes == null || quotaBytes == 0 ? null : quotaBytes);
    }

    /**
     * Refuse un ajout de {@code addedBytes} dans un dossier (ou a la racine si vide) qui ferait
     * depasser le quota de ce dossier, d'un dossier au-dessus, ou de la GED entiere.
     */
    @Transactional(readOnly = true)
    public void assertRoom(DocumentFolder folder, long addedBytes) {
        if (addedBytes <= 0) {
            return;
        }
        if (totalQuotaBytes > 0) {
            long used = usedBytes(null);
            if (used + addedBytes > totalQuotaBytes) {
                throw new ConflictException("ged-quota", "l'espace de la GED est plein (" + human(used) + " sur "
                        + human(totalQuotaBytes) + ") : il manque " + human(used + addedBytes - totalQuotaBytes));
            }
        }
        Limit limit = tightest(folder, addedBytes);
        if (limit != null && limit.used() + addedBytes > limit.quota()) {
            throw new ConflictException("ged-quota", "le dossier « " + limit.folderName() + " » a atteint son quota ("
                    + human(limit.used()) + " sur " + human(limit.quota()) + ") : il manque "
                    + human(limit.used() + addedBytes - limit.quota()));
        }
    }

    /** Une nouvelle version d'un document : elle compte dans le dossier du document. */
    @Transactional(readOnly = true)
    public void assertRoomForDocument(String documentCode, long addedBytes) {
        @SuppressWarnings("unchecked")
        List<Object> rows = entityManager.createNativeQuery("""
                SELECT f.id FROM document d LEFT JOIN document_folder f ON f.id = d.folder_id WHERE d.code = :code
                """).setParameter("code", documentCode).getResultList();
        if (rows.isEmpty()) {
            return;
        }
        Object folderId = rows.getFirst() instanceof Object[] row ? row[0] : rows.getFirst();
        DocumentFolder folder = folderId == null ? null
                : entityManager.find(DocumentFolder.class, ((Number) folderId).longValue());
        assertRoom(folder, addedBytes);
    }

    private record Limit(String folderCode, String folderName, long quota, long used) {}

    /** Le dossier a quota du chemin (le dossier compris) ou il reste le moins de place. */
    @SuppressWarnings("unchecked")
    private Limit tightest(DocumentFolder folder, long addedBytes) {
        if (folder == null) {
            return null;
        }
        List<Long> path = GedAccessService.pathIds(folder.getPath());
        if (path.isEmpty()) {
            return null;
        }
        List<Object[]> quotas = entityManager.createNativeQuery("""
                SELECT code, name, quota_bytes, path FROM document_folder
                WHERE id IN (:ids) AND quota_bytes IS NOT NULL
                """).setParameter("ids", path).getResultList();
        Limit tightest = null;
        for (Object[] row : quotas) {
            long quota = ((Number) row[2]).longValue();
            long used = usedBytes((String) row[3]);
            Limit candidate = new Limit((String) row[0], (String) row[1], quota, used);
            if (tightest == null || quota - used < tightest.quota() - tightest.used()) {
                tightest = candidate;
            }
        }
        return tightest;
    }

    /** La place des versions des fichiers non detruits sous un chemin (tout, si vide). */
    private long usedBytes(String path) {
        var query = entityManager.createNativeQuery(path == null ? """
                SELECT COALESCE(SUM(v.file_size_bytes), 0) FROM document_version v
                JOIN document d ON d.id = v.document_id
                WHERE d.purged_at IS NULL AND d.folder_id IS NOT NULL
                """ : """
                SELECT COALESCE(SUM(v.file_size_bytes), 0) FROM document_version v
                JOIN document d ON d.id = v.document_id
                JOIN document_folder f ON f.id = d.folder_id
                WHERE d.purged_at IS NULL AND f.path LIKE :path
                """);
        if (path != null) {
            query.setParameter("path", path + "%");
        }
        return ((Number) query.getSingleResult()).longValue();
    }

    private long files(String path) {
        var query = entityManager.createNativeQuery(path == null ? """
                SELECT COUNT(*) FROM document d WHERE d.purged_at IS NULL AND d.deleted = FALSE AND d.folder_id IS NOT NULL
                """ : """
                SELECT COUNT(*) FROM document d JOIN document_folder f ON f.id = d.folder_id
                WHERE d.purged_at IS NULL AND d.deleted = FALSE AND f.path LIKE :path
                """);
        if (path != null) {
            query.setParameter("path", path + "%");
        }
        return ((Number) query.getSingleResult()).longValue();
    }

    /** 1,5 Mo, 230 Ko... pour les messages. */
    static String human(long bytes) {
        if (bytes < 1024) return bytes + " o";
        double value = bytes;
        String[] units = {"Ko", "Mo", "Go", "To"};
        int unit = -1;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return String.format(java.util.Locale.FRANCE, "%.1f %s", value, units[unit]);
    }
}
