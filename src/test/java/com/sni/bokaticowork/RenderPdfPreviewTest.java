package com.sni.bokaticowork;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.imageio.ImageIO;
import java.io.File;
import java.nio.file.Path;

/**
 * Utilitaire de verification visuelle : convertit un PDF en PNG pour controler une mise en page.
 * <p>
 * Desactive par defaut, ce n'est pas un test de non-regression. Lancer avec :
 * {@code mvn test -Dtest=RenderPdfPreviewTest -Dpdf.preview=<chemin.pdf>}
 */
class RenderPdfPreviewTest {

    @Test
    @EnabledIfSystemProperty(named = "pdf.preview", matches = ".+")
    void renderFirstPageToPng() throws Exception {
        Path source = Path.of(System.getProperty("pdf.preview"));
        try (PDDocument document = PDDocument.load(source.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            File target = source.resolveSibling(
                    source.getFileName().toString().replaceAll("\\.pdf$", "") + ".png").toFile();
            ImageIO.write(renderer.renderImage(0, 2f, ImageType.RGB), "png", target);
            System.out.println("PNG ecrit : " + target.getAbsolutePath());
        }
    }
}
