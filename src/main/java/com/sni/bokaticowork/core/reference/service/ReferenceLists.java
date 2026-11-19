package com.sni.bokaticowork.core.reference.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.ListPatch;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.ListSummary;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.Value;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.ValuePatch;
import com.sni.bokaticowork.core.reference.dto.ReferenceDtos.ValueRequest;
import com.sni.bokaticowork.core.reference.model.ReferenceList;
import com.sni.bokaticowork.core.reference.model.ReferenceValue;
import com.sni.bokaticowork.core.reference.repository.ReferenceListRepository;
import com.sni.bokaticowork.core.reference.repository.ReferenceValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Listes de l'administrateur (socle des listes de choix, lot L0).
 *
 * <p>Un ecran lit une liste ({@link #values}), l'administrateur l'enrichit depuis son panneau, et
 * le service qui recoit un choix le controle par {@link #require} ou {@link #describe} : exposition
 * et controle lisent la meme table, ils ne divergent jamais. Chaque emploi est compte, ce qui fait
 * remonter les valeurs utiles et signale les « Autre, preciser » frequents.</p>
 *
 * <p>Motifs : un motif se saisit en {@code reasonCode} (valeur d'une liste) plus {@code reasonText}
 * (precision) ; l'ancien champ texte reste accepte ({@link #describeReason}).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceLists {

    public static final String OTHER = "OTHER";

    private final ReferenceListRepository listRepository;
    private final ReferenceValueRepository valueRepository;
    private final SessionActor actor;

    // ------------------------------------------------------------------ lecture

    /** Les listes, vides comprises, avec leurs compteurs ; filtre facultatif par module. */
    @Transactional(readOnly = true)
    public List<ListSummary> lists(String module) {
        List<ReferenceList> lists = StringUtils.hasText(module)
                ? listRepository.findByModuleOrderByLabelAsc(module.trim().toUpperCase(Locale.ROOT))
                : listRepository.findAllByOrderByModuleAscLabelAsc();
        if (lists.isEmpty()) {
            return List.of();
        }
        Map<String, List<ReferenceValue>> values = valueRepository
                .findByListCodeInOrderByListCodeAscSortOrderAscLabelAsc(lists.stream().map(ReferenceList::getCode).toList()).stream()
                .collect(Collectors.groupingBy(ReferenceValue::getListCode));
        return lists.stream().map(list -> summary(list, values.getOrDefault(list.getCode(), List.of()))).toList();
    }

    /** Valeurs d'une liste dans l'ordre d'affichage ; actives seulement sauf demande. */
    @Transactional(readOnly = true)
    public List<Value> values(String listCode, boolean includeInactive) {
        String code = normalize(listCode);
        List<ReferenceValue> values = valueRepository.findByListCodeOrderBySortOrderAscLabelAsc(code);
        if (values.isEmpty() && listRepository.findByCode(code).isEmpty()) {
            throw new ResourceNotFoundException("Liste inconnue : " + listCode);
        }
        return values.stream().filter(value -> includeInactive || Boolean.TRUE.equals(value.getActive())).map(ReferenceLists::toValue).toList();
    }

    /** Libelle d'un code, ou le code lui-meme s'il n'est plus dans la liste. */
    @Transactional(readOnly = true)
    public String label(String listCode, String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return valueRepository.findByListCodeAndCode(normalize(listCode), normalize(code)).map(ReferenceValue::getLabel).orElse(code);
    }

    // ------------------------------------------------------------------ controle

    /**
     * La valeur choisie, active.
     *
     * @throws BadRequestException code absent, inconnu (liste stricte) ou inactif
     */
    @Transactional
    public ReferenceValue require(String listCode, String code) {
        if (!StringUtils.hasText(code)) {
            throw BadRequestException.coded("VALEUR_REQUISE", "Choisissez une valeur dans la liste « " + listLabel(listCode) + " »");
        }
        String list = normalize(listCode);
        String normalized = normalize(code);
        Optional<ReferenceValue> found = valueRepository.findByListCodeAndCode(list, normalized);
        if (found.isEmpty()) {
            ReferenceList definition = listRepository.findByCode(list).orElse(null);
            if (definition != null && !Boolean.TRUE.equals(definition.getStrict())) {
                return addAutomatically(list, normalized, code.trim());
            }
            throw BadRequestException.coded("VALEUR_INCONNUE", "Valeur inconnue « " + code + " » dans la liste « " + listLabel(listCode) + " »");
        }
        ReferenceValue value = found.get();
        if (!Boolean.TRUE.equals(value.getActive())) {
            throw BadRequestException.coded("VALEUR_INACTIVE", "« " + value.getLabel() + " » n'est plus proposé dans la liste « "
                    + listLabel(listCode) + " »");
        }
        return value;
    }

    /**
     * Libelle a enregistrer pour un choix : le libelle de la liste, suivi de la precision s'il y
     * en a une. L'emploi est compte.
     *
     * @throws BadRequestException comme {@link #require}, ou precision manquante (« Autre, preciser »)
     */
    @Transactional
    public String describe(String listCode, String code, String text) {
        ReferenceValue value = require(listCode, code);
        String precision = StringUtils.hasText(text) ? text.trim() : null;
        if (Boolean.TRUE.equals(value.getRequiresText()) && precision == null) {
            throw BadRequestException.coded("PRECISION_REQUISE", "Précisez « " + value.getLabel() + " »");
        }
        value.setUsageCount((value.getUsageCount() == null ? 0L : value.getUsageCount()) + 1);
        valueRepository.save(value);
        return precision == null ? value.getLabel() : value.getLabel() + " : " + precision;
    }

    /**
     * Motif : {@code reasonCode} et {@code reasonText} si un code est donne ; sinon l'ancien texte
     * libre, accepte tel quel (compatibilite des ecrans), ou {@code null}.
     */
    @Transactional
    public String describeReason(String listCode, String reasonCode, String reasonText, String legacyText) {
        if (StringUtils.hasText(reasonCode)) {
            return describe(listCode, reasonCode, reasonText);
        }
        if (StringUtils.hasText(reasonText)) {
            return reasonText.trim();
        }
        return StringUtils.hasText(legacyText) ? legacyText.trim() : null;
    }

    // ------------------------------------------------------------------ administration

    /** Ajoute une valeur ; refuse un code deja present. */
    @Transactional
    public Value add(String listCode, ValueRequest request) {
        ReferenceList list = requireList(listCode);
        String code = normalize(request.code());
        if (valueRepository.findByListCodeAndCode(list.getCode(), code).isPresent()) {
            throw BadRequestException.coded("VALEUR_EXISTANTE", "La valeur « " + code + " » existe déjà dans la liste « " + list.getLabel() + " »");
        }
        int order = request.sortOrder() != null ? request.sortOrder()
                : valueRepository.findByListCodeOrderBySortOrderAscLabelAsc(list.getCode()).stream()
                .filter(value -> !OTHER.equals(value.getCode())).mapToInt(ReferenceValue::getSortOrder).max().orElse(0) + 10;
        ReferenceValue value = ReferenceValue.builder().listCode(list.getCode()).code(code).label(request.label().trim()).sortOrder(order)
                .active(request.active() == null || request.active())
                .requiresText(OTHER.equals(code) || Boolean.TRUE.equals(request.requiresText()))
                .systemValue(false).autoAdded(false).updatedBy(actor.resolve(null)).build();
        return toValue(valueRepository.save(value));
    }

    /** Modifie une valeur : libelle, ordre, active, precision. Une valeur relue n'est plus « ajoutee d'elle-meme ». */
    @Transactional
    public Value update(String listCode, String code, ValuePatch patch) {
        ReferenceList list = requireList(listCode);
        ReferenceValue value = valueRepository.findByListCodeAndCode(list.getCode(), normalize(code))
                .orElseThrow(() -> new ResourceNotFoundException("Valeur inconnue « " + code + " » dans la liste « " + list.getLabel() + " »"));
        if (patch.label() != null) {
            if (patch.label().isBlank()) {
                throw new BadRequestException("Le libellé ne peut pas être vide");
            }
            value.setLabel(patch.label().trim());
        }
        if (patch.sortOrder() != null) {
            value.setSortOrder(patch.sortOrder());
        }
        if (patch.active() != null) {
            value.setActive(patch.active());
        }
        if (patch.requiresText() != null) {
            value.setRequiresText(OTHER.equals(value.getCode()) || patch.requiresText());
        }
        value.setAutoAdded(false);
        value.setUpdatedBy(actor.resolve(null));
        return toValue(valueRepository.save(value));
    }

    /**
     * Remplace le contenu d'une liste. Une valeur absente n'est pas supprimee mais desactivee : les
     * enregistrements qui la citent gardent leur libelle.
     */
    @Transactional
    public List<Value> replace(String listCode, List<ValueRequest> requests) {
        String list = normalize(listCode);
        if (listRepository.findByCode(list).isEmpty() && valueRepository.findByListCodeOrderBySortOrderAscLabelAsc(list).isEmpty()) {
            throw new ResourceNotFoundException("Liste inconnue : " + listCode);
        }
        String who = actor.resolve(null);
        Map<String, ReferenceValue> existing = valueRepository.findByListCodeOrderBySortOrderAscLabelAsc(list).stream()
                .collect(Collectors.toMap(ReferenceValue::getCode, Function.identity(), (first, second) -> first, LinkedHashMap::new));
        List<ReferenceValue> saved = new ArrayList<>();
        int order = 10;
        for (ValueRequest request : requests == null ? List.<ValueRequest>of() : requests) {
            String code = normalize(request.code());
            ReferenceValue value = existing.remove(code);
            if (value == null) {
                value = ReferenceValue.builder().listCode(list).code(code).systemValue(false).build();
            }
            value.setLabel(request.label().trim());
            value.setSortOrder(request.sortOrder() == null ? order : request.sortOrder());
            value.setActive(request.active() == null || request.active());
            value.setRequiresText(OTHER.equals(code) || Boolean.TRUE.equals(request.requiresText()));
            value.setAutoAdded(false);
            value.setUpdatedBy(who);
            saved.add(value);
            order += 10;
        }
        for (ReferenceValue removed : existing.values()) {
            removed.setActive(false);
            removed.setUpdatedBy(who);
            saved.add(removed);
        }
        valueRepository.saveAll(saved);
        return values(list, true);
    }

    /** Libelle, description et mode d'une liste. */
    @Transactional
    public ListSummary updateList(String listCode, ListPatch patch) {
        ReferenceList list = requireList(listCode);
        if (StringUtils.hasText(patch.label())) {
            list.setLabel(patch.label().trim());
        }
        if (patch.description() != null) {
            list.setDescription(patch.description().isBlank() ? null : patch.description().trim());
        }
        if (patch.strict() != null) {
            list.setStrict(patch.strict());
        }
        listRepository.save(list);
        return summary(list, valueRepository.findByListCodeOrderBySortOrderAscLabelAsc(list.getCode()));
    }

    // ------------------------------------------------------------------ outils

    /** Code de liste ou de valeur : majuscules, chiffres et soulignes. */
    public static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
    }

    public static Value toValue(ReferenceValue value) {
        return new Value(value.getListCode(), value.getCode(), value.getLabel(), value.getSortOrder(), value.getActive(), value.getRequiresText(),
                value.getSystemValue(), value.getAutoAdded(), value.getUsageCount());
    }

    private ReferenceValue addAutomatically(String list, String code, String typed) {
        log.info("Liste souple {} : valeur « {} » ajoutée d'elle-même, à relire", list, typed);
        return valueRepository.save(ReferenceValue.builder().listCode(list).code(code).label(typed).sortOrder(900).active(true)
                .requiresText(false).systemValue(false).autoAdded(true).updatedBy(actor.resolve(null)).build());
    }

    private ReferenceList requireList(String listCode) {
        return listRepository.findByCode(normalize(listCode)).orElseThrow(() -> new ResourceNotFoundException("Liste inconnue : " + listCode));
    }

    private String listLabel(String listCode) {
        return listRepository.findByCode(normalize(listCode)).map(ReferenceList::getLabel).orElse(listCode);
    }

    private static ListSummary summary(ReferenceList list, List<ReferenceValue> values) {
        int active = (int) values.stream().filter(value -> Boolean.TRUE.equals(value.getActive())).count();
        int auto = (int) values.stream().filter(value -> Boolean.TRUE.equals(value.getAutoAdded())).count();
        long usage = values.stream().mapToLong(value -> value.getUsageCount() == null ? 0 : value.getUsageCount()).sum();
        long other = values.stream().filter(value -> OTHER.equals(value.getCode()))
                .mapToLong(value -> value.getUsageCount() == null ? 0 : value.getUsageCount()).sum();
        return new ListSummary(list.getCode(), list.getModule(), list.getLabel(), list.getDescription(), Boolean.TRUE.equals(list.getStrict()),
                values.size(), active, auto, usage, other);
    }
}
