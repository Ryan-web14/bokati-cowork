package com.sni.bokaticowork.core.settings.platform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lecture des parametres au moment de servir · pour les reglages qui prennent effet aussitot.
 *
 * <p>Les valeurs modifiees restent en memoire, relues chaque minute et apres chaque enregistrement :
 * la lecture ne touche pas la base. Sans valeur modifiee, l'appelant garde celle de sa configuration.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformSettings {

    private final PlatformSettingRepository repository;
    private volatile Map<String, String> values = Map.of();

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
        return values.getOrDefault(key, fallback);
    }

    public long number(String key, long fallback) {
        String value = values.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            log.warn("Parametre {} illisible, valeur de configuration conservee", key);
            return fallback;
        }
    }
}
