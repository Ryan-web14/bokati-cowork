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

        String detectedMime = detectMime(bytes);
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

    private String detectMime(byte[] bytes) {
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
        throw new BadRequestException("Unsupported or unrecognized file content");
    }

    private DocumentAntivirusStatus scanForThreats(byte[] bytes) {
        if (startsWith(bytes, new byte[]{'M', 'Z'}) || startsWith(bytes, new byte[]{0x7F, 'E', 'L', 'F'})) {
            return DocumentAntivirusStatus.INFECTED;
        }
        return DocumentAntivirusStatus.CLEAN;
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
