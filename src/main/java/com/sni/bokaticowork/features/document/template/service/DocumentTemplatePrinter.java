package com.sni.bokaticowork.features.document.template.service;

import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.features.document.template.model.DocumentTemplate;
import com.sni.bokaticowork.features.document.template.model.DocumentTemplateVersion;
import com.sni.bokaticowork.features.document.template.render.DocumentData;
import com.sni.bokaticowork.features.document.template.render.TemplateContent;
import com.sni.bokaticowork.features.document.template.render.TemplateRenderer;
import com.sni.bokaticowork.features.document.template.repository.DocumentFieldValueRepository;
import com.sni.bokaticowork.features.document.template.repository.DocumentTemplateRepository;
import com.sni.bokaticowork.features.document.template.repository.DocumentTemplateVersionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
    private final DocumentFieldValueRepository fieldValues;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public DocumentTemplatePrinter(DocumentTemplateRepository templates, DocumentTemplateVersionRepository versions,
                                   OperatorIdentityService operatorIdentityService, BrandLogo brandLogo, SpringTemplateEngine templateEngine,
                                   DocumentFieldValueRepository fieldValues) {
        this.fieldValues = fieldValues;
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
        // Champs ajoutes (lot D3) : un champ obligatoire non saisi bloque l'impression, avec la liste
        // des champs a saisir. C'est une regle de l'exploitant, pas un echec du rendu : elle remonte.
        try {
            addFields(content.get(), data, false);
        } catch (MissingFieldsException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Champs ajoutes du modele {} illisibles, gabarit d'origine utilise : {}", templateCode, ex.getMessage());
            return Optional.empty();
        }
        try {
            return Optional.of(render(content.get(), data));
        } catch (RuntimeException ex) {
            log.warn("Rendu du modele {} en echec pour la piece {}, gabarit d'origine utilise : {}", templateCode, data.number(), ex.getMessage());
            return Optional.empty();
        }
    }

    /** PDF produit et numero de la version du modele qui l'a produit. */
    public record Printed(byte[] pdf, int version) {
    }

    /**
     * Rendu d'une piece dont la version de modele est figee (lot D5, factures) : {@code pinned}
     * nul prend la version publiee ; {@code 0} veut dire « gabarit d'origine » (rien n'est rendu
     * ici) ; un autre numero rend cette version, meme archivee. Vide quand le gabarit d'origine sert.
     */
    public Optional<Printed> tryRenderPinned(String templateCode, DocumentData data, Integer pinned) {
        if (pinned != null && pinned == 0) {
            return Optional.empty();
        }
        Optional<DocumentTemplateVersion> version;
        try {
            version = pinned == null
                    ? templates.findByCode(templateCode).filter(template -> Boolean.TRUE.equals(template.getActive()))
                    .flatMap(template -> versions.findFirstByTemplateCodeAndStatus(templateCode, "PUBLISHED"))
                    : versions.findByTemplateCodeAndVersionNumber(templateCode, pinned).filter(found -> !"DRAFT".equals(found.getStatus()));
        } catch (RuntimeException ex) {
            log.warn("Modele {} illisible, gabarit d'origine utilise : {}", templateCode, ex.getMessage());
            return Optional.empty();
        }
        if (version.isEmpty()) {
            return Optional.empty();
        }
        TemplateContent content = content(version.get());
        addFields(content, data, false);
        try {
            return Optional.of(new Printed(render(content, data), version.get().getVersionNumber()));
        } catch (RuntimeException ex) {
            log.warn("Rendu du modele {} v{} en echec pour la piece {}, gabarit d'origine utilise : {}", templateCode,
                    version.get().getVersionNumber(), data.number(), ex.getMessage());
            return Optional.empty();
        }
    }

    /** Rendu d'un contenu donne (apercu d'un brouillon) ; les erreurs remontent a l'appelant. */
    public byte[] render(TemplateContent content, DocumentData data) {
        return renderer.pdf(content, data, operatorIdentityService.current(), brandLogo.base64());
    }

    /**
     * Ajoute aux donnees les champs du modele : vides pour ceux a remplir a la main, valeurs saisies
     * pour les autres. Avec {@code sample}, une valeur fictive remplace la saisie et rien n'est exige.
     *
     * @throws MissingFieldsException si un champ obligatoire n'est pas saisi pour cette piece
     */
    public void addFields(TemplateContent content, DocumentData data, boolean sample) {
        List<TemplateContent.FieldSpec> specs = content.fieldSpecs();
        if (specs.isEmpty()) {
            return;
        }
        Map<String, String> values = new java.util.HashMap<>();
        if (!sample && data.sourceCode() != null) {
            fieldValues.findByTemplateCodeAndSourceCode(data.code(), data.sourceCode())
                    .forEach(value -> values.put(value.getFieldKey(), value.getValue()));
        }
        List<String> missing = new java.util.ArrayList<>();
        for (TemplateContent.FieldSpec spec : specs) {
            if (!spec.captured()) {
                data.blankField(spec.dataKey(), spec.label());
                continue;
            }
            String raw = sample ? sampleValue(spec) : values.get(spec.key());
            if ((raw == null || raw.isBlank()) && spec.required() && !sample) {
                missing.add(spec.label());
            }
            data.field(spec.dataKey(), spec.label(), printable(spec, raw));
        }
        if (!missing.isEmpty()) {
            throw new MissingFieldsException(data.sourceCode(), missing);
        }
    }

    /** Valeur imprimee : date jj/mm/aaaa, montant groupe par milliers, case cochee Oui ou Non. */
    public static String printable(TemplateContent.FieldSpec spec, String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            return switch (spec.type()) {
                case "DATE" -> java.time.LocalDate.parse(raw).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                case "AMOUNT" -> com.sni.bokaticowork.features.fund.voucher.VoucherService.money(new java.math.BigDecimal(raw));
                case "CHECKBOX" -> Boolean.parseBoolean(raw) ? "Oui" : "Non";
                default -> raw;
            };
        } catch (RuntimeException ex) {
            return raw;
        }
    }

    private static String sampleValue(TemplateContent.FieldSpec spec) {
        return switch (spec.type()) {
            case "DATE" -> "2026-11-12";
            case "AMOUNT" -> "50000";
            case "NUMBER" -> "3";
            case "CHECKBOX" -> "true";
            case "LIST" -> spec.options().isEmpty() ? "" : spec.options().get(0);
            default -> "Valeur saisie";
        };
    }

    /** Champs obligatoires non saisis : l'impression est refusee (code {@code CHAMPS_A_SAISIR}). */
    public static class MissingFieldsException extends com.sni.bokaticowork.core.exception.customs.BadRequestException {
        public MissingFieldsException(String sourceCode, List<String> labels) {
            super("Champs à saisir avant d'imprimer la pièce " + sourceCode + " : " + String.join(", ", labels));
            withReason("CHAMPS_A_SAISIR");
        }
    }

    public void evict(String templateCode) {
        cache.remove(templateCode);
    }

    public Optional<TemplateContent> published(String templateCode) {
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
        return new TemplateContent(version.getPageFormat(), version.getCopies(), version.getStyle(), version.getBlocks(), version.getFields());
    }
}
