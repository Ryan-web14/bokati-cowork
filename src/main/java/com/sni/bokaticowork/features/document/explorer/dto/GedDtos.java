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
            String downloadUrl,
            boolean restricted,
            GedLock lock,
            boolean held,
            Instant destructionDueAt
    ) {}

    /**
     * Le verrou d'un fichier, pose pour le modifier sans etre derange. Tant qu'il court, seul
     * {@code lockedBy} remplace, renomme, deplace ou supprime le fichier. {@code mine} : c'est vous.
     */
    public record GedLock(String lockedBy, Instant lockedAt, Instant expiresAt, boolean mine) {}

    /** Ce que la personne connectee peut faire dans le dossier ouvert (la racine : tout). */
    public record GedAccessSummary(com.sni.bokaticowork.features.document.explorer.access.GedAccessLevel level,
                                   boolean canDownload, boolean canEdit, boolean canDelete, boolean canManage) {}

    /**
     * Un droit sur un dossier restreint. {@code granteeType} : {@code USER} ({@code grantee} est
     * l'e-mail) ou {@code ROLE} ({@code grantee} est le nom du role, {@code granteeLabel} son libelle).
     */
    public record GedGrant(String granteeType, String grantee, String granteeLabel,
                           com.sni.bokaticowork.features.document.explorer.access.GedAccessLevel level,
                           Instant grantedAt, String grantedBy) {}

    /** Donner un droit : un utilisateur par son e-mail, un role par son nom. */
    public record GedGrantRequest(@NotBlank String granteeType, @NotBlank String grantee,
                                  @jakarta.validation.constraints.NotNull com.sni.bokaticowork.features.document.explorer.access.GedAccessLevel level) {}

    public record GedRestrictRequest(List<@jakarta.validation.Valid GedGrantRequest> grants) {}

    /**
     * L'acces d'un dossier. {@code inheritedFrom} : les dossiers restreints au-dessus, dont les droits
     * s'appliquent aussi. {@code systemFolder} : dossier du plan, qui ne se restreint pas.
     */
    public record GedFolderAccess(String folderCode, boolean restricted, Instant restrictedAt, String restrictedBy,
                                  boolean systemFolder, List<GedFolderRef> inheritedFrom, List<GedGrant> grants,
                                  com.sni.bokaticowork.features.document.explorer.access.GedAccessLevel myLevel,
                                  boolean canManage) {}

    /** Le dossier ouvert ; vide a la racine. */
    public record GedFolderRef(String code, String name, String parentCode) {}

    public record GedPage(int page, int size, long totalElements, int totalPages, long folderCount, long fileCount) {}

    /** {@code access} : ce que la personne peut faire dans ce dossier, pour griser les actions. */
    public record GedListing(GedFolderRef folder, List<BreadcrumbItem> breadcrumb, List<GedItem> items, GedPage page,
                             GedAccessSummary access) {}

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

    // Texte et recherche (lot 3) ----------------------------------------------------------------

    /**
     * Le texte lu dans un fichier.
     *
     * <p>{@code status} : {@code PENDING} (pas encore lu), {@code DONE}, {@code EMPTY} (rien a lire,
     * page blanche), {@code UNSUPPORTED} (format non lu), {@code FAILED} (voir {@code error}).
     * {@code method} : {@code TEXT_LAYER} (PDF numerique), {@code OCR} (scan, image), {@code MIXED},
     * {@code PLAIN} (texte, CSV), {@code OFFICE}. {@code upToDate} est faux quand une nouvelle
     * version attend sa lecture.</p>
     */
    public record GedFileText(String code, String status, String method, Integer pageCount, Integer ocrPages,
                              int length, boolean truncated, Instant extractedAt, String error,
                              boolean upToDate, String content) {}

    /**
     * Un resultat de recherche. {@code snippet} : un extrait du contenu, les mots trouves entre
     * {@code ⟦} et {@code ⟧} (a afficher en texte, jamais en HTML) ; vide si seul le nom correspond.
     * {@code location} : le chemin du dossier, « Membres / Awa Ndoye · MBR-12 / KYC ».
     */
    public record GedSearchHit(GedItem item, String snippet, double rank, String location) {}

    public record GedSearchResult(String query, String folderCode, List<GedSearchHit> hits,
                                  int page, int size, long totalElements, int totalPages) {}

    /** L'avancement de la lecture des fichiers : en attente, et par etat. */
    public record GedTextStatus(long pending, java.util.Map<String, Long> byStatus) {}

    public record GedTextRunResult(int processed, int done, int empty, int unsupported, int failed, long remaining) {}

    // Conservation, gel juridique, quotas (lot 5) ----------------------------------------------------

    /** Poser un gel : {@code targetType} FOLDER (le dossier et tout son contenu) ou DOCUMENT. */
    public record GedHoldRequest(@NotBlank String targetType, @NotBlank String targetCode, @NotBlank String reason,
                                 String reference) {}

    public record GedReasonRequest(@NotBlank String reason) {}

    /** Un gel juridique ; {@code active} tant qu'il n'est pas leve. */
    public record GedHold(String code, String targetType, String targetCode, String targetName, String reason,
                          String reference, String placedBy, Instant placedAt, String releasedBy, Instant releasedAt,
                          String releaseReason, boolean active) {}

    /**
     * Le resultat d'une destruction : lots, dossiers et fichiers detruits, fichiers effaces du
     * stockage, fichiers epargnes car geles, et echecs (le fichier reste entier).
     */
    public record GedPurgeResult(int batches, int folders, int documents, int filesErased, int heldSkipped, int failed) {}

    /** Un fichier marque « a detruire » par une regle de conservation. */
    public record GedDestructionCandidate(GedItem item, String policyCode, Instant dueAt, String location) {}

    public record GedDocumentsDecision(@jakarta.validation.constraints.NotEmpty List<String> documentCodes,
                                       @NotBlank String reason) {}

    /**
     * L'espace d'un dossier (sous-dossiers et corbeille compris, toutes versions) ou de la GED.
     * {@code remainingBytes} : ce qu'il reste sous le quota le plus serre du chemin,
     * {@code limitingFolderCode} : le dossier qui porte ce quota ; vides sans quota.
     */
    public record GedUsage(String folderCode, String folderName, long usedBytes, long files, Long quotaBytes,
                           Long remainingBytes, String limitingFolderCode, long totalUsedBytes, Long totalQuotaBytes) {}

    /** Fixer le quota d'un dossier en octets ; vide ou 0 : le retirer. */
    public record GedQuotaRequest(Long quotaBytes) {}

    /** Un lot de la corbeille : ce qui a ete supprime ensemble, et se restaure ensemble. */
    public record GedTrashBatch(String batch, Instant trashedAt, Long trashedBy, long folders, long documents, List<GedItem> items) {}
}
