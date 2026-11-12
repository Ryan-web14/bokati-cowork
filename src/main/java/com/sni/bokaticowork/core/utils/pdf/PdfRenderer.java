package com.sni.bokaticowork.core.utils.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Rendu unique HTML vers PDF · jsoup, puis W3CDom, puis OpenHtmlToPDF.
 *
 * <p>La meme chaine etait copiee dans quatorze services, avec de petites variantes (reglages de
 * sortie jsoup, polices declarees ou non, analyse XML stricte pour l'une d'elles). Un seul point
 * d'entree garantit que toutes les pieces sont produites de la meme facon et qu'un reglage (police,
 * URI de base) s'applique partout.</p>
 *
 * <p>La taille de page reste au CSS ({@code @page}) de chaque gabarit. Les traitements propres a
 * certaines pieces (filigrane, signature PAdES et scellement des factures, original et duplicata
 * de {@link PdfCopies}) s'appliquent aux octets produits ici.</p>
 *
 * <p>Classe statique · plusieurs services sont construits hors de Spring (tests de rendu, rendus
 * statiques) et n'ont pas a recevoir un composant de plus.</p>
 */
public final class PdfRenderer {

    private PdfRenderer() {
    }

    /** Rend le HTML en PDF, sans URI de base. */
    public static byte[] render(String html) {
        return render(html, null);
    }

    /**
     * Rend le HTML en PDF.
     *
     * @param baseUri URI de base des ressources relatives, ou {@code null} (les gabarits
     *                embarquent leurs images en {@code data:})
     * @throws PdfRenderException si le moteur echoue · l'appelant la traduit dans son propre message
     */
    public static byte[] render(String html, String baseUri) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            PdfFonts.shared().register(builder);
            builder.withW3cDocument(new W3CDom().fromJsoup(toXhtml(html)), baseUri);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new PdfRenderException(ex);
        }
    }

    /** Analyse tolerante du HTML, reglee pour une sortie XHTML (entites, UTF-8). */
    public static Document toXhtml(String html) {
        Document document = Jsoup.parse(html == null ? "" : html);
        document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset(StandardCharsets.UTF_8)
                .prettyPrint(false);
        return document;
    }

    /** Echec du moteur PDF · le message de la cause est repris pour le diagnostic. */
    public static class PdfRenderException extends RuntimeException {
        public PdfRenderException(Throwable cause) {
            super(cause.getMessage(), cause);
        }
    }
}
