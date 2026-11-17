package com.sni.bokaticowork.features.document.kyc.service.support.identity;

import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Locale;

/**
 * Lot KG9 : analyse « est-ce bien une piece d'identite ? » d'une piece KYC deposee.
 *
 * <ul>
 *   <li>{@code app.kyc.identity.enabled} (vrai) : analyse active ;</li>
 *   <li>{@code app.kyc.identity.engine} : {@code JAVA} (defaut, sans bibliotheque native) ou
 *   {@code OPENCV} (contour plus robuste et visage ; charge a la premiere analyse ; si le chargement
 *   echoue, retour au moteur Java jusqu'au prochain demarrage, un seul message) ;</li>
 *   <li>{@code app.kyc.identity.document-types} : morceaux de code des types de pieces d'identite
 *   analyses (par defaut cartes, passeports, permis, titres de sejour, pieces de representant).</li>
 * </ul>
 */
@Slf4j
@Service
public class KycIdentityAnalysisService {

    static final String ENABLED = "app.kyc.identity.enabled";
    static final String ENGINE = "app.kyc.identity.engine";
    static final String TYPES = "app.kyc.identity.document-types";
    static final String DEFAULT_TYPES = "ID_CARD,NATIONAL_ID,PASSPORT,DRIVERS_LICENSE,RESIDENCE_PERMIT,REPRESENTATIVE_ID";

    private PlatformSettings settings;
    private volatile OpenCvIdentityEngine openCv;
    private volatile boolean openCvFailed;

    @Autowired(required = false)
    void setSettings(PlatformSettings settings) {
        this.settings = settings;
    }

    /** La piece de ce type est-elle une piece d'identite a analyser ? */
    public boolean applies(String documentTypeCode) {
        if (!StringUtils.hasText(documentTypeCode) || (settings != null && !settings.bool(ENABLED, true))) {
            return false;
        }
        String code = documentTypeCode.toUpperCase(Locale.ROOT);
        String types = settings == null ? DEFAULT_TYPES : settings.text(TYPES, DEFAULT_TYPES);
        return Arrays.stream((types == null ? DEFAULT_TYPES : types).split(","))
                .map(String::trim).filter(StringUtils::hasText)
                .anyMatch(part -> code.contains(part.toUpperCase(Locale.ROOT)));
    }

    public KycIdentityReport analyse(BufferedImage image, String documentTypeCode, boolean back) {
        boolean passport = documentTypeCode != null && documentTypeCode.toUpperCase(Locale.ROOT).contains("PASSPORT");
        if (wantsOpenCv()) {
            OpenCvIdentityEngine engine = openCv();
            if (engine != null) {
                try {
                    return engine.analyse(image, passport, back);
                } catch (RuntimeException | LinkageError ex) {
                    log.warn("Analyse OpenCV impossible, moteur Java employe : {}", ex.getMessage());
                }
            }
        }
        return JavaIdentityEngine.analyse(image, passport, back);
    }

    /** Moteur effectivement employe pour la prochaine analyse (pour l'ecran d'administration). */
    public KycIdentityReport.Engine activeEngine() {
        return wantsOpenCv() && !openCvFailed ? KycIdentityReport.Engine.OPENCV : KycIdentityReport.Engine.JAVA;
    }

    private boolean wantsOpenCv() {
        String engine = settings == null ? "JAVA" : settings.text(ENGINE, "JAVA");
        return engine != null && "OPENCV".equalsIgnoreCase(engine.trim());
    }

    private OpenCvIdentityEngine openCv() {
        if (openCvFailed) {
            return null;
        }
        if (openCv == null) {
            synchronized (this) {
                if (openCv == null && !openCvFailed) {
                    try {
                        openCv = new OpenCvIdentityEngine();
                        log.info("Moteur OpenCV charge pour l'analyse des pieces d'identite");
                    } catch (Exception | LinkageError ex) {
                        openCvFailed = true;
                        log.warn("OpenCV indisponible ({}) : moteur Java employe jusqu'au prochain demarrage", ex.toString());
                    }
                }
            }
        }
        return openCv;
    }
}
