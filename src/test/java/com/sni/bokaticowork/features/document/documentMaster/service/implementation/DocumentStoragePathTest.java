package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Un fichier depose ne s ecrit pas ou l appelant veut.
 *
 * <p>Le nom du fichier etait bien remplace par un UUID · mais son <b>extension</b> etait reprise
 * telle quelle, et {@code lastIndexOf('.')} sur un nom comme
 * {@code photo.../../../quelque/part} rendait « extension » tout ce qui suivait le dernier point,
 * separateurs compris. Le chemin cible etait normalise, mais jamais confine : le fichier
 * atterrissait hors du repertoire des documents, avec un contenu choisi par le deposant.</p>
 *
 * <p>Les validations d upload existantes portent sur le <b>type MIME</b>, pas sur le nom · un
 * {@code image/jpeg} parfaitement valable pouvait porter un nom hostile.</p>
 */
class DocumentStoragePathTest {

    private DocumentStorageService storage(Path root) {
        return new DocumentStorageService("filesystem", root.toString(),
                "http://localhost:9000", "bokati-documents", "", "", "");
    }

    // -----------------------------------------------------------------------------------------
    // L'extension
    // -----------------------------------------------------------------------------------------

    @ParameterizedTest
    @DisplayName("Une extension légitime est conservée")
    @CsvSource({
            "photo.jpg,jpg",
            "RELEVE.PDF,pdf",
            "archive.tar.gz,gz",
            "scan.jpeg,jpeg",
            "feuille.xlsx,xlsx"
    })
    void aLegitimateExtensionIsKept(String filename, String expected) throws Exception {
        assertThat(extensionOf(filename)).isEqualTo(expected);
    }

    @ParameterizedTest
    @DisplayName("Une extension porteuse de séparateurs est tronquée · c'était le vecteur")
    @CsvSource(value = {
            // Le dernier point tombe dans la sequence de remontee · tout ce qui suit disparait.
            "photo.../../../etc/passwd|''",
            "piece.jpg/../../evil|''",
            // Ici le dernier point precede « ssh » · l extension retenue est inoffensive.
            "x./../../home/app/.ssh/authorized_keys|ssh",
            "doc.p d f|p",
            "note.jp\\\\g|jp",
            "truc.|''",
            "sans-extension|''"
    }, delimiter = '|')
    void anExtensionCarryingSeparatorsIsCut(String filename, String expected) throws Exception {
        assertThat(extensionOf(filename)).isEqualTo(expected.replace("''", ""));
    }

    @ParameterizedTest
    @DisplayName("Quelle que soit l'entrée, l'extension retenue ne contient jamais de séparateur")
    @ValueSource(strings = {
            "photo.../../../etc/passwd",
            "piece.jpg/../../evil",
            "x./../../home/app/.ssh/authorized_keys",
            "a.b/c",
            "a..%2f..%2fetc",
            "truc.tar.gz"
    })
    void noSeparatorEverSurvivesInTheExtension(String filename) throws Exception {
        // C'est l'invariant qui ferme la faille · le detail de ce qui reste importe moins.
        assertThat(extensionOf(filename)).matches("[a-z0-9]{0,8}");
    }

    @Test
    @DisplayName("Une extension interminable est bornée à huit caractères")
    void anEndlessExtensionIsCapped() throws Exception {
        assertThat(extensionOf("fichier." + "a".repeat(200))).hasSize(8);
    }

    // -----------------------------------------------------------------------------------------
    // Le chemin écrit
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("Un nom hostile écrit quand même sous la racine des documents")
    void aHostileNameStillLandsUnderTheRoot(@TempDir Path root) {
        DocumentStorageService service = storage(root);

        DocumentStorageService.StoredDocument stored = service.storeBytes(
                "member/MBR-1", "DOC-1", 1, "photo.../../../etc/passwd", "contenu".getBytes());

        Path written = Path.of(stored.storagePath()).toAbsolutePath().normalize();
        assertThat(written).startsWith(root.toAbsolutePath().normalize());
        assertThat(Files.exists(written)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Un dossier de propriétaire ou un code hostile ne remonte pas non plus")
    @ValueSource(strings = {"../../..", "member/../../..", "..", "./.."})
    void aHostileOwnerFolderDoesNotEscape(String ownerFolder, @TempDir Path root) {
        DocumentStorageService service = storage(root);

        DocumentStorageService.StoredDocument stored = service.storeBytes(
                ownerFolder, "DOC-1", 1, "piece.pdf", "contenu".getBytes());

        assertThat(Path.of(stored.storagePath()).toAbsolutePath().normalize())
                .startsWith(root.toAbsolutePath().normalize());
    }

    @Test
    @DisplayName("Le nom stocké reste un UUID suivi de l'extension · jamais le nom fourni")
    void theStoredNameIsAlwaysAUuid(@TempDir Path root) {
        DocumentStorageService.StoredDocument stored = storage(root).storeBytes(
                "member/MBR-1", "DOC-1", 1, "passeport-de-joel.pdf", "contenu".getBytes());

        assertThat(stored.storedFileName()).endsWith(".pdf").doesNotContain("passeport");
        assertThat(stored.storedFileName().replace(".pdf", "")).hasSize(36);
    }

    // -----------------------------------------------------------------------------------------
    // La lecture
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("Une lecture hors de la racine est refusée · un enregistrement ancien peut être hostile")
    void readingOutsideTheRootIsRefused(@TempDir Path root) throws Exception {
        Path outside = root.getParent().resolve("secret-hors-racine.txt");
        Files.writeString(outside, "ce qui ne doit pas sortir");

        assertThatThrownBy(() -> storage(root).read("FILESYSTEM", outside.toString()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sort du répertoire");
    }

    @Test
    @DisplayName("Une lecture sous la racine passe")
    void readingUnderTheRootWorks(@TempDir Path root) {
        DocumentStorageService service = storage(root);
        DocumentStorageService.StoredDocument stored = service.storeBytes(
                "member/MBR-1", "DOC-1", 1, "piece.pdf", "contenu".getBytes());

        assertThat(service.read("FILESYSTEM", stored.storagePath())).asString().isEqualTo("contenu");
    }

    // -----------------------------------------------------------------------------------------

    /** L extension telle que le service la retient · lue sur le nom du fichier ecrit. */
    private String extensionOf(String filename) throws Exception {
        Path root = Files.createTempDirectory("doc-ext");
        try {
            DocumentStorageService.StoredDocument stored = storage(root).storeBytes(
                    "member/MBR-1", "DOC-1", 1, filename, "x".getBytes());
            String name = stored.storedFileName();
            int dot = name.indexOf('.');
            return dot < 0 ? "" : name.substring(dot + 1);
        } finally {
            try (var walk = Files.walk(root)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                        // Un repertoire temporaire qui resiste ne fait pas echouer un test.
                    }
                });
            }
        }
    }
}
