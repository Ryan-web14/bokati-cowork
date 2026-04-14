package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final Path rootPath;

    public DocumentStorageService(@Value("${app.document.storage.root-path:storage/documents}") String rootPath) {
        this.rootPath = Path.of(rootPath).toAbsolutePath().normalize();
    }

    public StoredDocument store(String ownerFolder, String documentCode, int versionNumber, MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename() == null ? "document.bin" : file.getOriginalFilename();
            String extension = extractExtension(originalFilename);
            String storedFileName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
            Path directory = rootPath.resolve(ownerFolder).resolve(documentCode).resolve("v" + versionNumber);
            Files.createDirectories(directory);
            Path target = directory.resolve(storedFileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredDocument("FILESYSTEM", target.toString(), storedFileName);
        } catch (IOException ex) {
            throw new BadRequestException("Unable to store document file", ex);
        }
    }

    public StoredDocument storeBytes(String ownerFolder, String documentCode, int versionNumber, String originalFilename, byte[] bytes) {
        try {
            String extension = extractExtension(originalFilename);
            String storedFileName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
            Path directory = rootPath.resolve(ownerFolder).resolve(documentCode).resolve("v" + versionNumber);
            Files.createDirectories(directory);
            Path target = directory.resolve(storedFileName);
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return new StoredDocument("FILESYSTEM", target.toString(), storedFileName);
        } catch (IOException ex) {
            throw new BadRequestException("Unable to store document file", ex);
        }
    }

    private String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot < 0 ? "" : filename.substring(lastDot + 1).toLowerCase();
    }

    public record StoredDocument(String provider, String storagePath, String storedFileName) {
    }
}
