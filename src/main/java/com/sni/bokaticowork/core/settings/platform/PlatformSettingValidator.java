package com.sni.bokaticowork.core.settings.platform;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Une valeur est controlee avant d'etre enregistree · elle sera lue au demarrage, et une valeur
 * invalide empecherait l'application de repartir.
 */
final class PlatformSettingValidator {

    /** En dessous, une cadence ferait tourner un traitement en continu. */
    private static final long MIN_DELAY_MS = 1_000;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private PlatformSettingValidator() {
    }

    /** Rend la valeur a enregistrer, sans espaces autour. */
    static String validated(PlatformSettingCatalog.Definition definition, String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.contains("${")) {
            throw invalid(definition, "ne peut pas contenir de référence à une autre valeur");
        }
        switch (definition.type()) {
            case SECRET -> throw new BadRequestException("Un secret ne se modifie pas ici : il reste dans la configuration du serveur.");
            case BOOLEAN -> {
                if (!"true".equals(value) && !"false".equals(value)) {
                    throw invalid(definition, "doit valoir true ou false");
                }
            }
            case INTEGER -> integer(definition, value);
            case DECIMAL -> decimal(definition, value);
            case CRON -> {
                if (!CronExpression.isValidExpression(value)) {
                    throw invalid(definition, "n'est pas une planification valide");
                }
            }
            case EMAIL -> {
                if (StringUtils.hasText(value) && !EMAIL.matcher(value).matches()) {
                    throw invalid(definition, "n'est pas une adresse e-mail valide");
                }
            }
            case TIME -> time(definition, value);
            case TIME_RANGE -> timeRange(definition, value);
            case TEXT, ENUM -> { /* tout texte, y compris vide · une liste est controlee par son module */ }
        }
        return value;
    }

    private static void integer(PlatformSettingCatalog.Definition definition, String value) {
        long number;
        try {
            number = Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw invalid(definition, "doit être un nombre entier");
        }
        if (number < 0) {
            throw invalid(definition, "ne peut pas être négatif");
        }
        if (definition.key().endsWith("delay-ms") && !definition.key().contains("initial") && number < MIN_DELAY_MS) {
            throw invalid(definition, "doit être d'au moins " + MIN_DELAY_MS + " ms");
        }
    }

    private static void decimal(PlatformSettingCatalog.Definition definition, String value) {
        try {
            if (new BigDecimal(value).signum() < 0) {
                throw invalid(definition, "ne peut pas être négatif");
            }
        } catch (NumberFormatException ex) {
            throw invalid(definition, "doit être un nombre");
        }
    }

    private static void time(PlatformSettingCatalog.Definition definition, String value) {
        try {
            LocalTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw invalid(definition, "doit être une heure au format HH:mm");
        }
    }

    /** Vide, le jour est ferme. */
    private static void timeRange(PlatformSettingCatalog.Definition definition, String value) {
        if (value.isEmpty()) {
            return;
        }
        String[] parts = value.split("-");
        try {
            if (parts.length != 2 || !LocalTime.parse(parts[0].trim()).isBefore(LocalTime.parse(parts[1].trim()))) {
                throw invalid(definition, "doit être une plage HH:mm-HH:mm, ouverture avant fermeture");
            }
        } catch (DateTimeParseException ex) {
            throw invalid(definition, "doit être une plage HH:mm-HH:mm, ouverture avant fermeture");
        }
    }

    private static BadRequestException invalid(PlatformSettingCatalog.Definition definition, String problem) {
        return new BadRequestException("« " + definition.label() + " » " + problem + ".");
    }
}
