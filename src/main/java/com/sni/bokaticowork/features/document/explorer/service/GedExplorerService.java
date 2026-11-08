package com.sni.bokaticowork.features.document.explorer.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.CreateFolderRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.MoveFolderRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.UpdateFolderRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.BreadcrumbItem;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFileResult;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFolderTreeNode;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.mapper.interfaces.DocumentMapper;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentFolderService;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedCopyRequest;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedCreateFolderRequest;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedFolderRef;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedItem;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedListing;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedMoveRequest;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedPage;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedSelectionRequest;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedTrashBatch;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedTrashResult;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedUploadFailure;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedUploadResult;
import com.sni.bokaticowork.features.document.explorer.repository.GedExplorerQueries;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * L'explorateur de la GED : parcourir, deposer, ranger, renommer, copier, mettre a la corbeille.
 *
 * <p>Il s'appuie sur les dossiers et documents existants. Un document « de la GED » est un
 * document interne ou de type {@code GENERAL} : il se gere entierement d'ici. Un document produit
 * par un autre module (piece KYC, contrat, facture) se range et se renomme, mais sa suppression
 * reste l'affaire de son module : la corbeille le cacherait aussi la ou il est attendu.</p>
 *
 * <p>Le depot et la copie traitent chaque fichier dans sa propre transaction : un fichier refuse
 * n'annule pas les autres, il est rendu dans la liste des echecs.</p>
 */
@Service
@RequiredArgsConstructor
public class GedExplorerService {

    public static final String GENERAL_TYPE = "GENERAL";
    private static final int MAX_PAGE_SIZE = 200;
    private static final int TRASH_BATCHES = 100;

    private final GedExplorerQueries queries;
    private final DocumentFolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final DocumentFolderService folderService;
    private final DocumentService documentService;
    private final DocumentMapper documentMapper;
    private final TransactionTemplate transactionTemplate;

    // Lecture -----------------------------------------------------------------------------------

    /**
     * Le contenu d'un dossier : ses sous-dossiers d'abord, puis ses fichiers, dans une seule
     * pagination. {@code sort} : {@code name}, {@code updatedAt}, {@code size} ou {@code type} ;
     * les dossiers suivent le nom, ou la date si l'on trie par date.
     */
    @Transactional(readOnly = true)
    public GedListing list(String folderCode, String search, String sort, String direction, int page, int size) {
        DocumentFolder folder = StringUtils.hasText(folderCode) ? folder(folderCode) : null;
        String needle = StringUtils.hasText(search) ? search.trim() : null;
        String sortKey = sort == null ? "name" : sort;
        boolean descending = "desc".equalsIgnoreCase(direction);
        int pageSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int pageNumber = Math.max(0, page);
        int offset = pageNumber * pageSize;

        List<DocumentFolder> folders = queries.folders(folder, needle, "updatedAt".equals(sortKey), descending);
        long fileCount = queries.countFiles(folder, needle);
        long total = folders.size() + fileCount;

        List<DocumentFolder> folderSlice = offset < folders.size()
                ? folders.subList(offset, Math.min(folders.size(), offset + pageSize))
                : List.of();
        int remaining = pageSize - folderSlice.size();
        int fileOffset = Math.max(0, offset - folders.size());
        List<Document> files = remaining > 0
                ? queries.files(folder, needle, sortKey, descending, fileOffset, remaining)
                : List.of();

        List<GedItem> items = new ArrayList<>(folderItems(folderSlice));
        files.forEach(document -> items.add(fileItem(document)));

        GedFolderRef ref = folder == null ? null
                : new GedFolderRef(folder.getCode(), folder.getName(), folder.getParent() == null ? null : folder.getParent().getCode());
        List<BreadcrumbItem> breadcrumb = folder == null ? List.of() : folderService.getBreadcrumb(folder.getCode());
        int totalPages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        return new GedListing(ref, breadcrumb, items,
                new GedPage(pageNumber, pageSize, total, totalPages, folders.size(), fileCount));
    }

    /** Toute l'arborescence, pour le panneau de gauche. */
    @Transactional(readOnly = true)
    public List<DocumentFolderTreeNode> tree() {
        List<DocumentFolder> all = folderRepository.findAll();
        List<Long> ids = all.stream().map(DocumentFolder::getId).toList();
        Map<Long, Long> documents = queries.documentCounts(ids);
        Map<Long, List<DocumentFolder>> byParent = new HashMap<>();
        List<DocumentFolder> roots = new ArrayList<>();
        for (DocumentFolder folder : all) {
            if (folder.getParent() == null) {
                roots.add(folder);
            } else {
                byParent.computeIfAbsent(folder.getParent().getId(), key -> new ArrayList<>()).add(folder);
            }
        }
        return roots.stream().sorted(this::byName).map(root -> node(root, byParent, documents)).toList();
    }

