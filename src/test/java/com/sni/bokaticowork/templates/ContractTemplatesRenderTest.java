package com.sni.bokaticowork.templates;

import com.sni.bokaticowork.features.contract.dto.request.GenerateContractRequest;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce que tout gabarit de contrat doit tenir, quel qu'il soit.
 *
 * <p>Les quatre gabarits ont ete ecrits separement et ont derive separement · trois portaient un
 * vidage brut de la table des variables, trois forçaient la largeur du corps a 210mm sous
 * {@code @media print} alors que la zone utile vaut 210mm <b>moins</b> les marges de
 * {@code @page}. Les invariants ci-dessous valent pour tous, et un gabarit ajoute demain y sera
 * soumis en l'ajoutant a la liste.</p>
 */
class ContractTemplatesRenderTest {

    private static final Path DOSSIER = Path.of("src", "main", "resources", "templates", "contracts");

    private final SpringTemplateEngine engine = engine();

    @ParameterizedTest
    @DisplayName("Aucun gabarit ne force la largeur du corps · c'est ce qui coupait les phrases")
    @ValueSource(strings = {"subscription-pass-non-refundable", "membership-agreement",
            "business-service-agreement", "contrat-domiciliation"})
    void noTemplateForcesTheBodyWidth(String gabarit) throws IOException {
        String source = Files.readString(DOSSIER.resolve(gabarit + ".html"), StandardCharsets.UTF_8);

        // @page definit deja la zone utile. La forcer a 210mm rendait le corps plus large que la
        // page imprimable, et tout ce qui depassait etait coupe au bord · les phrases sortaient
        // incompletes, et un contrat tronque n'est pas opposable.
        assertThat(source.replace(" ", "")).doesNotContain("width:210mm");
    }

    @ParameterizedTest
    @DisplayName("Aucune cle technique anglaise ne fuit dans le document rendu")
    @ValueSource(strings = {"subscription-pass-non-refundable", "membership-agreement",
            "business-service-agreement", "contrat-domiciliation"})
    void noTechnicalKeyLeaks(String gabarit) {
        String html = render(gabarit);

        // C'etait le contenu de l'article « Parametres de l'offre » · un vidage de
        // variables.entrySet() en deux colonnes, cles anglaises comprises.
        assertThat(html)
                .doesNotContain("contractPolicyCode")
                .doesNotContain("operatorSignatureName")
                .doesNotContain("billingCycle\"")
                .doesNotContain("SUBSCRIPTION_PASS_NON_REFUNDABLE")
                .doesNotContain("MONTHLY");
    }

    @ParameterizedTest
    @DisplayName("Aucune espace avant un point ou une virgule")
    @ValueSource(strings = {"subscription-pass-non-refundable", "membership-agreement",
            "business-service-agreement", "contrat-domiciliation"})
    void noSpaceBeforeAPeriodOrAComma(String gabarit) {
        String corps = texteDuCorps(render(gabarit));

        assertThat(corps.lines().filter(l -> l.contains(" .") || l.contains(" ,")))
                .as("lignes portant une espace avant un point ou une virgule")
                .isEmpty();
    }

    @ParameterizedTest
    @DisplayName("Le droit applicable et la juridiction sont nommes")
    @ValueSource(strings = {"subscription-pass-non-refundable", "membership-agreement",
            "business-service-agreement", "contrat-domiciliation"})
    void theApplicableLawAndJurisdictionAreNamed(String gabarit) {
        String html = render(gabarit);

        // « le droit applicable dans le pays d'etablissement du Prestataire » ne designe rien ·
        // une clause de juridiction se lit le jour du litige, pas avant.
        assertThat(html).contains("République du Congo");
        assertThat(html).contains("Pointe-Noire");
        assertThat(html).doesNotContain("pays d'établissement du Prestataire");
    }

    @ParameterizedTest
    @DisplayName("Les termes importants sont definis")
    @ValueSource(strings = {"subscription-pass-non-refundable", "membership-agreement",
            "business-service-agreement", "contrat-domiciliation"})
    void theImportantTermsAreDefined(String gabarit) {
        String html = render(gabarit);

        assertThat(html).contains("Définitions");
        assertThat(html).containsAnyOf("Le Prestataire", "Le Domiciliataire");
    }

    @ParameterizedTest
    @DisplayName("Le montant paye est annonce quand il est connu")
    @ValueSource(strings = {"subscription-pass-non-refundable", "membership-agreement",
            "business-service-agreement"})
    void theAmountIsStatedWhenKnown(String gabarit) {
        String corps = texteDuCorps(render(gabarit));

        assertThat(corps).contains("180 000 FCFA");
    }

