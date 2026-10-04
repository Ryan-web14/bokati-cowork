package com.sni.bokaticowork.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * L'adresse du client · celle qu'on ne peut pas se choisir soi-meme.
 *
 * <p>Le plafond de requetes etait indexe sur la <b>premiere</b> valeur de {@code X-Forwarded-For}.
 * Or cet en-tete est construit de gauche a droite au fil des relais : la partie gauche est ce que
 * l'appelant a envoye, donc ce qu'il a choisi. Il suffisait d'ajouter
 * {@code X-Forwarded-For: <au hasard>} a chaque requete pour obtenir un compteur neuf a chaque
 * fois · ce qui annulait tous les plafonds, celui du login compris, pour qui le savait.</p>
 *
 * <p>Ce sont les valeurs de <b>droite</b> qui sont dignes de foi : chacune a ete ajoutee par un
 * relais, et seuls les notres le sont. En connaissant leur nombre, on sait exactement quelle
 * position lire · les valeurs forgees se retrouvent a gauche et sont ignorees.</p>
 *
 * <p>Sur Heroku, un seul relais s'interpose · c'est la valeur par defaut.</p>
 */
@Component
public class ClientIpResolver {

    private static final String UNKNOWN = "unknown";

    /**
     * Combien de relais de confiance s'interposent devant l'application.
     *
     * <p>Zero signifie « aucun » · l'en-tete est alors entierement ignore, parce que personne
     * d'autre que l'appelant ne peut l'avoir ecrit.</p>
     */
    @Value("${app.security.rate-limit.trusted-proxy-count:1}")
    private int trustedProxyCount;

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String direct = StringUtils.hasText(request.getRemoteAddr()) ? request.getRemoteAddr() : UNKNOWN;
        if (trustedProxyCount <= 0) {
            return direct;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(forwarded)) {
            return direct;
        }
        return fromForwardedChain(forwarded, trustedProxyCount, direct);
    }

    /**
     * La position a lire dans la chaine · {@code trustedProxyCount} rangs depuis la droite.
     *
     * <p>Un relais ajoute l'adresse du pair dont il a recu la requete. Avec un seul relais de
     * confiance, l'adresse reelle est donc la derniere de la chaine · tout ce qui la precede a ete
     * fourni par l'appelant. Une chaine plus courte que le nombre de relais annonces signifie que
     * la configuration ne correspond pas a la realite : on retombe sur l'adresse du pair direct,
     * qui ne se falsifie pas.</p>
     */
    static String fromForwardedChain(String forwarded, int trustedProxyCount, String fallback) {
        String[] entries = forwarded.split(",");
        int index = entries.length - trustedProxyCount;
        if (index < 0 || index >= entries.length) {
            return fallback;
        }
        String candidate = entries[index].trim();
        return StringUtils.hasText(candidate) ? candidate : fallback;
    }
}
