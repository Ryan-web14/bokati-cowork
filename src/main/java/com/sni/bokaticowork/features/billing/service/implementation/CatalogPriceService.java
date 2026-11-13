package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.dto.request.CatalogLinksRequest;
import com.sni.bokaticowork.features.billing.dto.request.CatalogPriceRequest;
import com.sni.bokaticowork.features.billing.dto.response.CatalogMarginResponse;
import com.sni.bokaticowork.features.billing.dto.response.CatalogPriceResponse;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogPrice;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogPriceRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Catalogue (lot B10) : prix par segment de client et dates avec historique, lien vers l'inventaire,
 * compte de produit, import et export CSV, apercu des marges. Familles et sous-familles : champs
 * {@code category} et {@code subcategory} de la fiche.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CatalogPriceService {

    static final String CSV_HEADER = "itemCode;name;category;subcategory;unit;unitPrice;currency;costPrice;floorPrice;maxDiscountRate;"
            + "inventoryItemCode;revenueAccount;active";

    private final ServiceCatalogItemRepository items;
    private final ServiceCatalogPriceRepository prices;
    private final InventoryItemRepository inventory;
    /** Lot C8 : tarifs negocies par client (facultatif pour les tests unitaires). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CustomerPriceService customerPrices;

    /** Lot C0 : listes de reference (facultatif pour les tests unitaires). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CatalogReferenceService references;

    // ------------------------------------------------------------------ prix dates

    /** Nouveau prix : le prix en vigueur du meme segment s'arrete la veille ; rien n'est efface. */
    public CatalogPriceResponse setPrice(String itemCode, CatalogPriceRequest request) {
        ServiceCatalogItem item = item(itemCode);
        String segment = StringUtils.hasText(request.segment()) ? request.segment() : "ALL";
        LocalDate from = request.validFrom() == null ? LocalDate.now() : request.validFrom();
        for (ServiceCatalogPrice open : prices.findByItemCodeAndSegmentOrderByValidFromDescIdDesc(item.getItemCode(), segment)) {
            if (open.getValidUntil() == null || !open.getValidUntil().isBefore(from)) {
                if (!open.getValidFrom().isBefore(from)) {
                    throw new BadRequestException("Un prix " + segment + " commence déjà le " + open.getValidFrom() + " : choisissez une date postérieure");
                }
                open.setValidUntil(from.minusDays(1));
                prices.save(open);
            }
        }
        ServiceCatalogPrice price = prices.save(ServiceCatalogPrice.builder().itemCode(item.getItemCode()).segment(segment)
                .unitPrice(request.unitPrice()).validFrom(from).reason(StringUtils.hasText(request.reason()) ? request.reason().trim() : null)
                .createdBy(actor()).build());
        if ("ALL".equals(segment) && !from.isAfter(LocalDate.now())) {
            item.setUnitPrice(request.unitPrice());
            items.save(item);
        }
        return view(price, "ALL".equals(segment) ? "ALL" : "SEGMENT");
    }

    @Transactional(readOnly = true)
    public List<CatalogPriceResponse> history(String itemCode) {
        item(itemCode);
        return prices.findByItemCodeOrderByValidFromDescIdDesc(itemCode).stream()
                .map(price -> view(price, "ALL".equals(price.getSegment()) ? "ALL" : "SEGMENT")).toList();
    }

    /**
     * Lot C8 : prix applicable a un client. Son contrat d'abord (prix fixe, ou taux de remise sur le
     * prix du segment), puis le prix du segment, puis celui de la fiche (decision CD8).
     */
    @Transactional(readOnly = true)
    public CatalogPriceResponse price(String itemCode, String segment, LocalDate date, String customerType, String customerCode) {
        CatalogPriceResponse base = price(itemCode, segment, date);
        if (customerPrices == null) {
            return base;
        }
        return customerPrices.contractPrice(customerType, customerCode, base.itemCode(), date, base.unitPrice())
                .map(contract -> new CatalogPriceResponse(base.itemCode(), base.segment(), contract.unitPrice(), contract.price().getValidFrom(),
                        contract.price().getValidUntil(), contract.price().getContractReference(), contract.price().getCreatedBy(),
                        contract.price().getCreatedAt(), "CONTRACT"))
                .orElse(base);
    }

    /** Prix applicable : celui du segment a la date, sinon le prix date commun, sinon celui de la fiche. */
    @Transactional(readOnly = true)
    public CatalogPriceResponse price(String itemCode, String segment, LocalDate date) {
        ServiceCatalogItem item = item(itemCode);
        LocalDate day = date == null ? LocalDate.now() : date;
        String wanted = StringUtils.hasText(segment) ? segment.trim().toUpperCase(Locale.ROOT) : "ALL";
        for (String candidate : "ALL".equals(wanted) ? List.of("ALL") : List.of(wanted, "ALL")) {
            Optional<ServiceCatalogPrice> found = prices.findByItemCodeAndSegmentOrderByValidFromDescIdDesc(item.getItemCode(), candidate).stream()
                    .filter(price -> !price.getValidFrom().isAfter(day) && (price.getValidUntil() == null || !price.getValidUntil().isBefore(day)))
                    .findFirst();
            if (found.isPresent()) {
                return view(found.get(), "ALL".equals(candidate) ? "ALL" : "SEGMENT");
            }
        }
        return new CatalogPriceResponse(item.getItemCode(), "ALL", item.getUnitPrice(), item.getValidFrom(), item.getValidUntil(), null, null,
                item.getUpdatedAt(), "CATALOG");
    }

    /** Trace un changement du prix de la fiche dans l'historique (mise a jour de l'article). */
    public void recordCatalogPriceChange(ServiceCatalogItem item, BigDecimal previous) {
        if (item.getUnitPrice() == null || (previous != null && previous.compareTo(item.getUnitPrice()) == 0)) {
            return;
        }
        LocalDate today = LocalDate.now();
        for (ServiceCatalogPrice open : prices.findByItemCodeAndSegmentOrderByValidFromDescIdDesc(item.getItemCode(), "ALL")) {
            if (open.getValidUntil() == null) {
                open.setValidUntil(open.getValidFrom().isBefore(today) ? today.minusDays(1) : open.getValidFrom());
                prices.save(open);
            }
        }
        prices.save(ServiceCatalogPrice.builder().itemCode(item.getItemCode()).segment("ALL").unitPrice(item.getUnitPrice()).validFrom(today)
                .reason("Prix de la fiche modifié").createdBy(actor()).build());
    }

    // ------------------------------------------------------------------ liens, marges, CSV

    public CatalogMarginResponse links(String itemCode, CatalogLinksRequest request) {
        ServiceCatalogItem item = item(itemCode);
        if (StringUtils.hasText(request.inventoryItemCode()) && inventory.findByItemCode(request.inventoryItemCode().trim()).isEmpty()) {
            throw new BadRequestException("Article d'inventaire introuvable : " + request.inventoryItemCode());
        }
        item.setInventoryItemCode(StringUtils.hasText(request.inventoryItemCode()) ? request.inventoryItemCode().trim() : null);
        item.setRevenueAccount(StringUtils.hasText(request.revenueAccount()) ? request.revenueAccount().trim() : null);
        return margin(items.save(item));
    }

    @Transactional(readOnly = true)
    public List<CatalogMarginResponse> margins(String search, String category) {
        return items.search(null, StringUtils.hasText(category) ? category.trim() : null, StringUtils.hasText(search) ? search.trim() : null,
                PageRequest.of(0, 500)).stream().map(CatalogPriceService::margin).toList();
    }

    static CatalogMarginResponse margin(ServiceCatalogItem item) {
        BigDecimal price = item.getUnitPrice();
        BigDecimal cost = item.getCostPrice();
        BigDecimal margin = price == null || cost == null ? null : price.subtract(cost);
        BigDecimal rate = margin == null || price.signum() == 0 ? null : margin.multiply(BigDecimal.valueOf(100)).divide(price, 1, RoundingMode.HALF_UP);
        return new CatalogMarginResponse(item.getItemCode(), item.getName(), item.getCategory(), item.getSubcategory(), price, cost, margin, rate,
                item.getFloorPrice(), item.getMaxDiscountRate(), item.getInventoryItemCode(), item.getRevenueAccount(), Boolean.TRUE.equals(item.getActive()));
    }

    @Transactional(readOnly = true)
    public String exportCsv() {
        StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
        for (ServiceCatalogItem item : items.findAll()) {
            csv.append(String.join(";", cell(item.getItemCode()), cell(item.getName()), cell(item.getCategory()), cell(item.getSubcategory()),
                    cell(item.getUnit()), number(item.getUnitPrice()), cell(item.getCurrency()), number(item.getCostPrice()), number(item.getFloorPrice()),
                    number(item.getMaxDiscountRate()), cell(item.getInventoryItemCode()), cell(item.getRevenueAccount()),
                    Boolean.TRUE.equals(item.getActive()) ? "true" : "false")).append('\n');
        }
        return csv.toString();
    }

    /** Import CSV (en-tete de l'export) : met a jour les articles existants par leur code, cree les autres ; erreurs par ligne. */
    public ImportResult importCsv(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BadRequestException("Fichier vide");
        }
        String[] rows = content.replace("\uFEFF", "").split("\\r?\\n");
        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();
        for (int index = 1; index < rows.length; index++) {
            if (!StringUtils.hasText(rows[index])) {
                continue;
            }
            String[] cells = rows[index].split(";", -1);
            try {
                if (cells.length < 6 || !StringUtils.hasText(cells[0]) || !StringUtils.hasText(cells[1])) {
                    throw new BadRequestException("code, nom et prix sont requis");
                }
                Optional<ServiceCatalogItem> existing = items.findByItemCode(cells[0].trim());
                ServiceCatalogItem item = existing.orElseGet(() -> ServiceCatalogItem.builder().itemCode(cells[0].trim()).build());
                BigDecimal previous = item.getUnitPrice();
                item.setName(cells[1].trim());
                String category = text(cells, 2);
                String subcategory = text(cells, 3);
                String unit = text(cells, 4);
                if (references != null) {
                    // Lot C0 : memes controles que l'ecran (mode souple ou strict).
                    CatalogReferenceService.Resolved refs = references.resolveForItem(
                            new CatalogReferenceService.ItemReferences(category, subcategory, unit, null),
                            new CatalogReferenceService.ItemReferences(item.getCategory(), item.getSubcategory(), item.getUnit(), null));
                    category = refs.category();
                    subcategory = refs.subcategory();
                    unit = refs.unit();
                }
                item.setCategory(category);
                item.setSubcategory(subcategory);
                item.setUnit(unit);
                item.setUnitPrice(decimal(cells, 5));
                if (text(cells, 6) != null) {
                    item.setCurrency(text(cells, 6));
                }
                item.setCostPrice(decimal(cells, 7));
                item.setFloorPrice(decimal(cells, 8));
                item.setMaxDiscountRate(decimal(cells, 9));
                item.setInventoryItemCode(text(cells, 10));
                item.setRevenueAccount(text(cells, 11));
                if (text(cells, 12) != null) {
                    item.setActive(Boolean.parseBoolean(text(cells, 12)));
                }
                ServiceCatalogItem saved = items.save(item);
                recordCatalogPriceChange(saved, previous);
                if (existing.isPresent()) {
                    updated++;
                } else {
                    created++;
                }
            } catch (RuntimeException exception) {
                errors.add("Ligne " + (index + 1) + " : " + exception.getMessage());
            }
        }
        return new ImportResult(created, updated, errors);
    }

    public record ImportResult(int created, int updated, List<String> errors) {
    }

    // ------------------------------------------------------------------ outils

    private ServiceCatalogItem item(String itemCode) {
        return items.findByItemCode(itemCode).orElseThrow(() -> new ResourceNotFoundException("Article du catalogue introuvable : " + itemCode));
    }

    private static CatalogPriceResponse view(ServiceCatalogPrice price, String source) {
        return new CatalogPriceResponse(price.getItemCode(), price.getSegment(), price.getUnitPrice(), price.getValidFrom(), price.getValidUntil(),
                price.getReason(), price.getCreatedBy(), price.getCreatedAt(), source);
    }

    private static String actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !StringUtils.hasText(authentication.getName()) ? "SYSTEM" : authentication.getName();
    }

    private static String text(String[] cells, int index) {
        return index < cells.length && StringUtils.hasText(cells[index]) ? cells[index].trim() : null;
    }

    private static BigDecimal decimal(String[] cells, int index) {
        String value = text(cells, index);
        return value == null ? null : new BigDecimal(value.replace(" ", "").replace(',', '.'));
    }

    private static String cell(String value) {
        return value == null ? "" : value.replace(";", ",").replace("\n", " ");
    }

    private static String number(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
