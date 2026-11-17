package com.sni.bokaticowork.features.document.kyc.service.support;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** Netteté, résolution et exposition mesurées sur des images synthétiques. */
class KycImageQualityServiceTest {

    private static BufferedImage card(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(235, 230, 220));
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, height / 14)));
        for (int line = 1; line <= 8; line++) {
            g.drawString("REPUBLIQUE DU CONGO  NOM BOKATI  N 0123456789", 20, line * height / 9);
        }
        g.drawRect(10, 10, width - 20, height - 20);
        g.dispose();
        return image;
    }

    private static BufferedImage blur(BufferedImage source, int radius) {
        int size = radius * 2 + 1;
        float[] kernel = new float[size * size];
        Arrays.fill(kernel, 1f / kernel.length);
        BufferedImage out = source;
        for (int pass = 0; pass < 3; pass++) {
            out = new ConvolveOp(new Kernel(size, size, kernel), ConvolveOp.EDGE_NO_OP, null).filter(out, null);
        }
        return out;
    }

    @Test
    void aSharpCardIsOk() {
        KycImageQualityService.Report report = KycImageQualityService.analyse(card(1400, 880), 100, 600);
        assertThat(report.status()).isEqualTo(KycImageQualityService.Status.OK);
        assertThat(report.blurScore()).isGreaterThan(100);
        assertThat(report.warnings()).isEmpty();
    }

    @Test
    void aBlurredCardIsFlagged() {
        KycImageQualityService.Report report = KycImageQualityService.analyse(blur(card(1400, 880), 6), 100, 600);
        assertThat(report.status()).isEqualTo(KycImageQualityService.Status.WARNING);
        assertThat(report.blurScore()).isLessThan(100);
        assertThat(report.warnings()).anyMatch(w -> w.contains("floue"));
    }

    @Test
    void aTinyOrDarkImageIsFlagged() {
        assertThat(KycImageQualityService.analyse(card(400, 250), 1, 600).warnings()).anyMatch(w -> w.contains("faible résolution"));
        BufferedImage dark = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
        assertThat(KycImageQualityService.analyse(dark, 0, 600).warnings()).anyMatch(w -> w.contains("trop sombre"));
    }

    @Test
    void anEncodedImageIsDecodedAndAnUnknownFormatIsNotAnError() throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(card(1400, 880), "png", png);
        KycImageQualityService service = new KycImageQualityService();

        assertThat(service.check(png.toByteArray(), "cni.png", "image/png").status()).isEqualTo(KycImageQualityService.Status.OK);
        assertThat(service.check(new byte[]{1, 2, 3, 4, 5}, "cni.heic", "image/heic").status()).isEqualTo(KycImageQualityService.Status.UNKNOWN);
    }
}
