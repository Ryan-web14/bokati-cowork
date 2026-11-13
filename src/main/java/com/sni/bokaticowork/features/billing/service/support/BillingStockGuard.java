package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Controle de stock des lignes d'articles d'inventaire (lots B13 et D5) : une alerte sur un devis
 * ou une proforma ; un blocage sur une facture, sauf si l'article autorise le stock negatif. Seuls
 * les articles suivis en stock sont controles (ni service, ni suivi NONE).
 */
@Component
@RequiredArgsConstructor
public class BillingStockGuard {

    /** Pieces qui font sortir la marchandise : le manque de stock y bloque. */
    private static final Set<BillingDocumentType> MOVES_GOODS = EnumSet.of(BillingDocumentType.INVOICE,
            BillingDocumentType.CORRECTIVE_INVOICE, BillingDocumentType.DEBIT_NOTE);

    private final InventoryItemRepository items;
    private final StockLevelRepository levels;

    /** Une ligne a controler : son rang dans la piece, son article, sa provenance et sa quantite. */
    public record Line(int index, String itemCode, String sourceType, BillingLineType lineType, String description, BigDecimal quantity) {
    }

    /** Manque de stock sur une ligne ; severite BLOCK (facture) ou WARN. */
    public record StockAlert(int lineIndex, String itemCode, String description, BigDecimal requested, BigDecimal available,
                             String severity, String message) {
    }

    public List<StockAlert> evaluate(BillingDocumentType type, List<Line> lines) {
        List<StockAlert> alerts = new ArrayList<>();
        if (lines == null) {
            return alerts;
        }
        for (Line line : lines) {
            if (!inventoryLine(line) || line.quantity() == null || line.quantity().signum() <= 0) {
                continue;
            }
            Optional<InventoryItem> found = items.findByItemCode(line.itemCode().trim());
            if (found.isEmpty() || !tracked(found.get())) {
                continue;
            }
            InventoryItem item = found.get();
            BigDecimal available = Optional.ofNullable(levels.sumAvailable(item.getId())).orElse(BigDecimal.ZERO);
            if (line.quantity().compareTo(available) <= 0) {
                continue;
            }
            boolean block = type != null && MOVES_GOODS.contains(type) && !Boolean.TRUE.equals(item.getAllowNegativeStock());
            alerts.add(new StockAlert(line.index(), item.getItemCode(), line.description(), line.quantity(), available,
                    block ? "BLOCK" : "WARN",
                    "Stock insuffisant pour " + item.getName() + " : " + plain(line.quantity()) + " demandé(s), " + plain(available)
                            + " disponible(s)" + (block ? ". La facture ne peut pas être émise." : ".")));
        }
        return alerts;
    }

    private static boolean inventoryLine(Line line) {
        if (!StringUtils.hasText(line.itemCode())) {
            return false;
        }
        String source = line.sourceType() == null ? "" : line.sourceType().toUpperCase(Locale.ROOT);
        return line.lineType() == BillingLineType.PRODUCT || source.contains("INVENTORY");
    }

    private static boolean tracked(InventoryItem item) {
        return item.getItemType() != InventoryItemType.SERVICE
                && item.getTrackingType() != null && item.getTrackingType() != InventoryTrackingType.NONE;
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
