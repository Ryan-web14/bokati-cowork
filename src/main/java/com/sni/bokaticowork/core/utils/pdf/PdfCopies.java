package com.sni.bokaticowork.core.utils.pdf;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * L'original et son duplicata dans un meme PDF · la version d'impression de l'administration.
 *
 * <p>Le duplicata est l'original a l'identique, chaque page tamponnee « DUPLICATA » : en tete, en
 * rouge, et en filigrane leger. Les clients et les membres recoivent l'exemplaire unique, sans ce
 * passage.</p>
 *
 * <p>Le fichier assemble est un nouveau document : la signature electronique de l'original n'y
 * est plus verifiable. La version canonique, signee et scellee, reste celle servie aux clients.</p>
 */
public final class PdfCopies {

    public static final String DUPLICATE_LABEL = "DUPLICATA";

    private PdfCopies() {
    }

    /** L'original, suivi de son duplicata tamponne. */
    public static byte[] originalAndDuplicate(byte[] original) {
        return merge(original, stamp(original, DUPLICATE_LABEL));
    }

    /** Chaque page porte la mention, en tete et en filigrane. */
    public static byte[] stamp(byte[] pdf, String label) {
        try (PDDocument document = PDDocument.load(pdf);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Le duplicata n'est pas l'exemplaire signe · sa signature ne vaudrait plus rien.
            document.setAllSecurityToBeRemoved(true);
            PDType1Font font = PDType1Font.HELVETICA_BOLD;
            for (PDPage page : document.getPages()) {
                PDRectangle box = page.getMediaBox();
                try (PDPageContentStream content = new PDPageContentStream(document, page,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    // En tete, au centre · l'espace laisse libre entre l'emetteur et le titre, lisible a
                    // la photocopie sans rien masquer.
                    float headerSize = 14f;
                    float headerWidth = font.getStringWidth(label) / 1000f * headerSize;
                    float x = (box.getWidth() - headerWidth) / 2f;
                    float y = box.getHeight() - 34f;
                    content.setNonStrokingColor(new Color(200, 30, 30));
                    content.setStrokingColor(new Color(200, 30, 30));
                    content.setLineWidth(1.2f);
                    content.addRect(x - 6f, y - 5f, headerWidth + 12f, headerSize + 8f);
                    content.stroke();
                    content.beginText();
                    content.setFont(font, headerSize);
                    content.newLineAtOffset(x, y);
                    content.showText(label);
                    content.endText();

                    // En filigrane, en diagonale · impossible a faire passer pour l'original.
                    PDExtendedGraphicsState state = new PDExtendedGraphicsState();
                    state.setNonStrokingAlphaConstant(0.10f);
                    content.setGraphicsStateParameters(state);
                    float size = 80f;
                    float width = font.getStringWidth(label) / 1000f * size;
                    double angle = Math.toRadians(45);
                    float cx = box.getWidth() / 2f;
                    float cy = box.getHeight() / 2f;
                    content.beginText();
                    content.setFont(font, size);
                    content.setTextMatrix(new org.apache.pdfbox.util.Matrix(
                            (float) Math.cos(angle), (float) Math.sin(angle), (float) -Math.sin(angle), (float) Math.cos(angle),
                            cx - (float) (width / 2f * Math.cos(angle)), cy - (float) (width / 2f * Math.sin(angle))));
                    content.showText(label);
                    content.endText();
                }
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Duplicata impossible a produire", ex);
        }
    }

    /** Les documents bout a bout, dans l'ordre. */
    public static byte[] merge(byte[]... pdfs) {
        try (PDDocument target = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFMergerUtility merger = new PDFMergerUtility();
            for (byte[] pdf : pdfs) {
                try (PDDocument source = PDDocument.load(pdf)) {
                    merger.appendDocument(target, source);
                }
            }
            target.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Assemblage des exemplaires impossible", ex);
        }
    }
}
