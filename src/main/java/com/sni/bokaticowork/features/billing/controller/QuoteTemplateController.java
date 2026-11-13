package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.QuoteTemplateDtos;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.service.implementation.QuoteTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lot C14 : modeles de devis et articles composes (lots d'articles). */
@RestController
@RequestMapping(ApiPath.V1 + "/billing")
@RequiredArgsConstructor
public class QuoteTemplateController {

    private final QuoteTemplateService templates;

    @GetMapping("/quote-templates")
    public ResponseEntity<List<QuoteTemplateDtos.TemplateView>> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(templates.list(includeInactive));
    }

    @GetMapping("/quote-templates/{code}")
    public ResponseEntity<QuoteTemplateDtos.TemplateView> get(@PathVariable String code) {
        return ResponseEntity.ok(templates.get(code));
    }

    /** Cree ou remplace un modele (meme code). */
    @PutMapping("/quote-templates")
    public ResponseEntity<QuoteTemplateDtos.TemplateView> save(@Valid @RequestBody QuoteTemplateDtos.TemplateRequest request) {
        return ResponseEntity.ok(templates.save(request));
    }

    /** Devis en brouillon tire d'un modele, aux prix du client (contrat, segment, fiche). */
    @PostMapping("/quotes/from-template/{code}")
    public ResponseEntity<BillingDocumentResponse> fromTemplate(@PathVariable String code,
                                                                @Valid @RequestBody QuoteTemplateDtos.FromTemplateRequest request) {
        return ResponseEntity.ok(templates.quoteFromTemplate(code, request));
    }

    @GetMapping("/catalog/{itemCode}/components")
    public ResponseEntity<List<QuoteTemplateDtos.Component>> components(@PathVariable String itemCode) {
        return ResponseEntity.ok(templates.components(itemCode));
    }

    /** Remplace les composants d'un article ; une liste vide en fait un article simple. */
    @PutMapping("/catalog/{itemCode}/components")
    public ResponseEntity<List<QuoteTemplateDtos.Component>> setComponents(@PathVariable String itemCode,
                                                                          @Valid @RequestBody QuoteTemplateDtos.ComponentsRequest request) {
        return ResponseEntity.ok(templates.setComponents(itemCode, request.components()));
    }
}
