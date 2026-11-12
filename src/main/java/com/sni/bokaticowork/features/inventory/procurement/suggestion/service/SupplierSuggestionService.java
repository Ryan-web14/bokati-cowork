package com.sni.bokaticowork.features.inventory.procurement.suggestion.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.fund.service.FundNeedService;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryCategoryRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.procurement.enums.SupplierStatus;
import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;
import com.sni.bokaticowork.features.inventory.procurement.model.SupplierItem;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierItemRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Suggestion de fournisseurs pour une categorie de besoin, par regles simples et explicables (ni IA,
 * ni appel externe). Seuls les fournisseurs d'inventaire actifs sont proposes.
 *
 * <p>Score d'un fournisseur, chaque point justifie par une raison lisible :</p>
 * <ol>
 *   <li>appartenance declaree (le meilleur des cas compte) : rattache directement a la nature 100 ;
 *   classe dans une categorie fournisseur reliee a la nature, ou dans la categorie fournisseur
 *   demandee, 70 ; rattache directement a la categorie de stock 60 ; via une categorie fournisseur
 *   reliee a la categorie de stock 45 ;</li>
 *   <li>historique : 5 par ligne ou commande passee sur la meme nature (ou categorie de stock), au
 *   plus 50 ; un fournisseur sans categorie mais avec un historique est donc propose, plus bas ;</li>
 *   <li>prix fournisseur de l'article : 20 avec un prix contractuel (10 sans prix), 10 de plus pour
 *   le moins cher, 5 pour le fournisseur prefere ;</li>
 *   <li>delai : 10 moins le delai moyen en jours (jamais negatif).</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SupplierSuggestionService {

    static final String LIMIT_KEY = "inventory.supplier-suggestion.limit";
    static final int DEFAULT_LIMIT = 5;
    static final int MAX_LIMIT = 50;

    static final int DIRECT_NATURE = 100;
    static final int CATEGORY_FOR_NATURE = 70;
    static final int DIRECT_INVENTORY_CATEGORY = 60;
    static final int CATEGORY_FOR_INVENTORY_CATEGORY = 45;
    static final int HISTORY_PER_USE = 5;
    static final int HISTORY_MAX = 50;
    static final int CONTRACT_PRICE = 20;
    static final int ITEM_LISTED = 10;
    static final int CHEAPEST = 10;
    static final int PREFERRED = 5;
    static final int LEAD_TIME_BASE = 10;

    private final SupplierRepository supplierRepository;
    private final SupplierItemRepository supplierItemRepository;
    private final SupplierCategoryLinkRepository linkRepository;
    private final SupplierCategoryRefRepository refRepository;
    private final SupplierCategoryRefTargetRepository targetRepository;
    private final SupplierCategoryService categories;
    private final SupplierSuggestionHistory history;
    private final CashOperationNatureRepository natureRepository;
    private final InventoryCategoryRepository inventoryCategoryRepository;
    private final InventoryItemRepository itemRepository;
    private final PlatformSettings settings;

    /** Suggestions pour une categorie de besoin ; il faut au moins un critere exploitable. */
    public SuggestionResult suggest(SuggestionQuery query) {
        SuggestionResult result = compute(query);
        if (result.natureCode() == null && result.inventoryCategoryCode() == null && result.supplierCategoryCode() == null
                && result.itemCode() == null) {
            throw new BadRequestException("Indiquez une nature, une categorie de stock, une categorie fournisseur, un article "
                    + "ou une designation reconnue");
        }
        return result;
    }

    /** Suggestions pour chaque ligne (etat de besoin ou demande de fonds). */
    public List<LineSuggestion> forLines(List<LineInput> lines, Integer limit) {
        Map<String, SuggestionResult> byNature = new HashMap<>();
        List<LineSuggestion> result = new ArrayList<>();
        for (LineInput line : lines) {
            SuggestionResult suggestions;
            if (StringUtils.hasText(line.natureCode())) {
                String key = line.natureCode().trim().toUpperCase(Locale.ROOT);
                suggestions = byNature.computeIfAbsent(key,
                        nature -> compute(new SuggestionQuery(nature, null, null, null, null, null, limit)));
                suggestions = new SuggestionResult(suggestions.natureCode(), suggestions.natureLabel(), suggestions.natureGuessed(),
                        null, null, null, line.amount(), suggestions.limit(), suggestions.suggestions());
            } else {
                suggestions = compute(new SuggestionQuery(null, null, null, line.designation(), null, line.amount(), limit));
            }
            result.add(new LineSuggestion(line.lineNo(), line.designation(), line.natureCode(), line.supplierCode(), line.supplier(), suggestions));
        }
        return result;
    }

    SuggestionResult compute(SuggestionQuery query) {
        int limit = limit(query.limit());
        InventoryItem item = null;
        if (StringUtils.hasText(query.itemCode())) {
            item = itemRepository.findByItemCode(query.itemCode().trim())
                    .or(() -> itemRepository.findByItemCode(query.itemCode().trim().toUpperCase(Locale.ROOT)))
                    .orElseThrow(() -> new BadRequestException("Article inconnu : " + query.itemCode()));
        }
        String inventoryCategory = null;
        if (StringUtils.hasText(query.inventoryCategoryCode())) {
            inventoryCategory = categories.verifiedCode(SupplierCategoryKind.INVENTORY_CATEGORY, query.inventoryCategoryCode());
        } else if (item != null && item.getCategory() != null) {
            inventoryCategory = item.getCategory().getCode();
        }
        CashOperationNature nature = null;
        boolean guessed = false;
        if (StringUtils.hasText(query.natureCode())) {
            String code = query.natureCode().trim().toUpperCase(Locale.ROOT);
            nature = natureRepository.findByCode(code).orElseThrow(() -> new BadRequestException("Nature d'operation inconnue : " + code));
        } else if (StringUtils.hasText(query.designation())) {
            List<CashOperationNature> all = natureRepository.findAll();
            String code = FundNeedService.guessNature(all, query.designation());
            nature = code == null ? null : all.stream().filter(candidate -> code.equals(candidate.getCode())).findFirst().orElse(null);
            guessed = nature != null;
        }
        SupplierCategoryRef wantedCategory = StringUtils.hasText(query.supplierCategoryCode()) ? categories.ref(query.supplierCategoryCode()) : null;

        List<SupplierCategoryRef> refs = refRepository.findAllByOrderBySortOrderAscLabelAsc();
        Map<String, SupplierCategoryRef> refsByCode = refs.stream().collect(Collectors.toMap(SupplierCategoryRef::getCode, Function.identity()));
        Map<Long, Candidate> candidates = new HashMap<>();

        if (nature != null) {
            String label = nature.getLabel();
            for (SupplierCategoryLink link : linkRepository.findAllByCategoryKindAndCategoryCodeIn(SupplierCategoryKind.NATURE, List.of(nature.getCode()))) {
                candidate(candidates, link.getSupplierId()).member(DIRECT_NATURE, "rattache a la nature « " + label + " »");
            }
            viaCategories(candidates, refs, refsByCode, SupplierCategoryKind.NATURE, nature.getCode(), CATEGORY_FOR_NATURE);
            history.byNature(nature.getCode()).forEach((supplierId, count) -> candidate(candidates, supplierId).history(count,
                    "deja retenu " + count + " fois pour cette nature"));
        }
        if (inventoryCategory != null) {
            String label = categories.label(SupplierCategoryKind.INVENTORY_CATEGORY, inventoryCategory);
            for (SupplierCategoryLink link : linkRepository.findAllByCategoryKindAndCategoryCodeIn(SupplierCategoryKind.INVENTORY_CATEGORY,
                    List.of(inventoryCategory))) {
                candidate(candidates, link.getSupplierId()).member(DIRECT_INVENTORY_CATEGORY, "rattache a la categorie de stock « " + label + " »");
            }
            viaCategories(candidates, refs, refsByCode, SupplierCategoryKind.INVENTORY_CATEGORY, inventoryCategory, CATEGORY_FOR_INVENTORY_CATEGORY);
            history.byInventoryCategory(inventoryCategory).forEach((supplierId, count) -> candidate(candidates, supplierId).history(count,
                    count + " commande(s) de stock pour cette categorie"));
        }
        if (wantedCategory != null) {
            Set<Long> ids = SupplierCategoryService.descendants(Set.of(wantedCategory.getId()), refs);
            Set<String> codes = refs.stream().filter(ref -> ids.contains(ref.getId()) && Boolean.TRUE.equals(ref.getActive()))
                    .map(SupplierCategoryRef::getCode).collect(Collectors.toSet());
            if (!codes.isEmpty()) {
                for (SupplierCategoryLink link : linkRepository.findAllByCategoryKindAndCategoryCodeIn(SupplierCategoryKind.SUPPLIER_CATEGORY, codes)) {
                    candidate(candidates, link.getSupplierId()).member(CATEGORY_FOR_NATURE,
                            "categorie « " + refsByCode.get(link.getCategoryCode()).getLabel() + " »");
                }
            }
        }
        Map<Long, SupplierItem> itemOffers = new HashMap<>();
        if (item != null) {
            List<SupplierItem> offers = supplierItemRepository.findActiveByItemOrderByPriceAndLeadTime(item);
            Long cheapest = offers.stream().map(SupplierItem::getUnitPrice).filter(Objects::nonNull).min(Long::compare).orElse(null);
            for (SupplierItem offer : offers) {
                Long supplierId = offer.getSupplier().getId();
                itemOffers.putIfAbsent(supplierId, offer);
                Candidate candidate = candidate(candidates, supplierId);
                if (offer.getUnitPrice() != null) {
                    candidate.add(CONTRACT_PRICE, "prix contractuel pour l'article : " + money(offer.getUnitPrice()) + " XAF");
                    candidate.contractUnitPrice = offer.getUnitPrice();
                    if (offer.getUnitPrice().equals(cheapest)) {
                        candidate.add(CHEAPEST, "prix le plus bas pour l'article");
                    }
                } else {
                    candidate.add(ITEM_LISTED, "reference l'article");
                }
                if (Boolean.TRUE.equals(offer.getPreferred())) {
                    candidate.add(PREFERRED, "fournisseur prefere pour l'article");
                }
            }
        }

        Map<Long, Supplier> active = supplierRepository.findAllByStatusOrderByNameAsc(SupplierStatus.ACTIVE).stream()
                .filter(supplier -> candidates.containsKey(supplier.getId()))
                .collect(Collectors.toMap(Supplier::getId, Function.identity()));
        List<Map.Entry<Supplier, Candidate>> ranked = new ArrayList<>();
        candidates.forEach((supplierId, candidate) -> {
            Supplier supplier = active.get(supplierId);
            if (supplier == null) {
                return;
            }
            SupplierItem offer = itemOffers.get(supplierId);
            Integer lead = offer != null && offer.getLeadTimeDays() != null ? offer.getLeadTimeDays() : supplier.getAverageLeadTimeDays();
            if (lead != null) {
                candidate.leadTimeDays = lead;
                candidate.add(Math.max(0, LEAD_TIME_BASE - lead), "delai moyen " + lead + " j");
            }
            ranked.add(Map.entry(supplier, candidate));
        });
        ranked.sort(Comparator.<Map.Entry<Supplier, Candidate>>comparingInt(entry -> -entry.getValue().score())
                .thenComparing(entry -> -entry.getValue().historyCount)
                .thenComparing(entry -> entry.getKey().getName(), String.CASE_INSENSITIVE_ORDER));
        List<Map.Entry<Supplier, Candidate>> top = ranked.stream().limit(limit).toList();
        Map<Long, List<SupplierCategoryView>> views = categories.viewsBySupplier(top.stream().map(entry -> entry.getKey().getId()).toList());
        List<Suggestion> suggestions = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Supplier, Candidate> entry : top) {
            Supplier supplier = entry.getKey();
            Candidate candidate = entry.getValue();
            suggestions.add(new Suggestion(rank++, supplier.getSupplierCode(), supplier.getName(), supplier.getPhone(), supplier.getEmail(),
                    candidate.score(), List.copyOf(candidate.reasons), candidate.membership > 0, candidate.historyCount,
                    candidate.contractUnitPrice, candidate.leadTimeDays, views.getOrDefault(supplier.getId(), List.of())));
        }
        return new SuggestionResult(nature == null ? null : nature.getCode(), nature == null ? null : nature.getLabel(), guessed,
                inventoryCategory, wantedCategory == null ? null : wantedCategory.getCode(), item == null ? null : item.getItemCode(),
                query.amount(), limit, suggestions);
    }

    /** Fournisseurs classes dans une categorie fournisseur (active) reliee a la nature ou a la categorie de stock. */
    private void viaCategories(Map<Long, Candidate> candidates, List<SupplierCategoryRef> refs, Map<String, SupplierCategoryRef> refsByCode,
                               SupplierCategoryKind targetKind, String targetCode, int points) {
        Set<Long> roots = targetRepository.findAllByTargetKindAndTargetCode(targetKind, targetCode).stream()
                .map(SupplierCategoryRefTarget::getCategoryId).collect(Collectors.toSet());
        if (roots.isEmpty()) {
            return;
        }
        Set<Long> ids = SupplierCategoryService.descendants(roots, refs);
        Set<String> codes = refs.stream().filter(ref -> ids.contains(ref.getId()) && Boolean.TRUE.equals(ref.getActive()))
                .map(SupplierCategoryRef::getCode).collect(Collectors.toSet());
        if (codes.isEmpty()) {
            return;
        }
        for (SupplierCategoryLink link : linkRepository.findAllByCategoryKindAndCategoryCodeIn(SupplierCategoryKind.SUPPLIER_CATEGORY, codes)) {
            candidate(candidates, link.getSupplierId()).member(points, "categorie « " + refsByCode.get(link.getCategoryCode()).getLabel() + " »");
        }
    }

    private int limit(Integer requested) {
        long value = requested != null ? requested : settings.number(LIMIT_KEY, DEFAULT_LIMIT);
        return (int) Math.max(1, Math.min(MAX_LIMIT, value));
    }

    private static Candidate candidate(Map<Long, Candidate> candidates, Long supplierId) {
        return candidates.computeIfAbsent(supplierId, id -> new Candidate());
    }

    private static String money(Long value) {
        return String.format(Locale.FRANCE, "%,d", value).replace(' ', ' ').replace(' ', ' ');
    }

    /** Points et raisons d'un fournisseur. */
    static final class Candidate {
        private int membership;
        private int other;
        private long historyCount;
        private Long contractUnitPrice;
        private Integer leadTimeDays;
        private final List<String> reasons = new ArrayList<>();

        void member(int points, String reason) {
            membership = Math.max(membership, points);
            if (!reasons.contains(reason)) {
                reasons.add(reason);
            }
        }

        void history(long count, String reason) {
            historyCount += count;
            reasons.add(reason);
        }

        void add(int points, String reason) {
            other += points;
            reasons.add(reason);
        }

        int score() {
            return membership + (int) Math.min(HISTORY_MAX, historyCount * HISTORY_PER_USE) + other;
        }
    }
}
