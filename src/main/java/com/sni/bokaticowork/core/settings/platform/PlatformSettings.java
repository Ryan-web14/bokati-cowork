package com.sni.bokaticowork.core.settings.platform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Lecture des parametres au moment de servir · pour les reglages qui prennent effet aussitot.
 *
 * <p>Les valeurs modifiees restent en memoire, relues chaque minute et apres chaque enregistrement :
 * la lecture ne touche pas la base.</p>
 *
 * <p>Sans valeur modifiee, la valeur rendue est celle du fichier de configuration et de
 * l'environnement, pas celle que l'application a lue en demarrant : un champ {@code @Value} recoit
 * au demarrage la valeur enregistree en base, et la rendre telle quelle empecherait une remise a
 * zero de prendre effet avant le redemarrage. Le repli de l'appelant ne sert que si la
 * configuration ne dit rien de la cle.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformSettings {

    private static final Optional<String> NONE = Optional.empty();

    private final PlatformSettingRepository repository;
    private final ConfigurableEnvironment environment;
    private volatile Map<String, String> values = Map.of();
    private final Map<String, Optional<String>> fileValues = new ConcurrentHashMap<>();
    private volatile PropertySourcesPropertyResolver fileResolver;

    @Scheduled(fixedDelay = 60_000)
    public void refresh() {
        try {
            values = repository.findAll().stream()
                    .collect(Collectors.toUnmodifiableMap(PlatformSetting::getKey, PlatformSetting::getValue));
        } catch (RuntimeException ex) {
            log.warn("Parametres de la plateforme non relus, les valeurs en memoire restent : {}", ex.getMessage());
        }
    }

    public String text(String key, String fallback) {
        String value = values.get(key);
        if (value != null) {
            return value;
        }
        return fileValue(key).orElse(fallback);
    }

    public long number(String key, long fallback) {
        String value = text(key, null);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            log.warn("Parametre {} illisible, valeur de configuration conservee", key);
            return fallback;
        }
    }

    public int integer(String key, int fallback) {
        long value = number(key, fallback);
        return value > Integer.MAX_VALUE || value < Integer.MIN_VALUE ? fallback : (int) value;
    }

    public boolean bool(String key, boolean fallback) {
        String value = text(key, null);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("true") || normalized.equals("false")) {
            return Boolean.parseBoolean(normalized);
        }
        log.warn("Parametre {} illisible (booleen attendu), valeur de configuration conservee", key);
        return fallback;
    }

    public BigDecimal decimal(String key, BigDecimal fallback) {
        String value = text(key, null);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            log.warn("Parametre {} illisible (decimal attendu), valeur de configuration conservee", key);
            return fallback;
        }
    }

    public double decimal(String key, double fallback) {
        return decimal(key, BigDecimal.valueOf(fallback)).doubleValue();
    }

    /** Valeur du fichier de configuration et de l'environnement, sans les valeurs enregistrees en base. */
    Optional<String> fileValue(String key) {
        if (environment == null) {
            return NONE;
        }
        return fileValues.computeIfAbsent(key, k -> {
            try {
                String value = resolver().getProperty(k);
                return value == null ? NONE : Optional.of(value);
            } catch (RuntimeException ex) {
                return NONE;
            }
        });
    }

    private PropertySourcesPropertyResolver resolver() {
        PropertySourcesPropertyResolver resolver = fileResolver;
        if (resolver == null) {
            MutablePropertySources sources = new MutablePropertySources(environment.getPropertySources());
            sources.remove(PlatformSettingsEnvironmentPostProcessor.SOURCE_NAME);
            resolver = new PropertySourcesPropertyResolver(sources);
            resolver.setIgnoreUnresolvableNestedPlaceholders(true);
            fileResolver = resolver;
        }
        return resolver;
    }
}
