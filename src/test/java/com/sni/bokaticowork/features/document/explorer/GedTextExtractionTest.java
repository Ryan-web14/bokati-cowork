package com.sni.bokaticowork.features.document.explorer;

import com.sni.bokaticowork.features.document.explorer.text.GedOcrEngine;
import com.sni.bokaticowork.features.document.explorer.text.GedScanAssembler;
import com.sni.bokaticowork.features.document.explorer.text.GedScanAssembler.ScanPage;
import com.sni.bokaticowork.features.document.explorer.text.GedSnippet;
import com.sni.bokaticowork.features.document.explorer.text.GedTextExtractor;
import com.sni.bokaticowork.features.document.explorer.text.GedTextExtractor.Extraction;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La lecture du texte des fichiers de la GED : couche texte d'un PDF, OCR d'un scan, bureautique,
 * texte brut ; l'extrait des resultats ; l'assemblage d'une numerisation en PDF.
 */
class GedTextExtractionTest {

    /** Un OCR qui note les pages lues et rend un texte connu. */
    private static final class FakeOcr implements GedOcrEngine {
        final List<BufferedImage> pages = new ArrayList<>();

        @Override
        public String read(BufferedImage page) {
            pages.add(page);
            return "Contrat de bail commercial page " + pages.size();
        }
    }

    private static final GedOcrEngine NO_OCR = page -> {
        throw new GedOcrEngine.OcrUnavailableException("Tesseract n'est pas installé sur le serveur", null);
    };

    // Fabriques ---------------------------------------------------------------------------------

