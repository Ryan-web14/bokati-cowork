package com.sni.bokaticowork.core.utils.time;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Heure de l'application (lot B8, ecart Bn-26).
 *
 * <p>Les creneaux sont saisis en heure locale (Brazzaville). Le serveur, lui, peut tourner en UTC :
 * comparer un creneau a {@code LocalDateTime.now()} decalait alors tout d'une heure (depart compte
 * en retard, rappel en retard, preavis faux). Le module reservation et les ressources prennent
 * « maintenant » ici, dans le fuseau {@code app.time-zone} (Africa/Brazzaville par defaut). Un fuseau
 * mal ecrit est signale et remplace par le defaut, sans bloquer le demarrage.</p>
 *
 * <p>Le fuseau de la JVM n'est pas change : le pilote PostgreSQL l'utiliserait pour la session et
 * decalerait les colonnes horodatees sans fuseau.</p>
 */
@Slf4j
@Component
public class AppTime {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Africa/Brazzaville");

    private static volatile ZoneId zone = DEFAULT_ZONE;

    @Value("${app.time-zone:Africa/Brazzaville}")
    private String configured;

    @PostConstruct
    void init() {
        zone = parse(configured);
        log.info("Heure de l'application : fuseau {}", zone);
    }

    static ZoneId parse(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_ZONE;
        }
        try {
            return ZoneId.of(value.trim());
        } catch (Exception ex) {
            log.warn("app.time-zone mal ecrit ({}) : fuseau {} utilise", value, DEFAULT_ZONE);
            return DEFAULT_ZONE;
        }
    }

    public static ZoneId zone() {
        return zone;
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(zone);
    }

    public static LocalDate today() {
        return LocalDate.now(zone);
    }

    public static LocalTime time() {
        return LocalTime.now(zone);
    }
}
