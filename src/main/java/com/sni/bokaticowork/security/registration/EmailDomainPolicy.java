package com.sni.bokaticowork.security.registration;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Les adresses qu'on accepte a l'inscription en libre-service.
 *
 * <p>Un robot s'est inscrit avec {@code pentest-...@example.com} · un domaine reserve, qui ne
 * recevra jamais le code de verification. Le compte restait la, inutile, comme tout compte cree
 * avec une boite jetable. Trois refus, dans l'ordre :</p>
 * <ol>
 *   <li>les domaines reserves par la RFC 2606 et la RFC 6761 ({@code example.com},
 *       {@code .test}, {@code .invalid}, {@code .localhost}...) ;</li>
 *   <li>les boites jetables connues (liste dans {@code security/disposable-email-domains.txt}, plus
 *       {@code app.security.registration.blocked-domains}), sous-domaines compris ;</li>
 *   <li>un domaine qui ne recoit pas de courrier · ni MX ni adresse dans le DNS. Une panne du DNS ne
 *       refuse personne.</li>
 * </ol>
 *
 * <p>Ne s'applique qu'a l'inscription publique · l'accueil cree des membres sans passer ici.</p>
 */
@Slf4j
@Component
public class EmailDomainPolicy {

    static final String REFUSED_MESSAGE =
            "Cette adresse e-mail n'est pas acceptee. Utilisez une adresse personnelle ou professionnelle valide.";

    private static final Set<String> RESERVED_DOMAINS = Set.of(
            "example.com", "example.net", "example.org", "example.edu", "localhost", "invalid", "test", "example");

    private static final Set<String> RESERVED_SUFFIXES = Set.of(
            ".example", ".test", ".invalid", ".localhost", ".local");

    private final MailDomainResolver resolver;
    private final Set<String> blockedDomains;
    private final boolean mxCheckEnabled;

    public EmailDomainPolicy(MailDomainResolver resolver,
                             @Value("${app.security.registration.blocked-domains:}") String extraBlockedDomains,
                             @Value("${app.security.registration.mx-check-enabled:true}") boolean mxCheckEnabled) {
        this.resolver = resolver;
        this.mxCheckEnabled = mxCheckEnabled;
        Set<String> domains = new HashSet<>(loadDisposableDomains());
        Arrays.stream(extraBlockedDomains.split(","))
                .map(EmailDomainPolicy::normalize)
                .filter(StringUtils::hasText)
                .forEach(domains::add);
        this.blockedDomains = Set.copyOf(domains);
    }

    /** Refuse l'adresse si elle ne peut pas servir · le message ne dit pas laquelle des regles a joue. */
    public void assertAcceptable(String email) {
        String domain = domainOf(email);
        if (domain == null) {
            throw new BadRequestException(REFUSED_MESSAGE);
        }
        String reason = refusalReason(domain);
        if (reason != null) {
            log.info("Inscription refusee pour le domaine {} · {}", domain, reason);
            throw new BadRequestException(REFUSED_MESSAGE);
        }
    }

    /** Le motif interne du refus, ou {@code null} si le domaine est accepte. */
    String refusalReason(String domain) {
        if (isReserved(domain)) {
            return "domaine reserve";
        }
        if (isBlocked(domain)) {
            return "boite jetable";
        }
        if (mxCheckEnabled && resolver.resolve(domain) == MailDomainResolver.Verdict.NO_MAIL) {
            return "aucun serveur de courrier";
        }
        return null;
    }

    private static boolean isReserved(String domain) {
        return RESERVED_DOMAINS.contains(domain)
                || RESERVED_SUFFIXES.stream().anyMatch(domain::endsWith)
                || RESERVED_DOMAINS.stream().anyMatch(reserved -> domain.endsWith("." + reserved));
    }

    /** Le domaine ou l'un de ses parents est-il bloque · {@code x.mailinator.com} l'est. */
    private boolean isBlocked(String domain) {
        String candidate = domain;
        while (candidate.contains(".")) {
            if (blockedDomains.contains(candidate)) {
                return true;
            }
            candidate = candidate.substring(candidate.indexOf('.') + 1);
        }
        return false;
    }

    static String domainOf(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        String trimmed = email.trim();
        int at = trimmed.lastIndexOf('@');
        if (at <= 0 || at == trimmed.length() - 1) {
            return null;
        }
        String domain = normalize(trimmed.substring(at + 1));
        return domain.contains(".") || "localhost".equals(domain) ? domain : null;
    }

    private static String normalize(String domain) {
        String value = domain == null ? "" : domain.trim().toLowerCase(Locale.ROOT);
        return value.endsWith(".") ? value.substring(0, value.length() - 1) : value;
    }

    private static Set<String> loadDisposableDomains() {
        ClassPathResource resource = new ClassPathResource("security/disposable-email-domains.txt");
        if (!resource.exists()) {
            log.warn("Liste des boites jetables introuvable · seuls les domaines configures sont bloques");
            return Set.of();
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(EmailDomainPolicy::normalize)
                    .collect(Collectors.toSet());
        } catch (IOException ex) {
            throw new IllegalStateException("Lecture de la liste des boites jetables impossible", ex);
        }
    }
}
