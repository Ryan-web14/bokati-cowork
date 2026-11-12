package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentAntivirusStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class DocumentSecurityService {

    private static final List<String> DEFAULT_ALLOWED_MIME_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public SecurityInspection inspect(MultipartFile file, List<String> allowedMimeTypes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Document file is required");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read document file", ex);
        }

        return inspectBytes(file.getOriginalFilename(), file.getContentType(), bytes, allowedMimeTypes);
    }

    public SecurityInspection inspectBytes(String fileName, String declaredMimeType, byte[] bytes, List<String> allowedMimeTypes) {
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("Document file is required");
        }

        String detectedMime = detectMime(bytes, fileName);
        List<String> effectiveAllowed = allowedMimeTypes == null || allowedMimeTypes.isEmpty()
                ? DEFAULT_ALLOWED_MIME_TYPES
                : allowedMimeTypes;

        if (!effectiveAllowed.contains(detectedMime)) {
            throw new BadRequestException("Unsupported document MIME type: " + detectedMime);
        }

        DocumentAntivirusStatus antivirusStatus = scanForThreats(bytes);
        if (antivirusStatus != DocumentAntivirusStatus.CLEAN) {
            throw new BadRequestException("Document failed security scan");
        }

        return new SecurityInspection(
                declaredMimeType,
                detectedMime,
                sha256(bytes),
                (long) bytes.length,
                antivirusStatus
        );
    }

    /** Les formats bureautiques se reconnaissent a leur conteneur, puis a leur extension. */
    private static final java.util.Map<String, String> ZIP_FORMATS = java.util.Map.of(
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "odt", "application/vnd.oasis.opendocument.text",
            "ods", "application/vnd.oasis.opendocument.spreadsheet");

    private static final java.util.Map<String, String> OLE_FORMATS = java.util.Map.of(
            "doc", "application/msword",
            "xls", "application/vnd.ms-excel",
            "ppt", "application/vnd.ms-powerpoint");

    private static final java.util.Map<String, String> TEXT_FORMATS = java.util.Map.of(
            "txt", "text/plain",
            "csv", "text/csv");

    private String detectMime(byte[] bytes, String fileName) {
        String extension = normalizeExtension(fileName);
        if (startsWith(bytes, new byte[]{0x25, 0x50, 0x44, 0x46})) {
            return "application/pdf";
        }
        if (startsWith(bytes, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return "image/jpeg";
        }
        if (startsWith(bytes, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47})) {
            return "image/png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        if (startsWith(bytes, new byte[]{'I', 'I', 0x2A, 0x00}) || startsWith(bytes, new byte[]{'M', 'M', 0x00, 0x2A})) {
            return "image/tiff";
        }
        if (startsWith(bytes, new byte[]{'G', 'I', 'F', '8'})) {
            return "image/gif";
        }
        if (startsWith(bytes, new byte[]{'P', 'K', 0x03, 0x04})) {
            // Une archive quelconque ne passe pas : seul un document bureautique est reconnu.
            String mime = ZIP_FORMATS.get(extension);
            if (mime != null) {
                return mime;
            }
            return "application/zip";
        }
        if (startsWith(bytes, new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1})) {
            String mime = OLE_FORMATS.get(extension);
            if (mime != null) {
                return mime;
            }
        }
        if (TEXT_FORMATS.containsKey(extension) && looksLikeText(bytes)) {
            return TEXT_FORMATS.get(extension);
        }
        throw new BadRequestException("Unsupported or unrecognized file content");
    }

    private DocumentAntivirusStatus scanForThreats(byte[] bytes) {
        if (startsWith(bytes, new byte[]{'M', 'Z'}) || startsWith(bytes, new byte[]{0x7F, 'E', 'L', 'F'})) {
            return DocumentAntivirusStatus.INFECTED;
        }
        return DocumentAntivirusStatus.CLEAN;
    }

    /** Un texte ne contient pas d'octet nul : c'est ce qui distingue un binaire deguise en .txt. */
    private boolean looksLikeText(byte[] bytes) {
        int limit = Math.min(bytes.length, 8192);
        for (int i = 0; i < limit; i++) {
            if (bytes[i] == 0) {
                return false;
            }
        }
        return true;
    }

    public String normalizeExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    public record SecurityInspection(
            String declaredMimeType,
            String detectedMimeType,
            String checksumSha256,
            Long fileSizeBytes,
            DocumentAntivirusStatus antivirusStatus
    ) {
    }
}
