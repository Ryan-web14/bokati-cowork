package com.sni.bokaticowork.features.document.kyc.service.support.identity;

import com.sni.bokaticowork.features.document.kyc.service.support.identity.KycIdentityReport.Format;
import com.sni.bokaticowork.features.document.kyc.service.support.identity.KycIdentityReport.Verdict;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** Lot KG9 : analyse « piece d'identite » sur des images fabriquees. */
class KycIdentityAnalysisTest {

    /** Carte au format ID1 posee sur un fond, avec lignes de texte et, si demande, deux lignes MRZ. */
    static BufferedImage card(int cardWidth, double ratio, boolean mrz, int textLines) {
        int cardHeight = (int) Math.round(cardWidth / ratio);
        BufferedImage image = new BufferedImage(cardWidth + 300, cardHeight + 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(40, 40, 45));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        int x0 = 150;
        int y0 = 150;
        g.setColor(new Color(235, 232, 225));
        g.fillRect(x0, y0, cardWidth, cardHeight);
        g.setColor(Color.BLACK);
        Random random = new Random(7);
        int lineHeight = Math.max(10, cardHeight / 30);
        for (int i = 0; i < textLines; i++) {
            int y = y0 + cardHeight / 8 + i * lineHeight * 2;
            glyphs(g, random, x0 + cardWidth * 2 / 5, y, cardWidth / 2, lineHeight);
        }
        if (mrz) {
            for (int i = 0; i < 2; i++) {
                int y = y0 + cardHeight - cardHeight / 5 + i * (lineHeight + lineHeight / 2);
                glyphs(g, random, x0 + cardWidth / 20, y, cardWidth * 9 / 10, lineHeight);
            }
        }
        g.dispose();
        return image;
    }

    private static void glyphs(Graphics2D g, Random random, int x, int y, int width, int height) {
        int cursor = x;
        while (cursor < x + width - 8) {
            int glyph = 4 + random.nextInt(5);
            g.fillRect(cursor, y, glyph, height);
            cursor += glyph + 4 + random.nextInt(3);
        }
    }

    @Test
    void anIdCardWithTextAndMrzIsLikelyAnIdentityDocument() {
        KycIdentityReport report = JavaIdentityEngine.analyse(card(856, JavaIdentityEngine.ID1_RATIO, true, 5), false, true);

        assertThat(report.engine()).isEqualTo(KycIdentityReport.Engine.JAVA);
        assertThat(report.contourFound()).isTrue();
        assertThat(report.format()).isEqualTo(Format.ID1);
        assertThat(report.mrzDetected()).isTrue();
        assertThat(report.verdict()).isEqualTo(Verdict.LIKELY_IDENTITY);
        assertThat(report.faceDetected()).isNull();
        assertThat(report.warnings()).isEmpty();
    }

    @Test
    void aPictureWithoutTextOrCardFormatIsNotAnIdentityDocument() {
        BufferedImage photo = new BufferedImage(800, 1000, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = photo.createGraphics();
        for (int y = 0; y < 1000; y++) {
            g.setColor(new Color(90 + y / 10, 120, 150));
            g.drawLine(0, y, 799, y);
        }
        g.dispose();

        KycIdentityReport report = JavaIdentityEngine.analyse(photo, false, false);

        assertThat(report.verdict()).isEqualTo(Verdict.NOT_IDENTITY);
        assertThat(report.warnings()).anyMatch(w -> w.contains("ne ressemble pas"));
    }

    @Test
    void aPassportPageWithoutMrzIsFlagged() {
        KycIdentityReport report = JavaIdentityEngine.analyse(card(1000, JavaIdentityEngine.ID3_RATIO, false, 6), true, false);

        assertThat(report.format()).isEqualTo(Format.ID3);
        assertThat(report.mrzDetected()).isFalse();
        assertThat(report.warnings()).anyMatch(w -> w.contains("MRZ"));
    }

    @Test
    void identityTypesAreRecognisedByTheirCode() {
        KycIdentityAnalysisService service = new KycIdentityAnalysisService();
        assertThat(service.applies("MEMBER_NATIONAL_ID")).isTrue();
        assertThat(service.applies("CUSTOMER_PERSON_PASSPORT")).isTrue();
        assertThat(service.applies("BUSINESS_REPRESENTATIVE_ID")).isTrue();
        assertThat(service.applies("MEMBER_PROOF_OF_ADDRESS")).isFalse();
        assertThat(service.applies("BUSINESS_RCCM")).isFalse();
        assertThat(service.activeEngine()).isEqualTo(KycIdentityReport.Engine.JAVA);
    }

    @Test
    void theOpenCvEngineFindsTheCardAndLooksForAFace() {
        OpenCvIdentityEngine engine;
        try {
            engine = new OpenCvIdentityEngine();
        } catch (Exception | LinkageError ex) {
            Assumptions.abort("OpenCV non chargeable sur ce poste : " + ex);
            return;
        }
        KycIdentityReport report = engine.analyse(card(856, JavaIdentityEngine.ID1_RATIO, true, 5), false, false);

        assertThat(report.engine()).isEqualTo(KycIdentityReport.Engine.OPENCV);
        assertThat(report.contourFound()).isTrue();
        assertThat(report.format()).isEqualTo(Format.ID1);
        assertThat(report.faceDetected()).isFalse();
        assertThat(report.warnings()).anyMatch(w -> w.contains("visage"));
    }
}
