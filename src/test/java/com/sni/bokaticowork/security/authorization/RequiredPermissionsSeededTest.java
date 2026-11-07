package com.sni.bokaticowork.security.authorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Une permission exigee par le code doit exister en base · sinon la route est fermee a tout le monde,
 * {@code SUPER_ADMIN} compris, et rien ne le signale avant qu'un utilisateur ne tombe dessus.
 */
class RequiredPermissionsSeededTest {

    private static final Pattern AUTHORITY_CHECK = Pattern.compile("has(?:Any)?Authority\\(([^)]*)\\)");
    private static final Pattern QUOTED = Pattern.compile("'([^']+)'");

    @Test
    @DisplayName("Chaque permission de @PreAuthorize est creee par une migration, en dev comme en prod")
    void everyRequiredPermissionIsSeeded() throws IOException {
        Set<String> required = new TreeSet<>();
        for (String source : read(Path.of("src/main/java"), ".java")) {
            Matcher check = AUTHORITY_CHECK.matcher(source);
            while (check.find()) {
                Matcher name = QUOTED.matcher(check.group(1));
                while (name.find()) {
                    if (!name.group(1).startsWith("ROLE_")) {
                        required.add(name.group(1).replace(':', '_'));
                    }
                }
            }
        }

        assertThat(required).isNotEmpty();
        String dev = String.join("\n", read(Path.of("src/main/resources/db/migration"), ".sql"));
        String prod = String.join("\n", read(Path.of("src/main/resources/db/migration-prod"), ".sql"));
        assertThat(required).allSatisfy(permission -> {
            assertThat(dev).as("migration dev : " + permission).contains("'" + permission + "'");
            assertThat(prod).as("migration prod : " + permission).contains("'" + permission + "'");
        });
    }

    private static java.util.List<String> read(Path root, String extension) throws IOException {
        java.util.List<String> contents = new java.util.ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(extension)).toList()) {
                contents.add(Files.readString(file));
            }
        }
        return contents;
    }
}
