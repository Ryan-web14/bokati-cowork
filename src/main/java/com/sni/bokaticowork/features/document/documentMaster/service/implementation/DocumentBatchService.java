package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.BatchUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.CreateFolderRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTagAssignRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentBulkActionResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentBulkItemResult;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.FolderDetailResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.ZipImportResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentBatchService {

    private static final int MAX_BATCH_FILES = 20;
    private static final int MAX_ZIP_FILES = 50;
    private static final long MAX_ZIP_SIZE = 100L * 1024 * 1024; // 100 MB

    private final DocumentService documentService;
    private final DocumentTagService tagService;
    private final DocumentFolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final DocumentFolderService folderService;

    public DocumentBulkActionResponse uploadBatch(List<MultipartFile> files,
                                                   BatchUploadMetadataRequest metadata) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one file is required");
        }
        if (files.size() > MAX_BATCH_FILES) {
            throw new BadRequestException("Maximum " + MAX_BATCH_FILES + " files per batch");
        }

        List<DocumentBulkItemResult> results = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                DocumentResponse response = uploadSingleFile(file, metadata);
                results.add(DocumentBulkItemResult.builder()
                        .code(response.getCode())
                        .status("UPLOADED")
                        .reason(file.getOriginalFilename())
                        .build());
            } catch (Exception ex) {
                results.add(DocumentBulkItemResult.builder()
                        .code(null)
                        .status("FAILED")
                        .reason(file.getOriginalFilename() + ": " + ex.getMessage())
                        .build());
            }
        }

        int succeeded = (int) results.stream().filter(r -> "UPLOADED".equals(r.getStatus())).count();
        return DocumentBulkActionResponse.builder()
                .total(results.size())
                .succeeded(succeeded)
                .failed(results.size() - succeeded)
                .results(results)
                .build();
    }

    public ZipImportResponse importZip(MultipartFile zipFile,
                                        BatchUploadMetadataRequest metadata) {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new BadRequestException("ZIP file is required");
        }
        if (zipFile.getSize() > MAX_ZIP_SIZE) {
            throw new BadRequestException("ZIP file exceeds maximum size of 100 MB");
        }

        List<ZipImportResponse.ZipImportItem> items = new ArrayList<>();
        Map<String, String> createdFolders = new HashMap<>();
        int fileCount = 0;

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    String dirPath = normalizePath(entry.getName());
                    ensureFolderForPath(dirPath, metadata, createdFolders);
                    continue;
                }

                fileCount++;
                if (fileCount > MAX_ZIP_FILES) {
                    items.add(ZipImportResponse.ZipImportItem.builder()
                            .originalPath(entry.getName())
                            .success(false)
                            .error("Maximum " + MAX_ZIP_FILES + " files per ZIP exceeded")
                            .build());
                    break;
                }

                try {
                    String entryPath = entry.getName();
                    String fileName = extractFileName(entryPath);
                    String dirPath = extractDirPath(entryPath);

                    String folderCode = null;
                    if (dirPath != null && !dirPath.isEmpty()) {
                        folderCode = ensureFolderForPath(dirPath, metadata, createdFolders);
                    } else if (metadata.getFolderCode() != null) {
                        folderCode = metadata.getFolderCode();
                    }

                    byte[] content = zis.readAllBytes();
                    String mimeType = URLConnection.guessContentTypeFromName(fileName);
                    if (mimeType == null) mimeType = "application/octet-stream";

                    MultipartFile mockFile = new InMemoryMultipartFile(
                            "file", fileName, mimeType, content);

                    BatchUploadMetadataRequest entryMeta = new BatchUploadMetadataRequest();
                    entryMeta.setOwnerType(metadata.getOwnerType());
                    entryMeta.setOwnerCode(metadata.getOwnerCode());
                    entryMeta.setDocumentTypeCode(metadata.getDocumentTypeCode());
                    entryMeta.setSpace(metadata.getSpace());
                    entryMeta.setFolderCode(folderCode);
                    entryMeta.setReferenceCode(metadata.getReferenceCode());
                    entryMeta.setTagCodes(metadata.getTagCodes());

                    DocumentResponse response = uploadSingleFile(mockFile, entryMeta);

                    items.add(ZipImportResponse.ZipImportItem.builder()
                            .originalPath(entryPath)
                            .documentCode(response.getCode())
                            .folderCode(folderCode)
                            .success(true)
                            .build());

                } catch (Exception ex) {
                    items.add(ZipImportResponse.ZipImportItem.builder()
                            .originalPath(entry.getName())
                            .success(false)
                            .error(ex.getMessage())
                            .build());
                } finally {
                    zis.closeEntry();
                }
            }
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read ZIP file: " + ex.getMessage());
        }

        int succeeded = (int) items.stream().filter(ZipImportResponse.ZipImportItem::isSuccess).count();
        return ZipImportResponse.builder()
                .totalFiles(items.size())
                .succeeded(succeeded)
                .failed(items.size() - succeeded)
                .foldersCreated(createdFolders.size())
                .items(items)
                .build();
    }

    private DocumentResponse uploadSingleFile(MultipartFile file, BatchUploadMetadataRequest metadata) {
        String fileName = file.getOriginalFilename();
        String title = fileName != null ? fileName.replaceAll("\\.[^.]+$", "").replace("_", " ") : "Untitled";

        DocumentUploadMetadataRequest uploadMeta = new DocumentUploadMetadataRequest();
        uploadMeta.setOwnerType(metadata.getOwnerType());
        uploadMeta.setOwnerCode(metadata.getOwnerCode());
        uploadMeta.setDocumentTypeCode(metadata.getDocumentTypeCode());
        uploadMeta.setTitle(title);

        DocumentResponse response = documentService.upload(uploadMeta, file);

        Document document = documentRepository.findByCode(response.getCode()).orElse(null);
        if (document != null) {
            if (metadata.getSpace() != null) {
                document.setSpace(metadata.getSpace());
            }
            if (metadata.getReferenceCode() != null) {
                document.setSpaceReferenceCode(metadata.getReferenceCode());
            }
            if (metadata.getFolderCode() != null) {
                DocumentFolder folder = folderRepository.findByCode(metadata.getFolderCode()).orElse(null);
                if (folder != null) document.setFolder(folder);
            }
            documentRepository.save(document);
        }

        if (metadata.getTagCodes() != null && !metadata.getTagCodes().isEmpty()) {
            DocumentTagAssignRequest tagReq = new DocumentTagAssignRequest();
            tagReq.setTagCodes(metadata.getTagCodes());
            tagService.assignTags(response.getCode(), tagReq);
        }

        return documentService.getByCode(response.getCode());
    }

    private String ensureFolderForPath(String dirPath, BatchUploadMetadataRequest metadata,
                                        Map<String, String> createdFolders) {
        if (createdFolders.containsKey(dirPath)) {
            return createdFolders.get(dirPath);
        }

        String[] parts = dirPath.split("/");
        String parentCode = metadata.getFolderCode();
        StringBuilder currentPath = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) continue;
            currentPath.append(part).append("/");
            String key = currentPath.toString();

            if (createdFolders.containsKey(key)) {
                parentCode = createdFolders.get(key);
                continue;
            }

            CreateFolderRequest req = new CreateFolderRequest();
            req.setName(part);
            req.setParentCode(parentCode);
            req.setSpace(metadata.getSpace() != null ? metadata.getSpace() : DocumentSpace.GENERIC);

            try {
                FolderDetailResponse folder = folderService.create(req);
                parentCode = folder.getCode();
                createdFolders.put(key, folder.getCode());
            } catch (BadRequestException ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("already exists")) {
                    DocumentFolder existing = findExistingFolder(parentCode, part);
                    if (existing != null) {
                        parentCode = existing.getCode();
                        createdFolders.put(key, existing.getCode());
                    }
                } else {
                    throw ex;
                }
            }
        }

        return parentCode;
    }

    private DocumentFolder findExistingFolder(String parentCode, String name) {
        DocumentFolder parent = parentCode != null
                ? folderRepository.findByCode(parentCode).orElse(null) : null;
        return folderRepository.findAllByParentAndDeletedFalseOrderBySortOrderAscNameAsc(parent)
                .stream()
                .filter(f -> f.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private String normalizePath(String path) {
        return path.replace("\\", "/").replaceAll("/+$", "");
    }

    private String extractFileName(String path) {
        String normalized = path.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    private String extractDirPath(String path) {
        String normalized = path.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash > 0 ? normalized.substring(0, lastSlash) : null;
    }

    private record InMemoryMultipartFile(String name, String originalFilename,
                                          String contentType, byte[] bytes) implements MultipartFile {
        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        @Override public void transferTo(File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), bytes);
        }
    }
}
