package com.sni.bokaticowork.features.document.explorer.service;

import org.springframework.beans.factory.annotation.Autowired;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentVersion;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentVersionRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentStorageService;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedFileText;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedSearchHit;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedSearchResult;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedTextRunResult;
import com.sni.bokaticowork.features.document.explorer.dto.GedDtos.GedTextStatus;
import com.sni.bokaticowork.features.document.explorer.repository.GedTextQueries;
import com.sni.bokaticowork.features.document.explorer.text.GedOcrEngine;
import com.sni.bokaticowork.features.document.explorer.text.GedSnippet;
import com.sni.bokaticowork.features.document.explorer.text.GedTextExtractor;
import com.sni.bokaticowork.features.document.explorer.text.GedTextExtractor.Extraction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le texte des fichiers de la GED et la recherche dans leur contenu.
 *
 * <p>La lecture d'un fichier (stockage, OCR) se fait hors transaction : l'OCR d'un scan de vingt
 * pages prend une minute, une connexion a la base ne reste pas ouverte pendant ce temps.</p>
 */
@Slf4j
@Service
public class GedTextService {

    private static final int SNIPPET_LENGTH = 240;
    private static final int MAX_PAGE_SIZE = 100;

    private final GedTextQueries queries;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentFolderRepository folderRepository;
    private final DocumentStorageService storageService;
    private final GedExplorerService explorerService;
    private final TransactionTemplate transactionTemplate;
    private final GedOcrEngine ocrEngine;
    private final boolean ocrEnabled;
    private final int maxOcrPages;
    private final int maxChars;
    private final com.sni.bokaticowork.features.document.explorer.access.GedAccessService access;

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    private static final String OCR_ENABLED_KEY = "app.ged.text.ocr-enabled";
    private static final String MAX_OCR_PAGES_KEY = "app.ged.text.max-ocr-pages";
    private static final String MAX_CHARS_KEY = "app.ged.text.max-chars";

    /** Extracteur construit a chaque lecture, pour suivre les reglages en vigueur. */
    private GedTextExtractor extractor() {
        boolean enabled = platformSettings == null ? ocrEnabled : platformSettings.bool(OCR_ENABLED_KEY, ocrEnabled);
        int pages = platformSettings == null ? maxOcrPages : platformSettings.integer(MAX_OCR_PAGES_KEY, maxOcrPages);
        int chars = platformSettings == null ? maxChars : platformSettings.integer(MAX_CHARS_KEY, maxChars);
        GedOcrEngine engine = enabled ? ocrEngine : page -> {
            throw new GedOcrEngine.OcrUnavailableException("OCR désactivé (app.ged.text.ocr-enabled)", null);
        };
        return new GedTextExtractor(engine, pages, chars);
    }

    public GedTextService(GedTextQueries queries,
                          DocumentRepository documentRepository,
                          DocumentVersionRepository versionRepository,
                          DocumentFolderRepository folderRepository,
                          DocumentStorageService storageService,
                          GedExplorerService explorerService,
                          TransactionTemplate transactionTemplate,
                          GedOcrEngine ocrEngine,
                          com.sni.bokaticowork.features.document.explorer.access.GedAccessService access,
                          @Value("${app.ged.text.ocr-enabled:true}") boolean ocrEnabled,
                          @Value("${app.ged.text.max-ocr-pages:30}") int maxOcrPages,
                          @Value("${app.ged.text.max-chars:300000}") int maxChars) {
        this.queries = queries;
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.folderRepository = folderRepository;
        this.storageService = storageService;
        this.explorerService = explorerService;
        this.transactionTemplate = transactionTemplate;
        this.access = access;
        this.ocrEngine = ocrEngine;
        this.ocrEnabled = ocrEnabled;
        this.maxOcrPages = maxOcrPages;
        this.maxChars = maxChars;
    }

    // Lecture des fichiers ----------------------------------------------------------------------

    private record Source(long id, Integer version, String provider, String path, String mimeType, String fileName) {}

