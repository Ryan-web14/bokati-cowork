package com.sni.bokaticowork.features.document.explorer.text;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Assemble les pages d'une numerisation (photos ou images de scanner) en un seul PDF.
 *
 * <p>Chaque image devient une page A4, en portrait ou en paysage selon l'image, l'image centree et
 * reduite pour tenir dans la page. Un JPEG garde sa compression : un PDF de vingt pages
 * photographiees reste leger. Un TIFF de plusieurs pages en donne plusieurs.</p>
 */
public final class GedScanAssembler {

    private static final float JPEG_QUALITY = 0.85f;

    private GedScanAssembler() {
    }

    /** Une image recue : son contenu et son nom (pour reconnaitre un JPEG). */
    public record ScanPage(byte[] content, String fileName) {}

    public static byte[] toPdf(List<ScanPage> pages) throws IOException {
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("Aucune page à assembler.");
        }
        try (PDDocument document = new PDDocument()) {
            int number = 0;
            for (ScanPage page : pages) {
                number++;
                if (isJpeg(page.content())) {
                    PDImageXObject image = JPEGFactory.createFromByteArray(document, page.content());
                    addPage(document, image);
                    continue;
                }
                List<BufferedImage> images = GedTextExtractor.readImages(page.content());
                if (images.isEmpty()) {
                    throw new IllegalArgumentException("La page " + number + " (" + page.fileName()
                            + ") n'est pas une image lisible (JPEG, PNG, TIFF, GIF ou BMP).");
                }
                for (BufferedImage buffered : images) {
                    // Un scan noir et blanc reste net et leger sans perte ; une photo en PNG
                    // deviendrait enorme sans perte : elle passe en JPEG
                    PDImageXObject image = buffered.getType() == BufferedImage.TYPE_BYTE_BINARY
                            ? LosslessFactory.createFromImage(document, buffered)
                            : JPEGFactory.createFromImage(document, opaque(buffered), JPEG_QUALITY);
                    addPage(document, image);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static void addPage(PDDocument document, PDImageXObject image) throws IOException {
        boolean landscape = image.getWidth() > image.getHeight();
        PDRectangle a4 = landscape ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()) : PDRectangle.A4;
        PDPage page = new PDPage(a4);
        document.addPage(page);
        float scale = Math.min(a4.getWidth() / image.getWidth(), a4.getHeight() / image.getHeight());
        float width = image.getWidth() * scale;
        float height = image.getHeight() * scale;
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            stream.drawImage(image, (a4.getWidth() - width) / 2, (a4.getHeight() - height) / 2, width, height);
        }
    }

    /** Le JPEG n'a pas de transparence : un PNG transparent est pose sur du blanc, comme du papier. */
    private static BufferedImage opaque(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return image;
        }
        BufferedImage flat = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = flat.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return flat;
    }

    private static boolean isJpeg(byte[] content) {
        return content != null && content.length > 3
                && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8 && (content[2] & 0xFF) == 0xFF;
    }
}
