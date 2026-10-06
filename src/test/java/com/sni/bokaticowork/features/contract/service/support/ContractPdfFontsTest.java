package com.sni.bokaticowork.features.contract.service.support;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La police des contrats est livree avec l'application, pas telechargee au rendu.
 *
 * <p>Le gabarit la demandait par un {@code <link>} vers Google Fonts. Un navigateur la telecharge ;
 * le moteur PDF, non · il retombait silencieusement sur une police systeme, et le document obtenu
 * n'etait pas celui qui avait ete dessine. Le lien restait par ailleurs un appel reseau a chaque
 * generation, qui echoue sur une instance sans resolution DNS sortante.</p>
 */
class ContractPdfFontsTest {

    private ContractPdfFonts fonts;

    @BeforeEach
    void setUp() {
        fonts = new ContractPdfFonts();
        fonts.load();
    }

    @Test
    @DisplayName("Les quatre graisses sont presentes dans le livrable")
    void allFourFacesAreShipped() {
        // Normale, grasse, italique et grasse italique · un contrat les emploie toutes les quatre.
        assertThat(fonts.availableFaces()).isEqualTo(4);
    }

    @ParameterizedTest
    @DisplayName("Chaque fichier de police est un vrai TrueType")
    @ValueSource(strings = {"Spectral-Regular", "Spectral-Bold", "Spectral-Italic", "Spectral-BoldItalic"})
    void eachFileIsARealTrueTypeFont(String nom) throws Exception {
        byte[] octets;
        try (var in = getClass().getResourceAsStream("/fonts/" + nom + ".ttf")) {
            assertThat(in).as("fichier /fonts/%s.ttf", nom).isNotNull();
            octets = in.readAllBytes();
        }

        assertThat(octets.length).isGreaterThan(100_000);
        // Signature TrueType · 0x00010000. Un fichier HTML d'erreur telecharge a la place ne la
        // porterait pas, et la police tomberait silencieusement.
        assertThat(new byte[]{octets[0], octets[1], octets[2], octets[3]})
                .containsExactly(0x00, 0x01, 0x00, 0x00);
    }

    @Test
    @DisplayName("La licence accompagne les fichiers · la SIL OFL l'exige")
    void theLicenceTravelsWithTheFiles() throws Exception {
        try (var in = getClass().getResourceAsStream("/fonts/OFL.txt")) {
            assertThat(in).as("fichier /fonts/OFL.txt").isNotNull();
            String texte = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(texte).contains("SIL OPEN FONT LICENSE");
        }
    }

    @Test
    @DisplayName("La police est reellement embarquee dans le PDF produit")
    void theFontIsActuallyEmbeddedInTheProducedPdf() throws Exception {
        byte[] pdf = renderPdf("""
                <html><head><style>
                @page { size: A4; margin: 20mm; }
                body { font-family: "Spectral", serif; }
                .gras { font-weight: 700; }
                .penche { font-style: italic; }
                </style></head><body>
                <p>Contrat de prestation.</p>
                <p class="gras">Montant de la prestation.</p>
                <p class="penche">Fait a Pointe-Noire.</p>
                </body></html>
                """);

        String noms = baseFonts(pdf);
        assertThat(noms).contains("Spectral-Regular").contains("Spectral-Bold").contains("Spectral-Italic");
        // Le fichier de police lui-meme, et non une simple reference · sans FontFile2 le lecteur
        // substituerait sa propre police.
        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).contains("/FontFile2");
    }

    @Test
    @DisplayName("Seuls les glyphes employes sont embarques · le PDF ne gonfle pas")
    void onlyTheUsedGlyphsAreEmbedded() throws Exception {
        byte[] pdf = renderPdf("""
                <html><head><style>
                @page { size: A4; margin: 20mm; }
                body { font-family: "Spectral", serif; }
                </style></head><body><p>Contrat.</p></body></html>
                """);

        // Les quatre fichiers pesent plus d'un mega-octet a eux seuls. Un document qui n'emploie
        // qu'une graisse et quelques lettres doit rester tres en dessous.
        assertThat(pdf.length).isLessThan(120_000);
    }

    @Test
    @DisplayName("Sans police chargee, le rendu reste possible · un contrat prime sur son apparence")
    void withoutFontsTheRenderStillWorks() throws Exception {
        ContractPdfFonts vides = new ContractPdfFonts();
        // load() n'est pas appele · aucune graisse disponible.
        assertThat(vides.availableFaces()).isZero();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        vides.register(builder);
        builder.withHtmlContent("<html><body><p>Contrat.</p></body></html>", null);
        builder.toStream(out);
        builder.run();

        assertThat(out.toByteArray()).isNotEmpty();
    }

    private byte[] renderPdf(String html) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        fonts.register(builder);
        builder.withHtmlContent(html, null);
        builder.toStream(out);
        builder.run();
        return out.toByteArray();
    }

    private String baseFonts(byte[] pdf) {
        Matcher matcher = Pattern.compile("/BaseFont\\s*/([A-Za-z0-9+,\\-]+)")
                .matcher(new String(pdf, StandardCharsets.ISO_8859_1));
        StringBuilder noms = new StringBuilder();
        while (matcher.find()) {
            noms.append(matcher.group(1)).append(' ');
        }
        return noms.toString();
    }
}
