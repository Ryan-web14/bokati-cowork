package com.sni.bokaticowork.features.booking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "bokati.checkin")
public class BookingCheckInProperties {

    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private String scannerKey = "";

    /**
     * Duree de validite de l'autorisation posee sur un poste de pointage.
     *
     * <p>Le cookie durait un an et portait la cle elle-meme · il n'expirait donc jamais
     * utilement. Trente jours obligent a repasser par la cle une fois par mois, ce qui borne la
     * duree de vie d'un appareil perdu sans rendre l'accueil impraticable.</p>
     */
    private int scannerGrantValidityDays = 30;

    /**
     * Le cookie du poste n'est envoye que sur une connexion chiffree.
     *
     * <p>A ne mettre a {@code false} qu'en developpement local, ou le navigateur refuserait de
     * conserver un cookie {@code Secure} servi en HTTP.</p>
     */
    private boolean scannerCookieSecure = true;
    /** Tolerance avant l'heure · le pointage est accepte, mais n'ouvre pas la salle pour autant. */
    private int earlyWindowMinutes = 15;

    /**
     * Tolerance apres l'heure de debut.
     *
     * <p>Le pointage etait accepte jusqu'a la fin de la reservation, ce qui vidait la notion de
     * presence : pointer a la derniere minute d'un creneau de neuf heures valait presence pleine,
     * et rendait le marquage automatique en absence inoperant.
     */
    private int lateWindowMinutes = 15;
    private Location location = new Location();

    @Getter
    @Setter
    public static class Location {
        private double latitude = 0;
        private double longitude = 0;
        private int radiusMeters = 200;
    }

    public boolean isScannerKeyConfigured() {
        return StringUtils.hasText(scannerKey);
    }

    /** La duree de validite d'une autorisation de poste · au moins un jour. */
    public Duration grantValidity() {
        return Duration.ofDays(Math.max(1, scannerGrantValidityDays));
    }

    /**
     * La cle du poste est-elle celle configuree · comparaison a temps constant.
     *
     * <p>C'etait un {@code String.equals}, qui s'arrete au premier caractere qui differe. Peu
     * exploitable a travers le reseau, mais la correction est gratuite et la cle est partagee par
     * tous les postes : elle merite d'etre comparee comme un secret.</p>
     */
    public boolean isScannerKeyValid(String key) {
        if (!isScannerKeyConfigured()) {
            return false;
        }
        return MessageDigest.isEqual(
                scannerKey.trim().getBytes(StandardCharsets.UTF_8),
                (key == null ? "" : key.trim()).getBytes(StandardCharsets.UTF_8));
    }

    public boolean isLocationConfigured() {
        return location != null && (location.latitude != 0 || location.longitude != 0);
    }

    public boolean isWithinRange(double lat, double lng) {
        if (!isLocationConfigured()) return true;
        double distance = haversine(location.latitude, location.longitude, lat, lng);
        return distance <= location.radiusMeters;
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}