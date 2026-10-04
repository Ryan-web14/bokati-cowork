package com.sni.bokaticowork.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Refuse de servir la production avec une configuration manifestement non sure.
 *
 * <p>La securite de cette application tenait entierement aux surcharges de
 * {@code application-prod.yml} · les valeurs ecrites dans le code etaient les valeurs dangereuses.
 * Un profil Spring absent ou mal nomme suffisait a ouvrir l'administration a tous, avec un secret
 * de signature public et des jetons valables vingt-cinq heures. Rien ne le signalait.</p>
 *
 * <p>Ce controle ferme cette possibilite : en profil {@code prod}, chaque reglage dont une mauvaise
 * valeur serait une faille est verifie, et l'application <b>s'arrete</b> si l'un d'eux ne va pas.
 * Hors production, les memes constats sont journalises en avertissement · un developpeur doit
 * pouvoir travailler, il doit juste savoir ce qui est relache.</p>
 *
 * <p>Le controle est a {@code ApplicationReadyEvent} et non au demarrage d'un bean : il doit voir
 * l'environnement entierement resolu, et il vaut mieux s'arreter juste apres le demarrage avec un
 * message clair que echouer au milieu de l'initialisation avec une trace illisible.</p>
 */
@Slf4j
@Component
public class SecurityConfigurationGuard {

    /** La duree au-dela de laquelle un jeton d'acces n'est plus « court ». */
    private static final long MAX_ACCESS_TOKEN_MS = 60 * 60 * 1000L;

    private static final int MIN_SECRET_LENGTH = 32;

    private final Environment environment;

    @Value("${app.security.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.security.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${billing.fiscal.signing-key:}")
    private String fiscalSigningKey;

    @Value("${app.security.bootstrap.secret:}")
    private String bootstrapSecret;

    public SecurityConfigurationGuard(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verify() {
        List<String> faults = faults();
        if (faults.isEmpty()) {
            return;
        }
        if (isProduction()) {
            faults.forEach(fault -> log.error("Configuration de securite refusee · {}", fault));
            throw new IllegalStateException(
                    "Configuration de sécurité inacceptable en production : " + String.join(" | ", faults));
        }
        faults.forEach(fault -> log.warn("Configuration de securite relachee (hors production) · {}", fault));
    }

    /** Ce qui est verifie · chaque constat nomme le reglage et ce qu'il laisse passer. */
    List<String> faults() {
        List<String> faults = new ArrayList<>();

        if (!StringUtils.hasText(jwtSecret)) {
            faults.add("app.security.jwt.secret est vide · aucun jeton ne peut etre signe");
        } else if (jwtSecret.trim().length() < MIN_SECRET_LENGTH) {
            faults.add("app.security.jwt.secret fait moins de " + MIN_SECRET_LENGTH
                    + " caracteres · une cle courte se retrouve par force brute");
        }

        if (accessTokenExpirationMs > MAX_ACCESS_TOKEN_MS) {
            faults.add("app.security.jwt.access-token-expiration-ms vaut " + accessTokenExpirationMs
                    + " ms · un jeton vole reste exploitable " + (accessTokenExpirationMs / 3_600_000)
                    + " h, la limite acceptee est une heure");
        }

        if (!StringUtils.hasText(fiscalSigningKey)) {
            faults.add("billing.fiscal.signing-key est vide · aucune signature fiscale n'est verifiable");
        }

        // L'amorcage doit etre ferme une fois la maison installee. Un secret present en production
        // signifie qu'une route publique peut encore creer un administrateur · acceptable le jour
        // de l'installation, pas au-dela.
        if (isProduction() && StringUtils.hasText(bootstrapSecret)) {
            log.warn("app.security.bootstrap.secret est renseigne en production · la route d'amorcage "
                    + "reste ouverte tant qu'aucun administrateur n'existe. A retirer apres installation.");
        }

        if (environment.getProperty("app.security.auto-admin.enabled") != null) {
            faults.add("app.security.auto-admin.enabled est encore configure · ce mecanisme "
                    + "authentifiait en administrateur sans jeton et a ete supprime, la propriete "
                    + "doit disparaitre de la configuration");
        }

        return faults;
    }

    private boolean isProduction() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
