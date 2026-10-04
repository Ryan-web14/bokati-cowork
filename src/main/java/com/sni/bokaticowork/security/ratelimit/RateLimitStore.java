package com.sni.bokaticowork.security.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Le comptage des requetes · partage entre les instances quand c'est possible.
 *
 * <p>Les compteurs vivaient dans une table en memoire du processus. Trois consequences :</p>
 * <ul>
 *   <li>avec plusieurs instances, le plafond etait multiplie par leur nombre ;</li>
 *   <li>un redemarrage remettait tout a zero · il suffisait d'attendre un deploiement ;</li>
 *   <li>la table ne se purgeait jamais · une cle par adresse et par chemin, gardee indefiniment,
 *       ce qui faisait du plafond lui-meme un moyen d'epuiser la memoire.</li>
 * </ul>
 *
 * <p>Redis fait le comptage quand il repond · {@code INCR} y est atomique, et l'expiration de la
 * cle tient la fenetre sans qu'on ait a la gerer. S'il ne repond pas, on retombe sur la memoire
 * locale, purgee cette fois · un plafond approximatif vaut mieux que pas de plafond, et beaucoup
 * mieux que de refuser tout le trafic parce qu'un cache est tombe.</p>
 */
@Slf4j
@Component
public class RateLimitStore {

    private static final String KEY_PREFIX = "ratelimit:";
    /** Au-dela, la table locale est purgee de ses fenetres echues. */
    private static final int LOCAL_PURGE_THRESHOLD = 10_000;

    private final ObjectProvider<StringRedisTemplate> redis;
    private final Map<String, Window> local = new ConcurrentHashMap<>();
    /** Pour ne pas repeter le meme avertissement a chaque requete. */
    private final AtomicBoolean degraded = new AtomicBoolean(false);

    public RateLimitStore(ObjectProvider<StringRedisTemplate> redis) {
        this.redis = redis;
    }

    /**
     * Compte une requete · vrai si elle reste sous le plafond.
     *
     * @param key         ce qu'on compte · adresse, et chemin quand le plafond est par route
     * @param maxRequests le nombre de requetes tolerees dans la fenetre
     * @param window      la duree de la fenetre
     */
    public boolean allow(String key, int maxRequests, Duration window) {
        Long count = countInRedis(key, window);
        if (count != null) {
            return count <= maxRequests;
        }
        return countLocally(key, maxRequests, window);
    }

    private Long countInRedis(String key, Duration window) {
        StringRedisTemplate template = redis.getIfAvailable();
        if (template == null) {
            return null;
        }
        try {
            String redisKey = KEY_PREFIX + key;
            Long count = template.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                // La fenetre commence avec la premiere requete · l'expiration de la cle la ferme.
                template.expire(redisKey, window);
            }
            if (degraded.compareAndSet(true, false)) {
                log.info("Plafond de requetes · comptage partage retabli");
            }
            return count;
        } catch (Exception ex) {
            if (degraded.compareAndSet(false, true)) {
                log.warn("Plafond de requetes · comptage partage indisponible, repli en memoire locale : {}",
                        ex.getMessage());
            }
            return null;
        }
    }

    private boolean countLocally(String key, int maxRequests, Duration window) {
        Instant now = Instant.now();
        if (local.size() > LOCAL_PURGE_THRESHOLD) {
            purge(now);
        }
        Window counted = local.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAt().isBefore(now)) {
                return new Window(1, now.plus(window));
            }
            return new Window(current.count() + 1, current.expiresAt());
        });
        return counted.count() <= maxRequests;
    }

    /** Les fenetres echues ne servent plus a rien · elles encombraient la memoire pour toujours. */
    private void purge(Instant now) {
        local.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    /** Visible pour les tests · le nombre de fenetres gardees localement. */
    int localSize() {
        return local.size();
    }

    private record Window(int count, Instant expiresAt) {
    }
}
