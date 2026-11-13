package com.sni.bokaticowork.features.document.template.service;

import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.features.document.template.model.DocumentTemplate;
import com.sni.bokaticowork.features.document.template.model.DocumentTemplateVersion;
import com.sni.bokaticowork.features.document.template.render.DocumentData;
import com.sni.bokaticowork.features.document.template.render.TemplateContent;
import com.sni.bokaticowork.features.document.template.render.TemplateRenderer;
import com.sni.bokaticowork.features.document.template.repository.DocumentTemplateRepository;
import com.sni.bokaticowork.features.document.template.repository.DocumentTemplateVersionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Impression d'une piece avec la version publiee de son modele (lot D2).
 *
 * <p>{@link #tryRender} rend vide quand le modele n'a pas de version publiee, est inactif, ou
 * quand son rendu echoue : l'appelant imprime alors avec son gabarit Thymeleaf d'origine. Un echec
 * est journalise, jamais remonte : une piece de caisse doit toujours pouvoir sortir.</p>
 *
 * <p>Les versions publiees sont gardees une minute en memoire ; une publication vide l'entree du
 * modele aussitot ({@link #evict}).</p>
 */
@Slf4j
@Component
public class DocumentTemplatePrinter {

    private static final Duration TTL = Duration.ofMinutes(1);

    private record Cached(Optional<TemplateContent> content, Instant loadedAt) {
    }

    private final DocumentTemplateRepository templates;
    private final DocumentTemplateVersionRepository versions;
    private final OperatorIdentityService operatorIdentityService;
    private final BrandLogo brandLogo;
    private final TemplateRenderer renderer;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public DocumentTemplatePrinter(DocumentTemplateRepository templates, DocumentTemplateVersionRepository versions,
                                   OperatorIdentityService operatorIdentityService, BrandLogo brandLogo, SpringTemplateEngine templateEngine) {
        this.templates = templates;
        this.versions = versions;
        this.operatorIdentityService = operatorIdentityService;
        this.brandLogo = brandLogo;
        this.renderer = new TemplateRenderer(templateEngine);
    }

    public Optional<byte[]> tryRender(String templateCode, DocumentData data) {
        Optional<TemplateContent> content;
        try {
            content = published(templateCode);
        } catch (RuntimeException ex) {
            log.warn("Modele {} illisible, gabarit d'origine utilise : {}", templateCode, ex.getMessage());
            return Optional.empty();
        }
        if (content.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(render(content.get(), data));
        } catch (RuntimeException ex) {
            log.warn("Rendu du modele {} en echec pour la piece {}, gabarit d'origine utilise : {}", templateCode, data.number(), ex.getMessage());
            return Optional.empty();
        }
    }

    /** Rendu d'un contenu donne (apercu d'un brouillon) ; les erreurs remontent a l'appelant. */
    public byte[] render(TemplateContent content, DocumentData data) {
        return renderer.pdf(content, data, operatorIdentityService.current(), brandLogo.base64());
    }

    public void evict(String templateCode) {
        cache.remove(templateCode);
    }

    Optional<TemplateContent> published(String templateCode) {
        Cached cached = cache.get(templateCode);
        if (cached != null && cached.loadedAt().plus(TTL).isAfter(Instant.now())) {
            return cached.content();
        }
        Optional<TemplateContent> content = templates.findByCode(templateCode)
                .filter(template -> Boolean.TRUE.equals(template.getActive()))
                .filter(template -> "PDF".equals(template.getFamily()))
                .map(DocumentTemplate::getCode)
                .flatMap(code -> versions.findFirstByTemplateCodeAndStatus(code, "PUBLISHED"))
                .map(DocumentTemplatePrinter::content);
        cache.put(templateCode, new Cached(content, Instant.now()));
        return content;
    }

    public static TemplateContent content(DocumentTemplateVersion version) {
        return new TemplateContent(version.getPageFormat(), version.getCopies(), version.getStyle(), version.getBlocks());
    }
}
