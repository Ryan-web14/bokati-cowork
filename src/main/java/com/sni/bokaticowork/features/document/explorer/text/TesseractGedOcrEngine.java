package com.sni.bokaticowork.features.document.explorer.text;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.awt.image.BufferedImage;

/**
 * OCR par Tesseract, le moteur deja utilise pour les pieces KYC.
 *
 * <p>Tesseract n'est pas sur d'un fil a l'autre : une instance par page. La bibliotheque native
 * doit etre installee sur le serveur ({@code tesseract-ocr} et les langues {@code fra}, {@code eng}) ;
 * sans elle, la lecture leve {@link OcrUnavailableException} et seuls les textes deja numeriques
 * sont indexes.</p>
 */
@Component
public class TesseractGedOcrEngine implements GedOcrEngine {

    @Value("${app.ged.text.ocr-language:fra+eng}")
    private String language;

    @Value("${app.ged.text.ocr-data-path:}")
    private String dataPath;

    @Override
    public String read(BufferedImage page) {
        Tesseract tesseract = new Tesseract();
        if (StringUtils.hasText(dataPath)) {
            tesseract.setDatapath(dataPath);
        }
        if (StringUtils.hasText(language)) {
            tesseract.setLanguage(language);
        }
        // Une image en memoire n'a pas de resolution ; les pages PDF sont rendues a 300 dpi
        tesseract.setVariable("user_defined_dpi", "300");
        try {
            String text = tesseract.doOCR(page);
            return text == null ? "" : text;
        } catch (TesseractException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("tessdata")) {
                throw new OcrUnavailableException("Données de langue Tesseract introuvables", ex);
            }
            throw new IllegalStateException("OCR en échec : " + ex.getMessage(), ex);
        } catch (VirtualMachineError ex) {
            throw ex;
        } catch (Error ex) {
            // Bibliotheque native absente (UnsatisfiedLinkError) ou donnees de langue illisibles :
            // JNA le signale par une Error « Invalid memory access »
            throw new OcrUnavailableException("Tesseract n'est pas installé sur le serveur, ou ses langues manquent", ex);
        }
    }
}
