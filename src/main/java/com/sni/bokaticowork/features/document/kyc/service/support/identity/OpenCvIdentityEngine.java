package com.sni.bokaticowork.features.document.kyc.service.support.identity;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_objdetect;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Lot KG9 : moteur OpenCV (facultatif, {@code app.kyc.identity.engine=OPENCV}). Contour de la piece
 * par Canny et approximation polygonale (plus robuste qu'en Java sur un fond charge), visage par la
 * cascade de Haar {@code haarcascade_frontalface_default} fournie par le paquet OpenCV ; texte et MRZ
 * repris du moteur Java dans le cadre trouve.
 *
 * <p>Classe ordinaire, creee a la premiere analyse : rien de natif n'est charge au demarrage ni par
 * l'entrainement du cache de demarrage. Un echec de chargement leve une erreur que le service
 * attrape pour revenir au moteur Java.</p>
 */
public final class OpenCvIdentityEngine {

    private static final String CASCADE =
            "/org/bytedeco/opencv/linux-x86_64/share/opencv4/haarcascades/haarcascade_frontalface_default.xml";

    private final CascadeClassifier faces;

    /** Charge les bibliotheques natives et la cascade ; peut lever une {@link LinkageError}. */
    public OpenCvIdentityEngine() throws IOException {
        Loader.load(opencv_core.class);
        Loader.load(opencv_imgproc.class);
        Loader.load(opencv_objdetect.class);
        Path cascade = Files.createTempFile("bokati-face-", ".xml");
        cascade.toFile().deleteOnExit();
        try (InputStream in = OpenCvIdentityEngine.class.getResourceAsStream(CASCADE)) {
            if (in == null) {
                throw new IOException("Cascade de visage introuvable dans le paquet OpenCV");
            }
            Files.copy(in, cascade, StandardCopyOption.REPLACE_EXISTING);
        }
        CascadeClassifier classifier = new CascadeClassifier(cascade.toString());
        if (classifier.empty()) {
            throw new IOException("Cascade de visage illisible");
        }
        this.faces = classifier;
    }

    public synchronized KycIdentityReport analyse(java.awt.image.BufferedImage source, boolean passport, boolean back) {
        int[][] gray = JavaIdentityEngine.gray(source);
        int h = gray.length;
        int w = gray[0].length;
        byte[] bytes = new byte[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bytes[y * w + x] = (byte) gray[y][x];
            }
        }
        try (Mat mat = new Mat(h, w, opencv_core.CV_8UC1)) {
            mat.data().put(bytes);
            int[] box = cardBox(mat, w, h);
            JavaIdentityEngine.Measures measures = box == null
                    ? JavaIdentityEngine.measure(gray)
                    : JavaIdentityEngine.measureInBox(gray, box[0], box[1], box[2], box[3], true);
            Boolean face = back ? null : hasFace(mat, w, h);
            return KycIdentityVerdicts.decide(KycIdentityReport.Engine.OPENCV, measures, face, passport, back);
        }
    }

    /** Plus grand quadrilatere convexe couvrant au moins 15 % de l'image : {gauche, haut, droite, bas}. */
    private static int[] cardBox(Mat gray, int w, int h) {
        try (Mat blurred = new Mat(); Mat edges = new Mat(); MatVector contours = new MatVector(); Mat hierarchy = new Mat()) {
            opencv_imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
            opencv_imgproc.Canny(blurred, edges, 50, 150);
            opencv_imgproc.dilate(edges, edges, new Mat());
            opencv_imgproc.findContours(edges, contours, hierarchy, opencv_imgproc.RETR_EXTERNAL, opencv_imgproc.CHAIN_APPROX_SIMPLE);
            double best = 0.15 * w * h;
            int[] box = null;
            for (long i = 0; i < contours.size(); i++) {
                Mat contour = contours.get(i);
                double area = opencv_imgproc.contourArea(contour);
                if (area < best) {
                    continue;
                }
                try (Mat approx = new Mat()) {
                    opencv_imgproc.approxPolyDP(contour, approx, 0.02 * opencv_imgproc.arcLength(contour, true), true);
                    if (approx.rows() == 4 && opencv_imgproc.isContourConvex(approx)) {
                        Rect rect = opencv_imgproc.boundingRect(approx);
                        best = area;
                        box = new int[]{rect.x(), rect.y(), Math.min(w - 1, rect.x() + rect.width() - 1),
                                Math.min(h - 1, rect.y() + rect.height() - 1)};
                    }
                }
            }
            return box;
        }
    }

    private boolean hasFace(Mat gray, int w, int h) {
        int minSide = Math.max(24, (int) (Math.min(w, h) * 0.08));
        try (RectVector found = new RectVector()) {
            faces.detectMultiScale(gray, found, 1.1, 4, 0, new Size(minSide, minSide), new Size());
            return found.size() > 0;
        }
    }
}