    /** Lit le texte de la version courante d'un document et l'enregistre ; rend l'etat obtenu. */
    public String index(long documentId) {
        Source source = transactionTemplate.execute(status -> source(documentId));
        if (source == null) {
            return null;
        }
        Extraction extraction;
        try {
            byte[] content = storageService.read(source.provider(), source.path());
            extraction = extractor().extract(content, source.mimeType(), source.fileName());
        } catch (RuntimeException ex) {
            log.warn("GED · fichier {} illisible dans le stockage : {}", documentId, ex.getMessage());
            extraction = new Extraction(GedTextExtractor.FAILED, null, null, 0, null, false,
                    abbreviate("Fichier illisible dans le stockage : " + ex.getMessage()));
        }
        Extraction result = extraction;
        transactionTemplate.executeWithoutResult(status -> queries.save(source.id(), source.version(), result.status(),
                result.method(), result.pageCount(), result.ocrPages(), result.content(), result.truncated(), result.error()));
        return result.status();
    }

    private Source source(long documentId) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return null;
        }
        DocumentVersion version = versionRepository.findByDocumentAndCurrentTrue(document).orElse(null);
        String provider = version == null ? "FILESYSTEM" : version.getStorageProvider();
        String path = version == null ? document.getFileUrl() : version.getStoragePath();
        String mime = version != null && StringUtils.hasText(version.getMimeTypeDetected())
                ? version.getMimeTypeDetected() : document.getMimeType();
        return new Source(document.getId(), document.getCurrentVersionNumber(), provider, path, mime, document.getFileName());
    }

    /** Lit les fichiers en attente, au plus {@code limit} ; chacun dans son coin, un echec n'arrete pas les autres. */
    public GedTextRunResult runPending(int limit) {
        int max = Math.max(1, Math.min(limit, 500));
        List<Long> ids = transactionTemplate.execute(status -> queries.pendingDocumentIds(max));
        int done = 0, empty = 0, unsupported = 0, failed = 0;
        for (Long id : ids == null ? List.<Long>of() : ids) {
            String status;
            try {
                status = index(id);
            } catch (RuntimeException ex) {
                log.warn("GED · lecture du fichier {} en échec : {}", id, ex.getMessage());
                status = GedTextExtractor.FAILED;
            }
            if (status == null) continue;
            switch (status) {
                case GedTextExtractor.DONE -> done++;
                case GedTextExtractor.EMPTY -> empty++;
                case GedTextExtractor.UNSUPPORTED -> unsupported++;
                default -> failed++;
            }
        }
        long remaining = transactionTemplate.execute(status -> queries.countPending());
        return new GedTextRunResult(done + empty + unsupported + failed, done, empty, unsupported, failed, remaining);
    }

    @Transactional(readOnly = true)
    public GedTextStatus status() {
        return new GedTextStatus(queries.countPending(), queries.countByStatus());
    }

    @Transactional(readOnly = true)
    public GedFileText fileText(String code) {
        Document document = document(code);
        access.requireDocument(document, com.sni.bokaticowork.features.document.explorer.access.GedAccessLevel.DOWNLOAD);
        return queries.textOf(document.getId())
                .map(row -> {
                    Integer version = row[9] == null ? null : ((Number) row[9]).intValue();
                    return new GedFileText(document.getCode(), (String) row[0], (String) row[1],
                            integer(row[2]), integer(row[3]), row[5] == null ? 0 : ((Number) row[5]).intValue(),
                            Boolean.TRUE.equals(row[6]), instant(row[8]), (String) row[7],
                            java.util.Objects.equals(version, document.getCurrentVersionNumber()), (String) row[4]);
                })
                .orElseGet(() -> new GedFileText(document.getCode(), "PENDING", null, null, null, 0, false, null, null, false, null));
    }

    /** Relit un fichier tout de suite, sans attendre le travailleur (apres l'installation de l'OCR par exemple). */
    public GedFileText refresh(String code) {
        Long id = transactionTemplate.execute(status -> {
            Document document = document(code);
            access.requireDocument(document, com.sni.bokaticowork.features.document.explorer.access.GedAccessLevel.EDIT);
            return document.getId();
        });
        index(id);
        return transactionTemplate.execute(status -> fileText(code));
    }

    // Recherche ---------------------------------------------------------------------------------

    /**
     * Cherche dans le contenu et le nom des fichiers, dans toute la GED ou sous un dossier.
     *
     * <p>Sans accents ni majuscules, avec les variantes du francais (« facture » trouve
     * « factures »). Syntaxe : {@code contrat bail}, {@code "avenant n°2"}, {@code -brouillon},
     * {@code devis or facture}.</p>
     */
    @Transactional(readOnly = true)
    public GedSearchResult search(String text, String folderCode, int page, int size) {
        String query = text == null ? "" : text.trim();
        if (query.length() < 2) {
            throw new BadRequestException("Saisissez au moins deux caractères.");
        }
        DocumentFolder folder = StringUtils.hasText(folderCode)
                ? folderRepository.findByCode(folderCode.trim()).orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable : " + folderCode))
                : null;
        if (folder != null) {
            access.require(folder, com.sni.bokaticowork.features.document.explorer.access.GedAccessLevel.READ);
        }
        java.util.Set<Long> hidden = access.hiddenFolderIds();
        String folderPath = folder == null ? null : folder.getPath();
        int pageSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int pageNumber = Math.max(0, page);

        long total = queries.countSearch(query, folderPath, hidden);
        List<Object[]> rows = total == 0 ? List.of() : queries.search(query, folderPath, hidden, pageNumber * pageSize, pageSize);
        List<Long> ids = rows.stream().map(row -> ((Number) row[0]).longValue()).toList();
        Map<Long, Document> documents = documentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Document::getId, Function.identity()));
        Map<Long, String> contents = queries.contents(ids);
        Map<Long, String> folderNames = folderNames(documents.values());

        List<GedSearchHit> hits = new ArrayList<>();
        for (Object[] row : rows) {
            Document document = documents.get(((Number) row[0]).longValue());
            if (document == null) continue;
            hits.add(new GedSearchHit(explorerService.fileItem(document),
                    GedSnippet.build(contents.get(document.getId()), query, SNIPPET_LENGTH),
                    ((Number) row[1]).doubleValue(),
                    location(document.getFolder(), folderNames)));
        }
        int totalPages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        return new GedSearchResult(query, folder == null ? null : folder.getCode(), hits, pageNumber, pageSize, total, totalPages);
    }

    /** Le nom de chaque dossier traverse par les chemins des documents trouves, en une requete. */
    private Map<Long, String> folderNames(java.util.Collection<Document> documents) {
        Set<Long> ids = new HashSet<>();
        for (Document document : documents) {
            if (document.getFolder() != null) ids.addAll(pathIds(document.getFolder().getPath()));
        }
        Map<Long, String> names = new HashMap<>();
        folderRepository.findAllById(ids).forEach(folder -> names.put(folder.getId(), folder.getName()));
        return names;
    }

    private static String location(DocumentFolder folder, Map<Long, String> names) {
        if (folder == null) return null;
        return pathIds(folder.getPath()).stream().map(names::get).filter(java.util.Objects::nonNull)
                .collect(Collectors.joining(" / "));
    }

    /** « /100/105/112/ » vers [100, 105, 112]. */
    static List<Long> pathIds(String path) {
        List<Long> ids = new ArrayList<>();
        if (path == null) return ids;
        for (String part : path.split("/")) {
            if (!part.isBlank()) {
                try {
                    ids.add(Long.parseLong(part));
                } catch (NumberFormatException ignored) {
                    // chemin ancien ou mal forme : la piece est sautee
                }
            }
        }
        return ids;
    }

    // Aides -------------------------------------------------------------------------------------

    private Document document(String code) {
        return documentRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable : " + code));
    }

    private static Integer integer(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static Instant instant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant instant) return instant;
        if (value instanceof java.time.OffsetDateTime offset) return offset.toInstant();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
        return null;
    }

    private static String abbreviate(String message) {
        if (message == null) return null;
        return message.length() <= 500 ? message : message.substring(0, 497) + "...";
    }
}
