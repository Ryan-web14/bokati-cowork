package com.sni.bokaticowork.features.document.explorer.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Les requetes de l'explorateur.
 *
 * <p>Le contenu d'un dossier melange dossiers et fichiers dans une seule pagination : les fichiers
 * se lisent donc a partir d'un decalage quelconque, ce qu'une {@code Pageable} ne sait pas faire.</p>
 *
 * <p>La corbeille vit hors des entites : un element supprime est cache par la restriction
 * {@code deleted = false} des entites, seules des requetes natives le voient encore.</p>
 */
@Repository
public class GedExplorerQueries {

    @PersistenceContext
    private EntityManager entityManager;

    /** Les sous-dossiers d'un dossier, ou les dossiers racines si {@code parent} est vide. */
    public List<DocumentFolder> folders(DocumentFolder parent, String search, boolean byDate, boolean descending) {
        String order = (byDate ? "f.updatedAt" : "LOWER(f.name)") + (descending ? " DESC" : " ASC");
        String jpql = "SELECT f FROM DocumentFolder f WHERE "
                + (parent == null ? "f.parent IS NULL" : "f.parent = :parent")
                + (search == null ? "" : " AND LOWER(f.name) LIKE :search")
                + " ORDER BY " + order + ", f.id";
        TypedQuery<DocumentFolder> query = entityManager.createQuery(jpql, DocumentFolder.class);
        if (parent != null) query.setParameter("parent", parent);
        if (search != null) query.setParameter("search", like(search));
        return query.getResultList();
    }

    public long countFiles(DocumentFolder folder, String search) {
        String jpql = "SELECT COUNT(d) FROM Document d WHERE " + fileFilter(folder, search);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        bindFileFilter(query, folder, search);
        return query.getSingleResult();
    }

    /** Les fichiers d'un dossier a partir de {@code offset}, triables sur une colonne connue. */
    public List<Document> files(DocumentFolder folder, String search, String sort, boolean descending, int offset, int limit) {
        String column = switch (sort) {
            case "updatedAt" -> "d.updatedAt";
            case "size" -> "d.fileSize";
            case "type" -> "d.mimeType";
            default -> "LOWER(d.title)";
        };
        String jpql = "SELECT d FROM Document d LEFT JOIN FETCH d.documentType WHERE " + fileFilter(folder, search)
                + " ORDER BY " + column + (descending ? " DESC" : " ASC") + ", d.id";
        TypedQuery<Document> query = entityManager.createQuery(jpql, Document.class);
        bindFileFilter(query, folder, search);
        return query.setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    /** Nombre de sous-dossiers par dossier, en une requete. */
    public Map<Long, Long> childrenCounts(Collection<Long> folderIds) {
        return counts("SELECT f.parent.id, COUNT(f) FROM DocumentFolder f WHERE f.parent.id IN :ids GROUP BY f.parent.id", folderIds);
    }

    /** Nombre de fichiers par dossier, en une requete. */
    public Map<Long, Long> documentCounts(Collection<Long> folderIds) {
        return counts("SELECT d.folder.id, COUNT(d) FROM Document d WHERE d.folder.id IN :ids GROUP BY d.folder.id", folderIds);
    }

    public List<Document> filesIn(Collection<Long> folderIds) {
        if (folderIds.isEmpty()) return List.of();
        return entityManager.createQuery("SELECT d FROM Document d WHERE d.folder.id IN :ids", Document.class)
                .setParameter("ids", folderIds)
                .getResultList();
    }

    public boolean titleTaken(DocumentFolder folder, String title) {
        String jpql = "SELECT COUNT(d) FROM Document d WHERE "
                + (folder == null ? "d.folder IS NULL" : "d.folder = :folder")
                + " AND LOWER(d.title) = :title";
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        if (folder != null) query.setParameter("folder", folder);
        return query.setParameter("title", title.toLowerCase()).getSingleResult() > 0;
    }

    public java.util.Optional<DocumentFolder> childNamed(DocumentFolder parent, String name) {
        String jpql = "SELECT f FROM DocumentFolder f WHERE "
                + (parent == null ? "f.parent IS NULL" : "f.parent = :parent")
                + " AND LOWER(f.name) = :name";
        TypedQuery<DocumentFolder> query = entityManager.createQuery(jpql, DocumentFolder.class);
        if (parent != null) query.setParameter("parent", parent);
        return query.setParameter("name", name.toLowerCase()).setMaxResults(1).getResultList().stream().findFirst();
    }

    // Corbeille ---------------------------------------------------------------------------------

    /** Les lots de la corbeille, du plus recent au plus ancien. */
    @SuppressWarnings("unchecked")
    public List<Object[]> trashBatches(int limit) {
        return entityManager.createNativeQuery("""
                SELECT batch, MIN(trashed_at) AS trashed_at, MAX(trashed_by) AS trashed_by,
                       SUM(CASE WHEN kind = 'FOLDER' THEN 1 ELSE 0 END) AS folders,
                       SUM(CASE WHEN kind = 'FILE' THEN 1 ELSE 0 END) AS files
                FROM (
                    SELECT trash_batch AS batch, trashed_at, trashed_by, 'FOLDER' AS kind
                    FROM document_folder WHERE trash_batch IS NOT NULL AND deleted = TRUE
                    UNION ALL
                    SELECT trash_batch, trashed_at, trashed_by, 'FILE'
                    FROM document WHERE trash_batch IS NOT NULL AND deleted = TRUE
                ) t
                GROUP BY batch
                ORDER BY MIN(trashed_at) DESC
                LIMIT :limit
                """)
                .setParameter("limit", limit)
                .getResultList();
    }

    /** Les dossiers en tete d'un lot : ceux dont le parent n'est pas parti avec eux. */
    @SuppressWarnings("unchecked")
    public List<Object[]> trashedTopFolders(String batch) {
        return entityManager.createNativeQuery("""
                SELECT f.id, f.code, f.name, f.parent_id, f.trashed_at, f.updated_at, f.color, f.icon
                FROM document_folder f
                WHERE f.trash_batch = :batch AND f.deleted = TRUE
                  AND (f.parent_id IS NULL OR f.parent_id NOT IN (
                        SELECT p.id FROM document_folder p WHERE p.trash_batch = :batch))
                ORDER BY LOWER(f.name)
                """)
                .setParameter("batch", batch)
                .getResultList();
    }

    /** Les fichiers en tete d'un lot : ceux dont le dossier n'est pas parti avec eux. */
    @SuppressWarnings("unchecked")
    public List<Object[]> trashedTopFiles(String batch) {
        return entityManager.createNativeQuery("""
                SELECT d.id, d.code, d.title, d.folder_id, d.trashed_at, d.updated_at, d.file_name, d.mime_type, d.file_size
                FROM document d
                WHERE d.trash_batch = :batch AND d.deleted = TRUE
                  AND (d.folder_id IS NULL OR d.folder_id NOT IN (
                        SELECT p.id FROM document_folder p WHERE p.trash_batch = :batch))
                ORDER BY LOWER(d.title)
                """)
                .setParameter("batch", batch)
                .getResultList();
    }

    /** Vrai si le dossier existe et n'est pas a la corbeille. */
    public boolean folderAlive(Long folderId) {
        Number count = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM document_folder WHERE id = :id AND deleted = FALSE")
                .setParameter("id", folderId)
                .getSingleResult();
        return count.longValue() > 0;
    }

    public boolean folderNameTakenAmongAlive(Long parentId, String name) {
        jakarta.persistence.Query query = entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM document_folder WHERE deleted = FALSE AND LOWER(name) = :name AND "
                                + (parentId == null ? "parent_id IS NULL" : "parent_id = :parent"))
                .setParameter("name", name.toLowerCase());
        if (parentId != null) query.setParameter("parent", parentId);
        return ((Number) query.getSingleResult()).longValue() > 0;
    }

