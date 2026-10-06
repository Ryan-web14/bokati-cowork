package com.sni.bokaticowork.templates;

import com.sni.bokaticowork.features.contract.dto.request.GenerateContractRequest;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le contrat d'abonnement se rend, en francais, et dit le montant paye.
 *
 * <p>Il affichait un vidage brut de la table des variables sous le titre « Parametres de
 * l'offre » · des cles techniques anglaises ({@code billingCycle}, {@code sourceType},
 * {@code contractPolicyCode}) et leurs valeurs ({@code MONTHLY}, {@code SUBSCRIPTION_ADDON}) dans
 * une piece juridique francaise. Et le montant reellement paye n'y figurait nulle part a sa place :
 * la ligne « Montant souscrit » lisait {@code passPrice}, une variable que les trois flux
 * d'abonnement ne renseignent jamais, tandis que {@code totalAmount} n'apparaissait que dans le
 * vidage.</p>
 */
class SubscriptionContractRenderTest {

    private static final String TEMPLATE = System.getProperty(
            "contract.template", "contracts/subscription-pass-non-refundable");

    private final SpringTemplateEngine engine = engine();

    /** Les variables telles que ContractGenerationServiceImpl les construit pour un abonnement. */
    private Map<String, String> subscriptionVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("clientName", "SARL Mbote");
        vars.put("clientCode", "CUS-000001");
        vars.put("operatorName", "ETS ELLE A OSE");
        vars.put("operatorRccm", "CG-PNR-01-2017-A11-00472");
        vars.put("operatorAddress", "84 Boulevard du General Charles de Gaulle, centre-ville, Pointe-Noire");
        vars.put("startDate", "1 octobre 2026");
        vars.put("endDate", "31 octobre 2026");
        vars.put("effectiveDate", "1 octobre 2026");
        vars.put("placeOfSigning", "Pointe-Noire");
        vars.put("signingDate", "5 octobre 2026");
        vars.put("sourceType", "SUBSCRIPTION");
        vars.put("subscriptionNumber", "SUB-000042");
        vars.put("planCode", "FLEX");
        vars.put("planVersion", "3");
        vars.put("billingCycle", "MONTHLY");
        vars.put("currency", "XAF");
        vars.put("totalAmount", "180000");
        vars.put("contractPolicyCode", "SUBSCRIPTION_PASS_NON_REFUNDABLE");
        vars.put("operatorSignatureName", "ETS ELLE A OSE");
        vars.put("operatorSignedAt", "5 octobre 2026");
        // Ce que frenchifyLabels ajoute.
        vars.put("billingCycleLabel", "tous les mois");
        vars.put("objectLabel", "abonnement");
        vars.put("amountLabel", "180 000 FCFA");
        return vars;
    }

    @Test
    @DisplayName("Le montant paye figure a sa place, en francais et avec la devise nommee")
    void theAmountPaidIsStated() {
        String html = render(subscriptionVariables());

        // Le montant vit dans la phrase, plus dans un tableau qui la repetait mot pour mot.
        assertThat(normalise(html))
                .contains("Le prix de la présente prestation")
                .contains("180 000 FCFA");
        // La devise ISO n'a rien a faire sur un contrat congolais.
        assertThat(html).doesNotContain(">XAF<");
    }

    @Test
    @DisplayName("Plus aucune cle technique anglaise dans le document rendu")
    void noTechnicalKeyLeaksIntoTheDocument() {
        String html = render(subscriptionVariables());

        // C'etait exactement le contenu de l'article « Parametres de l'offre ».
        assertThat(html)
                .doesNotContain("billingCycle")
                .doesNotContain("sourceType")
                .doesNotContain("contractPolicyCode")
                .doesNotContain("totalAmount")
                .doesNotContain("MONTHLY")
                .doesNotContain("SUBSCRIPTION_PASS_NON_REFUNDABLE")
                .doesNotContain("operatorSignatureName");
    }

    @Test
    @DisplayName("Le rappel des tarifs de l'espace a disparu")
    void theTariffReminderIsGone() {
        String html = render(subscriptionVariables());

        // Il annoncait les tarifs standards du poste fixe, du poste nomade et de la salle de
        // reunion · sans rapport avec la prestation souscrite, et de nature a contredire le prix
        // convenu.
        assertThat(normalise(html))
                .doesNotContain("Pour memoire")
                .doesNotContain("tarifs standards")
                .doesNotContain("200 000")
                .doesNotContain("150 000")
                .doesNotContain("21 000");
    }

    @Test
    @DisplayName("Les definitions des termes importants sont presentes")
    void theDefinitionsArePresent() {
        String html = render(subscriptionVariables());

        assertThat(html).contains("Definitions".replace("Definitions", "Définitions"));
        assertThat(normalise(html))
                .contains("Le Prestataire")
                .contains("Le Client")
                .contains("non remboursable");
    }

    @Test
    @DisplayName("La periodicite et l'objet sont en francais")
    void thePeriodicityAndObjectAreInFrench() {
        String html = normalise(render(subscriptionVariables()));

        assertThat(html).contains("tous les mois").contains("abonnement");
    }

    @Test
    @DisplayName("Un contrat de pass porte son montant, qu'il n'avait pas du tout")
    void aPassContractCarriesItsAmount() {
        Map<String, String> vars = new LinkedHashMap<>(subscriptionVariables());
        vars.remove("subscriptionNumber");
        vars.remove("billingCycle");
        vars.remove("billingCycleLabel");
        vars.put("sourceType", "PASS");
        vars.put("objectLabel", "pass");
        vars.put("passNumber", "PSS-000007");
        vars.put("passType", "DAY_PASS");
        vars.put("passTypeLabel", "pass journalier");
        vars.put("amountLabel", "15 000 FCFA");

        String html = normalise(render(vars));

        assertThat(html).contains("15 000 FCFA").contains("PSS-000007").contains("pass journalier");
        assertThat(html).doesNotContain("DAY_PASS");
    }

    @Test
    @DisplayName("Une option affiche son detail · quantite et prix unitaire")
    void anAddonShowsItsBreakdown() {
        Map<String, String> vars = new LinkedHashMap<>(subscriptionVariables());
        vars.put("sourceType", "SUBSCRIPTION_ADDON");
        vars.put("objectLabel", "option d abonnement");
        vars.put("addonId", "512");
        vars.put("quantity", "2");
        vars.put("unitPriceLabel", "90 000 FCFA");

        String html = normalise(render(vars));

        assertThat(html).contains("90 000 FCFA").contains("180 000 FCFA").contains("512");
        assertThat(html).doesNotContain("SUBSCRIPTION_ADDON");
    }

    @Test
    @DisplayName("Sans montant connu, le contrat reste coherent plutot que vide")
    void withoutAKnownAmountTheContractStaysCoherent() {
        Map<String, String> vars = new LinkedHashMap<>(subscriptionVariables());
        vars.remove("amountLabel");
        vars.remove("totalAmount");

        String html = normalise(render(vars));

        assertThat(html).contains("celui de l offre valid".replace("l offre", "l'offre"));
        assertThat(html).doesNotContain("FCFA");
    }

    @Test
    @DisplayName("Le lieu de la juridiction suit l'entite exploitante")
    void theJurisdictionFollowsTheOperatingEntity() {
        String html = render(subscriptionVariables());

        assertThat(html).contains("Pointe-Noire");
        assertThat(html).contains("République du Congo");
    }

    @Test
    @DisplayName("Les dix articles sont numerotes sans trou")
    void theArticlesAreNumberedWithoutAGap() {
        String html = render(subscriptionVariables());

        for (int i = 1; i <= 10; i++) {
            assertThat(html).as("article %d", i).contains("Article " + i + " :");
        }
    }

    @Test
    @DisplayName("Aucune espace avant un point ou une virgule")
    void noSpaceBeforeAPeriodOrAComma() {
        // Une balise suivie d'un retour a la ligne, puis du point, donne « SUB-000042 . » au
        // rendu. En typographie francaise il n'y a pas d'espace devant un point ni une virgule ·
        // devant un point-virgule ou un deux-points, oui, et c'est pourquoi le controle les exclut.
        // Seul le corps contractuel est examine · la feuille de style porte des commentaires.
        String rendu = normalise(render(subscriptionVariables()));
        String corps = rendu.substring(rendu.indexOf("Il a "));
        String texte = corps.replaceAll("(?i)</(p|div|td|tr|li)>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("[ \t]+", " ");

        assertThat(texte.lines().filter(ligne -> ligne.contains(" .") || ligne.contains(" ,")))
                .as("lignes portant une espace avant un point ou une virgule")
                .isEmpty();
    }

    @Test
    @DisplayName("Le script Cloudflare colle par erreur a disparu")
    void theStrayScriptIsGone() {
        assertThat(render(subscriptionVariables())).doesNotContain("cloudflare-static");
    }

    /**
     * Ecrit le rendu sur disque pour une relecture humaine · un test ne juge pas si une phrase se
     * lit bien. Desactive par defaut :
     * {@code mvn test -Dtest=SubscriptionContractRenderTest -Dcontract.preview=/chemin/apercu.html}
     */
    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "contract.preview", matches = ".+")
    void writeThePreviewForAHumanToRead() throws java.io.IOException {
        java.nio.file.Path target = java.nio.file.Path.of(System.getProperty("contract.preview"));
        java.nio.file.Files.writeString(target, render(subscriptionVariables()));
        System.out.println("Apercu ecrit : " + target.toAbsolutePath());
    }

    /**
     * Rend le PDF par le meme chemin que la production, puis sa premiere page en PNG.
     *
     * <p>Les debordements ne se voient pas dans le HTML · ils apparaissent a la mise en page. Le
     * corps portait {@code width: 210mm} sous {@code @media print}, alors que la zone utile vaut
     * 210mm moins les marges de {@code @page} : tout ce qui depassait etait coupe au bord de la
     * page, et les phrases sortaient incompletes. Desactive par defaut :
     * {@code mvn test -Dtest=SubscriptionContractRenderTest -Dpdf.out=/chemin/contrat.pdf}</p>
     */
    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "pdf.out", matches = ".+")
    void writeThePdfAndItsFirstPageAsPng() throws Exception {
        String html = render(subscriptionVariables());
        java.nio.file.Path pdf = java.nio.file.Path.of(System.getProperty("pdf.out"));

        org.jsoup.nodes.Document jsoup = org.jsoup.Jsoup.parse(html);
        jsoup.outputSettings()
                .syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
                .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)
                .charset(java.nio.charset.StandardCharsets.UTF_8)
                .prettyPrint(false);
        try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(pdf)) {
            com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder =
                    new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
            builder.useFastMode();
            com.sni.bokaticowork.features.contract.service.support.ContractPdfFonts fonts =
                    new com.sni.bokaticowork.features.contract.service.support.ContractPdfFonts();
            fonts.load();
            fonts.register(builder);
            builder.withW3cDocument(new org.jsoup.helper.W3CDom().fromJsoup(jsoup), null);
            builder.toStream(out);
            builder.run();
        }

        try (org.apache.pdfbox.pdmodel.PDDocument document =
                     org.apache.pdfbox.pdmodel.PDDocument.load(pdf.toFile())) {
            org.apache.pdfbox.rendering.PDFRenderer renderer =
                    new org.apache.pdfbox.rendering.PDFRenderer(document);
            for (int page = 0; page < Math.min(2, document.getNumberOfPages()); page++) {
                java.io.File png = pdf.resolveSibling(
                        pdf.getFileName().toString().replaceAll("[.]pdf$", "") + "-p" + (page + 1) + ".png")
                        .toFile();
                javax.imageio.ImageIO.write(renderer.renderImage(page, 2f,
                        org.apache.pdfbox.rendering.ImageType.RGB), "png", png);
                System.out.println("PNG ecrit : " + png.getAbsolutePath());
            }
            System.out.println("Pages : " + document.getNumberOfPages());
        }
    }

    /** Les espaces insecables francais se comparent mal · on les ramene a l'espace ordinaire. */
    private String normalise(String html) {
        return html.replace(' ', ' ').replace(' ', ' ').replace("&#160;", " ")
                .replace("&#39;", "'").replace("&rsquo;", "'");
    }

    private String render(Map<String, String> variables) {
        GenerateContractRequest request = new GenerateContractRequest();
        request.setTemplateCode("subscription-pass-non-refundable");
        request.setOwnerType(DocumentOwnerType.CUSTOMER);
        request.setOwnerCode("CUS-000001");
        request.setTitle("Contrat abonnement SUB-000042");

        Context context = new Context(Locale.FRANCE);
        context.setVariable("request", request);
        context.setVariable("variables", variables);
        context.setVariable("clauses", List.of("Le Client declare avoir pris connaissance du reglement interieur."));
        context.setVariable("ownerName", "SARL Mbote");
        context.setVariable("ownerCode", "CUS-000001");
        context.setVariable("ownerEmail", "contact@mbote.cg");
        context.setVariable("ownerPhone", "+242061234567");
        context.setVariable("ownerAddress", "Avenue Charles de Gaulle, Pointe-Noire");
        context.setVariable("ownerRccm", "CG-PNR-01-2020-B14-00123");
        context.setVariable("signingDate", variables.get("signingDate"));
        context.setVariable("placeOfSigning", variables.get("placeOfSigning"));
        context.setVariable("logo", "");
        context.setVariable("generatedAt", "2026-10-05");
        context.setVariable("business", null);
        return engine.process(TEMPLATE, context);
    }

    private SpringTemplateEngine engine() {
        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
