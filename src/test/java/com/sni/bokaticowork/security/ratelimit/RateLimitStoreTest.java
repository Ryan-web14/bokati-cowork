package com.sni.bokaticowork.security.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le comptage · partage quand Redis repond, local quand il ne repond pas, purge dans les deux cas.
 *
 * <p>Les trois defauts de la table en memoire du processus : le plafond etait multiplie par le
 * nombre d'instances, un redemarrage le remettait a zero, et la table ne se purgeait jamais · une
 * cle par adresse et par chemin gardee indefiniment, donc le plafond lui-meme servait a epuiser
 * la memoire.</p>
 */
@SuppressWarnings("unchecked")
class RateLimitStoreTest {

    /** Aucun Redis en vue · c'est le repli en memoire locale qui compte. */
    private RateLimitStore localStore() {
        return new RateLimitStore(providerOf(null));
    }

    private ObjectProvider<StringRedisTemplate> providerOf(StringRedisTemplate template) {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(template);
        return provider;
    }

    @Test
    @DisplayName("En memoire locale, le plafond est atteint a la requete de trop")
    void localCeilingIsEnforced() {
        RateLimitStore store = localStore();

        assertThat(store.allow("login:1.2.3.4", 3, Duration.ofMinutes(1))).isTrue();
        assertThat(store.allow("login:1.2.3.4", 3, Duration.ofMinutes(1))).isTrue();
        assertThat(store.allow("login:1.2.3.4", 3, Duration.ofMinutes(1))).isTrue();
        assertThat(store.allow("login:1.2.3.4", 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    @DisplayName("Deux cles ne se consomment pas l'une l'autre")
    void keysAreIndependent() {
        RateLimitStore store = localStore();

        assertThat(store.allow("login:1.2.3.4", 1, Duration.ofMinutes(1))).isTrue();
        assertThat(store.allow("login:1.2.3.4", 1, Duration.ofMinutes(1))).isFalse();
        assertThat(store.allow("login:5.6.7.8", 1, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    @DisplayName("La fenetre echue redonne le droit de passer")
    void expiredWindowReopens() throws InterruptedException {
        RateLimitStore store = localStore();

        assertThat(store.allow("otp:1.2.3.4", 1, Duration.ofMillis(40))).isTrue();
        assertThat(store.allow("otp:1.2.3.4", 1, Duration.ofMillis(40))).isFalse();
        Thread.sleep(80);
        assertThat(store.allow("otp:1.2.3.4", 1, Duration.ofMillis(40))).isTrue();
    }

    @Test
    @DisplayName("La table locale se purge de ses fenetres echues · elle ne grossissait jamais")
    void localTableIsPurged() {
        RateLimitStore store = localStore();

        // Au-dela du seuil, les fenetres deja echues sont retirees. Sans purge, ces dix mille et
        // une cles restaient en memoire pour la duree de vie du processus.
        for (int i = 0; i <= 10_000; i++) {
            store.allow("ephemere:" + i, 1, Duration.ofMillis(1));
        }
        store.allow("declencheur", 1, Duration.ofMinutes(1));

        assertThat(store.localSize()).isLessThan(10_000);
    }

    @Test
    @DisplayName("Quand Redis repond, c'est lui qui compte · le plafond vaut pour toutes les instances")
    void redisDoesTheCountingWhenAvailable() {
        AtomicLong counter = new AtomicLong();
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(ops.increment(anyString())).thenAnswer(invocation -> counter.incrementAndGet());
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.opsForValue()).thenReturn(ops);
        RateLimitStore store = new RateLimitStore(providerOf(template));

        assertThat(store.allow("login:1.2.3.4", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(store.allow("login:1.2.3.4", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(store.allow("login:1.2.3.4", 2, Duration.ofMinutes(1))).isFalse();
        // Rien en local · le comptage n'est pas duplique.
        assertThat(store.localSize()).isZero();
    }

    @Test
    @DisplayName("La fenetre est posee a la premiere requete, pas aux suivantes")
    void windowIsSetOnFirstRequestOnly() {
        AtomicLong counter = new AtomicLong();
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(ops.increment(anyString())).thenAnswer(invocation -> counter.incrementAndGet());
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.opsForValue()).thenReturn(ops);
        RateLimitStore store = new RateLimitStore(providerOf(template));

        store.allow("login:1.2.3.4", 5, Duration.ofSeconds(60));
        store.allow("login:1.2.3.4", 5, Duration.ofSeconds(60));
        store.allow("login:1.2.3.4", 5, Duration.ofSeconds(60));

        verify(template, times(1)).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Un Redis en panne ne ferme pas le service · on retombe sur la memoire locale")
    void redisFailureFallsBackInsteadOfRejecting() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(ops.increment(anyString())).thenThrow(new IllegalStateException("connexion perdue"));
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.opsForValue()).thenReturn(ops);
        RateLimitStore store = new RateLimitStore(providerOf(template));

        // Le trafic passe · un plafond approximatif vaut mieux que de refuser tout le monde
        // parce qu'un cache est tombe.
        assertThat(store.allow("login:1.2.3.4", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(store.allow("login:1.2.3.4", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(store.allow("login:1.2.3.4", 2, Duration.ofMinutes(1))).isFalse();
        verify(template, never()).expire(anyString(), any(Duration.class));
    }
}