    public void renameTrashedFolder(Long id, String name) {
        entityManager.createNativeQuery("UPDATE document_folder SET name = :name WHERE id = :id")
                .setParameter("name", name)
                .setParameter("id", id)
                .executeUpdate();
    }

    /** Sort un lot de la corbeille. Renvoie le nombre de dossiers et de fichiers restaures. */
    public long[] restore(String batch) {
        int folders = entityManager.createNativeQuery("""
                UPDATE document_folder SET deleted = FALSE, trashed_at = NULL, trashed_by = NULL, trash_batch = NULL
                WHERE trash_batch = :batch AND deleted = TRUE
                """).setParameter("batch", batch).executeUpdate();
        int files = entityManager.createNativeQuery("""
                UPDATE document SET deleted = FALSE, trashed_at = NULL, trashed_by = NULL, trash_batch = NULL
                WHERE trash_batch = :batch AND deleted = TRUE
                """).setParameter("batch", batch).executeUpdate();
        return new long[]{folders, files};
    }

    public void markFolderTrashed(Collection<Long> ids, String batch, Long actor, Instant at) {
        if (ids.isEmpty()) return;
        entityManager.createNativeQuery("""
                UPDATE document_folder SET deleted = TRUE, trashed_at = :at, trashed_by = :actor, trash_batch = :batch
                WHERE id IN (:ids)
                """).setParameter("at", at).setParameter("actor", actor).setParameter("batch", batch)
                .setParameter("ids", ids).executeUpdate();
    }

    public void markFilesTrashed(Collection<Long> ids, String batch, Long actor, Instant at) {
        if (ids.isEmpty()) return;
        entityManager.createNativeQuery("""
                UPDATE document SET deleted = TRUE, trashed_at = :at, trashed_by = :actor, trash_batch = :batch
                WHERE id IN (:ids)
                """).setParameter("at", at).setParameter("actor", actor).setParameter("batch", batch)
                .setParameter("ids", ids).executeUpdate();
    }

    // -------------------------------------------------------------------------------------------

    private String fileFilter(DocumentFolder folder, String search) {
        return (folder == null ? "d.folder IS NULL" : "d.folder = :folder")
                + (search == null ? "" : " AND LOWER(d.title) LIKE :search");
    }

    private void bindFileFilter(TypedQuery<?> query, DocumentFolder folder, String search) {
        if (folder != null) query.setParameter("folder", folder);
        if (search != null) query.setParameter("search", like(search));
    }

    private static String like(String search) {
        String escaped = search.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private Map<Long, Long> counts(String jpql, Collection<Long> ids) {
        Map<Long, Long> counts = new HashMap<>();
        if (ids.isEmpty()) return counts;
        List<Object[]> rows = entityManager.createQuery(jpql, Object[].class).setParameter("ids", ids).getResultList();
        for (Object[] row : rows) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }
}
