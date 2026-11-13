package com.sni.bokaticowork.features.document.template.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.Catalog;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.ColumnEntry;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.Content;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.Detail;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.FieldEntry;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.SignerEntry;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.Summary;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.TableEntry;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.Version;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.VersionSummary;
import com.sni.bokaticowork.features.document.template.model.DocumentTemplate;
import com.sni.bokaticowork.features.document.template.model.DocumentTemplateVersion;
import com.sni.bokaticowork.features.document.template.render.DocumentData;
import com.sni.bokaticowork.features.document.template.render.TemplateContent;
import com.sni.bokaticowork.features.document.template.repository.DocumentTemplateRepository;
import com.sni.bokaticowork.features.document.template.repository.DocumentTemplateVersionRepository;
import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Administration des modeles de documents (lot D2) : un brouillon par modele, publication (la
 * version publiee precedente passe aux archives), restauration d'une ancienne version en
 * brouillon, retour au gabarit d'origine, apercu PDF et catalogue des cles utilisables.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DocumentTemplateService {

    static final String DRAFT = "DRAFT";
    static final String PUBLISHED = "PUBLISHED";
    static final String ARCHIVED = "ARCHIVED";

    private final DocumentTemplateRepository templates;
    private final DocumentTemplateVersionRepository versions;
    private final DocumentTemplatePrinter printer;
    private final DocumentDataSources dataSources;

    @Transactional(readOnly = true)
    public List<Summary> list() {
        Map<String, DocumentTemplateVersion> published = new java.util.HashMap<>();
        versions.findByStatus(PUBLISHED).forEach(version -> published.put(version.getTemplateCode(), version));
        java.util.Set<String> drafts = new java.util.HashSet<>();
        versions.findByStatus(DRAFT).forEach(version -> drafts.add(version.getTemplateCode()));
        return templates.findAllByOrderByFamilyAscDomainAscLabelAsc().stream().map(template -> {
            DocumentTemplateVersion version = published.get(template.getCode());
            return new Summary(template.getCode(), template.getLabel(), template.getFamily(), template.getDomain(),
                    Boolean.TRUE.equals(template.getActive()), version == null ? null : version.getVersionNumber(),
                    version == null ? null : version.getPublishedAt(), drafts.contains(template.getCode()));
        }).toList();
    }

    @Transactional(readOnly = true)
    public Detail get(String code) {
        DocumentTemplate template = template(code);
        List<DocumentTemplateVersion> all = versions.findByTemplateCodeOrderByVersionNumberDesc(template.getCode());
        Version published = all.stream().filter(version -> PUBLISHED.equals(version.getStatus())).findFirst().map(DocumentTemplateService::view).orElse(null);
        Version draft = all.stream().filter(version -> DRAFT.equals(version.getStatus())).findFirst().map(DocumentTemplateService::view).orElse(null);
        List<VersionSummary> history = all.stream().map(version -> new VersionSummary(version.getVersionNumber(), version.getStatus(), version.getNote(),
                version.getCreatedBy(), version.getCreatedAt(), version.getPublishedBy(), version.getPublishedAt(), version.getArchivedAt())).toList();
        return new Detail(template.getCode(), template.getLabel(), template.getFamily(), template.getDomain(), template.getFallbackTemplate(),
                template.getDescription(), Boolean.TRUE.equals(template.getActive()), published, draft, history);
    }

    /** Cree ou remplace le brouillon ; le contenu est controle avant d'etre enregistre. */
    public Version saveDraft(String code, Content request) {
        DocumentTemplate template = template(code);
        TemplateContent content = content(request).validate();
        DocumentTemplateVersion draft = versions.findFirstByTemplateCodeAndStatus(template.getCode(), DRAFT).orElseGet(() ->
                DocumentTemplateVersion.builder().templateCode(template.getCode()).versionNumber(versions.maxVersion(template.getCode()) + 1)
                        .status(DRAFT).createdBy(actor()).build());
        apply(draft, content, request.note());
        draft.setUpdatedAt(Instant.now());
        return view(versions.save(draft));
    }

    public void deleteDraft(String code) {
        DocumentTemplate template = template(code);
        DocumentTemplateVersion draft = versions.findFirstByTemplateCodeAndStatus(template.getCode(), DRAFT)
                .orElseThrow(() -> BadRequestException.coded("BROUILLON_ABSENT", "Le modèle " + code + " n'a pas de brouillon"));
        versions.delete(draft);
    }

    /** Publie le brouillon : il sert aussitot a l'impression ; la version publiee precedente est archivee. */
    public Version publish(String code) {
        DocumentTemplate template = template(code);
        DocumentTemplateVersion draft = versions.findFirstByTemplateCodeAndStatus(template.getCode(), DRAFT)
                .orElseThrow(() -> BadRequestException.coded("BROUILLON_ABSENT", "Le modèle " + code + " n'a pas de brouillon à publier"));
        TemplateContent content = DocumentTemplatePrinter.content(draft).validate();
        // Un modele qui ne se rend pas avec les donnees d'exemple n'est pas publie.
        if (dataSources.supports(template.getCode())) {
            try {
                printer.render(content, dataSources.sample(template.getCode()));
            } catch (PdfRenderer.PdfRenderException | org.thymeleaf.exceptions.TemplateProcessingException ex) {
                throw BadRequestException.coded("MODELE_INVALIDE", "Le modèle ne se rend pas : " + ex.getMessage());
            }
        }
        Instant now = Instant.now();
        versions.findFirstByTemplateCodeAndStatus(template.getCode(), PUBLISHED).ifPresent(previous -> {
            previous.setStatus(ARCHIVED);
            previous.setArchivedAt(now);
            versions.saveAndFlush(previous);
        });
        draft.setStatus(PUBLISHED);
        draft.setPublishedBy(actor());
        draft.setPublishedAt(now);
        DocumentTemplateVersion saved = versions.save(draft);
        printer.evict(template.getCode());
        return view(saved);
    }

    /** Copie une ancienne version dans le brouillon (cree ou remplace), a relire puis publier. */
    public Version restore(String code, int versionNumber) {
        DocumentTemplate template = template(code);
        DocumentTemplateVersion source = versions.findByTemplateCodeAndVersionNumber(template.getCode(), versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Version " + versionNumber + " du modèle " + code + " introuvable"));
        if (DRAFT.equals(source.getStatus())) {
            throw ConflictException.coded("VERSION_BROUILLON", "La version " + versionNumber + " est déjà le brouillon");
        }
        return saveDraft(code, new Content(source.getPageFormat(), source.getCopies(), source.getStyle(), source.getBlocks(),
                "Reprise de la version " + versionNumber));
    }

    /** Retour au gabarit d'origine : la version publiee est archivee, aucune n'est plus publiee. */
    public Detail reset(String code) {
        DocumentTemplate template = template(code);
        DocumentTemplateVersion published = versions.findFirstByTemplateCodeAndStatus(template.getCode(), PUBLISHED)
                .orElseThrow(() -> BadRequestException.coded("AUCUNE_VERSION_PUBLIEE", "Le modèle " + code + " sort déjà du gabarit d'origine"));
        published.setStatus(ARCHIVED);
        published.setArchivedAt(Instant.now());
        versions.save(published);
        printer.evict(template.getCode());
        return get(code);
    }

    /**
     * PDF d'apercu : le contenu fourni, sinon le brouillon, sinon la version publiee ; avec la piece
     * {@code number} si elle est donnee, sinon des donnees d'exemple.
     */
    @Transactional(readOnly = true)
    public byte[] preview(String code, String number, Content request) {
        DocumentTemplate template = template(code);
        if (!"PDF".equals(template.getFamily())) {
            throw BadRequestException.coded("APERCU_INDISPONIBLE", "L'aperçu PDF ne concerne que les pièces imprimées");
        }
        TemplateContent content;
        if (request != null && request.blocks() != null && !request.blocks().isEmpty()) {
            content = content(request).validate();
        } else {
            content = versions.findFirstByTemplateCodeAndStatus(template.getCode(), DRAFT)
                    .or(() -> versions.findFirstByTemplateCodeAndStatus(template.getCode(), PUBLISHED))
                    .map(DocumentTemplatePrinter::content)
                    .orElseThrow(() -> BadRequestException.coded("AUCUN_CONTENU", "Le modèle " + code + " n'a ni brouillon ni version publiée"));
        }
        DocumentData data = number == null || number.isBlank() ? dataSources.sample(template.getCode()) : dataSources.load(template.getCode(), number);
        try {
            return printer.render(content, data);
        } catch (PdfRenderer.PdfRenderException | org.thymeleaf.exceptions.TemplateProcessingException ex) {
            throw BadRequestException.coded("MODELE_INVALIDE", "Le modèle ne se rend pas : " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Catalog catalog(String code) {
        DocumentTemplate template = template(code);
        boolean supported = dataSources.supports(template.getCode());
        List<FieldEntry> fields = new ArrayList<>();
        List<TableEntry> tables = new ArrayList<>();
        List<SignerEntry> signers = new ArrayList<>();
        if (supported) {
            DocumentData sample = dataSources.sample(template.getCode());
            sample.fields().forEach((key, field) -> fields.add(new FieldEntry(key, field.label(), field.value())));
            sample.tables().forEach((key, table) -> tables.add(new TableEntry(key, table.title(),
                    table.columns().stream().map(column -> new ColumnEntry(column.key(), column.label(), column.numeric())).toList())));
            sample.signers().forEach((key, signer) -> signers.add(new SignerEntry(key, signer.role())));
        }
        return new Catalog(template.getCode(), fields, tables, signers, List.of("numero", "titre", "montant", "date_emission", "emis_par", "exploitant"),
                TemplateContent.BLOCK_TYPES.stream().sorted().toList(), TemplateContent.PAGE_FORMATS.stream().sorted().toList(), supported,
                dataSources.hint(template.getCode()));
    }

    // ------------------------------------------------------------------ outils

    private DocumentTemplate template(String code) {
        return templates.findByCode(code == null ? "" : code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Modèle de document introuvable : " + code));
    }

    private static TemplateContent content(Content request) {
        if (request == null) {
            throw BadRequestException.coded("MODELE_INVALIDE", "Contenu du modèle manquant");
        }
        return new TemplateContent(request.pageFormat(), request.copies(), request.style(), request.blocks());
    }

    private static void apply(DocumentTemplateVersion version, TemplateContent content, String note) {
        version.setPageFormat(content.pageFormat());
        version.setCopies(new ArrayList<>(content.copies()));
        version.setStyle(new java.util.LinkedHashMap<>(content.style()));
        version.setBlocks(new ArrayList<>(content.blocks()));
        version.setNote(note);
    }

    static Version view(DocumentTemplateVersion version) {
        return new Version(version.getVersionNumber(), version.getStatus(), version.getPageFormat(), version.getCopies(), version.getStyle(),
                version.getBlocks(), version.getNote(), version.getCreatedBy(), version.getCreatedAt(), version.getUpdatedAt(),
                version.getPublishedBy(), version.getPublishedAt(), version.getArchivedAt());
    }

    private static String actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Optional.ofNullable(authentication).map(Authentication::getName).filter(name -> !name.isBlank()).orElse("SYSTEM");
    }
}
