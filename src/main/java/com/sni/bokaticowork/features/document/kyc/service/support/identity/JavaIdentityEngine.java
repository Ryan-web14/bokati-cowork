package com.sni.bokaticowork.features.document.kyc.service.support.identity;

import com.sni.bokaticowork.features.document.kyc.service.support.identity.KycIdentityReport.Format;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Lot KG9 : analyse d'une piece d'identite en Java seul (moteur par defaut, aucune bibliotheque
 * native). Trois mesures sur l'image en gris ramenee a 900 px :
 * <ul>
 *   <li>contour : lignes droites longues (bords de la carte) par profils de gradient horizontaux et
 *   verticaux ; a defaut, l'image entiere est supposee recadree sur la piece ;</li>
 *   <li>format : rapport des cotes, ID1 (carte, 1,586) ou ID3 (page de passeport, 1,42) ;</li>
 *   <li>texte : lignes de texte par densite de transitions ; bande MRZ en bas (au moins deux lignes
 *   serrees sur toute la largeur).</li>
 * </ul>
 * Pas de visage : il demande le moteur OpenCV.
 */
public final class JavaIdentityEngine {

    static final double ID1_RATIO = 85.60 / 53.98;
    static final double ID3_RATIO = 125.0 / 88.0;
    private static final int WORK_SIZE = 900;
    private static final int EDGE_THRESHOLD = 60;
    private static final int TRANSITION_THRESHOLD = 35;

    private JavaIdentityEngine() {
    }

    /** Mesures brutes, partagees avec le moteur OpenCV (qui remplace le contour et ajoute le visage). */
    public record Measures(int left, int top, int right, int bottom, boolean contourFound, double ratio, Format format,
                           boolean mrz, int textLines) {
    }

    public static KycIdentityReport analyse(BufferedImage source, boolean passport, boolean back) {
        int[][] gray = gray(source);
        Measures measures = measure(gray);
        return KycIdentityVerdicts.decide(KycIdentityReport.Engine.JAVA, measures, null, passport, back);
    }

    public static Measures measure(int[][] gray) {
        int height = gray.length;
        int width = gray[0].length;
        int[] box = findCardBox(gray);
        boolean contour = box != null;
        if (box == null) {
            box = new int[]{0, 0, width - 1, height - 1};
        }
        return measureInBox(gray, box[0], box[1], box[2], box[3], contour);
    }

    /** Format et texte dans un cadre donne (le moteur OpenCV fournit le sien). */
    public static Measures measureInBox(int[][] gray, int left, int top, int right, int bottom, boolean contour) {
        int w = Math.max(1, right - left + 1);
        int h = Math.max(1, bottom - top + 1);
        double ratio = Math.max(w, h) / (double) Math.min(w, h);
        Format format = formatOf(ratio);
        List<int[]> bands = textBands(gray, left, top, right, bottom, w >= h);
        int lines = (int) bands.stream().filter(b -> b[2] >= 30).count();
        boolean mrz = mrz(bands, w >= h ? h : w);
        return new Measures(left, top, right, bottom, contour, Math.round(ratio * 1000) / 1000.0, format, mrz, lines);
    }

    static Format formatOf(double ratio) {
        double toId1 = Math.abs(ratio - ID1_RATIO) / ID1_RATIO;
        double toId3 = Math.abs(ratio - ID3_RATIO) / ID3_RATIO;
        if (Math.min(toId1, toId3) > 0.07) {
            return Format.UNKNOWN;
        }
        return toId1 <= toId3 ? Format.ID1 : Format.ID3;
    }

    // ------------------------------------------------------------------ image

