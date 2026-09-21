package com.sni.bokaticowork.core.utils.mail;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Une propriete « adresse » peut en porter plusieurs · virgules ou points-virgules, espaces tolerés.
 *
 * <p>Le module courriel envoie un message par destinataire ; celui qui publie boucle sur cette
 * liste. Les doublons tombent, l'ordre est garde.</p>
 */
public final class Recipients {

    private Recipients() {
    }

    public static List<String> split(String configured) {
        if (!StringUtils.hasText(configured)) {
            return List.of();
        }
        return List.copyOf(new LinkedHashSet<>(Arrays.stream(configured.split("[,;]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList()));
    }
}
