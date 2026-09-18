package com.sni.bokaticowork.core.generator.uuid;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Identifiant aleatoire, mais horodate · version 7.
 *
 * <p>Un {@code UUID.randomUUID()} est uniformement reparti : deux identifiants crees a une seconde
 * d'intervalle atterrissent n'importe ou dans l'index, qui se fragmente et dont les pages chaudes
 * se dispersent. La version 7 place les quarante-huit bits de l'horodatage en tete · les
 * identifiants d'une meme journee restent voisins, l'index croit par la droite, et l'on peut lire
 * l'instant de creation d'une ecriture a partir de son seul identifiant.</p>
 *
 * <p>Le reste est tire d'un generateur cryptographique : l'ordre est previsible, la valeur ne
 * l'est pas.</p>
 */
public final class TimeOrderedUuid {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TimeOrderedUuid() {
    }

    public static UUID next() {
        byte[] bytes = new byte[10];
        RANDOM.nextBytes(bytes);

        long millis = System.currentTimeMillis();

        // 48 bits d'horodatage, puis la version 7 sur les quatre bits suivants.
        long most = (millis & 0xFFFFFFFFFFFFL) << 16;
        most |= 0x7000L;
        most |= ((long) (bytes[0] & 0x0F) << 8) | (bytes[1] & 0xFFL);

        // La variante RFC occupe les deux bits de tete du second mot.
        long least = 0x8000000000000000L;
        least |= ((long) (bytes[2] & 0x3F) << 56);
        for (int index = 3; index < 10; index++) {
            least |= (bytes[index] & 0xFFL) << ((9 - index) * 8);
        }
        return new UUID(most, least);
    }
}
