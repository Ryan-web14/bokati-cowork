package com.sni.bokaticowork.features.document.documentMaster.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentSecurityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La GED accepte les formats d'un vrai poste de travail, sans ouvrir la porte a n'importe quel
 * fichier : un format se reconnait a son contenu, l'extension ne fait que preciser un conteneur.
 */
class DocumentFormatDetectionTest {

    private static final List<String> GENERAL = List.of(
            "application/pdf", "image/tiff", "text/plain", "text/csv",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword");
    private static final List<String> KYC = List.of("application/pdf", "image/jpeg", "image/png", "image/webp");
    private static final byte[] ZIP = {'P', 'K', 0x03, 0x04, 0x14, 0x00};
    private static final byte[] OLE = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 0};

    private final DocumentSecurityService security = new DocumentSecurityService();

    private String detect(String fileName, byte[] bytes, List<String> allowed) {
        return security.inspectBytes(fileName, null, bytes, allowed).detectedMimeType();
    }

    @Test
    @DisplayName("Word, ancien Word, TIFF numerise et texte sont reconnus")
    void officeAndScanFormats() {
        assertThat(detect("contrat.docx", ZIP, GENERAL))
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(detect("courrier.doc", OLE, GENERAL)).isEqualTo("application/msword");
        assertThat(detect("scan.tif", new byte[]{'I', 'I', 0x2A, 0x00, 1}, GENERAL)).isEqualTo("image/tiff");
        assertThat(detect("export.csv", "nom;montant\nA;12\n".getBytes(StandardCharsets.UTF_8), GENERAL)).isEqualTo("text/csv");
    }

    @Test
    @DisplayName("Une archive quelconque, un binaire deguise en texte et un executable sont refuses")
    void disguisedFilesAreRefused() {
        assertThatThrownBy(() -> detect("photos.zip", ZIP, GENERAL)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> detect("note.txt", new byte[]{'a', 0, 'b'}, GENERAL)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> detect("setup.pdf", new byte[]{'M', 'Z', 0, 0}, GENERAL)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Une piece KYC reste limitee au PDF et aux images")
    void kycTypesKeepTheirFormats() {
        assertThatThrownBy(() -> detect("cni.docx", ZIP, KYC))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported document MIME type");
        assertThat(detect("cni.pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII), KYC)).isEqualTo("application/pdf");
    }
}
