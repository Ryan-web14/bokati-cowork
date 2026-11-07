package com.sni.bokaticowork.core.settings.platform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le catalogue reste complet · un reglage ajoute au code sans y figurer serait invisible pour
 * l'administration.
 */
class PlatformSettingCatalogTest {

    private static final Pattern SETTING = Pattern.compile("\\$\\{((?:bokati|app)\\.[a-zA-Z0-9._-]+)");

    @Test
    @DisplayName("Chaque reglage lu par le code figure au catalogue")
    void everySettingIsListed() throws IOException {
        Set<String> found = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = SETTING.matcher(Files.readString(file));
                while (matcher.find()) {
                    found.add(matcher.group(1));
                }
            }
        }

        assertThat(found).isNotEmpty();
        assertThat(found).allSatisfy(key -> assertThat(PlatformSettingCatalog.find(key)).as(key).isPresent());
    }

    @Test
    @DisplayName("Tous les domaines ont des reglages, chacun un libelle, aucun secret sans type SECRET")
    void catalogIsCoherent() {
        assertThat(PlatformSettingCatalog.all()).allSatisfy(definition -> {
            assertThat(definition.label()).isNotBlank();
            if (definition.key().endsWith(".secret") || definition.key().contains("passcode") || definition.key().endsWith("secret-key")) {
                assertThat(definition.type()).isEqualTo(PlatformSettingCatalog.Type.SECRET);
            }
        });
        assertThat(Arrays.stream(SettingDomain.values()))
                .allSatisfy(domain -> assertThat(PlatformSettingCatalog.all())
                        .anyMatch(definition -> definition.domain() == domain));
        assertThat(PlatformSettingCatalog.all().size()).isGreaterThanOrEqualTo(237);
    }
}