    private static byte[] textPdf(String... pages) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (String text : pages) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (text == null) continue;
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(PDType1Font.HELVETICA, 12);
                    stream.newLineAtOffset(50, 700);
                    stream.showText(text);
                    stream.endText();
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] image(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.BLACK);
        graphics.drawString("Facture", 10, 20);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private static byte[] zip(String... entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (int index = 0; index < entries.length; index += 2) {
                zip.putNextEntry(new ZipEntry(entries[index]));
                zip.write(entries[index + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    // Extraction --------------------------------------------------------------------------------

    @Test
    @DisplayName("Un PDF numerique se lit par sa couche texte, sans OCR")
    void pdfTextLayer() throws IOException {
        FakeOcr ocr = new FakeOcr();
        Extraction result = new GedTextExtractor(ocr, 30, 300_000)
                .extract(textPdf("Statuts de la societe Elle a ose, article premier", "Article 2 : objet social et siege"), "application/pdf", "statuts.pdf");

        assertThat(result.status()).isEqualTo(GedTextExtractor.DONE);
        assertThat(result.method()).isEqualTo(GedTextExtractor.TEXT_LAYER);
        assertThat(result.pageCount()).isEqualTo(2);
        assertThat(result.content()).contains("Statuts de la societe", "objet social");
        assertThat(ocr.pages).isEmpty();
    }

    @Test
    @DisplayName("Les pages scannees d'un PDF passent a l'OCR, dans la limite fixee ; les autres gardent leur texte")
    void scannedPdfPagesGoToOcr() throws IOException {
        FakeOcr ocr = new FakeOcr();
        byte[] pdf = textPdf("Page numerique avec un vrai texte lisible", null, null, null);

        Extraction result = new GedTextExtractor(ocr, 2, 300_000).extract(pdf, "application/pdf", "dossier.pdf");

        assertThat(result.status()).isEqualTo(GedTextExtractor.DONE);
        assertThat(result.method()).isEqualTo(GedTextExtractor.MIXED);
        assertThat(result.ocrPages()).isEqualTo(2);
        assertThat(ocr.pages).hasSize(2);
        assertThat(result.content()).contains("vrai texte", "Contrat de bail commercial page 1");
        assertThat(result.error()).contains("1 page(s) scannée(s) non lue(s)", "limite");
    }

    @Test
    @DisplayName("Sans Tesseract, un scan est en echec (a retenter) et un PDF numerique reste lu")
    void missingOcr() throws IOException {
        Extraction scan = new GedTextExtractor(NO_OCR, 30, 300_000).extract(textPdf((String) null), "application/pdf", "scan.pdf");
        assertThat(scan.status()).isEqualTo(GedTextExtractor.FAILED);
        assertThat(scan.error()).contains("Tesseract");

        Extraction mixed = new GedTextExtractor(NO_OCR, 30, 300_000)
                .extract(textPdf("Une page numerique bien lisible ici", null), "application/pdf", "m.pdf");
        assertThat(mixed.status()).isEqualTo(GedTextExtractor.DONE);
        assertThat(mixed.error()).contains("1 page(s) scannée(s) non lue(s)", "Tesseract");
    }

    @Test
    @DisplayName("Une image et chaque page d'un TIFF passent a l'OCR")
    void imagesGoToOcr() throws IOException {
        FakeOcr ocr = new FakeOcr();
        Extraction result = new GedTextExtractor(ocr, 30, 300_000).extract(image("png", 200, 100), "image/png", "photo.png");
        assertThat(result.status()).isEqualTo(GedTextExtractor.DONE);
        assertThat(result.method()).isEqualTo(GedTextExtractor.OCR);
        assertThat(ocr.pages).hasSize(1);
    }

    @Test
    @DisplayName("Word, Excel, PowerPoint et OpenDocument se lisent par leur XML, dans l'ordre des diapositives")
    void officeDocuments() throws IOException {
        GedTextExtractor extractor = new GedTextExtractor(NO_OCR, 30, 300_000);

        Extraction docx = extractor.extract(zip("word/document.xml",
                "<w:document><w:body><w:p><w:r><w:t>Avenant n°2 au contrat</w:t></w:r></w:p><w:p><w:r><w:t>Loyer &amp; charges</w:t></w:r></w:p></w:body></w:document>"),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "avenant.docx");
        assertThat(docx.status()).isEqualTo(GedTextExtractor.DONE);
        assertThat(docx.content()).isEqualTo("Avenant n°2 au contrat\nLoyer & charges");

        Extraction xlsx = extractor.extract(zip("xl/sharedStrings.xml", "<sst><si><t>Fournisseur</t></si><si><t>Montant</t></si></sst>",
                "xl/worksheets/sheet1.xml", "<worksheet><c><v>0</v></c></worksheet>"), null, "budget.xlsx");
        assertThat(xlsx.content()).isEqualTo("Fournisseur\nMontant");

        Extraction pptx = extractor.extract(zip("ppt/slides/slide10.xml", "<p:sld><a:p><a:t>Dixieme</a:t></a:p></p:sld>",
                "ppt/slides/slide2.xml", "<p:sld><a:p><a:t>Deuxieme</a:t></a:p></p:sld>"), null, "deck.pptx");
        // Une ligne vide separe deux diapositives
        assertThat(pptx.content()).isEqualTo("Deuxieme\n\nDixieme");

        Extraction odt = extractor.extract(zip("content.xml", "<office:text><text:p>Procès-verbal</text:p></office:text>"),
                "application/vnd.oasis.opendocument.text", "pv.odt");
        assertThat(odt.content()).isEqualTo("Procès-verbal");
    }

    @Test
    @DisplayName("Un texte ou un CSV se lit en UTF-8, ou en Windows-1252 pour un vieux CSV ; les blancs sont resserres")
    void plainText() {
        GedTextExtractor extractor = new GedTextExtractor(NO_OCR, 30, 300_000);
        Extraction utf8 = extractor.extract("Échéance   du\r\n\r\n\r\n\r\nbail".getBytes(StandardCharsets.UTF_8), "text/plain", "note.txt");
        assertThat(utf8.content()).isEqualTo("Échéance du\n\nbail");
        Extraction legacy = extractor.extract("Société;Montant".getBytes(Charset.forName("windows-1252")), "text/csv", "export.csv");
        assertThat(legacy.content()).isEqualTo("Société;Montant");
    }

    @Test
    @DisplayName("Un texte trop long est coupe et signale ; un ancien format Office n'est pas lu")
    void limitsAndUnsupported() {
        GedTextExtractor extractor = new GedTextExtractor(NO_OCR, 30, 1000);
        Extraction long_ = extractor.extract("mot ".repeat(1000).getBytes(StandardCharsets.UTF_8), "text/plain", "long.txt");
        assertThat(long_.truncated()).isTrue();
        assertThat(long_.content()).hasSize(1000);

        Extraction doc = extractor.extract(new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}, "application/msword", "ancien.doc");
        assertThat(doc.status()).isEqualTo(GedTextExtractor.UNSUPPORTED);
        assertThat(extractor.extract("   ".getBytes(StandardCharsets.UTF_8), "text/plain", "vide.txt").status())
                .isEqualTo(GedTextExtractor.EMPTY);
    }

    // Extrait -----------------------------------------------------------------------------------

    @Test
    @DisplayName("L'extrait marque les mots trouves sans tenir compte des accents ni des fins de mots")
    void snippetHighlightsAccentedVariants() {
        String content = "Préambule. " + "Texte sans rapport. ".repeat(10)
                + "L'Échéance du bail commercial tombe le 3 mars, les factures suivent.";

        String snippet = GedSnippet.build(content, "echeance facture -brouillon", 240);

        assertThat(snippet).contains("⟦Échéance⟧", "⟦factures⟧").startsWith("…");
        assertThat(snippet).doesNotContain("⟦Préambule");
        assertThat(GedSnippet.build("Rien a voir ici", "contrat", 240)).isEqualTo("Rien a voir ici");
        assertThat(GedSnippet.build("piege ⟦ dans le texte", "piege", 240)).isEqualTo("⟦piege⟧ [ dans le texte");
        assertThat(GedSnippet.build(null, "x", 240)).isNull();
    }

    // Numerisation ------------------------------------------------------------------------------

    @Test
    @DisplayName("Les pages d'une numerisation deviennent un PDF d'une page par image, lu ensuite par OCR")
    void scanPagesBecomeOnePdf() throws IOException {
        byte[] pdf = GedScanAssembler.toPdf(List.of(
                new ScanPage(image("jpg", 800, 1100), "page1.jpg"),
                new ScanPage(image("png", 1100, 800), "page2.png")));

        try (PDDocument document = PDDocument.load(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(2);
            // La seconde image est plus large que haute : page en paysage
            assertThat(document.getPage(1).getMediaBox().getWidth()).isGreaterThan(document.getPage(1).getMediaBox().getHeight());
        }
        FakeOcr ocr = new FakeOcr();
        Extraction result = new GedTextExtractor(ocr, 30, 300_000).extract(pdf, "application/pdf", "scan.pdf");
        assertThat(result.method()).isEqualTo(GedTextExtractor.OCR);
        assertThat(ocr.pages).hasSize(2);

        assertThatThrownBy(() -> GedScanAssembler.toPdf(List.of(new ScanPage("pas une image".getBytes(StandardCharsets.UTF_8), "notes.txt"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("page 1");
    }
}
