package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentPdfService;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentStorageService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Couvre la comparaison d'empreinte · c'est elle qui tranche un litige, et la seule partie du
 * scellement qui n'exige pas de rendre reellement un PDF.
 */
class BillingDocumentPdfSealingTest {

    private final BillingDocumentRepository documentRepository = mock(BillingDocumentRepository.class);
    private final DocumentStorageService storageService = mock(DocumentStorageService.class);

    private final BillingDocumentPdfServiceImpl service = new BillingDocumentPdfServiceImpl(
            mock(com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService.class),
            mock(org.thymeleaf.spring6.SpringTemplateEngine.class),
            new com.fasterxml.jackson.databind.ObjectMapper(),
            mock(com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository.class),
            mock(com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository.class),
            mock(com.sni.bokaticowork.features.billing.service.fiscal.FiscalQrCodeService.class),
            documentRepository,
            storageService,
            java.util.Locale.FRANCE);

    @Test
    void shouldConfirmAFileThatMatchesTheSealedVersion() {
        byte[] bytes = "facture".getBytes(StandardCharsets.UTF_8);
        // On scelle l'empreinte que le service calcule lui-meme, puis on lui represente le meme
        // contenu · c'est exactement le parcours d'un client qui redepose son PDF.
        String sealed = sha256(bytes);
        documentSealedWith(sealed, BillingDocumentStatus.VALIDATED);

        BillingDocumentPdfService.PdfComparison result = service.compare("INV-001", bytes);

        assertThat(result.sealed()).isTrue();
        assertThat(result.match()).isTrue();
        assertThat(result.provided()).isEqualTo(sealed);
    }

    @Test
    void shouldRejectAFileThatDiffersByASingleByte() {
        byte[] original = "facture".getBytes(StandardCharsets.UTF_8);
        documentSealedWith(sha256(original), BillingDocumentStatus.VALIDATED);

        BillingDocumentPdfService.PdfComparison result =
                service.compare("INV-001", "Facture".getBytes(StandardCharsets.UTF_8));

        assertThat(result.sealed()).isTrue();
        assertThat(result.match()).isFalse();
    }

    @Test
    void shouldReportThatNoComparisonIsPossibleOnAnUnsealedDocument() {
        // Annoncer « different » sur un document jamais scelle serait faux · rien ne permet de
        // conclure, et ce serait faire soupconner un fichier parfaitement legitime.
        documentSealedWith(null, BillingDocumentStatus.DRAFT);

        BillingDocumentPdfService.PdfComparison result =
                service.compare("INV-001", "quoi que ce soit".getBytes(StandardCharsets.UTF_8));

        assertThat(result.sealed()).isFalse();
        assertThat(result.match()).isFalse();
        assertThat(result.expected()).isNull();
        assertThat(result.provided()).isNotBlank();
    }

    @Test
    void shouldFlagACancelledDocumentSoAMismatchIsNotMistakenForFraud() {
        // Un document annule est servi avec un filigrane · sa copie telechargee differe donc
        // legitimement de la version canonique.
        documentSealedWith("0".repeat(64), BillingDocumentStatus.CANCELLED);

        BillingDocumentPdfService.PdfComparison result =
                service.compare("INV-001", "facture".getBytes(StandardCharsets.UTF_8));

        assertThat(result.match()).isFalse();
        assertThat(result.watermarked()).isTrue();
    }

    @Test
    void shouldIgnoreCaseOnTheStoredFingerprint() {
        byte[] bytes = "facture".getBytes(StandardCharsets.UTF_8);
        documentSealedWith(sha256(bytes).toUpperCase(java.util.Locale.ROOT), BillingDocumentStatus.VALIDATED);

        assertThat(service.compare("INV-001", bytes).match()).isTrue();
    }

    @Test
    void shouldTreatAnEmptyUploadAsAMismatchRatherThanFail() {
        documentSealedWith(sha256("facture".getBytes(StandardCharsets.UTF_8)), BillingDocumentStatus.VALIDATED);

        BillingDocumentPdfService.PdfComparison result = service.compare("INV-001", null);

        assertThat(result.match()).isFalse();
        assertThat(result.provided()).isNotBlank();
    }

    // =================================================================================

    private void documentSealedWith(String sha256, BillingDocumentStatus status) {
        BillingDocument document = BillingDocument.builder()
                .documentNumber("INV-001")
                .status(status)
                .pdfSha256(sha256)
                .pdfStorageProvider(sha256 == null ? null : "FILESYSTEM")
                .pdfStoragePath(sha256 == null ? null : "billing/INV-001/1/x.pdf")
                .build();
        when(documentRepository.findByDocumentNumber(anyString())).thenReturn(Optional.of(document));
    }

    private String sha256(byte[] bytes) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
