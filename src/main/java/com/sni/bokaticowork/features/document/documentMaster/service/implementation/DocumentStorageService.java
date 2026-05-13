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

    private final Path rootPath;
    private final String provider;
    private final String minioBucket;
    private final MinioClient minioClient;

    public DocumentStorageService(
            @Value("${app.document.storage.provider:filesystem}") String provider,
            @Value("${app.document.storage.root-path:storage/documents}") String rootPath,
            @Value("${app.document.storage.minio.endpoint:http://localhost:9000}") String minioEndpoint,
            @Value("${app.document.storage.minio.bucket:bokati-documents}") String minioBucket,
            @Value("${app.document.storage.minio.access-key:minioadmin}") String minioAccessKey,
            @Value("${app.document.storage.minio.secret-key:minioadmin}") String minioSecretKey,
            @Value("${app.document.storage.minio.region:}") String minioRegion
    ) {
        this.provider = normalizeProvider(provider);
        this.rootPath = Path.of(rootPath).toAbsolutePath().normalize();
        this.minioBucket = minioBucket;
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey);
        if (StringUtils.hasText(minioRegion)) {
            builder.region(minioRegion);
        }
        this.minioClient = builder.build();
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
            return Files.readAllBytes(Path.of(storagePath));
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
            Path target = rootPath.resolve(objectKey).normalize();
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

    private String objectKey(String ownerFolder, String documentCode, int versionNumber, String storedFileName) {
        return ownerFolder.replace('\\', '/')
                + "/" + documentCode
                + "/v" + versionNumber
                + "/" + storedFileName;
    }

    private String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot < 0 ? "" : filename.substring(lastDot + 1).toLowerCase();
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
