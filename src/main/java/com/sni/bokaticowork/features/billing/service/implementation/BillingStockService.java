package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.BillingStockReservation;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingStockReservationRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingStockGuard;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockOutRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockReservationRequest;
import com.sni.bokaticowork.features.inventory.stock.enums.StockOutReasonCode;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Devis et inventaire (lot B7) : le stock des articles d'un devis est reserve a son acceptation,
 * dans la limite du disponible, libere au refus, a l'expiration ou a la revision ; a l'emission de
 * la facture, les reservations du devis sont consommees et le reste sort du stock (parametre
 * {@code billing.stock.issue-on} : INVOICE par defaut, NONE pour ne rien sortir). Le cout sort
 * au 603 par le lot de stock.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BillingStockService {

    static final String ISSUE_ON_KEY = "billing.stock.issue-on";

    private final BillingStockReservationRepository reservations;
    private final BillingDocumentLineRepository lines;
    private final InventoryItemRepository items;
    private final StockLevelRepository levels;
    private final StockService stock;
    private final BillingStockGuard guard;

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Devis accepte : reserve le stock de chaque article suivi, dans la limite du disponible ; rend ce qui manque. */
    public BigDecimal reserveForQuote(BillingDocument quote) {
        if (!reservations.findByDocumentNumberAndStatus(quote.getDocumentNumber(), "ACTIVE").isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal missing = BigDecimal.ZERO;
        for (BillingDocumentLine line : lines.findAllByDocumentOrderByLineOrderAscIdAsc(quote)) {
            InventoryItem item = trackedItem(line);
            if (item == null) {
                continue;
            }
            BigDecimal needed = line.getQuantity() == null ? BigDecimal.ONE : line.getQuantity();
            for (StockLevel level : levels.findAllByItemAndQuantityAvailableGreaterThanOrderByQuantityAvailableDesc(item, BigDecimal.ZERO)) {
                if (needed.signum() <= 0) {
                    break;
                }
                BigDecimal take = needed.min(level.getQuantityAvailable());
                StockReservationRequest request = new StockReservationRequest();
                request.setItemCode(item.getItemCode());
                request.setLocationCode(level.getLocation().getLocationCode());
                request.setQuantity(take);
                request.setReferenceType(StockReferenceType.OTHER);
                request.setReferenceCode(quote.getDocumentNumber());
                request.setReservedBy("DEVIS " + quote.getDocumentNumber());
                var reserved = stock.reserve(request);
                reservations.save(BillingStockReservation.builder().documentNumber(quote.getDocumentNumber()).lineOrder(line.getLineOrder())
                        .itemCode(item.getItemCode()).locationCode(level.getLocation().getLocationCode())
                        .reservationCode(reserved.getReservationCode()).quantity(take).status("ACTIVE").build());
                needed = needed.subtract(take);
            }
            missing = missing.add(needed.max(BigDecimal.ZERO));
        }
        return missing;
    }

    /** Devis refuse, expire, annule ou revise : ses reservations sont liberees. */
    public void releaseForQuote(String quoteNumber) {
        for (BillingStockReservation reservation : reservations.findByDocumentNumberAndStatus(quoteNumber, "ACTIVE")) {
            try {
                stock.releaseReservation(reservation.getReservationCode());
            } catch (RuntimeException exception) {
                log.warn("Reservation {} du devis {} non liberee : {}", reservation.getReservationCode(), quoteNumber, exception.getMessage());
            }
            reservation.setStatus("RELEASED");
            reservation.setClosedAt(Instant.now());
            reservations.save(reservation);
        }
    }

    /**
     * Facture emise : les reservations de son devis sont consommees, le reste de chaque ligne d'article
     * suivi sort du stock (sortie autorisee en negatif si l'article le permet).
     */
    public void issueForInvoice(BillingDocument invoice) {
        String mode = platformSettings == null ? "INVOICE" : platformSettings.text(ISSUE_ON_KEY, "INVOICE");
        if (!"INVOICE".equalsIgnoreCase(mode) || invoice.getDocumentType() != BillingDocumentType.INVOICE
                || StringUtils.hasText(invoice.getDepositForDocumentNumber())) {
            return;
        }
        String quoteNumber = "QUOTE".equalsIgnoreCase(invoice.getSourceType()) ? invoice.getSourceCode() : null;
        List<BillingStockReservation> reserved = quoteNumber == null ? List.of() : reservations.findByDocumentNumberAndStatus(quoteNumber, "ACTIVE");
        for (BillingDocumentLine line : lines.findAllByDocumentOrderByLineOrderAscIdAsc(invoice)) {
            InventoryItem item = trackedItem(line);
            if (item == null) {
                continue;
            }
            BigDecimal needed = line.getQuantity() == null ? BigDecimal.ONE : line.getQuantity();
            for (BillingStockReservation reservation : reserved) {
                if (needed.signum() <= 0 || !"ACTIVE".equals(reservation.getStatus()) || !reservation.getItemCode().equals(item.getItemCode())) {
                    continue;
                }
                stock.consumeReservation(reservation.getReservationCode(), "FACTURE " + invoice.getDocumentNumber());
                reservation.setStatus("CONSUMED");
                reservation.setClosedBy(invoice.getDocumentNumber());
                reservation.setClosedAt(Instant.now());
                reservations.save(reservation);
                needed = needed.subtract(reservation.getQuantity());
            }
            if (needed.signum() > 0) {
                issueRemainder(invoice.getDocumentNumber(), "Vente, facture " + invoice.getDocumentNumber(), item, needed);
            }
        }
        // Ce qui reste reserve au devis (lignes retirees de la facture) est libere.
        if (quoteNumber != null) {
            releaseForQuote(quoteNumber);
        }
    }

    private void issueRemainder(String reference, String details, InventoryItem item, BigDecimal quantity) {
        BigDecimal left = quantity;
        List<StockLevel> available = levels.findAllByItemAndQuantityAvailableGreaterThanOrderByQuantityAvailableDesc(item, BigDecimal.ZERO);
        for (StockLevel level : available) {
            if (left.signum() <= 0) {
                break;
            }
            BigDecimal take = left.min(level.getQuantityAvailable());
            stock.issue(out(reference, details, item, level.getLocation().getLocationCode(), take, false));
            left = left.subtract(take);
        }
        if (left.signum() > 0) {
            if (!Boolean.TRUE.equals(item.getAllowNegativeStock())) {
                throw new BadRequestException("Stock insuffisant pour " + item.getName() + " : " + left.stripTrailingZeros().toPlainString() + " manquant(s)");
            }
            List<StockLevel> any = levels.findAllByItemIdOrderByQuantityAvailableAsc(item.getId());
            if (any.isEmpty()) {
                throw new BadRequestException("Aucun emplacement de stock pour " + item.getName());
            }
            stock.issue(out(reference, details, item, any.get(any.size() - 1).getLocation().getLocationCode(), left, true));
        }
    }

    private static StockOutRequest out(String reference, String details, InventoryItem item, String location, BigDecimal quantity, boolean negative) {
        StockOutRequest request = new StockOutRequest();
        request.setItemCode(item.getItemCode());
        request.setLocationCode(location);
        request.setQuantity(quantity);
        request.setReasonCode(StockOutReasonCode.OTHER);
        request.setReferenceType(StockReferenceType.OTHER);
        request.setReferenceCode(reference);
        request.setReasonDetails(details);
        request.setAllowNegativeOverride(negative);
        request.setPerformedBy("FACTURATION");
        return request;
    }

    /** Mode de sortie du stock : INVOICE (defaut), DELIVERY (lot C4) ou NONE. */
    public String issueMode() {
        String mode = platformSettings == null ? "INVOICE" : platformSettings.text(ISSUE_ON_KEY, "INVOICE");
        return mode == null ? "INVOICE" : mode.trim().toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * Lot C4 : bon livre, en mode DELIVERY. Les reservations du devis d'origine sont consommees
     * d'abord, puis le reste sort du stock. Rend vrai si au moins une ligne a sorti du stock.
     */
    public boolean issueForDelivery(String noteNumber, String quoteNumber, List<BillingDocumentLine> delivered) {
        if (!"DELIVERY".equals(issueMode())) {
            return false;
        }
        List<BillingStockReservation> reserved = quoteNumber == null ? List.of() : reservations.findByDocumentNumberAndStatus(quoteNumber, "ACTIVE");
        boolean issued = false;
        for (BillingDocumentLine line : delivered) {
            InventoryItem item = trackedItem(line);
            if (item == null) {
                continue;
            }
            BigDecimal needed = line.getQuantity() == null ? BigDecimal.ONE : line.getQuantity();
            for (BillingStockReservation reservation : reserved) {
                if (needed.signum() <= 0 || !"ACTIVE".equals(reservation.getStatus()) || !reservation.getItemCode().equals(item.getItemCode())
                        || reservation.getQuantity().compareTo(needed) > 0) {
                    continue;
                }
                stock.consumeReservation(reservation.getReservationCode(), "LIVRAISON " + noteNumber);
                reservation.setStatus("CONSUMED");
                reservation.setClosedBy(noteNumber);
                reservation.setClosedAt(Instant.now());
                reservations.save(reservation);
                needed = needed.subtract(reservation.getQuantity());
            }
            if (needed.signum() > 0) {
                issueRemainder(noteNumber, "Livraison, bon " + noteNumber, item, needed);
            }
            issued = true;
        }
        return issued;
    }

    /** Lot C4 : bon livre puis annule. La marchandise revient en stock, a l'emplacement le plus fourni. */
    public void returnForDelivery(String noteNumber, List<BillingDocumentLine> delivered) {
        for (BillingDocumentLine line : delivered) {
            InventoryItem item = trackedItem(line);
            if (item == null) {
                continue;
            }
            List<StockLevel> any = levels.findAllByItemIdOrderByQuantityAvailableAsc(item.getId());
            if (any.isEmpty()) {
                throw new BadRequestException("Aucun emplacement de stock pour " + item.getName());
            }
            com.sni.bokaticowork.features.inventory.stock.dto.request.StockInRequest in =
                    new com.sni.bokaticowork.features.inventory.stock.dto.request.StockInRequest();
            in.setItemCode(item.getItemCode());
            in.setLocationCode(any.get(any.size() - 1).getLocation().getLocationCode());
            in.setQuantity(line.getQuantity());
            in.setReferenceType(StockReferenceType.OTHER);
            in.setReferenceCode(noteNumber);
            in.setReason("Retour, bon de livraison annulé " + noteNumber);
            in.setPerformedBy("FACTURATION");
            stock.receive(in);
        }
    }

    private InventoryItem trackedItem(BillingDocumentLine line) {
        if (Boolean.TRUE.equals(line.getOptional())) {
            return null;
        }
        String code = StringUtils.hasText(line.getItemCode()) ? line.getItemCode() : line.getSourceCode();
        BillingStockGuard.Line view = new BillingStockGuard.Line(0, code, line.getSourceType(), line.getLineType(), line.getDescription(),
                line.getQuantity());
        if (!BillingStockGuard.isInventoryLine(view) || !guard.trackedItem(code)) {
            return null;
        }
        return items.findByItemCode(code.trim()).orElse(null);
    }

    static String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
