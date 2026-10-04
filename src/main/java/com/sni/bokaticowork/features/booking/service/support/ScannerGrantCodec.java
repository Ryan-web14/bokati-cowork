package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.booking.config.BookingCheckInProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * L'autorisation d'un poste de pointage · signee, datee, et sans la cle dedans.
 *
 * <p>Le cookie du terminal contenait <b>la cle d'administration en clair</b>, pour un an, avec
 * {@code path=/} · elle etait donc renvoyee a toutes les requetes du domaine, API comprise. Un
 * cookie n'est pas un coffre : tout ce qui y est ecrit est lisible par qui met la main sur
 * l'appareil, et part sur le reseau a chaque appel.</p>
 *
 * <p>Ce qu'on y met maintenant est une <b>preuve</b> et non un secret : l'horodatage d'expiration,
 * et une signature de cet horodatage par la cle. La cle sert a verifier, elle ne circule plus.
 * Trois proprietes en decoulent :</p>
 * <ul>
 *   <li>lire le cookie ne donne pas la cle, donc ne permet pas d'activer un autre poste ;</li>
 *   <li>l'autorisation expire d'elle-meme · elle ne valait rien avant, le cookie portant la cle
 *       restait valable aussi longtemps qu'elle ;</li>
 *   <li>changer la cle revoque <b>tous</b> les postes d'un coup, puisque plus aucune signature ne
 *       se verifie.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScannerGrantCodec {

    /** Le prefixe de version · il permettra de changer de format sans accepter l'ancien. */
    private static final String VERSION = "s1";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int PART_COUNT = 3;

    private final BookingCheckInProperties properties;

    /**
     * Emet une autorisation valable pour la duree configuree.
     *
     * @return la valeur a poser dans le cookie, ou {@code null} si aucune cle n'est configuree
     */
    public String issue(Instant now) {
        if (!properties.isScannerKeyConfigured()) {
            return null;
        }
        long expiresAt = now.plus(properties.grantValidity()).getEpochSecond();
        String payload = VERSION + "." + expiresAt;
        return payload + "." + sign(payload);
    }

    /**
     * Verifie une autorisation · signature puis date, dans cet ordre.
     *
     * <p>L'ordre importe peu ici pour la securite, mais verifier la signature d'abord evite de
     * traiter une date qui n'a pas ete prouvee.</p>
     */
    public boolean isValid(String value, Instant now) {
        if (!properties.isScannerKeyConfigured() || !StringUtils.hasText(value)) {
            return false;
        }
        String[] parts = value.trim().split("\\.");
        if (parts.length != PART_COUNT || !VERSION.equals(parts[0])) {
            return false;
        }
        String payload = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(payload), parts[2])) {
            return false;
        }
        try {
            return Instant.ofEpochSecond(Long.parseLong(parts[1])).isAfter(now);
        } catch (NumberFormatException ex) {
            // Signee mais illisible · ce serait une autorisation emise par une autre version.
            return false;
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    properties.getScannerKey().trim().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            // Une signature impossible doit refuser, jamais laisser passer.
            throw new IllegalStateException("Signature de l'autorisation de pointage impossible", ex);
        }
    }

    /**
     * Comparaison a temps constant · {@code String.equals} s'arrete au premier octet qui differe,
     * ce qui laisse filtrer la longueur du prefixe correct.
     */
    private static boolean constantTimeEquals(String expected, String candidate) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                candidate.getBytes(StandardCharsets.UTF_8));
    }
}
