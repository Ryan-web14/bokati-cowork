package com.sni.bokaticowork.features.billing.service.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentVersionResponse;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentEditHistory;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentEditHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Versionnage des documents · un document ne s'ecrase plus, chaque modification archive l'etat
 * precedent.
 *
 * <h2>Ce qui manquait</h2>
 * {@code billing_document_edit_history} existe depuis V121 et etait bien ecrite a chaque
 * modification, mais {@code snapshot_json} n'a jamais ete renseignee et {@code changed_by} valait
 * la constante {@code "SYSTEM"}. On savait qu'une modification avait eu lieu, jamais ce que le
 * document contenait avant, ni qui l'avait changee.
 *
 * <h2>Ce qui est archive</h2>
 * L'etat <b>avant</b> modification, pas apres : c'est ce qui permet de reconstituer ce que le
 * client avait sous les yeux. La version courante, elle, est le document lui-meme.
 *
 * <p>Un document scelle n'est pas versionne · il est immuable, et sa correction passe par un avoir
 * ou une facture rectificative.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingDocumentVersionService {

    private final BillingDocumentEditHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    /** Champs d'en-tete suivis · les montants et le destinataire, ce qui se negocie. */
    private static final List<String> TRACKED_HEADER_FIELDS = List.of(
            "documentNumber", "status", "customerCode", "customerName", "customerEmail",
            "currency", "issueDate", "dueDate", "title", "description", "terms",
            "subtotalAmount", "discountAmount", "taxableAmount", "vatAmount",
            "additionalCentAmount", "taxAmount", "totalAmount");

    /**
     * Archive l'etat courant du document comme nouvelle version, avant qu'il ne soit modifie.
     *
     * @param lines lignes telles qu'elles sont aujourd'hui · a lire avant toute modification
     */
    public BillingDocumentEditHistory archive(BillingDocument document, List<BillingDocumentLine> lines,
                                              String editType) {
        Map<String, Object> snapshot = snapshotOf(document, lines);
        Integer previousVersion = historyRepository.findLastVersionNumber(document);
        int versionNumber = previousVersion == null ? 1 : previousVersion + 1;

        BillingDocumentEditHistory entry = BillingDocumentEditHistory.builder()
                .document(document)
                .editType(editType)
                .changedBy(currentUser())
                .changedAt(Instant.now())
                .versionNumber(versionNumber)
                .snapshotJson(writeJson(snapshot))
                .changeSummary(summaryAgainstPrevious(document, snapshot))
                .build();
        return historyRepository.save(entry);
    }

    /**
     * Marque la derniere version comme transmise au client. Sans horodatage de transmission, rien
     * ne distingue un brouillon retouche d'une proposition reellement envoyee.
     */
    public void markLastVersionSent(BillingDocument document) {
        historyRepository.findFirstByDocumentOrderByVersionNumberDesc(document)
                .ifPresent(version -> {
                    if (version.getSentToCustomerAt() == null) {
                        version.setSentToCustomerAt(Instant.now());
                        historyRepository.save(version);
                    }
                });
    }

    /** Numero de revision a afficher · 1 tant qu'aucune version n'a ete archivee. */
    public int currentRevision(BillingDocument document) {
        Integer last = historyRepository.findLastVersionNumber(document);
        return last == null ? 1 : last + 1;
    }

    public List<BillingDocumentVersionResponse> versions(BillingDocument document) {
        return historyRepository.findAllByDocumentOrderByVersionNumberAsc(document).stream()
                .map(entry -> toResponse(entry, false))
                .toList();
    }

    public BillingDocumentVersionResponse version(BillingDocument document, Integer versionNumber) {
        return historyRepository.findByDocumentAndVersionNumber(document, versionNumber)
                .map(entry -> toResponse(entry, true))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " introuvable pour " + document.getDocumentNumber()));
    }

    /**
     * Ecart entre deux versions archivees. Les listes de lignes sont comparees dans leur ensemble
     * plutot que ligne a ligne : un reordonnancement, une suppression ou un ajout deplacent les
     * indices, et une comparaison positionnelle rapporterait des changements qui n'en sont pas.
     */
    public BillingDocumentVersionResponse.Diff diff(BillingDocument document, Integer from, Integer to) {
        Map<String, Object> before = snapshotAt(document, from);
        Map<String, Object> after = snapshotAt(document, to);

        List<BillingDocumentVersionResponse.Diff.FieldChange> changes = new ArrayList<>();
        for (String field : new TreeSet<>(union(before, after))) {
            Object valueBefore = before.get(field);
            Object valueAfter = after.get(field);
            if (!Objects.equals(valueBefore, valueAfter)) {
                changes.add(new BillingDocumentVersionResponse.Diff.FieldChange(field, valueBefore, valueAfter));
            }
        }
        return new BillingDocumentVersionResponse.Diff(from, to, changes);
    }

    // =================================================================================
    // Construction de l'instantane
    // =================================================================================

    private Map<String, Object> snapshotOf(BillingDocument document, List<BillingDocumentLine> lines) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("documentNumber", document.getDocumentNumber());
        snapshot.put("status", document.getStatus() == null ? null : document.getStatus().name());
        snapshot.put("customerCode", document.getCustomerCode());
        snapshot.put("customerName", document.getCustomerName());
        snapshot.put("customerEmail", document.getCustomerEmail());
        snapshot.put("currency", document.getCurrency());
        snapshot.put("issueDate", asText(document.getIssueDate()));
        snapshot.put("dueDate", asText(document.getDueDate()));
        snapshot.put("title", document.getTitle());
        snapshot.put("description", document.getDescription());
        snapshot.put("terms", document.getTerms());
        snapshot.put("subtotalAmount", asText(document.getSubtotalAmount()));
        snapshot.put("discountAmount", asText(document.getDiscountAmount()));
        snapshot.put("taxableAmount", asText(document.getTaxableAmount()));
        snapshot.put("vatAmount", asText(document.getVatAmount()));
        snapshot.put("additionalCentAmount", asText(document.getAdditionalCentAmount()));
        snapshot.put("taxAmount", asText(document.getTaxAmount()));
        snapshot.put("totalAmount", asText(document.getTotalAmount()));

        List<Map<String, Object>> lineSnapshots = new ArrayList<>();
        if (lines != null) {
            for (BillingDocumentLine line : lines) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("lineOrder", line.getLineOrder());
                entry.put("itemCode", line.getItemCode());
                entry.put("category", line.getCategory());
                entry.put("description", line.getDescription());
                entry.put("unit", line.getUnit());
                entry.put("quantity", asText(line.getQuantity()));
                entry.put("unitPrice", asText(line.getUnitPrice()));
                entry.put("discountRate", asText(line.getDiscountRate()));
                entry.put("discountAmount", asText(line.getDiscountAmount()));
                entry.put("subtotalAmount", asText(line.getSubtotalAmount()));
                entry.put("totalAmount", asText(line.getTotalAmount()));
                entry.put("taxable", line.getTaxable());
                entry.put("optional", line.getOptional());
                lineSnapshots.add(entry);
            }
        }
        snapshot.put("lines", lineSnapshots);
        return snapshot;
    }

    /**
     * Resume des champs modifies depuis la version precedente. Rendu lisible : c'est ce que
     * l'utilisateur lit dans la liste, sans avoir a ouvrir le detail ni a comparer deux etats.
     */
    private String summaryAgainstPrevious(BillingDocument document, Map<String, Object> current) {
        Optional<BillingDocumentEditHistory> previous =
                historyRepository.findFirstByDocumentOrderByVersionNumberDesc(document);
        if (previous.isEmpty() || !StringUtils.hasText(previous.get().getSnapshotJson())) {
            return "Version initiale";
        }
        Map<String, Object> before = readJson(previous.get().getSnapshotJson());

        List<String> changed = new ArrayList<>();
        for (String field : TRACKED_HEADER_FIELDS) {
            if (!Objects.equals(before.get(field), current.get(field))) {
                changed.add(field);
            }
        }
        if (!Objects.equals(before.get("lines"), current.get("lines"))) {
            changed.add("lignes");
        }
        return changed.isEmpty() ? "Aucun champ suivi modifie" : String.join(", ", changed);
    }

    private Map<String, Object> snapshotAt(BillingDocument document, Integer versionNumber) {
        return historyRepository.findByDocumentAndVersionNumber(document, versionNumber)
                .map(entry -> readJson(entry.getSnapshotJson()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " introuvable pour " + document.getDocumentNumber()));
    }

    private BillingDocumentVersionResponse toResponse(BillingDocumentEditHistory entry, boolean withSnapshot) {
        return new BillingDocumentVersionResponse(
                entry.getVersionNumber(),
                entry.getEditType(),
                entry.getChangedBy(),
                entry.getChangedAt(),
                entry.getSentToCustomerAt(),
                entry.getChangeSummary(),
                withSnapshot ? readJson(entry.getSnapshotJson()) : null);
    }

    // =================================================================================
    // Utilitaires
    // =================================================================================

    private List<String> union(Map<String, Object> a, Map<String, Object> b) {
        List<String> keys = new ArrayList<>(a.keySet());
        b.keySet().stream().filter(key -> !keys.contains(key)).forEach(keys::add);
        return keys;
    }

    /**
     * Les montants et dates sont archives en texte. Un {@code BigDecimal} relu depuis JSON
     * reviendrait en {@code Double} et deux versions identiques ressortiraient differentes a la
     * comparaison, sur une simple difference de representation.
     */
    private String asText(Object value) {
        return value == null ? null : value.toString();
    }

    private String writeJson(Map<String, Object> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            log.warn("Instantane de version illisible · document archive sans contenu : {}", ex.getMessage());
            return null;
        }
    }

    private Map<String, Object> readJson(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            log.warn("Instantane illisible en base : {}", ex.getMessage());
            return Map.of();
        }
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !StringUtils.hasText(authentication.getName())
                ? "SYSTEM"
                : authentication.getName();
    }
}
