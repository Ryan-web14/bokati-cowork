package com.sni.bokaticowork.core.utils.pdf;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Polices embarquees dans les PDF (Spectral, quatre graisses).
 *
 * <p>Reprise de la police des contrats, pour que tout rendu passe par le meme enregistrement.
 * Une police declaree n'est lue et embarquee que si un gabarit la demande ({@code font-family:
 * "Spectral"}) · les pieces en Helvetica ne changent donc pas. Le sous-ensemble est actif, seuls
 * les glyphes employes sont embarques.</p>
 *
 * <p>Spectral est sous licence SIL Open Font License 1.1 ({@code resources/fonts/OFL.txt}).</p>
 */
@Slf4j
public class PdfFonts {

    public static final String FAMILY = "Spectral";

    private static final List<Face> FACES = List.of(
            new Face("/fonts/Spectral-Regular.ttf", 400, FontStyle.NORMAL),
            new Face("/fonts/Spectral-Bold.ttf", 700, FontStyle.NORMAL),
            new Face("/fonts/Spectral-Italic.ttf", 400, FontStyle.ITALIC),
            new Face("/fonts/Spectral-BoldItalic.ttf", 700, FontStyle.ITALIC));

    private static volatile PdfFonts shared;

    private final Map<Face, byte[]> chargees = new LinkedHashMap<>();

    /** Instance commune, chargee au premier rendu. */
    public static PdfFonts shared() {
        PdfFonts local = shared;
        if (local == null) {
            synchronized (PdfFonts.class) {
                local = shared;
                if (local == null) {
                    local = new PdfFonts();
                    local.load();
                    shared = local;
                }
            }
        }
        return local;
    }

    /** Lit les fichiers de police · publique pour un rendu hors contexte Spring. */
    public void load() {
        for (Face face : FACES) {
            try (InputStream in = getClass().getResourceAsStream(face.resource())) {
                if (in == null) {
                    log.warn("Police absente · {} · le PDF retombera sur une police systeme", face.resource());
                    continue;
                }
                chargees.put(face, in.readAllBytes());
            } catch (IOException ex) {
                // Une police illisible ne doit pas empecher une piece d'etre produite.
                log.warn("Police illisible · {} · {}", face.resource(), ex.getMessage());
            }
        }
        log.info("Polices PDF chargees · {} sur {}", chargees.size(), FACES.size());
    }

    /** Declare les graisses disponibles au moteur, avant le rendu. */
    public void register(PdfRendererBuilder builder) {
        chargees.forEach((face, octets) -> builder.useFont(
                () -> new ByteArrayInputStream(octets),
                FAMILY,
                face.weight(),
                face.style(),
                true));
    }

    /** Nombre de graisses effectivement disponibles. */
    public int availableFaces() {
        return chargees.size();
    }

    private record Face(String resource, int weight, FontStyle style) {
    }
}