    /** Image en gris, plus grand cote ramene a 900 px. */
    public static int[][] gray(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        double scale = Math.min(1.0, WORK_SIZE / (double) Math.max(width, height));
        int w = Math.max(1, (int) Math.round(width * scale));
        int h = Math.max(1, (int) Math.round(height * scale));
        BufferedImage small = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, w, h, null);
        g.dispose();
        int[][] gray = new int[h][w];
        var raster = small.getRaster();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                gray[y][x] = raster.getSample(x, y, 0);
            }
        }
        return gray;
    }

    // ------------------------------------------------------------------ contour

    /**
     * Cadre de la carte : premiere et derniere ligne (et colonne) ou un bord droit occupe au moins
     * 35 % de la largeur (ou de la hauteur). Nul si l'on ne trouve pas les quatre bords, ou si le
     * cadre couvre moins de 15 % de l'image.
     */
    static int[] findCardBox(int[][] gray) {
        int h = gray.length;
        int w = gray[0].length;
        if (w < 40 || h < 40) {
            return null;
        }
        int[] rows = new int[h];
        int[] cols = new int[w];
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int gx = gray[y][x + 1] - gray[y][x - 1];
                int gy = gray[y + 1][x] - gray[y - 1][x];
                if (Math.abs(gy) >= EDGE_THRESHOLD && Math.abs(gy) > Math.abs(gx)) {
                    rows[y]++;
                }
                if (Math.abs(gx) >= EDGE_THRESHOLD && Math.abs(gx) > Math.abs(gy)) {
                    cols[x]++;
                }
            }
        }
        int top = firstAbove(rows, 0.35 * w, 2, h / 2, 1);
        int bottom = firstAbove(rows, 0.35 * w, h - 3, h / 2, -1);
        int left = firstAbove(cols, 0.35 * h, 2, w / 2, 1);
        int right = firstAbove(cols, 0.35 * h, w - 3, w / 2, -1);
        if (top < 0 || bottom < 0 || left < 0 || right < 0 || bottom - top < h * 0.2 || right - left < w * 0.2) {
            return null;
        }
        if ((bottom - top) * (double) (right - left) < 0.15 * w * h) {
            return null;
        }
        return new int[]{left, top, right, bottom};
    }

    private static int firstAbove(int[] profile, double threshold, int from, int to, int step) {
        for (int i = from; step > 0 ? i < to : i > to; i += step) {
            // Un bord peut s'etaler sur deux lignes voisines.
            int value = profile[i] + Math.max(profile[Math.max(0, i - 1)], profile[Math.min(profile.length - 1, i + 1)]) / 2;
            if (value >= threshold) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ texte

    /**
     * Bandes de texte dans le cadre, le long du grand cote : {debut, fin, etendue en % du cote}.
     * Une ligne de texte compte beaucoup de transitions de luminosite.
     */
    static List<int[]> textBands(int[][] gray, int left, int top, int right, int bottom, boolean landscape) {
        int along = landscape ? right - left + 1 : bottom - top + 1;
        int across = landscape ? bottom - top + 1 : right - left + 1;
        boolean[] active = new boolean[across];
        int[] spans = new int[across];
        for (int i = 0; i < across; i++) {
            int count = 0;
            int first = -1;
            int last = -1;
            for (int j = 1; j < along; j++) {
                int a = landscape ? gray[top + i][left + j - 1] : gray[top + j - 1][left + i];
                int b = landscape ? gray[top + i][left + j] : gray[top + j][left + i];
                if (Math.abs(a - b) >= TRANSITION_THRESHOLD) {
                    count++;
                    if (first < 0) {
                        first = j;
                    }
                    last = j;
                }
            }
            active[i] = count >= Math.max(6, along * 0.06);
            spans[i] = first < 0 ? 0 : (int) Math.round(100.0 * (last - first) / along);
        }
        List<int[]> bands = new ArrayList<>();
        int start = -1;
        int maxSpan = 0;
        for (int i = 0; i <= across; i++) {
            boolean on = i < across && active[i];
            if (on) {
                if (start < 0) {
                    start = i;
                    maxSpan = 0;
                }
                maxSpan = Math.max(maxSpan, spans[i]);
            } else if (start >= 0) {
                int thickness = i - start;
                if (thickness >= Math.max(2, across * 0.012) && thickness <= across * 0.12) {
                    bands.add(new int[]{start, i - 1, maxSpan});
                }
                start = -1;
            }
        }
        return bands;
    }

    /** Au moins deux bandes larges (70 % du cote) et serrees dans le dernier tiers du petit cote. */
    static boolean mrz(List<int[]> bands, int across) {
        List<int[]> low = bands.stream().filter(b -> b[0] >= across * 0.6 && b[2] >= 70).toList();
        if (low.size() < 2) {
            return false;
        }
        for (int i = 1; i < low.size(); i++) {
            int gap = low.get(i)[0] - low.get(i - 1)[1];
            if (gap <= across * 0.08) {
                return true;
            }
        }
        return false;
    }
}
