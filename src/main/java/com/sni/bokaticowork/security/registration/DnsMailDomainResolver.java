package com.sni.bokaticowork.security.registration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interroge le DNS · MX d'abord, puis A et AAAA, comme le fait un serveur d'envoi (RFC 5321).
 *
 * <p>Une panne du DNS ne refuse personne · le verdict est alors {@code UNKNOWN} et l'inscription
 * continue. Seule une reponse nette (domaine inexistant, ou sans aucun enregistrement de courrier)
 * conduit a un refus.</p>
 *
 * <p>Les reponses sont gardees en memoire une heure · un robot qui insiste sur un meme domaine ne
 * declenche pas une requete DNS par tentative.</p>
 */
@Slf4j
@Component
public class DnsMailDomainResolver implements MailDomainResolver {

    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final int MAX_CACHE_ENTRIES = 5_000;

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    @Value("${app.security.registration.dns-timeout-ms:3000}")
    private long timeoutMs = 3000;

    private record Cached(Verdict verdict, Instant expiresAt) {
    }

    @Override
    public Verdict resolve(String domain) {
        Cached cached = cache.get(domain);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.verdict();
        }
        Verdict verdict = lookup(domain);
        if (verdict != Verdict.UNKNOWN) {
            if (cache.size() >= MAX_CACHE_ENTRIES) {
                cache.clear();
            }
            cache.put(domain, new Cached(verdict, Instant.now().plus(CACHE_TTL)));
        }
        return verdict;
    }

    private Verdict lookup(String domain) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", String.valueOf(timeoutMs));
        env.put("com.sun.jndi.dns.timeout.retries", "1");
        DirContext context = null;
        try {
            context = new InitialDirContext(env);
            Attributes attributes = context.getAttributes("dns:/" + domain, new String[]{"MX", "A", "AAAA"});
            return hasValue(attributes.get("MX")) || hasValue(attributes.get("A")) || hasValue(attributes.get("AAAA"))
                    ? Verdict.RECEIVES_MAIL
                    : Verdict.NO_MAIL;
        } catch (NameNotFoundException ex) {
            return Verdict.NO_MAIL;
        } catch (NamingException ex) {
            log.warn("Verification DNS du domaine {} impossible · l'inscription n'est pas bloquee : {}",
                    domain, ex.getMessage());
            return Verdict.UNKNOWN;
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException ignored) {
                    // rien a liberer de plus
                }
            }
        }
    }

    private static boolean hasValue(Attribute attribute) {
        return attribute != null && attribute.size() > 0;
    }
}
