package com.sni.bokaticowork.features.document.kyc.service.support.identity;

import java.util.List;

/**
 * Lot KG9 : ce que l'analyse dit d'un depot cense etre une piece d'identite. Avertissement
 * seulement, jamais un refus : c'est le reviseur qui decide.
 *
 * @param verdict      LIKELY_IDENTITY, UNCERTAIN, NOT_IDENTITY ; NOT_CHECKED si rien n'a ete analyse
 * @param engine       moteur employe (JAVA ou OPENCV)
 * @param format       ID1 (carte 85,6 x 54 mm), ID3 (page de passeport 125 x 88 mm) ou UNKNOWN
 * @param ratio        rapport grand cote / petit cote de la piece reperee
 * @param contourFound contour de la piece repere dans l'image (sinon, image supposee recadree)
 * @param mrzDetected  bande MRZ (lignes de texte serrees sur toute la largeur, en bas) reperee
 * @param textLines    lignes de texte reperees sur la piece
 * @param faceDetected visage repere ; nul avec le moteur Java, qui ne le cherche pas
 * @param warnings     avertissements pour le reviseur et le deposant
 */
public record KycIdentityReport(Verdict verdict, Engine engine, Format format, Double ratio, boolean contourFound,
                                boolean mrzDetected, int textLines, Boolean faceDetected, List<String> warnings) {

    public enum Verdict { LIKELY_IDENTITY, UNCERTAIN, NOT_IDENTITY, NOT_CHECKED }

    public enum Engine { JAVA, OPENCV }

    public enum Format { ID1, ID3, UNKNOWN }

    public static KycIdentityReport notChecked(Engine engine) {
        return new KycIdentityReport(Verdict.NOT_CHECKED, engine, Format.UNKNOWN, null, false, false, 0, null, List.of());
    }
}
