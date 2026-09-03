package com.sni.bokaticowork.features.booking.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Getter
@Setter
@ConfigurationProperties(prefix = "bokati.checkin")
public class BookingCheckInProperties {

    private static final String SCANNER_COOKIE_NAME = "bokati_scanner";
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private String scannerKey = "";
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

    public boolean isScannerKeyValid(String key) {
        return isScannerKeyConfigured() && scannerKey.trim().equals(key == null ? "" : key.trim());
    }

    public boolean hasScannerCookie(HttpServletRequest request) {
        if (!isScannerKeyConfigured()) return false;
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        return Arrays.stream(cookies)
                .anyMatch(c -> SCANNER_COOKIE_NAME.equals(c.getName()) && isScannerKeyValid(c.getValue()));
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