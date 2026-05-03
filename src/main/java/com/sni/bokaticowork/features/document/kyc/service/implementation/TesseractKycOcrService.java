package com.sni.bokaticowork.features.document.kyc.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentVersion;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentVersionRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentStorageService;
import com.sni.bokaticowork.features.document.kyc.config.KycAutomationProperties;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.model.KycDocumentOcrResult;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentOcrResultRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TesseractKycOcrService implements KycOcrService {

    private static final Pattern DOCUMENT_NUMBER_PATTERN = Pattern.compile("(?i)(?:N[o°]|numero|number|id)\\s*[:#-]?\\s*([A-Z0-9\\-]{5,})");
    private static final List<Pattern> DATE_PATTERNS = List.of(
            Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b"),
            Pattern.compile("\\b(\\d{2}/\\d{2}/\\d{4})\\b"),
            Pattern.compile("\\b(\\d{2}-\\d{2}-\\d{4})\\b")
    );
    private static final DateTimeFormatter DMY_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DMY_DASH = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final KycAutomationProperties properties;
    private final KycDocumentOcrResultRepository ocrResultRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentStorageService storageService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void process(KycDocument kycDocument) {
        if (kycDocument == null) {
            return;
        }
        if (!Boolean.TRUE.equals(properties.getOcr().getEnabled())) {
            upsertSkipped(kycDocument, "OCR_DISABLED");
            return;
        }
        if (!"TESSERACT".equalsIgnoreCase(properties.getOcr().getProvider())) {
            upsertSkipped(kycDocument, "UNSUPPORTED_PROVIDER_" + properties.getOcr().getProvider());
            return;
        }

        Document document = kycDocument.getDocument();
        if (document == null || !StringUtils.hasText(document.getFileUrl())) {
            upsertSkipped(kycDocument, "NO_FILE");
            return;
        }

        Path tempFile = null;
        try {
            tempFile = materializeDocument(document);
            ITesseract tesseract = new Tesseract();
            if (StringUtils.hasText(properties.getOcr().getDataPath())) {
                tesseract.setDatapath(properties.getOcr().getDataPath());
            }
            if (StringUtils.hasText(properties.getOcr().getLanguage())) {
                tesseract.setLanguage(properties.getOcr().getLanguage());
            }
            String text = tesseract.doOCR(tempFile.toFile());
            upsertSuccess(kycDocument, text);
        } catch (Exception ex) {
            log.warn("Tesseract OCR failed for KYC document {}", kycDocument.getId(), ex);
            upsertFailure(kycDocument, ex.getMessage());
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private Path materializeDocument(Document document) throws Exception {
        DocumentVersion currentVersion = documentVersionRepository.findByDocumentAndCurrentTrue(document).orElse(null);
        String storageProvider = currentVersion == null ? "FILESYSTEM" : currentVersion.getStorageProvider();
        String storagePath = currentVersion == null ? document.getFileUrl() : currentVersion.getStoragePath();
        byte[] content = storageService.read(storageProvider, storagePath);
        String suffix = extensionSuffix(document.getFileName());
        Path tempFile = Files.createTempFile("kyc-ocr-", suffix);
        Files.write(tempFile, content);
        return tempFile;
    }

    private String extensionSuffix(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return ".bin";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    private void upsertSuccess(KycDocument kycDocument, String rawText) {
        KycDocumentOcrResult result = getOrCreate(kycDocument);
        result.setExtractedDocumentNumber(firstMatch(DOCUMENT_NUMBER_PATTERN, rawText));
        result.setExtractedExpiryDate(firstDate(rawText));
        result.setConfidenceScore(BigDecimal.ONE);
        result.setRawOcrJson(toJson("SUCCESS", rawText, null));
        result.setProcessedAt(Instant.now());
        ocrResultRepository.save(result);
    }

    private void upsertSkipped(KycDocument kycDocument, String reason) {
        KycDocumentOcrResult result = getOrCreate(kycDocument);
        result.setExtractedExpiryDate(kycDocument.getExpiryDate());
        result.setExtractedDocumentNumber(kycDocument.getDocumentNumber());
        result.setConfidenceScore(BigDecimal.ZERO);
        result.setRawOcrJson(toJson("SKIPPED", null, reason));
        result.setProcessedAt(Instant.now());
        ocrResultRepository.save(result);
    }

    private void upsertFailure(KycDocument kycDocument, String error) {
        KycDocumentOcrResult result = getOrCreate(kycDocument);
        result.setConfidenceScore(BigDecimal.ZERO);
        result.setRawOcrJson(toJson("FAILED", null, error));
        result.setProcessedAt(Instant.now());
        ocrResultRepository.save(result);
    }

    private KycDocumentOcrResult getOrCreate(KycDocument kycDocument) {
        return ocrResultRepository.findByKycDocument(kycDocument)
                .orElseGet(() -> KycDocumentOcrResult.builder().kycDocument(kycDocument).build());
    }

    private String firstMatch(Pattern pattern, String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim().toUpperCase(Locale.ROOT) : null;
    }

    private LocalDate firstDate(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                LocalDate parsed = parseDate(matcher.group(1));
                if (parsed != null && parsed.isAfter(LocalDate.now().minusYears(1))) {
                    return parsed;
                }
            }
        }
        return null;
    }

    private LocalDate parseDate(String value) {
        try {
            if (value.contains("/")) {
                return LocalDate.parse(value, DMY_SLASH);
            }
            if (value.charAt(2) == '-') {
                return LocalDate.parse(value, DMY_DASH);
            }
            return LocalDate.parse(value);
        } catch (DateTimeParseException | IndexOutOfBoundsException ex) {
            return null;
        }
    }

    private String toJson(String status, String rawText, String reason) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("provider", "TESSERACT");
            payload.put("status", status);
            payload.put("language", properties.getOcr().getLanguage());
            payload.put("reason", reason);
            payload.put("rawText", rawText);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{\"provider\":\"TESSERACT\",\"status\":\"" + status + "\"}";
        }
    }
}
