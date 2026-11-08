package com.sni.bokaticowork.features.document.explorer.dto;

import com.sni.bokaticowork.features.document.documentMaster.dto.response.BreadcrumbItem;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

/**
 * Les echanges de l'explorateur de la GED.
 *
 * <p>Un dossier et un fichier sont deux sortes d'elements d'une meme liste, distingues par
 * {@code kind} : l'ecran les affiche dans un seul tableau, comme un explorateur de fichiers.</p>
 */
public final class GedDtos {

    private GedDtos() {
    }

    public static final String FOLDER = "FOLDER";
    public static final String FILE = "FILE";

    /**
     * Un element du contenu d'un dossier.
     *
     * <p>Pour un dossier, seuls les champs communs et {@code childrenCount}, {@code documentsCount},
     * {@code color}, {@code icon} sont remplis. Pour un fichier, les champs de fichier.
     * {@code protectedItem} vaut vrai pour un document produit par un autre module (piece KYC,
     * contrat, facture) : il se range et se renomme, mais ne part pas a la corbeille d'ici. Pour un
     * dossier, il signale un dossier du plan de classement : ni renomme, ni deplace, ni supprime.</p>
     */
    public record GedItem(
            String kind,
            String code,
            String name,
            String folderCode,
            Instant createdAt,
            Instant updatedAt,
            Long childrenCount,
            Long documentsCount,
            String color,
            String icon,
            String fileName,
            String extension,
            String mimeType,
            Long size,
            String status,
            String documentTypeCode,
            String documentTypeName,
            String ownerType,
            Integer version,
            boolean protectedItem,
            String previewUrl,
            String downloadUrl
    ) {}

    /** Le dossier ouvert ; vide a la racine. */
    public record GedFolderRef(String code, String name, String parentCode) {}

    public record GedPage(int page, int size, long totalElements, int totalPages, long folderCount, long fileCount) {}

    public record GedListing(GedFolderRef folder, List<BreadcrumbItem> breadcrumb, List<GedItem> items, GedPage page) {}

    public record GedUploadFailure(String fileName, String message) {}

    public record GedUploadResult(List<GedItem> uploaded, List<GedUploadFailure> failed) {}

    public record GedCreateFolderRequest(@NotBlank String name, String parentCode, String description, String color, String icon) {}

    public record GedRenameRequest(@NotBlank String name) {}

    /** Une selection de l'explorateur : des fichiers et des dossiers. */
    public record GedSelectionRequest(List<String> documentCodes, List<String> folderCodes) {}

    /** Deplacer une selection ; {@code targetFolderCode} vide : a la racine. */
    public record GedMoveRequest(List<String> documentCodes, List<String> folderCodes, String targetFolderCode) {}

    /** Copier des fichiers ; {@code targetFolderCode} vide : a la racine. */
    public record GedCopyRequest(List<String> documentCodes, String targetFolderCode) {}

    public record GedTrashResult(String batch, long folders, long documents) {}

    /** Une reprise du classement : rangés, sautés (sans place possible), restant sans dossier. */
    public record GedFilingResult(int filed, int skipped, long remaining) {}

    /** Un lot de la corbeille : ce qui a ete supprime ensemble, et se restaure ensemble. */
    public record GedTrashBatch(String batch, Instant trashedAt, Long trashedBy, long folders, long documents, List<GedItem> items) {}
}
