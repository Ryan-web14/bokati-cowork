package com.sni.bokaticowork.core.settings.platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Le catalogue des parametres · {@code settings/platform-settings.tsv}, une ligne par reglage.
 *
 * <p>Il se lit sans Spring : le chargement des valeurs au demarrage en a besoin avant que le
 * contexte n'existe.</p>
 */
public final class PlatformSettingCatalog {

    private static final String RESOURCE = "settings/platform-settings.tsv";
    private static final Map<String, Definition> DEFINITIONS = load();

    public enum Type { TEXT, EMAIL, INTEGER, DECIMAL, BOOLEAN, CRON, TIME, TIME_RANGE, SECRET }

    /** {@code LIVE} : pris en compte aussitot · {@code RESTART} : au prochain redemarrage. */
    public enum Effect { LIVE, RESTART }

    public record Definition(String key, SettingDomain domain, Type type, Effect effect, String defaultValue, String label) {
    }

    private PlatformSettingCatalog() {
    }

    public static Collection<Definition> all() {
        return DEFINITIONS.values();
    }

    public static Optional<Definition> find(String key) {
        return Optional.ofNullable(DEFINITIONS.get(key));
    }

    private static Map<String, Definition> load() {
        Map<String, Definition> definitions = new LinkedHashMap<>();
        try (InputStream stream = PlatformSettingCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Catalogue des parametres introuvable : " + RESOURCE);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] columns = line.split("\t", -1);
                definitions.put(columns[0], new Definition(columns[0], SettingDomain.valueOf(columns[1]),
                        Type.valueOf(columns[2]), Effect.valueOf(columns[3]), columns[4], columns[5]));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return definitions;
    }
}
