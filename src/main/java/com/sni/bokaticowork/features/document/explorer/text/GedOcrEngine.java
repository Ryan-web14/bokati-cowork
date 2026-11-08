package com.sni.bokaticowork.features.document.explorer.text;

import java.awt.image.BufferedImage;

/** Reconnaissance de caracteres sur une image de page. */
public interface GedOcrEngine {

    /**
     * @return le texte lu, vide si la page n'en contient pas
     * @throws OcrUnavailableException si le moteur n'est pas installe sur le serveur
     */
    String read(BufferedImage page);

    /** Le moteur manque (bibliotheque native ou donnees de langue absentes) : inutile d'insister. */
    class OcrUnavailableException extends RuntimeException {
        public OcrUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
