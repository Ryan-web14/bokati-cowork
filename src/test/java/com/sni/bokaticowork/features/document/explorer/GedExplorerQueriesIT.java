package com.sni.bokaticowork.features.document.explorer;

import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.explorer.repository.GedExplorerQueries;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'explorateur de la GED sur un PostgreSQL reel : la migration, le contenu mixte d'un dossier
 * pagine d'un seul tenant, et la corbeille, invisible des entites mais restaurable.
 */
@Import(GedExplorerQueries.class)
class GedExplorerQueriesIT extends PostgresIntegrationTestBase {

    private static final AtomicLong IDS = new AtomicLong(990_000_000L);

    @Autowired private EntityManager entityManager;
    @Autowired private GedExplorerQueries queries;
    @Autowired private DocumentFolderRepository folders;
    @Autowired private DocumentRepository documents;

    private long folder(Long parentId, String name) {
        long id = IDS.incrementAndGet();
        String parentPath = parentId == null ? "/" : folders.findById(parentId).orElseThrow().getPath();
        entityManager.createNativeQuery("""
                INSERT INTO document_folder (id, code, name, parent_id, space, path, depth, created_at, updated_at, deleted)
                VALUES (:id, :code, :name, :parent, 'GENERIC', :path, :depth, now(), now(), FALSE)
                """).setParameter("id", id).setParameter("code", "FLD-" + id).setParameter("name", name)
                .setParameter("parent", parentId).setParameter("path", parentPath + id + "/")
                .setParameter("depth", parentId == null ? 0 : 1).executeUpdate();
        return id;
    }

    private long file(Long folderId, String title) {
        long id = IDS.incrementAndGet();
        entityManager.createNativeQuery("""
                INSERT INTO document (id, code, owner_type, owner_id, category, document_type_id, title, file_url,
                                      status, uploaded_at, updated_at, correction_count, space, deleted, folder_id)
                SELECT :id, :code, 'INTERNAL', NULL, 'OTHER', t.id, :title, 'internal/x', 'APPROVED',
                       now(), now(), 0, 'GENERIC', FALSE, :folder
                FROM document_type t WHERE t.code = 'GENERAL'
                """).setParameter("id", id).setParameter("code", "DOC-" + id).setParameter("title", title)
                .setParameter("folder", folderId).executeUpdate();
        return id;
    }

    private DocumentFolder load(long id) {
        entityManager.flush();
        entityManager.clear();
        return folders.findById(id).orElseThrow();
    }

