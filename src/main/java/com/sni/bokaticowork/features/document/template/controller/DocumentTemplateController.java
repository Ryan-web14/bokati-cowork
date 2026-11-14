package com.sni.bokaticowork.features.document.template.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.Catalog;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.Content;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.Detail;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.Summary;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.Version;
import com.sni.bokaticowork.features.document.template.service.DocumentTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Modeles de documents (lot D2), back-office, droit {@code SYSTEM:DOCUMENT_TEMPLATES}
 * ({@code AdminApiAuthorizationManager}). Un modele en blocs remplace a l'impression le gabarit
 * d'origine de sa piece tant qu'une version est publiee.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/document-templates")
@RequiredArgsConstructor
public class DocumentTemplateController {

    private final DocumentTemplateService service;

    @GetMapping
    public ResponseEntity<List<Summary>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{code}")
    public ResponseEntity<Detail> get(@PathVariable String code) {
        return ResponseEntity.ok(service.get(code));
    }

    /** Champs, tableaux et signataires que les blocs peuvent designer, avec des valeurs d'exemple. */
    @GetMapping("/{code}/fields/catalog")
    public ResponseEntity<Catalog> catalog(@PathVariable String code) {
        return ResponseEntity.ok(service.catalog(code));
    }

    @PutMapping("/{code}/draft")
    @Audited(module = "DOCUMENT_TEMPLATE", action = "SAVE_DRAFT", ressource = "document_template_version")
    public ResponseEntity<Version> saveDraft(@PathVariable String code, @Valid @RequestBody Content request) {
        return ResponseEntity.ok(service.saveDraft(code, request));
    }

    @DeleteMapping("/{code}/draft")
    @Audited(module = "DOCUMENT_TEMPLATE", action = "DELETE_DRAFT", ressource = "document_template_version")
    public ResponseEntity<Void> deleteDraft(@PathVariable String code) {
        service.deleteDraft(code);
        return ResponseEntity.noContent().build();
    }

    /**
     * Apercu PDF : contenu du corps s'il a des blocs, sinon le brouillon, sinon la version publiee ;
     * avec la piece {@code number} si elle est donnee, sinon des donnees d'exemple. Rien n'est ecrit.
     */
    @PostMapping(value = "/{code}/preview", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> preview(@PathVariable String code, @RequestParam(required = false) String number,
                                          @RequestBody(required = false) Content request) {
        byte[] pdf = service.preview(code, number, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"apercu-" + code.toLowerCase() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** Apercu HTML d'un courriel (lot E1) : contenu du corps, sinon brouillon, sinon version publiee ; valeurs d'exemple. */
    @PostMapping(value = "/{code}/preview-email", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewEmail(@PathVariable String code, @RequestBody(required = false) Content request) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(service.previewEmail(code, request));
    }

    /** Envoi d'essai d'un courriel a l'adresse de l'utilisateur connecte (lot E1). */
    @PostMapping("/{code}/test-send")
    @Audited(module = "DOCUMENT_TEMPLATE", action = "TEST_SEND", ressource = "document_template_version")
    public ResponseEntity<java.util.Map<String, String>> testSend(@PathVariable String code, @RequestBody(required = false) Content request) {
        return ResponseEntity.ok(java.util.Map.of("sentTo", service.testSend(code, request)));
    }

    @PostMapping("/{code}/publish")
    @Audited(module = "DOCUMENT_TEMPLATE", action = "PUBLISH", ressource = "document_template_version")
    public ResponseEntity<Version> publish(@PathVariable String code) {
        return ResponseEntity.ok(service.publish(code));
    }

    /** Ancienne version recopiee dans le brouillon, a relire puis publier. */
    @PostMapping("/{code}/restore/{version}")
    @Audited(module = "DOCUMENT_TEMPLATE", action = "RESTORE", ressource = "document_template_version")
    public ResponseEntity<Version> restore(@PathVariable String code, @PathVariable int version) {
        return ResponseEntity.ok(service.restore(code, version));
    }

    /** Retour au gabarit d'origine : la version publiee est archivee. */
    @PostMapping("/{code}/reset")
    @Audited(module = "DOCUMENT_TEMPLATE", action = "RESET", ressource = "document_template_version")
    public ResponseEntity<Detail> reset(@PathVariable String code) {
        return ResponseEntity.ok(service.reset(code));
    }
}
