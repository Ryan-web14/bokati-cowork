package com.sni.bokaticowork.features.document.template.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.PieceField;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.PieceFields;
import com.sni.bokaticowork.features.document.template.model.DocumentFieldValue;
import com.sni.bokaticowork.features.document.template.model.DocumentTemplateVersion;
import com.sni.bokaticowork.features.document.template.render.TemplateContent;
import com.sni.bokaticowork.features.document.template.repository.DocumentFieldValueRepository;
import com.sni.bokaticowork.features.document.template.repository.DocumentTemplateVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Saisie des champs ajoutes d'une piece (lot D3) : les champs sont ceux de la version publiee du
 * modele, les valeurs sont rattachees a la piece metier et controlees selon le type du champ. Un
 * champ obligatoire non saisi bloque l'impression ({@link DocumentTemplatePrinter}).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DocumentFieldService {

    private final DocumentFieldValueRepository values;
    private final DocumentTemplateVersionRepository versions;

    @Transactional(readOnly = true)
    public PieceFields get(String templateCode, String sourceCode) {
        Optional<DocumentTemplateVersion> published = versions.findFirstByTemplateCodeAndStatus(templateCode, "PUBLISHED");
        List<TemplateContent.FieldSpec> specs = published.map(DocumentTemplatePrinter::content).map(TemplateContent::fieldSpecs).orElse(List.of());
        Map<String, DocumentFieldValue> saved = new HashMap<>();
        values.findByTemplateCodeAndSourceCode(templateCode, sourceCode).forEach(value -> saved.put(value.getFieldKey(), value));
        List<PieceField> fields = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (TemplateContent.FieldSpec spec : specs) {
            DocumentFieldValue value = spec.captured() ? saved.get(spec.key()) : null;
            fields.add(new PieceField(spec.key(), spec.dataKey(), spec.label(), spec.type(), spec.mode(), spec.required(), spec.options(), spec.help(),
                    value == null ? null : value.getValue(), value == null ? null : value.getEnteredBy(), value == null ? null : value.getEnteredAt()));
            if (spec.captured() && spec.required() && value == null) {
                missing.add(spec.key());
            }
        }
        return new PieceFields(templateCode, published.map(DocumentTemplateVersion::getVersionNumber).orElse(null), sourceCode, fields, missing);
    }

    /** Enregistre les valeurs donnees ; une valeur vide efface la saisie. Les autres champs ne changent pas. */
    public PieceFields save(String templateCode, String sourceCode, Map<String, Object> input) {
        TemplateContent content = versions.findFirstByTemplateCodeAndStatus(templateCode, "PUBLISHED").map(DocumentTemplatePrinter::content)
                .orElseThrow(() -> BadRequestException.coded("AUCUN_CHAMP", "Le modèle " + templateCode + " n'a pas de version publiée"));
        Map<String, TemplateContent.FieldSpec> specs = new HashMap<>();
        content.fieldSpecs().forEach(spec -> specs.put(spec.key(), spec));
        Map<String, Object> request = input == null ? Map.of() : input;
        List<String> errors = new ArrayList<>();
        Map<String, String> normalized = new HashMap<>();
        request.forEach((key, raw) -> {
            TemplateContent.FieldSpec spec = specs.get(key);
            if (spec == null) {
                errors.add("champ inconnu « " + key + " »");
            } else if (!spec.captured()) {
                errors.add("« " + spec.label() + " » s'imprime vide et se remplit à la main");
            } else {
                normalized.put(key, normalize(spec, raw, errors));
            }
        });
        if (!errors.isEmpty()) {
            throw BadRequestException.coded("CHAMP_INVALIDE", "Saisie refusée : " + String.join(" ; ", errors));
        }
        Map<String, DocumentFieldValue> saved = new HashMap<>();
        values.findByTemplateCodeAndSourceCode(templateCode, sourceCode).forEach(value -> saved.put(value.getFieldKey(), value));
        String actor = actor();
        Instant now = Instant.now();
        normalized.forEach((key, value) -> {
            DocumentFieldValue existing = saved.get(key);
            if (value == null) {
                if (existing != null) {
                    values.delete(existing);
                }
                return;
            }
            DocumentFieldValue row = existing != null ? existing
                    : DocumentFieldValue.builder().templateCode(templateCode).sourceCode(sourceCode).fieldKey(key).build();
            row.setValue(value);
            row.setEnteredBy(actor);
            row.setEnteredAt(now);
            values.save(row);
        });
        values.flush();
        return get(templateCode, sourceCode);
    }

    /** Valeur controlee et mise sous forme stable ; {@code null} pour effacer. */
    static String normalize(TemplateContent.FieldSpec spec, Object raw, List<String> errors) {
        String value = raw == null ? "" : raw.toString().trim();
        if (value.isEmpty()) {
            return null;
        }
        String label = "« " + spec.label() + " »";
        try {
            switch (spec.type()) {
                case "NUMBER" -> value = new BigDecimal(value.replace(',', '.').replace(" ", "")).stripTrailingZeros().toPlainString();
                case "AMOUNT" -> {
                    BigDecimal amount = new BigDecimal(value.replace(',', '.').replace(" ", ""));
                    if (amount.signum() < 0) {
                        errors.add(label + " : montant négatif");
                    }
                    value = amount.stripTrailingZeros().toPlainString();
                }
                case "DATE" -> value = LocalDate.parse(value).toString();
                case "CHECKBOX" -> {
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        errors.add(label + " : vrai ou faux attendu");
                    }
                    value = value.toLowerCase();
                }
                case "LIST" -> {
                    if (!spec.options().contains(value)) {
                        errors.add(label + " : valeur hors de la liste (" + String.join(", ", spec.options()) + ")");
                    }
                }
                default -> {
                }
            }
        } catch (RuntimeException ex) {
            errors.add(label + " : valeur invalide pour le type " + spec.type() + (spec.type().equals("DATE") ? " (AAAA-MM-JJ)" : ""));
            return null;
        }
        if (value.length() > 500) {
            errors.add(label + " : 500 caractères au plus");
        }
        return value;
    }

    private static String actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Optional.ofNullable(authentication).map(Authentication::getName).filter(name -> !name.isBlank()).orElse("SYSTEM");
    }
}
