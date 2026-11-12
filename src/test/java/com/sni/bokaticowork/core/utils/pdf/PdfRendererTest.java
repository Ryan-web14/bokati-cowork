package com.sni.bokaticowork.core.utils.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rendu commun HTML vers PDF · remplace les quatorze copies de la chaine jsoup, W3CDom,
 * OpenHtmlToPDF.
 */
class PdfRendererTest {

    @Test
    @DisplayName("Un HTML non XML (balises non fermees, entites) se rend en PDF lisible")
    void lenientHtmlIsRendered() throws Exception {
        byte[] pdf = PdfRenderer.render("""
                <html><head><style>@page { size: A4; margin: 20mm; }</style></head>
                <body><p>Reçu n°&nbsp;12<br><p>Montant : 1 000 XAF</body></html>
                """);

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Reçu").contains("Montant");
            assertThat(document.getPage(0).getMediaBox().getWidth()).isBetween(594f, 597f);
        }
    }

    @Test
    @DisplayName("La taille de page reste au CSS du gabarit")
    void pageSizeComesFromTheTemplate() throws Exception {
        byte[] pdf = PdfRenderer.render("""
                <html><head><style>@page { size: A5 landscape; margin: 5mm; }</style></head>
                <body><p>Badge</p></body></html>
                """);

        try (PDDocument document = PDDocument.load(pdf)) {
            // A5 paysage · 210 x 148 mm, soit environ 595 x 420 points.
            assertThat(document.getPage(0).getMediaBox().getHeight()).isBetween(418f, 422f);
        }
    }

    @Test
    @DisplayName("La police Spectral est embarquee quand un gabarit la demande")
    void sharedFontsAreRegistered() {
        byte[] pdf = PdfRenderer.render("""
                <html><head><style>body { font-family: "Spectral", serif; }</style></head>
                <body><p>Contrat.</p></body></html>
                """);

        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).contains("Spectral-Regular");
    }

    @Test
    @DisplayName("Les pieces en Helvetica n'embarquent pas Spectral")
    void unusedFontsAreNotEmbedded() {
        byte[] pdf = PdfRenderer.render("""
                <html><head><style>body { font-family: Helvetica, Arial, sans-serif; }</style></head>
                <body><p>Facture.</p></body></html>
                """);

        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).doesNotContain("Spectral");
    }

    @Test
    @DisplayName("L'analyse produit du XHTML · balises fermees, entites numeriques")
    void htmlIsNormalisedToXhtml() {
        String xhtml = PdfRenderer.toXhtml("<p>a<br>b&nbsp;c</p>").body().html();

        assertThat(xhtml).contains("<br />").doesNotContain("&nbsp;");
    }
}
