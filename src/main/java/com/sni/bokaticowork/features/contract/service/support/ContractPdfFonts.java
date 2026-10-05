package com.sni.bokaticowork.features.contract.service.support;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La police des contrats, embarquee dans le PDF.
 *
 * <p>Les gabarits demandent Spectral par un {@code <link>} vers Google Fonts. Un navigateur la
 * telecharge ; le moteur PDF, non · il retombait silencieusement sur Times, et la mise en page
 * obtenue n'etait pas celle qui avait ete dessinee. Le lien restait par ailleurs un appel reseau
 * au moment du rendu : il allonge chaque generation et echoue sur une instance sans resolution DNS
 * sortante, pour un resultat qui de toute facon n'etait pas utilise.</p>
 *
 * <p>Les fichiers sont donc livres avec l'application et declares au moteur. Quatre graisses
 * seulement · normale, grasse, italique et grasse italique. Les gabarits citent aussi 300, 500 et
 * 600 : le moteur prend la plus proche, ce qui vaut mieux que de porter huit fichiers dans chaque
 * document. Le sous-ensemble est actif, donc seuls les glyphes reellement employes sont
 * embarques.</p>
 *
 * <p>Spectral est sous licence SIL Open Font License 1.1, qui autorise l'embarquement. Le texte de
 * la licence accompagne les fichiers dans {@code resources/fonts/OFL.txt} · c'est une obligation de
 * cette licence, pas une precaution.</p>
 */
@Slf4j
@Component
public class ContractPdfFonts {

    public static final String FAMILY = "Spectral";

    private static final List<Face> FACES = List.of(
            new Face("/fonts/Spectral-Regular.ttf", 400, FontStyle.NORMAL),
            new Face("/fonts/Spectral-Bold.ttf", 700, FontStyle.NORMAL),
            new Face("/fonts/Spectral-Italic.ttf", 400, FontStyle.ITALIC),
            new Face("/fonts/Spectral-BoldItalic.ttf", 700, FontStyle.ITALIC));

    /** Lues une fois · un contrat se genere en lot, pas une fois par jour. */
    private final Map<Face, byte[]> chargees = new LinkedHashMap<>();

    /** Publique · un rendu hors contexte Spring (apercu, test) doit pouvoir les charger. */
    @PostConstruct
    public void load() {
        for (Face face : FACES) {
            try (InputStream in = getClass().getResourceAsStream(face.resource())) {
                if (in == null) {
                    log.warn("Police de contrat absente · {} · le PDF retombera sur une police systeme",
                            face.resource());
                    continue;
                }
                chargees.put(face, in.readAllBytes());
            } catch (IOException ex) {
                // Une police illisible ne doit pas empecher un contrat d'etre genere · le document
                // reste juste, seule son apparence change.
                log.warn("Police de contrat illisible · {} · {}", face.resource(), ex.getMessage());
            }
        }
        log.info("Polices de contrat chargees · {} sur {}", chargees.size(), FACES.size());
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

    /** Visible pour les tests · combien de graisses sont effectivement disponibles. */
    public int availableFaces() {
        return chargees.size();
    }

    private record Face(String resource, int weight, FontStyle style) {
    }
}
