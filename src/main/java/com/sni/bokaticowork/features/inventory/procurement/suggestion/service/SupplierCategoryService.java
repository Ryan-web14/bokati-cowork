package com.sni.bokaticowork.features.inventory.procurement.suggestion.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryCategory;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryCategoryRepository;
import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierRepository;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.dto.SupplierSuggestionDtos.*;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryKind;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryLink;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryRef;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryRefTarget;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.repository.SupplierCategoryLinkRepository;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.repository.SupplierCategoryRefRepository;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.repository.SupplierCategoryRefTargetRepository;
import com.sni.bokaticowork.features.payment.model.CashOperationNature;
import com.sni.bokaticowork.features.payment.repository.CashOperationNatureRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Referentiel des categories de fournisseurs et rattachement des fournisseurs d'inventaire a leurs
 * categories (nature d'operation, categorie de stock, categorie fournisseur).
 *
 * <p>Une categorie fournisseur est reliee a des natures d'operation et a des categories de stock :
 * c'est le pont entre la categorie d'une ligne de besoin et les fournisseurs a proposer.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SupplierCategoryService {

    private final SupplierCategoryRefRepository refRepository;
    private final SupplierCategoryRefTargetRepository targetRepository;
    private final SupplierCategoryLinkRepository linkRepository;
    private final SupplierRepository supplierRepository;
    private final CashOperationNatureRepository natureRepository;
    private final InventoryCategoryRepository inventoryCategoryRepository;
    private final CashActorResolver actors;

    // ------------------------------------------------------------------ referentiel

    @Transactional(readOnly = true)
    public List<RefCategoryView> categories(String query, Boolean active, String parentCode) {
        List<SupplierCategoryRef> all = refRepository.findAllByOrderBySortOrderAscLabelAsc();
        Long parentId = null;
        if (StringUtils.hasText(parentCode)) {
            parentId = ref(parentCode).getId();
        }
        String text = StringUtils.hasText(query) ? fold(query) : null;
        Long parentFilter = parentId;
        List<SupplierCategoryRef> selected = all.stream()
                .filter(category -> active == null || active.equals(category.getActive()))
                .filter(category -> parentFilter == null || parentFilter.equals(category.getParentId()))
                .filter(category -> text == null || fold(category.getCode()).contains(text) || fold(category.getLabel()).contains(text)
                        || (category.getDescription() != null && fold(category.getDescription()).contains(text)))
                .toList();
        return views(selected, all);
    }

    @Transactional(readOnly = true)
    public RefCategoryView category(String code) {
        SupplierCategoryRef category = ref(code);
        return views(List.of(category), refRepository.findAllByOrderBySortOrderAscLabelAsc()).get(0);
    }

    public RefCategoryView create(RefCategoryRequest request) {
        String code = StringUtils.hasText(request.code()) ? normalizeCode(request.code()) : uniqueCode(request.label());
        if (refRepository.existsByCode(code)) {
            throw new ConflictException("Une categorie fournisseur porte deja le code " + code);
        }
        String actor = actors.resolve("SYSTEM");
        SupplierCategoryRef category = SupplierCategoryRef.builder().code(code).createdBy(actor).updatedBy(actor).build();
        apply(category, request, true);
        return category(category.getCode());
    }

    public RefCategoryView update(String code, RefCategoryRequest request) {
        SupplierCategoryRef category = ref(code);
        category.setUpdatedBy(actors.resolve("SYSTEM"));
        apply(category, request, false);
        return category(category.getCode());
    }

    public RefCategoryView setActive(String code, boolean active) {
        SupplierCategoryRef category = ref(code);
        category.setActive(active);
        category.setUpdatedBy(actors.resolve("SYSTEM"));
        refRepository.save(category);
        return category(category.getCode());
    }

    /** Suppression d'une categorie jamais utilisee ; sinon, on la desactive. */
    public void delete(String code) {
        SupplierCategoryRef category = ref(code);
        long suppliers = linkRepository.countByCategoryKindAndCategoryCode(SupplierCategoryKind.SUPPLIER_CATEGORY, category.getCode());
        if (suppliers > 0) {
            throw new ConflictException("La categorie " + category.getLabel() + " classe " + suppliers
                    + " fournisseur(s) : desactivez-la plutot que de la supprimer");
        }
        if (refRepository.existsByParentId(category.getId())) {
            throw new ConflictException("La categorie " + category.getLabel() + " a des sous-categories : desactivez-la plutot que de la supprimer");
        }
        targetRepository.deleteAllByCategoryId(category.getId());
        refRepository.delete(category);
    }

    private void apply(SupplierCategoryRef category, RefCategoryRequest request, boolean creation) {
        if (!StringUtils.hasText(request.label())) {
            throw new BadRequestException("Le libelle de la categorie est obligatoire");
        }
        category.setLabel(request.label().trim());
        category.setDescription(trim(request.description()));
        if (request.active() != null) {
            category.setActive(request.active());
        }
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        Long parentId = null;
        if (StringUtils.hasText(request.parentCode())) {
            SupplierCategoryRef parent = ref(request.parentCode());
            if (!creation) {
                if (parent.getId().equals(category.getId())) {
                    throw new BadRequestException("Une categorie n'est pas sa propre parente");
                }
                Set<Long> below = descendants(Set.of(category.getId()), refRepository.findAllByOrderBySortOrderAscLabelAsc());
                if (below.contains(parent.getId())) {
                    throw new BadRequestException("La categorie " + parent.getLabel() + " est deja une sous-categorie de " + category.getLabel());
                }
            }
            parentId = parent.getId();
        }
        category.setParentId(parentId);
        SupplierCategoryRef saved = refRepository.save(category);
        if (creation || request.natureCodes() != null || request.inventoryCategoryCodes() != null) {
            List<SupplierCategoryRefTarget> existing = targetRepository.findAllByCategoryIdIn(List.of(saved.getId()));
            replaceTargets(saved, SupplierCategoryKind.NATURE, request.natureCodes(), existing, creation);
            replaceTargets(saved, SupplierCategoryKind.INVENTORY_CATEGORY, request.inventoryCategoryCodes(), existing, creation);
        }
    }

    private void replaceTargets(SupplierCategoryRef category, SupplierCategoryKind kind, List<String> codes, List<SupplierCategoryRefTarget> existing,
                                boolean creation) {
        if (codes == null && !creation) {
            return;
        }
        Set<String> wanted = new LinkedHashSet<>();
        for (String code : codes == null ? List.<String>of() : codes) {
            if (StringUtils.hasText(code)) {
                wanted.add(verifiedCode(kind, code));
            }
        }
        List<SupplierCategoryRefTarget> ofKind = existing.stream().filter(target -> target.getTargetKind() == kind).toList();
        ofKind.stream().filter(target -> !wanted.contains(target.getTargetCode())).forEach(targetRepository::delete);
        Set<String> kept = ofKind.stream().map(SupplierCategoryRefTarget::getTargetCode).collect(Collectors.toSet());
        String actor = actors.resolve("SYSTEM");
        wanted.stream().filter(code -> !kept.contains(code)).forEach(code -> targetRepository.save(SupplierCategoryRefTarget.builder()
                .categoryId(category.getId()).targetKind(kind).targetCode(code).createdBy(actor).build()));
        targetRepository.flush();
    }

    private List<RefCategoryView> views(List<SupplierCategoryRef> selected, List<SupplierCategoryRef> all) {
        if (selected.isEmpty()) {
            return List.of();
        }
        Map<Long, SupplierCategoryRef> byId = all.stream().collect(Collectors.toMap(SupplierCategoryRef::getId, Function.identity()));
        Map<Long, List<SupplierCategoryRefTarget>> targets = targetRepository.findAllByCategoryIdIn(selected.stream().map(SupplierCategoryRef::getId).toList())
                .stream().collect(Collectors.groupingBy(SupplierCategoryRefTarget::getCategoryId));
        Map<String, Long> supplierCounts = linkRepository.findAllByCategoryKindAndCategoryCodeIn(SupplierCategoryKind.SUPPLIER_CATEGORY,
                        selected.stream().map(SupplierCategoryRef::getCode).toList()).stream()
                .collect(Collectors.groupingBy(SupplierCategoryLink::getCategoryCode, Collectors.counting()));
        Map<Long, Long> childCounts = all.stream().filter(category -> category.getParentId() != null)
                .collect(Collectors.groupingBy(SupplierCategoryRef::getParentId, Collectors.counting()));
        return selected.stream().map(category -> {
            SupplierCategoryRef parent = category.getParentId() == null ? null : byId.get(category.getParentId());
            List<SupplierCategoryRefTarget> own = targets.getOrDefault(category.getId(), List.of());
            return new RefCategoryView(category.getCode(), category.getLabel(), category.getDescription(),
                    parent == null ? null : parent.getCode(), parent == null ? null : parent.getLabel(), category.getActive(), category.getSortOrder(),
                    own.stream().filter(target -> target.getTargetKind() == SupplierCategoryKind.NATURE)
                            .map(target -> new TargetView(target.getTargetCode(), label(SupplierCategoryKind.NATURE, target.getTargetCode()))).toList(),
                    own.stream().filter(target -> target.getTargetKind() == SupplierCategoryKind.INVENTORY_CATEGORY)
                            .map(target -> new TargetView(target.getTargetCode(), label(SupplierCategoryKind.INVENTORY_CATEGORY, target.getTargetCode())))
                            .toList(),
                    supplierCounts.getOrDefault(category.getCode(), 0L), childCounts.getOrDefault(category.getId(), 0L),
                    category.getCreatedBy(), category.getCreatedAt(), category.getUpdatedBy(), category.getUpdatedAt());
        }).toList();
    }

    // ------------------------------------------------------------------ categories d'un fournisseur

    @Transactional(readOnly = true)
    public List<SupplierCategoryView> supplierCategories(String supplierCode) {
        Supplier supplier = supplier(supplierCode);
        return viewsBySupplier(List.of(supplier.getId())).getOrDefault(supplier.getId(), List.of());
    }

    public List<SupplierCategoryView> replaceSupplierCategories(String supplierCode, List<CategoryRef> categories) {
        Supplier supplier = supplier(supplierCode);
        replaceFor(supplier, categories);
        return supplierCategories(supplier.getSupplierCode());
    }

    public List<SupplierCategoryView> addSupplierCategory(String supplierCode, CategoryRef category) {
        Supplier supplier = supplier(supplierCode);
        if (category == null || category.kind() == null || !StringUtils.hasText(category.code())) {
            throw new BadRequestException("Indiquez la sorte et le code de la categorie");
        }
        String code = verifiedCode(category.kind(), category.code());
        boolean present = linkRepository.findAllBySupplierIdOrderByCategoryKindAscCategoryCodeAsc(supplier.getId()).stream()
                .anyMatch(link -> link.getCategoryKind() == category.kind() && link.getCategoryCode().equals(code));
        if (!present) {
            linkRepository.save(SupplierCategoryLink.builder().supplierId(supplier.getId()).categoryKind(category.kind()).categoryCode(code)
                    .createdBy(actors.resolve("SYSTEM")).build());
        }
        return supplierCategories(supplier.getSupplierCode());
    }

    public List<SupplierCategoryView> removeSupplierCategory(String supplierCode, SupplierCategoryKind kind, String code) {
        Supplier supplier = supplier(supplierCode);
        String wanted = code == null ? "" : code.trim();
        linkRepository.findAllBySupplierIdOrderByCategoryKindAscCategoryCodeAsc(supplier.getId()).stream()
                .filter(link -> link.getCategoryKind() == kind && link.getCategoryCode().equalsIgnoreCase(wanted))
                .forEach(linkRepository::delete);
        return supplierCategories(supplier.getSupplierCode());
    }

    /** Remplace les categories d'un fournisseur (fiche creee ou modifiee avec ses categories). */
    public void replaceFor(Supplier supplier, List<CategoryRef> categories) {
        Map<String, CategoryRef> wanted = new LinkedHashMap<>();
        for (CategoryRef category : categories == null ? List.<CategoryRef>of() : categories) {
            if (category == null || category.kind() == null || !StringUtils.hasText(category.code())) {
                throw new BadRequestException("Chaque categorie indique sa sorte (NATURE, INVENTORY_CATEGORY, SUPPLIER_CATEGORY) et son code");
            }
            String code = verifiedCode(category.kind(), category.code());
            wanted.put(category.kind() + "|" + code, new CategoryRef(category.kind(), code));
        }
        List<SupplierCategoryLink> existing = linkRepository.findAllBySupplierIdOrderByCategoryKindAscCategoryCodeAsc(supplier.getId());
        existing.stream().filter(link -> !wanted.containsKey(link.getCategoryKind() + "|" + link.getCategoryCode())).forEach(linkRepository::delete);
        Set<String> kept = existing.stream().map(link -> link.getCategoryKind() + "|" + link.getCategoryCode()).collect(Collectors.toSet());
        String actor = actors.resolve("SYSTEM");
        wanted.forEach((key, category) -> {
            if (!kept.contains(key)) {
                linkRepository.save(SupplierCategoryLink.builder().supplierId(supplier.getId()).categoryKind(category.kind())
                        .categoryCode(category.code()).createdBy(actor).build());
            }
        });
        linkRepository.flush();
    }

    /** Categories (avec libelle) de plusieurs fournisseurs, en une fois. */
    @Transactional(readOnly = true)
    public Map<Long, List<SupplierCategoryView>> viewsBySupplier(Collection<Long> supplierIds) {
        if (supplierIds == null || supplierIds.isEmpty()) {
            return Map.of();
        }
        List<SupplierCategoryLink> links = linkRepository.findAllBySupplierIdIn(supplierIds);
        Map<String, SupplierCategoryRef> refs = links.stream().anyMatch(link -> link.getCategoryKind() == SupplierCategoryKind.SUPPLIER_CATEGORY)
                ? refRepository.findAll().stream().collect(Collectors.toMap(SupplierCategoryRef::getCode, Function.identity()))
                : Map.of();
        Map<String, String> labels = new HashMap<>();
        Map<Long, List<SupplierCategoryView>> result = new HashMap<>();
        links.stream()
                .sorted(Comparator.comparing(SupplierCategoryLink::getCategoryKind).thenComparing(SupplierCategoryLink::getCategoryCode))
                .forEach(link -> {
                    String label;
                    Boolean active = Boolean.TRUE;
                    if (link.getCategoryKind() == SupplierCategoryKind.SUPPLIER_CATEGORY) {
                        SupplierCategoryRef ref = refs.get(link.getCategoryCode());
                        label = ref == null ? link.getCategoryCode() : ref.getLabel();
                        active = ref == null ? Boolean.FALSE : ref.getActive();
                    } else {
                        label = labels.computeIfAbsent(link.getCategoryKind() + "|" + link.getCategoryCode(),
                                key -> label(link.getCategoryKind(), link.getCategoryCode()));
                    }
                    result.computeIfAbsent(link.getSupplierId(), id -> new ArrayList<>())
                            .add(new SupplierCategoryView(link.getCategoryKind(), link.getCategoryCode(), label, active));
                });
        return result;
    }

    /**
     * Fournisseurs rattaches a une categorie. Pour une categorie fournisseur, ses sous-categories
     * comptent aussi.
     */
    @Transactional(readOnly = true)
    public Set<Long> supplierIdsIn(SupplierCategoryKind kind, String code) {
        Set<String> codes = new HashSet<>();
        if (kind == SupplierCategoryKind.SUPPLIER_CATEGORY) {
            SupplierCategoryRef root = ref(code);
            List<SupplierCategoryRef> all = refRepository.findAllByOrderBySortOrderAscLabelAsc();
            Set<Long> ids = descendants(Set.of(root.getId()), all);
            all.stream().filter(category -> ids.contains(category.getId())).forEach(category -> codes.add(category.getCode()));
        } else {
            codes.add(code.trim());
            codes.add(code.trim().toUpperCase(Locale.ROOT));
        }
        return linkRepository.findAllByCategoryKindAndCategoryCodeIn(kind, codes).stream().map(SupplierCategoryLink::getSupplierId)
                .collect(Collectors.toSet());
    }

    // ------------------------------------------------------------------ outils

    /** Les categories donnees et toutes leurs sous-categories, a toute profondeur. */
    static Set<Long> descendants(Set<Long> roots, List<SupplierCategoryRef> all) {
        Set<Long> result = new HashSet<>(roots);
        boolean grown = true;
        while (grown) {
            grown = false;
            for (SupplierCategoryRef category : all) {
                if (category.getParentId() != null && result.contains(category.getParentId()) && result.add(category.getId())) {
                    grown = true;
                }
            }
        }
        return result;
    }

    /** Code verifie : la nature, la categorie de stock ou la categorie fournisseur (active) existe. */
    String verifiedCode(SupplierCategoryKind kind, String rawCode) {
        String code = rawCode.trim();
        return switch (kind) {
            case NATURE -> natureRepository.findByCode(code.toUpperCase(Locale.ROOT)).map(CashOperationNature::getCode)
                    .orElseThrow(() -> new BadRequestException("Nature d'operation inconnue : " + code));
            case INVENTORY_CATEGORY -> inventoryCategoryRepository.findByCode(code)
                    .or(() -> inventoryCategoryRepository.findByCode(code.toUpperCase(Locale.ROOT)))
                    .map(InventoryCategory::getCode)
                    .orElseThrow(() -> new BadRequestException("Categorie de stock inconnue : " + code));
            case SUPPLIER_CATEGORY -> {
                SupplierCategoryRef ref = refRepository.findByCode(code.toUpperCase(Locale.ROOT))
                        .orElseThrow(() -> new BadRequestException("Categorie fournisseur inconnue : " + code));
                if (!Boolean.TRUE.equals(ref.getActive())) {
                    throw new BadRequestException("La categorie fournisseur " + ref.getLabel() + " est desactivee");
                }
                yield ref.getCode();
            }
        };
    }

    String label(SupplierCategoryKind kind, String code) {
        return switch (kind) {
            case NATURE -> natureRepository.findByCode(code).map(CashOperationNature::getLabel).orElse(code);
            case INVENTORY_CATEGORY -> inventoryCategoryRepository.findByCode(code).map(InventoryCategory::getName).orElse(code);
            case SUPPLIER_CATEGORY -> refRepository.findByCode(code).map(SupplierCategoryRef::getLabel).orElse(code);
        };
    }

    SupplierCategoryRef ref(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Code de categorie fournisseur obligatoire");
        }
        return refRepository.findByCode(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Categorie fournisseur introuvable : " + code));
    }

    private Supplier supplier(String supplierCode) {
        if (!StringUtils.hasText(supplierCode)) {
            throw new BadRequestException("Code fournisseur obligatoire");
        }
        String code = supplierCode.trim();
        return supplierRepository.findBySupplierCode(code)
                .or(() -> supplierRepository.findBySupplierCode(code.toUpperCase(Locale.ROOT)))
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable : " + supplierCode));
    }

    private String uniqueCode(String label) {
        String base = normalizeCode(label);
        if (base.length() > 70) {
            base = base.substring(0, 70);
        }
        String code = base;
        int suffix = 2;
        while (refRepository.existsByCode(code)) {
            code = base + "_" + suffix++;
        }
        return code;
    }

    static String normalizeCode(String value) {
        String code = fold(value).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (code.isEmpty()) {
            throw new BadRequestException("Code de categorie invalide : " + value);
        }
        return code.length() > 80 ? code.substring(0, 80) : code;
    }

    static String fold(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