    @ParameterizedTest
    @DisplayName("Les articles se suivent sans trou · un saut fait croire a une page manquante")
    @ValueSource(strings = {"subscription-pass-non-refundable", "membership-agreement",
            "business-service-agreement", "contrat-domiciliation"})
    void theArticlesFollowWithoutAGap(String gabarit) {
        String corps = texteDuCorps(render(gabarit));

        // La domiciliation masquait son article 3 quand aucune prestation n'etait transmise · la
        // numerotation passait alors de 2 a 4. Sur un contrat, un numero absent fait douter d'une
        // page manquante, et c'est le genre de doute qui se paie au moment de la signature.
        java.util.List<Integer> numeros = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?i)Article[ ]+([0-9]+)[ ]*[:·]").matcher(corps);
        while (m.find()) {
            int numero = Integer.parseInt(m.group(1));
            if (numeros.isEmpty() || numeros.get(numeros.size() - 1) != numero) {
                numeros.add(numero);
            }
        }

        assertThat(numeros).as("numeros d'article rendus").isNotEmpty();
        assertThat(numeros).isEqualTo(
                java.util.stream.IntStream.rangeClosed(1, numeros.size()).boxed().toList());
    }

    @ParameterizedTest
    @DisplayName("La police Spectral est demandee par tous les gabarits")
    @ValueSource(strings = {"subscription-pass-non-refundable", "membership-agreement",
            "business-service-agreement", "contrat-domiciliation"})
    void everyTemplateAsksForSpectral(String gabarit) throws IOException {
        String source = Files.readString(DOSSIER.resolve(gabarit + ".html"), StandardCharsets.UTF_8);

        assertThat(source).contains("\"Spectral\"");
        // Le <link> vers Google Fonts ne sert a rien dans un PDF et retarde chaque generation.
        assertThat(source).doesNotContain("fonts.googleapis.com");
    }

    /** Le corps contractuel seul · la feuille de style porte des commentaires. */
    private String texteDuCorps(String html) {
        String rendu = html.replace(' ', ' ').replace(' ', ' ')
                .replace("&#160;", " ").replace("&#39;", "'").replace("&middot;", "·");
        int debut = rendu.indexOf("ARTICLE 1");
        if (debut < 0) {
            debut = rendu.indexOf("Article 1");
        }
        return rendu.substring(Math.max(debut, 0))
                .replaceAll("(?i)</(p|div|td|tr|li)>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("[ \t]+", " ");
    }

    private Map<String, String> variables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("clientName", "SARL Mbote");
        vars.put("operatorName", "ETS ELLE A OSE");
        vars.put("operatorRccm", "CG-PNR-01-2017-A11-00472");
        vars.put("operatorAddress", "84 Boulevard du General Charles de Gaulle, Pointe-Noire");
        vars.put("startDate", "1 octobre 2026");
        vars.put("endDate", "31 octobre 2026");
        vars.put("effectiveDate", "1 octobre 2026");
        vars.put("placeOfSigning", "Pointe-Noire");
        vars.put("signingDate", "5 octobre 2026");
        vars.put("planCode", "FLEX");
        vars.put("planVersion", "3");
        vars.put("billingCycleLabel", "tous les mois");
        vars.put("objectLabel", "abonnement");
        vars.put("amountLabel", "180 000 FCFA");
        return vars;
    }

    private String render(String gabarit) {
        GenerateContractRequest request = new GenerateContractRequest();
        request.setTemplateCode(gabarit);
        request.setOwnerType(DocumentOwnerType.CUSTOMER);
        request.setOwnerCode("CUS-000001");
        request.setTitle("Contrat " + gabarit);

        Map<String, String> vars = variables();
        Context context = new Context(Locale.FRANCE);
        context.setVariable("request", request);
        context.setVariable("variables", vars);
        context.setVariable("clauses", List.of("Le Client declare avoir pris connaissance du reglement interieur."));
        context.setVariable("ownerName", "SARL Mbote");
        context.setVariable("ownerCode", "CUS-000001");
        context.setVariable("ownerEmail", "contact@mbote.cg");
        context.setVariable("ownerPhone", "+242061234567");
        context.setVariable("ownerAddress", "Avenue Charles de Gaulle, Pointe-Noire");
        context.setVariable("ownerRccm", "CG-PNR-01-2020-B14-00123");
        context.setVariable("signingDate", vars.get("signingDate"));
        context.setVariable("placeOfSigning", vars.get("placeOfSigning"));
        context.setVariable("logo", "");
        context.setVariable("generatedAt", "2026-10-05");
        context.setVariable("business", null);
        return engine.process("contracts/" + gabarit, context);
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
