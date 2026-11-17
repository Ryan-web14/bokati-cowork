package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Qualite d'image d'une piece KYC, mesuree au depot, en Java seul (aucun service externe, aucune
 * bibliotheque native) : environ 50 ms par image.
 *
 * <ul>
 *   <li><b>Nettete</b> : variance du laplacien sur l'image en niveaux de gris ramenee a 1000 px de
 *       large ; une image floue a peu de contours nets, donc une variance faible ;</li>
 *   <li><b>Resolution</b> : plus petit cote en pixels ;</li>
 *   <li><b>Exposition</b> : image trop sombre.</li>
 * </ul>
 *
 * <p>Le resultat est un avertissement, jamais un refus. Un PDF est lu par sa premiere page. Un
 * format que Java ne sait pas decoder (HEIC, WEBP) donne {@code UNKNOWN}. Seuils dans les parametres
 * {@code app.kyc.quality.*}.</p>
 */
@Slf4j
@Component
public class KycImageQualityService {

    public enum Status { OK, WARNING, UNKNOWN }

    /** Ce que le controle a mesure. */
    public record Report(Status status, Double blurScore, Integer width, Integer height, List<String> warnings) {
        public static Report unknown(String reason) {
            return new Report(Status.UNKNOWN, null, null, null, reason == null ? List.of() : List.of(reason));
        }
    }

    static final String BLUR_THRESHOLD = "app.kyc.quality.blur-threshold";
    static final String MIN_SIDE = "app.kyc.quality.min-side-px";
    static final String ENABLED = "app.kyc.quality.enabled";

    private static final int ANALYSIS_WIDTH = 1000;

    private PlatformSettings settings;

    @Autowired(required = false)
    void setSettings(PlatformSettings settings) {
        this.settings = settings;
    }

    public Report check(byte[] content, String fileName, String mimeType) {
        if (settings != null && !settings.bool(ENABLED, true)) {
            return Report.unknown(null);
        }
        if (content == null || content.length == 0) {
            return Report.unknown(null);
        }
        try {
            BufferedImage image = decode(content, fileName, mimeType);
            if (image == null) {
                return Report.unknown("Format d'image non analysé : la netteté n'a pas pu être vérifiée.");
            }
            return analyse(image,
                    settings == null ? 100 : settings.integer(BLUR_THRESHOLD, 100),
                    settings == null ? 600 : settings.integer(MIN_SIDE, 600));
        } catch (Exception | OutOfMemoryError ex) {
            log.warn("Controle de qualite impossible pour {} : {}", fileName, ex.getMessage());
            return Report.unknown(null);
        }
    }

    static BufferedImage decode(byte[] content, String fileName, String mimeType) throws Exception {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        boolean pdf = "application/pdf".equalsIgnoreCase(mimeType) || name.endsWith(".pdf")
                || (content.length > 4 && content[0] == '%' && content[1] == 'P' && content[2] == 'D' && content[3] == 'F');
        if (pdf) {
            try (PDDocument document = PDDocument.load(content)) {
                if (document.getNumberOfPages() == 0) {
                    return null;
                }
                return new PDFRenderer(document).renderImageWithDPI(0, 150, ImageType.GRAY);
            }
        }
        return ImageIO.read(new ByteArrayInputStream(content));
    }

    /** Mesures et avertissements (public pour les tests). */
    public static Report analyse(BufferedImage source, double blurThreshold, int minSide) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[][] gray = grayscale(source, ANALYSIS_WIDTH);
        double blur = laplacianVariance(gray);
        double[] exposure = exposure(gray);
        List<String> warnings = new ArrayList<>();
        if (blur < blurThreshold) {
            warnings.add("L'image semble floue : reprenez la photo bien à plat, nette, sans bouger.");
        }
        if (Math.min(width, height) < minSide) {
            warnings.add("L'image est de faible résolution (" + width + " × " + height + " px) : les mentions risquent d'être illisibles.");
        }
        // Pas de controle de reflet : le fond blanc d'un scan le declencherait a tort.
        if (exposure[0] < 50) {
            warnings.add("L'image est trop sombre : photographiez la pièce dans un endroit bien éclairé.");
        }
        double rounded = Math.round(blur * 100d) / 100d;
        return new Report(warnings.isEmpty() ? Status.OK : Status.WARNING, rounded, width, height, List.copyOf(warnings));
    }

    /** Niveaux de gris, ramenes a {@code targetWidth} si l'image est plus large. */
    static int[][] grayscale(BufferedImage source, int targetWidth) {
        BufferedImage image = source;
        if (source.getWidth() > targetWidth) {
            int targetHeight = Math.max(1, (int) Math.round(source.getHeight() * (targetWidth / (double) source.getWidth())));
            BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
            g.dispose();
            image = scaled;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        int[][] gray = new int[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                gray[y][x] = (r * 299 + g * 587 + b * 114) / 1000;
            }
        }
        return gray;
    }

    /** Variance de la reponse au noyau laplacien [0 1 0 ; 1 -4 1 ; 0 1 0]. */
    static double laplacianVariance(int[][] gray) {
        int h = gray.length;
        int w = h == 0 ? 0 : gray[0].length;
        if (h < 3 || w < 3) {
            return 0;
        }
        double sum = 0;
        double sumSquares = 0;
        long count = 0;
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int value = gray[y - 1][x] + gray[y + 1][x] + gray[y][x - 1] + gray[y][x + 1] - 4 * gray[y][x];
                sum += value;
                sumSquares += (double) value * value;
                count++;
            }
        }
        double mean = sum / count;
        return sumSquares / count - mean * mean;
    }

    /** [luminosite moyenne, part de pixels brules (>= 250)]. */
    static double[] exposure(int[][] gray) {
        long total = 0;
        long burnt = 0;
        long count = 0;
        for (int[] row : gray) {
            for (int value : row) {
                total += value;
                if (value >= 250) burnt++;
                count++;
            }
        }
        return count == 0 ? new double[]{0, 0} : new double[]{total / (double) count, burnt / (double) count};
    }
}
