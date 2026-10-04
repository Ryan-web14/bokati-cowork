package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentStorageService {

    /** Une extension plus longue que huit caracteres n en est pas une. */
    private static final int MAX_EXTENSION_LENGTH = 8;
    /** De quoi contenir un code ou un identifiant, pas un chemin. */
    private static final int MAX_SEGMENT_LENGTH = 120;

    private final Path rootPath;
    private final String provider;
    private final String minioBucket;
    private final MinioClient minioClient;

    public DocumentStorageService(
            @Value("${app.document.storage.provider:filesystem}") String provider,
            @Value("${app.document.storage.root-path:storage/documents}") String rootPath,
            @Value("${app.document.storage.minio.endpoint:http://localhost:9000}") String minioEndpoint,
            @Value("${app.document.storage.minio.bucket:bokati-documents}") String minioBucket,
            @Value("${app.document.storage.minio.access-key:}") String minioAccessKey,
            @Value("${app.document.storage.minio.secret-key:}") String minioSecretKey,
            @Value("${app.document.storage.minio.region:}") String minioRegion
    ) {
        this.provider = normalizeProvider(provider);
        this.rootPath = Path.of(rootPath).toAbsolutePath().normalize();
        this.minioBucket = minioBucket;
        // Le client objet ne se construit que s'il sert · son constructeur refuse des identifiants
        // vides, et un stockage sur disque n'a aucune raison d'exiger des cles d'acces S3. Les
        // defauts de developpement qui remplissaient ces champs ont ete retires, ce qui rendait
        // l'absence de cles fatale au demarrage.
        boolean objectStorage = "MINIO".equals(this.provider) || "S3".equals(this.provider);
        if (objectStorage && StringUtils.hasText(minioAccessKey) && StringUtils.hasText(minioSecretKey)) {
            MinioClient.Builder builder = MinioClient.builder()
                    .endpoint(minioEndpoint)
                    .credentials(minioAccessKey, minioSecretKey);
            if (StringUtils.hasText(minioRegion)) {
                builder.region(minioRegion);
            }
            this.minioClient = builder.build();
        } else {
            if (objectStorage) {
                throw new IllegalStateException("Le stockage " + this.provider
                        + " demande app.document.storage.minio.access-key et .secret-key");
            }
            this.minioClient = null;
        }
    }

    public StoredDocument store(String ownerFolder, String documentCode, int versionNumber, MultipartFile file) {
        String originalFilename = file.getOriginalFilename() == null ? "document.bin" : file.getOriginalFilename();
        try {
            return store(ownerFolder, documentCode, versionNumber, originalFilename, file.getInputStream(), file.getSize());
        } catch (IOException ex) {
            throw new BadRequestException("Unable to store document file", ex);
        }
    }

    public StoredDocument storeBytes(String ownerFolder, String documentCode, int versionNumber, String originalFilename, byte[] bytes) {
        return store(ownerFolder, documentCode, versionNumber, originalFilename, new java.io.ByteArrayInputStream(bytes), bytes.length);
    }

    public byte[] read(String storageProvider, String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            throw new BadRequestException("Document storage path is missing");
        }
        String effectiveProvider = normalizeProvider(storageProvider);
        if ("MINIO".equals(effectiveProvider) || "S3".equals(effectiveProvider)) {
            return readMinio(storagePath);
        }
        try {
            // Le chemin vient de la base · un enregistrement ecrit avant ce correctif pourrait
            // pointer hors de la racine. On le confine aussi en lecture.
            return Files.readAllBytes(confine(Path.of(storagePath).toAbsolutePath().normalize()));
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read document file", ex);
        }
    }

    private StoredDocument store(String ownerFolder, String documentCode, int versionNumber, String originalFilename,
                                 InputStream inputStream, long contentLength) {
        String extension = extractExtension(originalFilename);
        String storedFileName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
        String objectKey = objectKey(ownerFolder, documentCode, versionNumber, storedFileName);
        if ("MINIO".equals(provider) || "S3".equals(provider)) {
            return storeMinio(objectKey, storedFileName, inputStream, contentLength);
        }
        return storeFilesystem(objectKey, storedFileName, inputStream);
    }

    private StoredDocument storeFilesystem(String objectKey, String storedFileName, InputStream inputStream) {
        try {
            Path target = confine(rootPath.resolve(objectKey).normalize());
            Path directory = target.getParent();
            Files.createDirectories(directory);
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredDocument("FILESYSTEM", target.toString(), storedFileName);
        } catch (IOException ex) {
            throw new BadRequestException("Unable to store document file", ex);
        }
    }

    private StoredDocument storeMinio(String objectKey, String storedFileName, InputStream inputStream, long contentLength) {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioBucket)
                    .object(objectKey)
                    .stream(inputStream, contentLength, -1)
                    .build());
            return new StoredDocument("MINIO", objectKey, storedFileName);
        } catch (Exception ex) {
            throw new BadRequestException("Unable to store document file in MinIO", ex);
        }
    }

    private byte[] readMinio(String objectKey) {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioBucket)
                .object(objectKey)
                .build())) {
            return stream.readAllBytes();
        } catch (Exception ex) {
            throw new BadRequestException("Unable to read document file from MinIO", ex);
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioBucket).build());
        if (!exists) {
            // Skip auto-create on managed S3 providers (e.g. Bucketeer) where the bucket
            // is pre-created and IAM credentials don't have s3:CreateBucket permission.
            // Only attempt creation when running against a self-hosted MinIO instance.
            if (!provider.equals("S3")) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioBucket).build());
            }
        }
    }

    /**
     * La cle de l objet · chaque segment assaini, aucun ne peut remonter.
     *
     * <p>Vaut pour le systeme de fichiers comme pour le stockage objet : une cle portant
     * {@code ..} ecrirait hors du prefixe prevu sur l un comme sur l autre.</p>
     */
    private String objectKey(String ownerFolder, String documentCode, int versionNumber, String storedFileName) {
        StringBuilder key = new StringBuilder();
        for (String part : ownerFolder.replace('\\', '/').split("/")) {
            String safe = safeSegment(part);
            if (!safe.isEmpty()) {
                key.append(safe).append('/');
            }
        }
        key.append(safeSegment(documentCode)).append('/')
                .append('v').append(versionNumber).append('/')
                .append(safeSegment(storedFileName));
        return key.toString();
    }

    /**
     * L extension du fichier depose · reduite a ce qui peut etre une extension.
     *
     * <p>Elle etait reprise telle quelle depuis le nom fourni par le client. Le nom du fichier,
     * lui, etait bien remplace par un UUID · mais l extension se collait derriere, et
     * {@code lastIndexOf('.')} sur un nom comme {@code photo.../../../quelque/part} rendait
     * « extension » tout ce qui suivait le dernier point, separateurs compris. Le chemin cible
     * etait normalise, mais jamais confine : le fichier atterrissait ou l appelant voulait.</p>
     *
     * <p>Seules des lettres et des chiffres, huit au plus. Tout le reste disparait · une extension
     * n a jamais eu besoin d autre chose, et ce qui n est pas reconnu vaut mieux perdu que
     * interprete.</p>
     */
    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }
        String raw = filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
        StringBuilder safe = new StringBuilder(MAX_EXTENSION_LENGTH);
        for (int i = 0; i < raw.length() && safe.length() < MAX_EXTENSION_LENGTH; i++) {
            char c = raw.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                safe.append(c);
            } else {
                // Un separateur ou un point au milieu n appartient pas a une extension · ce qui
                // suit non plus.
                break;
            }
        }
        return safe.toString();
    }

    /**
     * Un segment de chemin · reduit a ce qui peut en etre un.
     *
     * <p>Le dossier du proprietaire et le code du document viennent de donnees, pas d un
     * formulaire · ils sont surs aujourd hui. Les assainir coute deux lignes et rend le calcul du
     * chemin independant de cette hypothese.</p>
     */
    private String safeSegment(String segment) {
        if (!StringUtils.hasText(segment)) {
            return "";
        }
        String cleaned = segment.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        // « . » et « .. » designent un repertoire, jamais un nom · ils ne traversent pas ici.
        while (cleaned.startsWith(".")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned.length() > MAX_SEGMENT_LENGTH ? cleaned.substring(0, MAX_SEGMENT_LENGTH) : cleaned;
    }

    /**
     * Refuse tout chemin qui sort de la racine de stockage.
     *
     * <p>{@code normalize()} resout les {@code ..} · il ne dit pas si le resultat est encore chez
     * nous. C est cette verification qui manquait, et c est la seule qui compte : quelle que soit
     * la facon dont un {@code ..} arrive dans le calcul, il ne sort pas d ici.</p>
     */
    private Path confine(Path candidate) {
        if (!candidate.startsWith(rootPath)) {
            throw new BadRequestException("Chemin de stockage refusé · il sort du répertoire des documents");
        }
        return candidate;
    }

    private String normalizeProvider(String value) {
        if (!StringUtils.hasText(value)) {
            return "FILESYSTEM";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("MINIO") || normalized.equals("S3") ? normalized : "FILESYSTEM";
    }

    public record StoredDocument(String provider, String storagePath, String storedFileName) {
    }
}
