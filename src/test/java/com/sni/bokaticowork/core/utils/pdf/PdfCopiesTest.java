package com.sni.bokaticowork.core.utils.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'original et son duplicata dans un meme PDF · seul le second porte la mention.
 */
class PdfCopiesTest {

    /** Un document de deux pages, comme une facture longue. */
    private static byte[] invoice() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int index = 1; index <= 2; index++) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 12);
                    content.newLineAtOffset(72, 700);
                    content.showText("FACTURE INV-1 page " + index);
                    content.endText();
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private static String text(PDDocument document, int from, int to) throws Exception {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(from);
        stripper.setEndPage(to);
        return stripper.getText(document);
    }

    @Test
    @DisplayName("Deux exemplaires a la suite · l'original sans mention, le duplicata tamponne a chaque page")
    void originalThenDuplicate() throws Exception {
        byte[] both = PdfCopies.originalAndDuplicate(invoice());

        try (PDDocument document = PDDocument.load(both)) {
            assertThat(document.getNumberOfPages()).isEqualTo(4);
            assertThat(text(document, 1, 2)).contains("FACTURE INV-1 page 1").contains("page 2").doesNotContain("DUPLICATA");
            assertThat(text(document, 3, 3)).contains("FACTURE INV-1 page 1").contains("DUPLICATA");
            assertThat(text(document, 4, 4)).contains("page 2").contains("DUPLICATA");
        }
    }
}
