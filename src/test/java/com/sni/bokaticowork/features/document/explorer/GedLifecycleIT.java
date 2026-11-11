package com.sni.bokaticowork.features.document.explorer;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentStorageService;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedHold;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedHoldRequest;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedPurgeResult;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedUsage;
import com.sni.bokaticowork.features.document.explorer.lifecycle.GedHoldService;
import com.sni.bokaticowork.features.document.explorer.lifecycle.GedPurgeService;
import com.sni.bokaticowork.features.document.explorer.lifecycle.GedQuotaService;
import com.sni.bokaticowork.features.document.retention.service.implementation.DocumentRetentionServiceImpl;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le cycle de fin de vie de la GED sur un PostgreSQL reel : gel juridique, destruction definitive
 * (fichier efface du stockage, trace gardee), corbeille videe, quotas, regle « a detruire ».
 */
@Import({GedHoldService.class, GedPurgeService.class, GedQuotaService.class, DocumentStorageService.class,
        DocumentRetentionServiceImpl.class})
@TestPropertySource(properties = {
        "app.document.storage.provider=filesystem",
        "app.document.storage.root-path=target/ged-lifecycle-it",
        "app.ged.quota.total-bytes=0"
})
class GedLifecycleIT extends PostgresIntegrationTestBase {

    private static final AtomicLong IDS = new AtomicLong(995_000_000L);
    private static final Path ROOT = Path.of("target/ged-lifecycle-it").toAbsolutePath().normalize();

    @Autowired private EntityManager entityManager;
    @Autowired private GedHoldService holds;
    @Autowired private GedPurgeService purge;
    @Autowired private GedQuotaService quotas;
    @Autowired private DocumentRetentionServiceImpl retention;
    @Autowired private DocumentFolderRepository folders;
    @Autowired private DocumentRepository documents;
    @MockitoBean private DocumentService documentService;