    // Dossiers ----------------------------------------------------------------------------------

    public GedItem createFolder(GedCreateFolderRequest request) {
        CreateFolderRequest create = new CreateFolderRequest();
        create.setName(request.name());
        create.setParentCode(StringUtils.hasText(request.parentCode()) ? request.parentCode().trim() : null);
        create.setDescription(request.description());
        create.setColor(request.color());
        create.setIcon(request.icon());
        create.setSpace(DocumentSpace.GENERIC);
        String code = folderService.create(create).getCode();
        return transactionTemplate.execute(status -> folderItems(List.of(folder(code))).getFirst());
    }

    public GedItem renameFolder(String code, String name) {
        UpdateFolderRequest update = new UpdateFolderRequest();
        update.setName(name);
        folderService.update(code, update);
        return transactionTemplate.execute(status -> folderItems(List.of(folder(code))).getFirst());
    }

    // Fichiers ----------------------------------------------------------------------------------

    @Transactional
    public GedItem renameFile(String code, String name) {
        Document document = document(code);
        String title = name.trim();
        if (!title.equalsIgnoreCase(document.getTitle()) && queries.titleTaken(document.getFolder(), title)) {
            throw new ConflictException("Un fichier porte déjà ce nom dans ce dossier.");
        }
        document.setTitle(title);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        return fileItem(document);
    }

