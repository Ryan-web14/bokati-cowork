package com.sni.bokaticowork.features.document.template.service;

import com.sni.bokaticowork.features.document.template.model.DocumentTemplate;
import com.sni.bokaticowork.features.document.template.render.EmailTemplates;
import com.sni.bokaticowork.features.document.template.repository.DocumentTemplateRepository;
import com.sni.bokaticowork.features.document.template.repository.DocumentTemplateVersionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Courriels servis par un modele publie (lot E1) : nom du gabarit d'origine ({@code billing-document},
 * {@code email/billing-document}) vers le gabarit genere. Releu au plus une fois par minute et
 * aussitot apres une publication ; une lecture en echec laisse les gabarits d'origine servir.
 *
 * <p>Porte aussi les apercus : un gabarit genere y est depose sous un nom provisoire le temps d'un
 * rendu.</p>
 */
@Slf4j
@Component
public class EmailTemplateRegistry {

    public static final String PREVIEW_PREFIX = "x-email-preview/";
    private static final Duration TTL = Duration.ofMinutes(1);

    private final DocumentTemplateRepository templates;
    private final DocumentTemplateVersionRepository versions;
    private volatile Map<String, String> generated = Map.of();
    private volatile Instant loadedAt = Instant.EPOCH;
    private final Map<String, String> previews = new ConcurrentHashMap<>();

    private final org.springframework.beans.factory.ObjectProvider<org.thymeleaf.spring6.SpringTemplateEngine> engine;

    public EmailTemplateRegistry(DocumentTemplateRepository templates, DocumentTemplateVersionRepository versions,
                                 org.springframework.beans.factory.ObjectProvider<org.thymeleaf.spring6.SpringTemplateEngine> engine) {
        this.templates = templates;
        this.versions = versions;
        this.engine = engine;
    }

    /** Gabarit genere pour ce nom, s'il existe un modele publie ; vide sinon. */
    public Optional<String> lookup(String templateName) {
        if (templateName == null) {
            return Optional.empty();
        }
        if (templateName.startsWith(PREVIEW_PREFIX)) {
            return Optional.ofNullable(previews.get(templateName));
        }
        refreshIfStale();
        return Optional.ofNullable(generated.get(normalize(templateName)));
    }

    public void invalidate() {
        loadedAt = Instant.EPOCH;
    }

    /** Depose un gabarit d'apercu et rend son nom ; a retirer avec {@link #dropPreview}. */
    public String putPreview(String html) {
        String name = PREVIEW_PREFIX + java.util.UUID.randomUUID();
        previews.put(name, html);
        return name;
    }

    public void dropPreview(String name) {
        previews.remove(name);
    }

    static String normalize(String name) {
        String value = name.trim();
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.startsWith("templates/")) {
            value = value.substring("templates/".length());
        }
        if (value.startsWith("email/")) {
            value = value.substring("email/".length());
        }
        if (value.endsWith(".html")) {
            value = value.substring(0, value.length() - 5);
        }
        return value;
    }

    private synchronized void refreshIfStale() {
        if (loadedAt.plus(TTL).isAfter(Instant.now())) {
            return;
        }
        Map<String, String> next = new HashMap<>();
        try {
            for (DocumentTemplate template : templates.findAllByOrderByFamilyAscDomainAscLabelAsc()) {
                if (!"EMAIL".equals(template.getFamily()) || !Boolean.TRUE.equals(template.getActive()) || template.getFallbackTemplate() == null) {
                    continue;
                }
                versions.findFirstByTemplateCodeAndStatus(template.getCode(), "PUBLISHED").ifPresent(version -> {
                    try {
                        next.put(normalize(template.getFallbackTemplate()), EmailTemplates.generate(version.getBlocks(), version.getStyle()));
                    } catch (RuntimeException ex) {
                        log.warn("Modele de courriel {} illisible, gabarit d'origine utilise : {}", template.getCode(), ex.getMessage());
                    }
                });
            }
            Map<String, String> previous = generated;
            generated = Map.copyOf(next);
            if (!previous.equals(generated)) {
                // Le moteur garde les gabarits deja lus : sans cela, un courriel deja envoye une fois
                // resterait servi par son fichier d'origine apres la publication de son modele.
                org.thymeleaf.spring6.SpringTemplateEngine current = engine.getIfAvailable();
                if (current != null) {
                    current.clearTemplateCache();
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Modeles de courriels illisibles, gabarits d'origine utilises : {}", ex.getMessage());
        }
        loadedAt = Instant.now();
    }
}
