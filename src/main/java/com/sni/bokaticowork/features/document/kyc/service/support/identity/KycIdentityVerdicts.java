package com.sni.bokaticowork.features.document.kyc.service.support.identity;

import com.sni.bokaticowork.features.document.kyc.service.support.identity.KycIdentityReport.Engine;
import com.sni.bokaticowork.features.document.kyc.service.support.identity.KycIdentityReport.Format;
import com.sni.bokaticowork.features.document.kyc.service.support.identity.KycIdentityReport.Verdict;

import java.util.ArrayList;
import java.util.List;

/** Lot KG9 : verdict et avertissements, communs aux deux moteurs. */
public final class KycIdentityVerdicts {

    private KycIdentityVerdicts() {
    }

    /**
     * @param face     visage repere (OpenCV), nul si non cherche (Java)
     * @param passport la piece attendue est un passeport (format ID3 et MRZ au recto)
     * @param back     verso de la piece (pas de visage attendu)
     */
    public static KycIdentityReport decide(Engine engine, JavaIdentityEngine.Measures m, Boolean face, boolean passport, boolean back) {
        boolean format = m.format() != Format.UNKNOWN;
        boolean text = m.textLines() >= 3 || m.mrz();
        boolean faceFound = Boolean.TRUE.equals(face);

        Verdict verdict;
        if (format && (m.mrz() || faceFound || (text && m.contourFound()))) {
            verdict = Verdict.LIKELY_IDENTITY;
        } else if (!format && !text && !faceFound) {
            verdict = Verdict.NOT_IDENTITY;
        } else {
            verdict = Verdict.UNCERTAIN;
        }

        List<String> warnings = new ArrayList<>();
        if (verdict == Verdict.NOT_IDENTITY) {
            warnings.add("Le fichier ne ressemble pas à une pièce d'identité (ni format de carte ou de passeport, ni texte) : vérifiez la pièce.");
        } else if (!format) {
            warnings.add("Format de la pièce non reconnu : cadrez la carte ou la page du passeport entière, à plat, sur un fond uni.");
        } else if (verdict == Verdict.UNCERTAIN && !text) {
            warnings.add("Peu de texte repéré sur la pièce : elle est peut-être floue, partielle ou à l'envers.");
        }
        if (passport && !back && format && !m.mrz()) {
            warnings.add("Bande MRZ (lignes « <<< ») non repérée en bas de la page du passeport : vérifiez qu'elle est entière et lisible.");
        }
        if (engine == Engine.OPENCV && !back && face != null && !face) {
            warnings.add("Aucun visage repéré sur la pièce : vérifiez que la photo du titulaire est visible.");
        }
        return new KycIdentityReport(verdict, engine, m.format(), m.ratio(), m.contourFound(), m.mrz(), m.textLines(), face,
                List.copyOf(warnings));
    }
}