    @Test
    @DisplayName("Un document interne n'a pas de proprietaire, et le type general existe")
    void migrationOpensInternalDocuments() {
        Object nullable = entityManager.createNativeQuery("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'document' AND column_name = 'owner_id'
                """).getSingleResult();
        assertThat(nullable).isEqualTo("YES");
        Object[] general = (Object[]) entityManager.createNativeQuery(
                "SELECT requires_review, multiple_allowed, allowed_mime_types FROM document_type WHERE code = 'GENERAL'")
                .getSingleResult();
        assertThat(general[0]).isEqualTo(false);
        assertThat(general[1]).isEqualTo(true);
        assertThat((String) general[2]).contains("application/pdf", "spreadsheetml", "image/tiff", "text/csv");
    }

    @Test
    @DisplayName("Le contenu d'un dossier : ses sous-dossiers puis ses fichiers, et un nom se cherche sans la casse")
    void folderContent() {
        long root = folder(null, "Administratif GED");
        folder(root, "Courriers");
        folder(root, "Assurances");
        file(root, "statuts.pdf");
        file(root, "Bilan 2025.pdf");
        DocumentFolder parent = load(root);

        List<DocumentFolder> children = queries.folders(parent, null, false, false);
        assertThat(children).extracting(DocumentFolder::getName).containsExactly("Assurances", "Courriers");
        assertThat(queries.countFiles(parent, null)).isEqualTo(2);
        // Les fichiers se lisent a partir d'un decalage quelconque, apres les dossiers.
        assertThat(queries.files(parent, null, "name", false, 1, 5)).extracting(Document::getTitle).containsExactly("statuts.pdf");
        assertThat(queries.files(parent, "BILAN", "name", false, 0, 5)).extracting(Document::getTitle).containsExactly("Bilan 2025.pdf");

        assertThat(queries.titleTaken(parent, "STATUTS.PDF")).isTrue();
        assertThat(queries.childNamed(parent, "courriers")).isPresent();
        Map<Long, Long> counts = queries.documentCounts(List.of(root));
        assertThat(counts).containsEntry(root, 2L);
        assertThat(queries.childrenCounts(List.of(root))).containsEntry(root, 2L);
    }

    @Test
    @DisplayName("La corbeille cache un dossier et son contenu, puis les restaure ensemble")
    void trashAndRestore() {
        long root = folder(null, "Archives GED");
        long sub = folder(root, "2024");
        long inside = file(sub, "releve.pdf");
        long loose = file(null, "note GED.txt");
        entityManager.flush();

        queries.markFolderTrashed(List.of(root, sub), "lot-1", 7L, Instant.now());
        queries.markFilesTrashed(List.of(inside, loose), "lot-1", 7L, Instant.now());
        entityManager.flush();
        entityManager.clear();

        assertThat(folders.findById(root)).isEmpty();
        assertThat(documents.findById(inside)).isEmpty();
        List<Object[]> batches = queries.trashBatches(10);
        Object[] batch = batches.stream().filter(row -> "lot-1".equals(row[0])).findFirst().orElseThrow();
        assertThat(((Number) batch[3]).longValue()).isEqualTo(2);
        assertThat(((Number) batch[4]).longValue()).isEqualTo(2);
        // En tete du lot : le dossier racine et le fichier libre, pas ce qu'ils contiennent.
        assertThat(queries.trashedTopFolders("lot-1")).extracting(row -> row[2]).containsExactly("Archives GED");
        assertThat(queries.trashedTopFiles("lot-1")).extracting(row -> row[2]).containsExactly("note GED.txt");
        assertThat(queries.folderAlive(root)).isFalse();

        long[] restored = queries.restore("lot-1");
        entityManager.clear();

        assertThat(restored).containsExactly(2L, 2L);
        assertThat(folders.findById(sub)).isPresent();
        assertThat(documents.findById(inside).orElseThrow().getTrashBatch()).isNull();
        assertThat(queries.folderNameTakenAmongAlive(null, "archives ged")).isTrue();
    }

    @Test
    @DisplayName("La cle systeme d'un dossier du plan est unique, et la reprise ne range que les documents sans dossier")
    void filingQueries() {
        long plan = folder(null, "Membres GED");
        entityManager.createNativeQuery("UPDATE document_folder SET system_key = 'test:plan' WHERE id = :id")
                .setParameter("id", plan).executeUpdate();
        long other = folder(null, "Doublon");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            entityManager.createNativeQuery("UPDATE document_folder SET system_key = 'test:plan' WHERE id = :id")
                    .setParameter("id", other).executeUpdate();
            entityManager.flush();
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("La reprise lit les documents sans dossier a partir d'un curseur, et ne deplace pas un document deja range")
    void unfiledDocuments() {
        long placed = folder(null, "Deja range");
        long member = IDS.incrementAndGet();
        entityManager.createNativeQuery("""
                INSERT INTO document (id, code, owner_type, owner_id, category, document_type_id, title, file_url,
                                      status, uploaded_at, updated_at, correction_count, space, deleted)
                SELECT :id, :code, 'MEMBER', 12, 'OTHER', t.id, 'piece', 'member/12', 'APPROVED',
                       now(), now(), 0, 'GENERIC', FALSE
                FROM document_type t WHERE t.code = 'GENERAL'
                """).setParameter("id", member).setParameter("code", "DOC-" + member).executeUpdate();
        long internal = file(null, "interne.pdf");
        entityManager.flush();

        List<Long> ids = queries.unfiledDocumentIds(member - 1, 50);
        assertThat(ids).contains(member).doesNotContain(internal);
        assertThat(queries.unfiledDocumentIds(member, 50)).doesNotContain(member);

        assertThat(queries.fileDocument(member, placed)).isEqualTo(1);
        assertThat(queries.fileDocument(member, placed)).isZero();
        entityManager.clear();
        assertThat(documents.findById(member).orElseThrow().getFolder().getId()).isEqualTo(placed);
    }
}