    @Test
    @DisplayName("Le gel d'un dossier couvre tout ce qu'il contient, sous-dossiers compris, jusqu'a sa levee")
    void folderHoldCoversItsTree() {
        long litige = folder(null, "Litige Dupont");
        long pieces = folder(litige, "Pieces");
        long inside = file(pieces, "constat.pdf", 1000);
        long outside = file(null, "note.txt", 10);
        entityManager.flush();

        GedHold hold = holds.place(new GedHoldRequest("FOLDER", "FLD-LC-" + litige, "Contentieux Dupont", "RG 26/0142"));
        assertThat(hold.active()).isTrue();
        assertThat(hold.targetName()).isEqualTo("Litige Dupont");
        assertThat(holds.heldDocumentIds(List.of(inside, outside))).containsExactly(inside);
        assertThat(holds.isFolderHeld(folders.findById(pieces).orElseThrow())).isTrue();
        assertThatThrownBy(() -> holds.assertNotHeld(documents.findById(inside).orElseThrow(), "remplace"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("gel juridique");

        GedHold released = holds.release(hold.code(), "Affaire classee");
        assertThat(released.active()).isFalse();
        assertThat(released.releaseReason()).isEqualTo("Affaire classee");
        assertThat(holds.heldDocumentIds(List.of(inside))).isEmpty();
        assertThat(holds.list(true)).extracting(GedHold::code).doesNotContain(hold.code());
        assertThat(holds.list(false)).extracting(GedHold::code).contains(hold.code());
        assertThatThrownBy(() -> holds.release(hold.code(), "encore")).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Vider la corbeille efface les fichiers du stockage, garde la trace, et epargne ce qui est gele")
    void purgeErasesFilesAndKeepsATrace() throws IOException {
        long archives = folder(null, "Archives 2019");
        long gone = file(archives, "facture-2019.pdf", 2048);
        long kept = file(archives, "contrat-2019.pdf", 4096);
        Path goneFile = storedFile(gone);
        Path keptFile = storedFile(kept);
        trash(List.of(gone, kept), "lot-lc-1", 40);
        holds.place(new GedHoldRequest("DOCUMENT", "DOC-LC-" + kept, "Controle fiscal", null));
        entityManager.flush();

        GedPurgeResult result = purge.emptyTrash(30, "Corbeille videe");

        assertThat(result.batches()).isEqualTo(1);
        assertThat(result.documents()).isEqualTo(1);
        assertThat(result.filesErased()).isEqualTo(1);
        assertThat(result.heldSkipped()).isEqualTo(1);
        assertThat(Files.exists(goneFile)).isFalse();
        assertThat(Files.exists(keptFile)).isTrue();
        Object[] trace = (Object[]) entityManager.createNativeQuery(
                "SELECT purged_at IS NOT NULL, purge_reason, deleted FROM document WHERE id = :id")
                .setParameter("id", gone).getSingleResult();
        assertThat(trace[0]).isEqualTo(true);
        assertThat(trace[1]).isEqualTo("Corbeille videe");
        assertThat(trace[2]).isEqualTo(true);
        // Le lot garde son fichier gele : ses dossiers ne sont pas detruits
        Number aliveFolder = (Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM document_folder WHERE id = :id AND purged_at IS NULL").setParameter("id", archives).getSingleResult();
        assertThat(aliveFolder.longValue()).isEqualTo(1);
        // Rejouer ne refait rien
        assertThat(purge.emptyTrash(30, "Encore").documents()).isZero();
    }

    @Test
    @DisplayName("Un quota de dossier vaut pour tout son contenu ; le depassement est refuse en nommant le dossier")
    void quotas() {
        long service = folder(null, "Service juridique");
        long sub = folder(service, "Baux");
        file(sub, "bail.pdf", 600);
        file(service, "statuts.pdf", 300);
        entityManager.flush();
        quotas.setQuota(folders.findById(service).orElseThrow(), 1000L);
        entityManager.flush();
        entityManager.clear();

        GedUsage usage = quotas.usage(folders.findById(sub).orElseThrow());
        assertThat(usage.usedBytes()).isEqualTo(600);
        assertThat(usage.remainingBytes()).isEqualTo(100);
        assertThat(usage.limitingFolderCode()).isEqualTo("FLD-LC-" + service);

        assertThatCode(() -> quotas.assertRoom(folders.findById(sub).orElseThrow(), 100)).doesNotThrowAnyException();
        assertThatThrownBy(() -> quotas.assertRoom(folders.findById(sub).orElseThrow(), 101))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Service juridique");
        assertThatCode(() -> quotas.assertRoom(null, 1_000_000)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Une regle « a detruire » sur un dossier marque ses vieux fichiers, sauf les geles et les conserves")
    void retentionMarksForDestruction() {
        long courriers = folder(null, "Courriers");
        long old = file(courriers, "courrier-2015.pdf", 10);
        long frozen = file(courriers, "courrier-litige.pdf", 10);
        long exempt = file(courriers, "courrier-historique.pdf", 10);
        long elsewhere = file(null, "courrier-hors-dossier.pdf", 10);
        entityManager.createNativeQuery("UPDATE document SET uploaded_at = '2015-01-01T00:00:00Z' WHERE id IN (:ids)")
                .setParameter("ids", List.of(old, frozen, exempt, elsewhere)).executeUpdate();
        entityManager.createNativeQuery("UPDATE document SET retention_exempt = TRUE WHERE id = :id").setParameter("id", exempt).executeUpdate();
        holds.place(new GedHoldRequest("DOCUMENT", "DOC-LC-" + frozen, "Litige", null));
        entityManager.createNativeQuery("""
                INSERT INTO document_retention_policy (id, code, name, retention_days, retention_reference, action, active, created_at, folder_id)
                VALUES (:id, 'COURRIERS-5-ANS', 'Courriers 5 ans', 1825, 'UPLOAD_DATE', 'PURGE_FLAG', TRUE, now(), :folder)
                """).setParameter("id", IDS.incrementAndGet()).setParameter("folder", courriers).executeUpdate();
        entityManager.flush();
        entityManager.clear();

        int marked = retention.applyPolicies();
        entityManager.flush();
        entityManager.clear();

        assertThat(marked).isEqualTo(1);
        Document flagged = documents.findById(old).orElseThrow();
        assertThat(flagged.getDestructionDueAt()).isNotNull();
        assertThat(flagged.getDestructionPolicyCode()).isEqualTo("COURRIERS-5-ANS");
        assertThat(documents.findById(frozen).orElseThrow().getDestructionDueAt()).isNull();
        assertThat(documents.findById(exempt).orElseThrow().getDestructionDueAt()).isNull();
        assertThat(documents.findById(elsewhere).orElseThrow().getDestructionDueAt()).isNull();
    }

    // Donnees ------------------------------------------------------------------------------------

    private long folder(Long parentId, String name) {
        long id = IDS.incrementAndGet();
        String parentPath = parentId == null ? "/" : (String) entityManager
                .createNativeQuery("SELECT path FROM document_folder WHERE id = :id").setParameter("id", parentId).getSingleResult();
        entityManager.createNativeQuery("""
                INSERT INTO document_folder (id, code, name, parent_id, space, path, depth, created_at, updated_at, deleted)
                VALUES (:id, :code, :name, :parent, 'GENERIC', :path, 0, now(), now(), FALSE)
                """).setParameter("id", id).setParameter("code", "FLD-LC-" + id).setParameter("name", name)
                .setParameter("parent", parentId).setParameter("path", parentPath + id + "/").executeUpdate();
        return id;
    }

    /** Un document general approuve, avec une version de {@code size} octets. */
    private long file(Long folderId, String title, long size) {
        long id = IDS.incrementAndGet();
        entityManager.createNativeQuery("""
                INSERT INTO document (id, code, owner_type, owner_id, category, document_type_id, title, file_name, file_url,
                                      status, uploaded_at, updated_at, correction_count, space, deleted, folder_id, current_version_number)
                SELECT :id, :code, 'INTERNAL', NULL, 'OTHER', t.id, :title, :title, 'internal/x', 'APPROVED',
                       now(), now(), 0, 'GENERIC', FALSE, :folder, 1
                FROM document_type t WHERE t.code = 'GENERAL'
                """).setParameter("id", id).setParameter("code", "DOC-LC-" + id).setParameter("title", title)
                .setParameter("folder", folderId).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO document_version (id, document_id, version_number, storage_provider, storage_path, original_file_name,
                                              stored_file_name, mime_type_detected, checksum_sha256, file_size_bytes,
                                              upload_status, antivirus_status, uploaded_at, is_current)
                VALUES (:id, :doc, 1, 'FILESYSTEM', :path, :title, :title, 'application/pdf', 'x', :size, 'COMPLETED', 'CLEAN', now(), TRUE)
                """).setParameter("id", IDS.incrementAndGet()).setParameter("doc", id)
                .setParameter("path", ROOT.resolve("doc-" + id + ".pdf").toString()).setParameter("title", title)
                .setParameter("size", size).executeUpdate();
        return id;
    }

    private Path storedFile(long documentId) throws IOException {
        Files.createDirectories(ROOT);
        Path file = ROOT.resolve("doc-" + documentId + ".pdf");
        Files.writeString(file, "%PDF-1.4 contenu");
        return file;
    }

    private void trash(List<Long> documentIds, String batch, int daysAgo) {
        entityManager.createNativeQuery("""
                UPDATE document SET deleted = TRUE, trash_batch = :batch, trashed_at = now() - make_interval(days => :days)
                WHERE id IN (:ids)
                """).setParameter("batch", batch).setParameter("days", daysAgo).setParameter("ids", documentIds).executeUpdate();
        entityManager.createNativeQuery("""
                UPDATE document_folder SET deleted = TRUE, trash_batch = :batch, trashed_at = now() - make_interval(days => :days)
                WHERE id IN (SELECT folder_id FROM document WHERE id IN (:ids))
                """).setParameter("batch", batch).setParameter("days", daysAgo).setParameter("ids", documentIds).executeUpdate();
    }
}
