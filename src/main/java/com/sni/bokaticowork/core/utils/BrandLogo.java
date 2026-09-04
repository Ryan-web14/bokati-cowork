package com.sni.bokaticowork.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Base64;

/**
 * Logo de l'exploitant, encode pour etre embarque dans un PDF.
 *
 * <p>Le chargement etait ecrit deux fois · facture et recu de paiement · et absent des rapports
 * comme des contrats, qui sortaient donc sans logo. Un composant unique evite que la troisieme
 * copie diverge, et rend le logo disponible partout.
 *
 * <p>Le fichier est lu une fois puis garde en memoire : il pese une soixantaine de kilooctets et
 * ne change pas en cours d'execution. Le relire a chaque PDF n'apporterait rien.
 */
@Slf4j
@Component
public class BrandLogo {

    /**
     * Le fichier d'origine est <b>blanc sur transparent</b> · verifie au pixel : 28 526 points
     * opaques, tous clairs, aucun sombre. Il etait concu pour le bandeau sombre des documents.
     * Depuis que ces bandeaux ont cede la place a de l'encre sur blanc, ce logo y est invisible.
     * Les pieces imprimees prennent donc la silhouette encre, tiree du meme trace.
     */
    private static final String PATH = "/static/images/logo-ink.png";

    /** {@code null} tant que la lecture n'a pas eu lieu · l'absence de logo est un cas normal. */
    private volatile String cached;
    private volatile boolean loaded;

    /**
     * Logo en base64, sans prefixe {@code data:}, ou {@code null} s'il est introuvable.
     *
     * <p>Un logo manquant ne doit jamais empecher l'emission d'une piece : les gabarits retombent
     * sur le nom en toutes lettres.
     */
    public String base64() {
        if (loaded) {
            return cached;
        }
        synchronized (this) {
            if (!loaded) {
                cached = read();
                loaded = true;
            }
        }
        return cached;
    }

    private String read() {
        try (InputStream is = getClass().getResourceAsStream(PATH)) {
            if (is == null) {
                log.warn("Logo introuvable dans les ressources ({}) · les documents sortiront sans logo", PATH);
                return null;
            }
            return Base64.getEncoder().encodeToString(is.readAllBytes());
        } catch (Exception ex) {
            log.warn("Logo illisible ({}) : {} · les documents sortiront sans logo", PATH, ex.getMessage());
            return null;
        }
    }
}
