package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.CategoryRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.CategoryResponse;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.CategoryUpdateRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.MergeRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.ReferencesResponse;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.TaxRuleRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.TaxRuleResponse;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.TaxRuleSuccessorRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.TaxRuleSummary;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.TaxRuleUpdateRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.UnitRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.UnitResponse;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.UnitUpdateRequest;
import com.sni.bokaticowork.features.billing.enums.TaxTreatment;
import com.sni.bokaticowork.features.billing.model.CatalogCategory;
import com.sni.bokaticowork.features.billing.model.CatalogUnit;
import com.sni.bokaticowork.features.billing.model.TaxRule;
import com.sni.bokaticowork.features.billing.repository.CatalogCategoryRepository;
import com.sni.bokaticowork.features.billing.repository.CatalogUnitRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.repository.TaxRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Lot C0 : listes de reference du catalogue de facturation. Familles et sous-familles, unites,
 * regles de taxe : lues par le frontend en un appel, enrichies depuis l'ecran, controlees a
 * l'enregistrement d'un article.
 *
 * <p>Rien de cassant : l'article garde ses colonnes texte. En mode souple (par defaut), une
 * famille, sous-famille ou unite inconnue est ajoutee a la liste et l'enregistrement passe ; un
 * code de taxe inconnu est garde tel quel et signale. En mode strict
 * ({@value #STRICT_KEY}), toute valeur inconnue est refusee.</p>
 */
@Service
@RequiredArgsConstructor
public class CatalogReferenceService {

    static final String STRICT_KEY = "billing.catalog.reference-lists.strict";
    static final String OTHER_FAMILY = "AUTRES";
    static final String UNKNOWN = "REFERENCE_INCONNUE";
    static final String INACTIVE = "REFERENCE_INACTIVE";
    static final String USED = "REFERENCE_UTILISEE";
    static final String VAT = "VAT";
    static final String ADDITIONAL_CENT = "ADDITIONAL_CENT";

    private final CatalogCategoryRepository categories;
    private final CatalogUnitRepository units;
    private final TaxRuleRepository taxRules;
    private final ServiceCatalogItemRepository items;

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    public boolean strict() {
        return platformSettings != null && platformSettings.bool(STRICT_KEY, false);
    }

    // ================================================================== lecture groupee

    @Transactional(readOnly = true)
    public ReferencesResponse references(boolean includeInactive) {
        LocalDate today = LocalDate.now();
        List<TaxRuleResponse> taxes = currentVersions(today).stream()
                .filter(rule -> VAT.equalsIgnoreCase(rule.getTaxType()))
                .filter(rule -> includeInactive || rule.validOn(today))
                .map(rule -> toResponse(rule, today))
                .toList();
        return new ReferencesResponse(listCategories(includeInactive), listUnits(includeInactive), taxes, strict());
    }

    // ================================================================== familles

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(boolean includeInactive) {
        Map<String, Long> familyCounts = counts(items.countGroupedByCategory(), 1);
        Map<String, Long> subCounts = counts(items.countGroupedBySubcategory(), 2);
        List<CatalogCategory> all = categories.findAllOrdered();
        Predicate<CatalogCategory> visible = c -> includeInactive || Boolean.TRUE.equals(c.getActive());
        Map<String, List<CategoryResponse>> children = new HashMap<>();
        for (CatalogCategory sub : all) {
            if (!sub.isFamily() && visible.test(sub)) {
                long count = subCounts.getOrDefault(key(sub.getParentCode(), sub.getCode()), 0L);
                children.computeIfAbsent(lower(sub.getParentCode()), k -> new ArrayList<>()).add(toResponse(sub, count, List.of()));
            }
        }
        return all.stream()
                .filter(CatalogCategory::isFamily)
                .filter(visible)
                .map(family -> toResponse(family, familyCounts.getOrDefault(lower(family.getCode()), 0L),
                        children.getOrDefault(lower(family.getCode()), List.of())))
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String code = newCode(request.code(), request.label(), 100, candidate -> categories.findFamily(candidate).isPresent());
        CatalogCategory family = categories.save(CatalogCategory.builder()
                .code(code).label(request.label().trim()).description(request.description())
                .revenueAccount(trimToNull(request.revenueAccount()))
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .build());
        return toResponse(family, 0, List.of());
    }

    @Transactional
    public CategoryResponse createSubcategory(String familyCode, CategoryRequest request) {
        CatalogCategory family = family(familyCode);
        String code = newCode(request.code(), request.label(), 100,
                candidate -> categories.findSubcategory(family.getCode(), candidate).isPresent());
        CatalogCategory sub = categories.save(CatalogCategory.builder()
                .code(code).label(request.label().trim()).parentCode(family.getCode()).description(request.description())
                .revenueAccount(trimToNull(request.revenueAccount()))
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .build());
        return toResponse(sub, 0, List.of());
    }

    @Transactional
    public CategoryResponse updateCategory(String familyCode, String subCode, CategoryUpdateRequest request) {
        CatalogCategory entry = subCode == null ? family(familyCode) : subcategory(familyCode, subCode);
        if (StringUtils.hasText(request.label())) {
            entry.setLabel(request.label().trim());
        }
        if (request.description() != null) {
            entry.setDescription(StringUtils.hasText(request.description()) ? request.description() : null);
        }
        if (request.revenueAccount() != null) {
            entry.setRevenueAccount(trimToNull(request.revenueAccount()));
        }
        if (request.displayOrder() != null) {
            entry.setDisplayOrder(request.displayOrder());
        }
        entry.setCreatedFromItem(false);
        return toResponse(categories.save(entry), usage(entry), List.of());
    }

    @Transactional
    public CategoryResponse setCategoryActive(String familyCode, String subCode, boolean active) {
        CatalogCategory entry = subCode == null ? family(familyCode) : subcategory(familyCode, subCode);
        entry.setActive(active);
        return toResponse(categories.save(entry), usage(entry), List.of());
    }

    @Transactional
    public void deleteCategory(String familyCode, String subCode) {
        CatalogCategory entry = subCode == null ? family(familyCode) : subcategory(familyCode, subCode);
        long used = usage(entry);
        if (used > 0) {
            throw ConflictException.coded(USED, "« " + entry.getLabel() + " » est utilisée par " + used
                    + " article(s) : désactivez-la ou fusionnez-la avec une autre entrée.");
        }
        if (entry.isFamily() && !categories.findSubcategories(entry.getCode()).isEmpty()) {
            throw ConflictException.coded(USED, "La famille « " + entry.getLabel()
                    + " » a encore des sous-familles : supprimez-les ou déplacez-les d'abord.");
        }
        categories.delete(entry);
    }

    // ================================================================== unites

    @Transactional(readOnly = true)
    public List<UnitResponse> listUnits(boolean includeInactive) {
        Map<String, Long> unitCounts = counts(items.countGroupedByUnit(), 1);
        return units.findAllOrdered().stream()
                .filter(unit -> includeInactive || Boolean.TRUE.equals(unit.getActive()))
                .map(unit -> toResponse(unit, unitCounts.getOrDefault(lower(unit.getCode()), 0L)))
                .toList();
    }

    @Transactional
    public UnitResponse createUnit(UnitRequest request) {
        String code = newCode(request.code(), request.label(), 50, candidate -> units.findByCodeIgnoreCase(candidate).isPresent());
        CatalogUnit unit = units.save(CatalogUnit.builder()
                .code(code).label(request.label().trim()).pluralLabel(trimToNull(request.pluralLabel()))
                .symbol(trimToNull(request.symbol()))
                .quantityScale(request.quantityScale() != null ? request.quantityScale() : 2)
                .inventoryUnitCode(trimToNull(request.inventoryUnitCode()))
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .build());
        return toResponse(unit, 0);
    }

    @Transactional
    public UnitResponse updateUnit(String code, UnitUpdateRequest request) {
        CatalogUnit unit = unit(code);
        if (StringUtils.hasText(request.label())) {
            unit.setLabel(request.label().trim());
        }
        if (request.pluralLabel() != null) {
            unit.setPluralLabel(trimToNull(request.pluralLabel()));
        }
        if (request.symbol() != null) {
            unit.setSymbol(trimToNull(request.symbol()));
        }
        if (request.quantityScale() != null) {
            unit.setQuantityScale(request.quantityScale());
        }
        if (request.inventoryUnitCode() != null) {
            unit.setInventoryUnitCode(trimToNull(request.inventoryUnitCode()));
        }
        if (request.displayOrder() != null) {
            unit.setDisplayOrder(request.displayOrder());
        }
        unit.setCreatedFromItem(false);
        return toResponse(units.save(unit), items.countUsingUnit(unit.getCode()));
    }

    @Transactional
    public UnitResponse setUnitActive(String code, boolean active) {
        CatalogUnit unit = unit(code);
        unit.setActive(active);
        return toResponse(units.save(unit), items.countUsingUnit(unit.getCode()));
    }

    @Transactional
    public void deleteUnit(String code) {
        CatalogUnit unit = unit(code);
        long used = items.countUsingUnit(unit.getCode());
        if (used > 0) {
            throw ConflictException.coded(USED, "L'unité « " + unit.getLabel() + " » est utilisée par " + used
                    + " article(s) : désactivez-la ou fusionnez-la avec une autre unité.");
        }
        units.delete(unit);
    }

    // ================================================================== fusion

    /** Fusion d'une entree en double : les articles passent sur la cible, la source est desactivee. */
    @Transactional
    public long merge(MergeRequest request) {
        String list = request.list().trim().toUpperCase(Locale.ROOT);
        if (request.sourceCode().trim().equalsIgnoreCase(request.targetCode().trim())) {
            throw BadRequestException.coded("FUSION_IMPOSSIBLE", "La source et la cible sont la même entrée.");
        }
        switch (list) {
            case "CATEGORY" -> {
                CatalogCategory source = family(request.sourceCode());
                CatalogCategory target = family(request.targetCode());
                long moved = items.replaceCategory(source.getCode(), target.getCode());
                for (CatalogCategory sub : categories.findSubcategories(source.getCode())) {
                    Optional<CatalogCategory> twin = categories.findSubcategory(target.getCode(), sub.getCode());
                    if (twin.isPresent()) {
                        sub.setActive(false);
                    } else {
                        sub.setParentCode(target.getCode());
                    }
                    categories.save(sub);
                }
                source.setActive(false);
                categories.save(source);
                return moved;
            }
            case "SUBCATEGORY" -> {
                if (!StringUtils.hasText(request.parentCode())) {
                    throw BadRequestException.coded("FUSION_IMPOSSIBLE", "parentCode est requis pour fusionner deux sous-familles.");
                }
                CatalogCategory source = subcategory(request.parentCode(), request.sourceCode());
                CatalogCategory target = subcategory(request.parentCode(), request.targetCode());
                long moved = items.replaceSubcategory(source.getParentCode(), source.getCode(), target.getCode());
                source.setActive(false);
                categories.save(source);
                return moved;
            }
            case "UNIT" -> {
                CatalogUnit source = unit(request.sourceCode());
                CatalogUnit target = unit(request.targetCode());
                long moved = items.replaceUnit(source.getCode(), target.getCode());
                source.setActive(false);
                units.save(source);
                return moved;
            }
            default -> throw BadRequestException.coded("FUSION_IMPOSSIBLE", "list doit valoir CATEGORY, SUBCATEGORY ou UNIT.");
        }
    }

    // ================================================================== regles de taxe

    @Transactional(readOnly = true)
    public List<TaxRuleResponse> listTaxRules(boolean includeInactive) {
        LocalDate today = LocalDate.now();
        return taxRules.findAllOrdered().stream()
                .filter(rule -> includeInactive || Boolean.TRUE.equals(rule.getActive()))
                .map(rule -> toResponse(rule, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaxRuleResponse> taxRuleVersions(String baseCode) {
        LocalDate today = LocalDate.now();
        List<TaxRule> versions = taxRules.findVersions(baseCode);
        if (versions.isEmpty()) {
            throw new ResourceNotFoundException("Règle de taxe introuvable : " + baseCode);
        }
        return versions.stream().map(rule -> toResponse(rule, today)).toList();
    }

    @Transactional
    public TaxRuleResponse createTaxRule(TaxRuleRequest request) {
        String code = request.code().trim();
        if (taxRules.existsByCode(code) || !taxRules.findVersions(code).isEmpty()) {
            throw ConflictException.coded("TAXE_EXISTANTE", "Une règle de taxe porte déjà le code " + code + ".");
        }
        String type = StringUtils.hasText(request.taxType()) ? request.taxType().trim().toUpperCase(Locale.ROOT) : VAT;
        if (!VAT.equals(type) && !ADDITIONAL_CENT.equals(type)) {
            throw BadRequestException.coded("TAXE_INVALIDE", "taxType doit valoir VAT ou ADDITIONAL_CENT.");
        }
        TaxTreatment treatment = treatment(request.treatment());
        checkTreatment(treatment, request.rate(), request.exemptionReason());
        TaxRule rule = taxRules.save(TaxRule.builder()
                .code(code).baseCode(code).name(request.name().trim())
                .countryCode(StringUtils.hasText(request.countryCode()) ? request.countryCode().trim().toUpperCase(Locale.ROOT) : "CG")
                .taxType(type).rate(request.rate())
                .appliesOn(VAT.equals(type) ? "TAXABLE_AMOUNT" : "VAT_AMOUNT")
                .active(true)
                .validFrom(request.validFrom() != null ? request.validFrom() : LocalDate.now())
                .labelOnDocument(trimToNull(request.labelOnDocument()))
                .exemptionReason(trimToNull(request.exemptionReason()))
                .treatment(treatment)
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .build());
        return toResponse(rule, LocalDate.now());
    }

    @Transactional
    public TaxRuleResponse updateTaxRule(String code, TaxRuleUpdateRequest request) {
        TaxRule rule = taxRuleVersion(code);
        if (StringUtils.hasText(request.name())) {
            rule.setName(request.name().trim());
        }
        if (request.labelOnDocument() != null) {
            rule.setLabelOnDocument(trimToNull(request.labelOnDocument()));
        }
        if (request.exemptionReason() != null) {
            checkTreatment(rule.getTreatment(), rule.getRate(), request.exemptionReason());
            rule.setExemptionReason(trimToNull(request.exemptionReason()));
        }
        if (request.displayOrder() != null) {
            rule.setDisplayOrder(request.displayOrder());
        }
        return toResponse(taxRules.save(rule), LocalDate.now());
    }

    @Transactional
    public TaxRuleResponse setTaxRuleActive(String code, boolean active) {
        TaxRule rule = taxRuleVersion(code);
        rule.setActive(active);
        return toResponse(taxRules.save(rule), LocalDate.now());
    }

    /**
     * Changement de taux : la version en vigueur a cette date s'arrete la veille, une nouvelle
     * version (meme code de base) commence. Les articles gardent leur code ; les pieces deja
     * emises gardent leur taux.
     */
    @Transactional
    public TaxRuleResponse successor(String baseCode, TaxRuleSuccessorRequest request) {
        List<TaxRule> versions = taxRules.findVersions(baseCode);
        if (versions.isEmpty()) {
            throw new ResourceNotFoundException("Règle de taxe introuvable : " + baseCode);
        }
        LocalDate start = request.validFrom();
        TaxRule previous = versions.stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                .filter(rule -> rule.getValidFrom() == null || rule.getValidFrom().isBefore(start))
                .max(Comparator.comparing(rule -> rule.getValidFrom() == null ? LocalDate.MIN : rule.getValidFrom()))
                .orElseThrow(() -> BadRequestException.coded("TAXE_INVALIDE",
                        "Aucune version de " + baseCode + " ne commence avant le " + start + "."));
        if (versions.stream().anyMatch(rule -> Boolean.TRUE.equals(rule.getActive()) && rule.getValidFrom() != null
                && !rule.getValidFrom().isBefore(start))) {
            throw ConflictException.coded("TAXE_INVALIDE", "Une version de " + baseCode + " commence déjà le " + start + " ou après.");
        }
        checkTreatment(previous.getTreatment(), request.rate(), previous.getExemptionReason());
        previous.setValidUntil(start.minusDays(1));
        taxRules.save(previous);
        String code = previous.getBaseCode() + "_" + start.toString().replace("-", "");
        if (taxRules.existsByCode(code)) {
            throw ConflictException.coded("TAXE_EXISTANTE", "La version " + code + " existe déjà.");
        }
        TaxRule next = taxRules.save(TaxRule.builder()
                .code(code).baseCode(previous.getBaseCode())
                .name(StringUtils.hasText(request.name()) ? request.name().trim() : previous.getName())
                .countryCode(previous.getCountryCode()).taxType(previous.getTaxType()).rate(request.rate())
                .appliesOn(previous.getAppliesOn()).active(true).validFrom(start)
                .labelOnDocument(StringUtils.hasText(request.labelOnDocument()) ? request.labelOnDocument().trim() : previous.getLabelOnDocument())
                .exemptionReason(previous.getExemptionReason()).treatment(previous.getTreatment())
                .displayOrder(previous.getDisplayOrder())
                .build());
        return toResponse(next, LocalDate.now());
    }

    /** Version de la regle applicable a une date, par code de base ou code de version. */
    @Transactional(readOnly = true)
    public Optional<TaxRule> taxRuleOn(String code, LocalDate date) {
        if (!StringUtils.hasText(code)) {
            return Optional.empty();
        }
        List<TaxRule> versions = taxRules.findVersions(code.trim());
        if (versions.isEmpty()) {
            return taxRules.findByCode(code.trim()).filter(rule -> rule.validOn(date))
                    .or(() -> taxRules.findByCode(code.trim()).flatMap(rule -> taxRuleOn(rule.getBaseCode(), date)));
        }
        return versions.stream().filter(rule -> rule.validOn(date))
                .max(Comparator.comparing(rule -> rule.getValidFrom() == null ? LocalDate.MIN : rule.getValidFrom()));
    }

    // ================================================================== controle d'un article

    /** Valeurs d'un article a controler ; {@code current} : valeurs deja enregistrees (mise a jour). */
    public record ItemReferences(String category, String subcategory, String unit, String taxRuleCode) {
        static final ItemReferences NONE = new ItemReferences(null, null, null, null);
    }

    /** Valeurs a enregistrer (codes des listes) et avertissements a rendre. */
    public record Resolved(String category, String subcategory, String unit, String taxRuleCode, List<String> warnings) {
    }

    /**
     * Controle les references d'un article. Un champ nul reste nul (rien a controler). Une valeur
     * inchangee par rapport a l'article enregistre passe toujours : un ancien ecran qui renvoie
     * l'article tel quel ne doit jamais etre bloque.
     */
    @Transactional
    public Resolved resolveForItem(ItemReferences requested, ItemReferences current) {
        ItemReferences before = current != null ? current : ItemReferences.NONE;
        boolean strict = strict();
        List<String> warnings = new ArrayList<>();

        String category = requested.category() == null ? null
                : resolveFamily(requested.category(), before.category(), strict);
        String familyForSub = category != null ? category : before.category();
        String subcategory = requested.subcategory() == null ? null
                : resolveSubcategory(familyForSub, requested.subcategory(), before.subcategory(), strict);
        String unit = requested.unit() == null ? null
                : resolveUnit(requested.unit(), before.unit(), strict);
        String tax = requested.taxRuleCode() == null ? null
                : resolveTax(requested.taxRuleCode(), before.taxRuleCode(), strict, warnings);
        return new Resolved(category, subcategory, unit, tax, warnings);
    }

    private String resolveFamily(String value, String previous, boolean strict) {
        String wanted = value.trim();
        if (wanted.isEmpty()) {
            return value;
        }
        Optional<CatalogCategory> found = categories.findFamily(wanted)
                .or(() -> categories.findFamiliesByLabel(wanted).stream().findFirst());
        if (found.isPresent()) {
            return checkActive(found.get().getCode(), found.get().getActive(), found.get().getLabel(), previous, strict, "famille");
        }
        if (unchanged(wanted, previous)) {
            return previous;
        }
        if (strict) {
            throw BadRequestException.coded(UNKNOWN, "Famille inconnue : « " + wanted + " ». Choisissez-la dans la liste ou ajoutez-la d'abord.");
        }
        categories.save(CatalogCategory.builder().code(clip(wanted, 100)).label(clip(wanted, 150)).createdFromItem(true).build());
        return clip(wanted, 100);
    }

    private String resolveSubcategory(String family, String value, String previous, boolean strict) {
        String wanted = value.trim();
        if (wanted.isEmpty()) {
            return value;
        }
        Optional<CatalogCategory> found;
        if (StringUtils.hasText(family)) {
            found = categories.findSubcategory(family.trim(), wanted)
                    .or(() -> categories.findSubcategoriesByLabel(family.trim(), wanted).stream().findFirst());
        } else {
            List<CatalogCategory> any = categories.findAnySubcategory(wanted);
            found = any.size() == 1 ? Optional.of(any.get(0)) : Optional.empty();
        }
        if (found.isPresent()) {
            return checkActive(found.get().getCode(), found.get().getActive(), found.get().getLabel(), previous, strict, "sous-famille");
        }
        if (unchanged(wanted, previous)) {
            return previous;
        }
        if (strict) {
            throw BadRequestException.coded(UNKNOWN, "Sous-famille inconnue : « " + wanted + " »"
                    + (StringUtils.hasText(family) ? " dans la famille « " + family.trim() + " »" : "")
                    + ". Choisissez-la dans la liste ou ajoutez-la d'abord.");
        }
        String parent = StringUtils.hasText(family)
                ? categories.findFamily(family.trim()).map(CatalogCategory::getCode).orElse(family.trim())
                : otherFamily().getCode();
        categories.save(CatalogCategory.builder().code(clip(wanted, 100)).label(clip(wanted, 150)).parentCode(parent)
                .createdFromItem(true).build());
        return clip(wanted, 100);
    }

    private String resolveUnit(String value, String previous, boolean strict) {
        String wanted = value.trim();
        if (wanted.isEmpty()) {
            return value;
        }
        Optional<CatalogUnit> found = units.findByCodeIgnoreCase(wanted)
                .or(() -> units.findByLabelIgnoreCase(wanted).stream().findFirst());
        if (found.isPresent()) {
            return checkActive(found.get().getCode(), found.get().getActive(), found.get().getLabel(), previous, strict, "unité");
        }
        if (unchanged(wanted, previous)) {
            return previous;
        }
        if (strict) {
            throw BadRequestException.coded(UNKNOWN, "Unité inconnue : « " + wanted + " ». Choisissez-la dans la liste ou ajoutez-la d'abord.");
        }
        units.save(CatalogUnit.builder().code(clip(wanted, 50)).label(clip(wanted, 100)).createdFromItem(true).displayOrder(100).build());
        return clip(wanted, 50);
    }

    private String resolveTax(String value, String previous, boolean strict, List<String> warnings) {
        String wanted = value.trim();
        if (wanted.isEmpty()) {
            return value;
        }
        Optional<TaxRule> found = taxRuleOn(wanted, LocalDate.now())
                .or(() -> taxRules.findVersions(wanted).stream().findFirst())
                .or(() -> taxRules.findByCode(wanted));
        if (found.isPresent()) {
            TaxRule rule = found.get();
            if (!VAT.equalsIgnoreCase(rule.getTaxType())) {
                throw BadRequestException.coded("TAXE_INVALIDE", "La règle « " + rule.getName()
                        + " » se calcule sur la TVA : elle ne se choisit pas sur un article.");
            }
            return rule.getBaseCode();
        }
        if (unchanged(wanted, previous)) {
            return previous;
        }
        if (strict) {
            throw BadRequestException.coded(UNKNOWN, "Règle de taxe inconnue : « " + wanted + " ». Choisissez-la dans la liste des taxes.");
        }
        warnings.add("Règle de taxe inconnue « " + wanted + " » : enregistrée telle quelle, le taux par défaut s'applique.");
        return wanted;
    }

    private String checkActive(String code, Boolean active, String label, String previous, boolean strict, String kind) {
        if (Boolean.TRUE.equals(active) || unchanged(code, previous) || !strict) {
            return code;
        }
        throw BadRequestException.coded(INACTIVE, "La " + kind + " « " + label + " » est désactivée : choisissez-en une autre.");
    }

    // ================================================================== libelles pour les reponses

    /** Libelles des listes, charges une fois pour une page d'articles. */
    public record Snapshot(Map<String, String> familyLabels, Map<String, String> subcategoryLabels,
                           Map<String, String> unitLabels, Map<String, TaxRuleSummary> taxes) {

        public static final Snapshot EMPTY = new Snapshot(Map.of(), Map.of(), Map.of(), Map.of());

        public String familyLabel(String code) {
            return code == null ? null : familyLabels.get(lower(code));
        }

        public String subcategoryLabel(String family, String code) {
            if (code == null) {
                return null;
            }
            String parent = StringUtils.hasText(family) ? family : OTHER_FAMILY;
            String label = subcategoryLabels.get(key(parent, code));
            return label != null ? label : subcategoryLabels.get(key("*", code));
        }

        public String unitLabel(String code) {
            return code == null ? null : unitLabels.get(lower(code));
        }

        public TaxRuleSummary tax(String code) {
            return code == null ? null : taxes.get(lower(code));
        }
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot() {
        Map<String, String> families = new HashMap<>();
        Map<String, String> subs = new HashMap<>();
        for (CatalogCategory entry : categories.findAllOrdered()) {
            if (entry.isFamily()) {
                families.put(lower(entry.getCode()), entry.getLabel());
            } else {
                subs.put(key(entry.getParentCode(), entry.getCode()), entry.getLabel());
                subs.putIfAbsent(key("*", entry.getCode()), entry.getLabel());
            }
        }
        Map<String, String> unitLabels = new HashMap<>();
        units.findAllOrdered().forEach(unit -> unitLabels.put(lower(unit.getCode()), unit.getLabel()));
        Map<String, TaxRuleSummary> taxes = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (TaxRule rule : currentVersions(today)) {
            TaxRuleSummary summary = new TaxRuleSummary(rule.getBaseCode(), rule.getName(), rule.getRate(),
                    rule.getTreatment() != null ? rule.getTreatment().name() : null, rule.getLabelOnDocument());
            taxes.put(lower(rule.getBaseCode()), summary);
            taxes.putIfAbsent(lower(rule.getCode()), summary);
        }
        return new Snapshot(families, subs, unitLabels, taxes);
    }

    // ================================================================== outils

    /** Une version par code de base : celle en vigueur aujourd'hui, sinon la plus recente. */
    private List<TaxRule> currentVersions(LocalDate today) {
        Map<String, TaxRule> byBase = new LinkedHashMap<>();
        for (TaxRule rule : taxRules.findAllOrdered()) {
            String base = lower(rule.getBaseCode() != null ? rule.getBaseCode() : rule.getCode());
            TaxRule kept = byBase.get(base);
            if (kept == null || better(rule, kept, today)) {
                byBase.put(base, rule);
            }
        }
        return new ArrayList<>(byBase.values());
    }

    private static boolean better(TaxRule candidate, TaxRule kept, LocalDate today) {
        boolean candidateValid = candidate.validOn(today);
        boolean keptValid = kept.validOn(today);
        if (candidateValid != keptValid) {
            return candidateValid;
        }
        LocalDate a = candidate.getValidFrom() == null ? LocalDate.MIN : candidate.getValidFrom();
        LocalDate b = kept.getValidFrom() == null ? LocalDate.MIN : kept.getValidFrom();
        return candidateValid ? a.isAfter(b) && !a.isAfter(today) : a.isAfter(b);
    }

    private CatalogCategory otherFamily() {
        return categories.findFamily(OTHER_FAMILY).orElseGet(() -> categories.save(CatalogCategory.builder()
                .code(OTHER_FAMILY).label("Autres").displayOrder(999).build()));
    }

    private CatalogCategory family(String code) {
        return categories.findFamily(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Famille du catalogue introuvable : " + code));
    }

    private CatalogCategory subcategory(String familyCode, String code) {
        CatalogCategory family = family(familyCode);
        return categories.findSubcategory(family.getCode(), code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Sous-famille introuvable : " + code + " dans " + family.getLabel()));
    }

    private CatalogUnit unit(String code) {
        return units.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Unité introuvable : " + code));
    }

    private TaxRule taxRuleVersion(String code) {
        return taxRules.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Règle de taxe introuvable : " + code));
    }

    private long usage(CatalogCategory entry) {
        return entry.isFamily() ? items.countUsingCategory(entry.getCode())
                : items.countUsingSubcategory(entry.getParentCode(), entry.getCode());
    }

    private static TaxTreatment treatment(String value) {
        if (!StringUtils.hasText(value)) {
            return TaxTreatment.STANDARD;
        }
        try {
            return TaxTreatment.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw BadRequestException.coded("TAXE_INVALIDE", "treatment doit valoir STANDARD, REDUCED, EXEMPT ou OUT_OF_SCOPE.");
        }
    }

    private static void checkTreatment(TaxTreatment treatment, BigDecimal rate, String exemptionReason) {
        boolean zeroRated = treatment == TaxTreatment.EXEMPT || treatment == TaxTreatment.OUT_OF_SCOPE;
        if (zeroRated && rate != null && rate.signum() != 0) {
            throw BadRequestException.coded("TAXE_INVALIDE", "Une règle exonérée ou hors champ a un taux de 0.");
        }
        if (treatment == TaxTreatment.EXEMPT && !StringUtils.hasText(exemptionReason)) {
            throw BadRequestException.coded("TAXE_INVALIDE", "Une exonération demande son motif (exemptionReason), imprimé sur la pièce.");
        }
    }

    /** Code fourni, sinon tire du libelle ; suffixe _2, _3... en cas de doublon. */
    static String newCode(String requested, String label, int max, Predicate<String> taken) {
        if (StringUtils.hasText(requested)) {
            String code = requested.trim();
            if (taken.test(code)) {
                throw ConflictException.coded("REFERENCE_EXISTANTE", "Le code « " + code + " » existe déjà dans cette liste.");
            }
            return clip(code, max);
        }
        String base = slug(label);
        if (base.isEmpty()) {
            throw BadRequestException.coded("REFERENCE_INVALIDE", "Le libellé doit contenir au moins une lettre ou un chiffre.");
        }
        base = clip(base, max - 4);
        String code = base;
        for (int index = 2; taken.test(code); index++) {
            code = base + "_" + index;
        }
        return code;
    }

    static String slug(String label) {
        String plain = Normalizer.normalize(label == null ? "" : label, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private static boolean unchanged(String value, String previous) {
        return previous != null && previous.trim().equalsIgnoreCase(value.trim());
    }

    private static Map<String, Long> counts(List<Object[]> rows, int countIndex) {
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            String code = countIndex == 2 ? key((String) row[0], (String) row[1]) : (String) row[0];
            result.put(code, ((Number) row[countIndex]).longValue());
        }
        return result;
    }

    private static String key(String parent, String code) {
        return lower(parent) + "|" + lower(code);
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String clip(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static CategoryResponse toResponse(CatalogCategory entry, long count, List<CategoryResponse> subcategories) {
        return new CategoryResponse(entry.getId(), entry.getCode(), entry.getLabel(), entry.getParentCode(), entry.getDescription(),
                entry.getRevenueAccount(), entry.getDisplayOrder(), entry.getActive(), entry.getCreatedFromItem(), count,
                subcategories, entry.getUpdatedAt());
    }

    private static UnitResponse toResponse(CatalogUnit unit, long count) {
        return new UnitResponse(unit.getId(), unit.getCode(), unit.getLabel(), unit.getPluralLabel(), unit.getSymbol(),
                unit.getQuantityScale(), unit.getInventoryUnitCode(), unit.getDisplayOrder(), unit.getActive(),
                unit.getCreatedFromItem(), count, unit.getUpdatedAt());
    }

    private static TaxRuleResponse toResponse(TaxRule rule, LocalDate today) {
        return new TaxRuleResponse(rule.getId(), rule.getCode(), rule.getBaseCode(), rule.getName(), rule.getTaxType(), rule.getRate(),
                rule.getAppliesOn(), rule.getTreatment() != null ? rule.getTreatment().name() : null, rule.getLabelOnDocument(),
                rule.getExemptionReason(), rule.getCountryCode(), rule.getValidFrom(), rule.getValidUntil(), rule.getActive(),
                rule.getDisplayOrder(), rule.validOn(today), VAT.equalsIgnoreCase(rule.getTaxType()));
    }
}