    /**
     * Depose des fichiers dans un dossier.
     *
     * <p>{@code relativePaths}, s'il est fourni, donne pour chaque fichier son chemin dans le
     * dossier depose ({@code Factures/2025/janvier.pdf}) : les sous-dossiers manquants sont crees,
     * l'arborescence est conservee. Sans type ni proprietaire, le fichier est un document general
     * interne. Un nom deja pris dans le dossier recoit un suffixe « (2) », comme dans un
     * explorateur.</p>
     */
    public GedUploadResult upload(List<MultipartFile> files, String folderCode, List<String> relativePaths,
                                  String documentTypeCode, DocumentOwnerType ownerType, String ownerCode) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Aucun fichier reçu.");
        }
        if (relativePaths != null && !relativePaths.isEmpty() && relativePaths.size() != files.size()) {
            throw new BadRequestException("relativePaths doit donner un chemin par fichier.");
        }
        String rootCode = StringUtils.hasText(folderCode) ? transactionTemplate.execute(status -> folder(folderCode).getCode()) : null;
        Map<String, String> createdFolders = new HashMap<>();
        List<GedItem> uploaded = new ArrayList<>();
        List<GedUploadFailure> failed = new ArrayList<>();

        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index);
            String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "fichier";
            try {
                String path = relativePaths == null || relativePaths.isEmpty() ? null : relativePaths.get(index);
                String targetCode = ensureFolders(rootCode, path, createdFolders);
                String fileName = baseName(path == null ? original : path);
                DocumentUploadMetadataRequest metadata = new DocumentUploadMetadataRequest();
                metadata.setOwnerType(ownerType == null ? DocumentOwnerType.INTERNAL : ownerType);
                metadata.setOwnerCode(ownerCode);
                metadata.setDocumentTypeCode(StringUtils.hasText(documentTypeCode) ? documentTypeCode : GENERAL_TYPE);
                metadata.setTitle(transactionTemplate.execute(status -> freeTitle(targetCode, fileName)));
                metadata.setFolderCode(targetCode);
                DocumentResponse created = documentService.upload(metadata, file);
                uploaded.add(transactionTemplate.execute(status -> fileItem(document(created.getCode()))));
            } catch (RuntimeException failure) {
                failed.add(new GedUploadFailure(original, failure.getMessage()));
            }
        }
        return new GedUploadResult(uploaded, failed);
    }

    /** Copie des fichiers ; chaque copie est un document general interne, independant de l'original. */
    public GedUploadResult copy(GedCopyRequest request) {
        List<String> codes = request.documentCodes() == null ? List.of() : request.documentCodes();
        if (codes.isEmpty()) {
            throw new BadRequestException("Aucun fichier à copier.");
        }
        String targetCode = StringUtils.hasText(request.targetFolderCode())
                ? transactionTemplate.execute(status -> folder(request.targetFolderCode()).getCode())
                : null;
        List<GedItem> copied = new ArrayList<>();
        List<GedUploadFailure> failed = new ArrayList<>();
        for (String code : codes) {
            try {
                DocumentFileResult file = documentService.getFileWithMeta(code);
                String title = transactionTemplate.execute(status -> document(code).getTitle());
                DocumentUploadMetadataRequest metadata = new DocumentUploadMetadataRequest();
                metadata.setOwnerType(DocumentOwnerType.INTERNAL);
                metadata.setDocumentTypeCode(GENERAL_TYPE);
                metadata.setTitle(transactionTemplate.execute(status -> freeTitle(targetCode, title)));
                metadata.setFolderCode(targetCode);
                DocumentResponse created = documentService.createGeneratedDocument(
                        metadata, file.fileName(), file.mimeType(), file.content());
                copied.add(transactionTemplate.execute(status -> fileItem(document(created.getCode()))));
            } catch (RuntimeException failure) {
                failed.add(new GedUploadFailure(code, failure.getMessage()));
            }
        }
        return new GedUploadResult(copied, failed);
    }

    /** Deplace une selection, tout ou rien. Un fichier dont le nom est pris recoit un suffixe. */
    @Transactional
    public void move(GedMoveRequest request) {
        DocumentFolder target = StringUtils.hasText(request.targetFolderCode()) ? folder(request.targetFolderCode()) : null;
        for (String code : orEmpty(request.folderCodes())) {
            DocumentFolder moved = folder(code);
            if (target != null && target.getId().equals(moved.getId())) {
                throw new BadRequestException("Un dossier ne peut pas être déplacé dans lui-même.");
            }
            queries.childNamed(target, moved.getName())
                    .filter(existing -> !existing.getId().equals(moved.getId()))
                    .ifPresent(existing -> {
                        throw new ConflictException("Le dossier de destination contient déjà un dossier « " + moved.getName() + " ».");
                    });
            MoveFolderRequest move = new MoveFolderRequest();
            move.setTargetParentCode(target == null ? null : target.getCode());
            folderService.move(code, move);
        }
        for (String code : orEmpty(request.documentCodes())) {
            Document document = document(code);
            boolean sameFolder = target == null ? document.getFolder() == null
                    : document.getFolder() != null && target.getId().equals(document.getFolder().getId());
            if (sameFolder) {
                continue;
            }
            document.setTitle(freeTitle(target, document.getTitle()));
            document.setFolder(target);
            document.setUpdatedAt(Instant.now());
            documentRepository.save(document);
        }
    }

    // Corbeille ---------------------------------------------------------------------------------

    /**
     * Met une selection a la corbeille, avec tout le contenu des dossiers choisis, dans un meme lot.
     *
     * <p>Refuse entierement si un document protege s'y trouve : supprimer une piece KYC, un
     * contrat ou une facture d'ici la cacherait aussi dans son module.</p>
     */
    @Transactional
    public GedTrashResult trash(GedSelectionRequest request) {
        Set<Long> folderIds = new LinkedHashSet<>();
        for (String code : orEmpty(request.folderCodes())) {
            DocumentFolder folder = folder(code);
            folderRepository.findAllByPathStartingWith(folder.getPath()).forEach(item -> folderIds.add(item.getId()));
        }
        List<Document> documents = new ArrayList<>(queries.filesIn(folderIds));
        for (String code : orEmpty(request.documentCodes())) {
            documents.add(document(code));
        }
        if (folderIds.isEmpty() && documents.isEmpty()) {
            throw new BadRequestException("Rien à mettre à la corbeille.");
        }
        List<String> protectedTitles = documents.stream().filter(document -> !gedOwned(document))
                .map(Document::getTitle).distinct().limit(5).toList();
        if (!protectedTitles.isEmpty()) {
            throw new ConflictException("La sélection contient des documents gérés par un autre module (KYC, contrat, facture), "
                    + "qui ne se suppriment pas depuis l'explorateur : " + String.join(", ", protectedTitles) + ".");
        }
        String batch = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Long actor = currentUserId();
        Set<Long> documentIds = new LinkedHashSet<>();
        documents.forEach(document -> documentIds.add(document.getId()));
        queries.markFilesTrashed(documentIds, batch, actor, now);
        queries.markFolderTrashed(folderIds, batch, actor, now);
        return new GedTrashResult(batch, folderIds.size(), documentIds.size());
    }

    @Transactional(readOnly = true)
    public List<GedTrashBatch> trashContent() {
        List<GedTrashBatch> batches = new ArrayList<>();
        for (Object[] row : queries.trashBatches(TRASH_BATCHES)) {
            String batch = (String) row[0];
            List<GedItem> items = new ArrayList<>();
            for (Object[] folder : queries.trashedTopFolders(batch)) {
                items.add(new GedItem(com.sni.bokaticowork.features.document.explorer.dto.GedDtos.FOLDER,
                        (String) folder[1], (String) folder[2], null, null, instant(folder[5]),
                        null, null, (String) folder[6], (String) folder[7],
                        null, null, null, null, null, null, null, null, null, false, null, null));
            }
            for (Object[] file : queries.trashedTopFiles(batch)) {
                items.add(new GedItem(com.sni.bokaticowork.features.document.explorer.dto.GedDtos.FILE,
                        (String) file[1], (String) file[2], null, null, instant(file[5]),
                        null, null, null, null, (String) file[6], extension((String) file[6]), (String) file[7],
                        file[8] == null ? null : ((Number) file[8]).longValue(),
                        null, null, null, null, null, false, null, null));
            }
            batches.add(new GedTrashBatch(batch, instant(row[1]), row[2] == null ? null : ((Number) row[2]).longValue(),
                    ((Number) row[3]).longValue(), ((Number) row[4]).longValue(), items));
        }
        return batches;
    }

    /**
     * Restaure un lot a son emplacement d'origine.
     *
     * <p>Si l'emplacement d'origine est lui-meme a la corbeille, il faut d'abord le restaurer. Un
     * dossier dont le nom a ete repris entre-temps revient avec le suffixe « (restauré) ».</p>
     */
    @Transactional
    public GedTrashResult restore(String batch) {
        List<Object[]> topFolders = queries.trashedTopFolders(batch);
        List<Object[]> topFiles = queries.trashedTopFiles(batch);
        if (topFolders.isEmpty() && topFiles.isEmpty()) {
            throw new ResourceNotFoundException("Rien à restaurer pour ce lot.");
        }
        for (Object[] folder : topFolders) {
            requireAliveParent(folder[3]);
        }
        for (Object[] file : topFiles) {
            requireAliveParent(file[3]);
        }
        for (Object[] folder : topFolders) {
            Long id = ((Number) folder[0]).longValue();
            Long parentId = folder[3] == null ? null : ((Number) folder[3]).longValue();
            String name = (String) folder[2];
            if (queries.folderNameTakenAmongAlive(parentId, name)) {
                queries.renameTrashedFolder(id, name + " (restauré)");
            }
        }
        long[] restored = queries.restore(batch);
        return new GedTrashResult(batch, restored[0], restored[1]);
    }

    // Aides -------------------------------------------------------------------------------------

    private void requireAliveParent(Object parentId) {
        if (parentId != null && !queries.folderAlive(((Number) parentId).longValue())) {
            throw new ConflictException("Le dossier d'origine est lui-même à la corbeille : restaurez-le d'abord.");
        }
    }

    /** Cree les sous-dossiers d'un chemin relatif sous le dossier cible ; rend le dossier du fichier. */
    private String ensureFolders(String rootCode, String relativePath, Map<String, String> created) {
        if (!StringUtils.hasText(relativePath)) {
            return rootCode;
        }
        String[] parts = relativePath.replace('\\', '/').split("/");
        String current = rootCode;
        StringBuilder key = new StringBuilder(rootCode == null ? "" : rootCode);
        for (int index = 0; index < parts.length - 1; index++) {
            String name = parts[index].trim();
            if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
                continue;
            }
            key.append('/').append(name.toLowerCase(Locale.ROOT));
            String known = created.get(key.toString());
            if (known != null) {
                current = known;
                continue;
            }
            String parentCode = current;
            String existing = transactionTemplate.execute(status -> queries
                    .childNamed(parentCode == null ? null : folder(parentCode), name)
                    .map(DocumentFolder::getCode).orElse(null));
            if (existing == null) {
                CreateFolderRequest create = new CreateFolderRequest();
                create.setName(name);
                create.setParentCode(parentCode);
                create.setSpace(DocumentSpace.GENERIC);
                existing = folderService.create(create).getCode();
            }
            created.put(key.toString(), existing);
            current = existing;
        }
        return current;
    }

    private String freeTitle(String folderCode, String title) {
        return freeTitle(folderCode == null ? null : folder(folderCode), title);
    }

    /** « rapport.pdf », puis « rapport (2).pdf », « rapport (3).pdf »… */
    private String freeTitle(DocumentFolder folder, String title) {
        String clean = StringUtils.hasText(title) ? title.trim() : "fichier";
        if (!queries.titleTaken(folder, clean)) {
            return clean;
        }
        int dot = clean.lastIndexOf('.');
        String stem = dot > 0 ? clean.substring(0, dot) : clean;
        String extension = dot > 0 ? clean.substring(dot) : "";
        for (int counter = 2; counter < 1000; counter++) {
            String candidate = stem + " (" + counter + ")" + extension;
            if (!queries.titleTaken(folder, candidate)) {
                return candidate;
            }
        }
        return stem + " (" + UUID.randomUUID().toString().substring(0, 8) + ")" + extension;
    }

    /** Un document cree dans la GED, et donc entierement gere d'ici. */
    static boolean gedOwned(Document document) {
        return document.getOwnerType() == DocumentOwnerType.INTERNAL
                || (document.getDocumentType() != null && GENERAL_TYPE.equals(document.getDocumentType().getCode()));
    }

    private List<GedItem> folderItems(List<DocumentFolder> folders) {
        List<Long> ids = folders.stream().map(DocumentFolder::getId).toList();
        Map<Long, Long> children = queries.childrenCounts(ids);
        Map<Long, Long> documents = queries.documentCounts(ids);
        return folders.stream().map(folder -> new GedItem(
                com.sni.bokaticowork.features.document.explorer.dto.GedDtos.FOLDER,
                folder.getCode(), folder.getName(),
                folder.getParent() == null ? null : folder.getParent().getCode(),
                folder.getCreatedAt(), folder.getUpdatedAt(),
                children.getOrDefault(folder.getId(), 0L), documents.getOrDefault(folder.getId(), 0L),
                folder.getColor(), folder.getIcon(),
                null, null, null, null, null, null, null, null, null, false, null, null)).toList();
    }

    private GedItem fileItem(Document document) {
        DocumentResponse response = documentMapper.toResponse(document);
        return new GedItem(
                com.sni.bokaticowork.features.document.explorer.dto.GedDtos.FILE,
                document.getCode(), document.getTitle(),
                document.getFolder() == null ? null : document.getFolder().getCode(),
                document.getUploadedAt(), document.getUpdatedAt(),
                null, null, null, null,
                document.getFileName(), extension(document.getFileName()), document.getMimeType(), document.getFileSize(),
                document.getStatus() == null ? null : document.getStatus().name(),
                document.getDocumentType() == null ? null : document.getDocumentType().getCode(),
                document.getDocumentType() == null ? null : document.getDocumentType().getName(),
                document.getOwnerType() == null ? null : document.getOwnerType().name(),
                document.getCurrentVersionNumber(),
                !gedOwned(document),
                response == null ? null : response.getPreviewUrl(),
                response == null ? null : response.getDownloadUrl());
    }

    private DocumentFolderTreeNode node(DocumentFolder folder, Map<Long, List<DocumentFolder>> byParent, Map<Long, Long> documents) {
        return DocumentFolderTreeNode.builder()
                .code(folder.getCode())
                .name(folder.getName())
                .icon(folder.getIcon())
                .color(folder.getColor())
                .depth(folder.getDepth())
                .documentsCount(documents.getOrDefault(folder.getId(), 0L))
                .children(byParent.getOrDefault(folder.getId(), List.of()).stream()
                        .sorted(this::byName)
                        .map(child -> node(child, byParent, documents))
                        .toList())
                .build();
    }

    private int byName(DocumentFolder left, DocumentFolder right) {
        return left.getName().compareToIgnoreCase(right.getName());
    }

    private DocumentFolder folder(String code) {
        return folderRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable : " + code));
    }

    private Document document(String code) {
        return documentRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable : " + code));
    }

    private static List<String> orEmpty(List<String> values) {
        return values == null ? List.of() : values.stream().filter(StringUtils::hasText).toList();
    }

    private static String baseName(String path) {
        String normalized = path.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private static String extension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return null;
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static Instant instant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant instant) return instant;
        if (value instanceof java.time.OffsetDateTime offset) return offset.toInstant();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
        if (value instanceof java.time.LocalDateTime local) return local.atZone(java.time.ZoneOffset.UTC).toInstant();
        return null;
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser().getId();
        }
        return null;
    }
}
