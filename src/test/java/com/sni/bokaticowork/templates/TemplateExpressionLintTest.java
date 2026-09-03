package com.sni.bokaticowork.templates;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garde-fou sur les expressions des gabarits.
 *
 * <p>Une erreur d'expression ne se voit qu'au rendu, c'est-a-dire au moment ou le courriel part.
 * Un gabarit fautif ne casse donc pas la compilation, ni les tests, ni le demarrage : il casse
 * l'envoi, en production, sur le courriel concerne.
 *
 * <p>Ce controle ne prouve pas qu'un gabarit s'affiche correctement · il n'y a pas de rendu ici.
 * Il attrape la faute qui a failli passer : une apostrophe echappee par une contre-oblique dans un
 * litteral d'expression. SpEL ne connait pas {@code \'} ; une apostrophe s'y ecrit {@code ''}. La
 * formulation la plus sure reste d'eviter l'apostrophe droite dans ces textes.
 */
class TemplateExpressionLintTest {

    private static final Path TEMPLATES = Paths.get("src", "main", "resources", "templates");

    /** Contenu d'un attribut {@code th:*="${...}"}. */
    private static final Pattern EXPRESSION =
            Pattern.compile("th:[a-zA-Z-]+\\s*=\\s*\"([^\"]*)\"");

    @Test
    void shouldNotEscapeQuotesWithABackslashInsideAnExpression() throws IOException {
        List<String> offenders = new ArrayList<>();

        try (Stream<Path> files = Files.walk(TEMPLATES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".html")).toList()) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                Matcher matcher = EXPRESSION.matcher(content);
                while (matcher.find()) {
                    String expression = matcher.group(1);
                    if (expression.contains("\\'")) {
                        offenders.add(TEMPLATES.relativize(file) + " · " + shorten(expression));
                    }
                }
            }
        }

        assertThat(offenders)
                .withFailMessage("Apostrophe echappee par une contre-oblique dans une expression :"
                        + " SpEL leve une erreur au rendu, donc a l'envoi. Ecrivez '' ou reformulez"
                        + " sans apostrophe droite.%n  %s", String.join("%n  ", offenders))
                .isEmpty();
    }

    @Test
    void shouldFindTheEmailTemplatesItClaimsToCheck() throws IOException {
        // Un controle qui ne lit aucun fichier passe toujours · on verifie qu'il a bien de quoi lire.
        try (Stream<Path> files = Files.walk(TEMPLATES.resolve("email"))) {
            assertThat(files.filter(f -> f.toString().endsWith(".html")).count()).isGreaterThan(20);
        }
    }

    private String shorten(String expression) {
        String flat = expression.replaceAll("\\s+", " ").trim();
        return flat.length() <= 90 ? flat : flat.substring(0, 90) + "…";
    }
}
