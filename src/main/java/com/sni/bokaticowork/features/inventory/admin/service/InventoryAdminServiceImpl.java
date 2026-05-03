package com.sni.bokaticowork.features.inventory.admin.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryDashboardResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryLabelResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryAnomalyReportResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryMovementReportResponse;
import com.sni.bokaticowork.features.inventory.admin.service.support.InventoryMovementReportPdfRenderer;
import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import com.sni.bokaticowork.features.inventory.asset.repository.AssetRepository;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertStatus;
import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertType;
import com.sni.bokaticowork.features.inventory.intelligence.repository.InventoryAlertRepository;
import com.sni.bokaticowork.features.inventory.procurement.enums.PurchaseOrderStatus;
import com.sni.bokaticowork.features.inventory.procurement.repository.PurchaseOrderRepository;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.repository.InventoryLocationRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLotRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockMovementRepository;
import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import com.sni.bokaticowork.features.inventory.control.repository.InventoryCountItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InventoryAdminServiceImpl implements InventoryAdminService {

    private final InventoryAlertRepository alertRepository;
    private final StockLotRepository lotRepository;
    private final StockLevelRepository levelRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryItemLookupService itemLookupService;
    private final InventoryLocationRepository locationRepository;
    private final AssetRepository assetRepository;
    private final StockMovementRepository movementRepository;
    private final InventoryCountItemRepository countItemRepository;
    private final InventoryMovementReportPdfRenderer movementReportPdfRenderer;

    @Override
    public InventoryDashboardResponse dashboard() {
        return InventoryDashboardResponse.builder()
                .openAlerts(alertRepository.countByStatus(InventoryAlertStatus.OPEN))
                .lowStockAlerts(alertRepository.countByStatusAndAlertType(InventoryAlertStatus.OPEN, InventoryAlertType.LOW_STOCK)
                        + alertRepository.countByStatusAndAlertType(InventoryAlertStatus.OPEN, InventoryAlertType.RECURRING_LOW_STOCK))
                .outOfStockAlerts(alertRepository.countByStatusAndAlertType(InventoryAlertStatus.OPEN, InventoryAlertType.OUT_OF_STOCK))
                .negativeStockAlerts(alertRepository.countByStatusAndAlertType(InventoryAlertStatus.OPEN, InventoryAlertType.NEGATIVE_STOCK))
                .expirySoonAlerts(alertRepository.countByStatusAndAlertType(InventoryAlertStatus.OPEN, InventoryAlertType.EXPIRY_SOON))
                .expiringLots30Days(lotRepository.countByExpiryDateLessThanEqualAndRemainingQuantityGreaterThanAndActiveTrue(
                        LocalDate.now().plusDays(30), BigDecimal.ZERO))
                .outOfStockLevels(levelRepository.countByQuantityAvailableLessThanEqual(BigDecimal.ZERO))
                .openPurchaseOrders(purchaseOrderRepository.countByStatusIn(List.of(
                        PurchaseOrderStatus.APPROVED, PurchaseOrderStatus.ORDERED, PurchaseOrderStatus.PARTIALLY_RECEIVED)))
                .totalStockValue(levelRepository.totalStockValue())
                .build();
    }

    @Override
    public InventoryLabelResponse label(String type, String code) {
        String normalizedType = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedType) {
            case "ITEM" -> itemLabel(code);
            case "LOCATION" -> locationLabel(code);
            case "ASSET" -> assetLabel(code);
            case "DISPLAY" -> displayLabel(code);
            default -> throw new BadRequestException("Unsupported label type. Use ITEM, LOCATION, ASSET or DISPLAY");
        };
    }

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("UTC"));

    @Override
    public InventoryMovementReportResponse movementReport(String itemCode, String locationCode, Instant fromDate, Instant toDate) {
        String normalizedItem = normalizeOptional(itemCode);
        String normalizedLocation = normalizeOptional(locationCode);

        StringBuilder title = new StringBuilder("Rapport de mouvements de stock");
        if (normalizedItem != null) title.append(" — Article ").append(normalizedItem);
        if (normalizedLocation != null) title.append(" — Emplacement ").append(normalizedLocation);
        if (fromDate != null) title.append(" | Du ").append(DISPLAY_FMT.format(fromDate));
        if (toDate != null) title.append(" au ").append(DISPLAY_FMT.format(toDate));

        List<InventoryMovementReportResponse.Line> lines = movementRepository
                .movementReport(normalizedItem, normalizedLocation, fromDate, toDate)
                .stream()
                .map(row -> InventoryMovementReportResponse.Line.builder()
                        .movementType(StockMovementType.valueOf(row[0].toString()))
                        .movementCount(((Number) row[1]).longValue())
                        .totalQuantity((BigDecimal) row[2])
                        .totalValue(((Number) row[3]).longValue())
                        .signedQuantity(signedQuantity(StockMovementType.valueOf(row[0].toString()), (BigDecimal) row[2]))
                        .signedValue(signedValue(StockMovementType.valueOf(row[0].toString()), ((Number) row[3]).longValue()))
                        .build())
                .toList();
        Object[] summaryRow = movementRepository.movementReportSummary(normalizedItem, normalizedLocation, fromDate, toDate);
        return InventoryMovementReportResponse.builder()
                .reportTitle(title.toString())
                .itemCode(normalizedItem)
                .locationCode(normalizedLocation)
                .fromDate(fromDate == null ? null : DISPLAY_FMT.format(fromDate))
                .toDate(toDate == null ? null : DISPLAY_FMT.format(toDate))
                .generatedAt(DISPLAY_FMT.format(Instant.now()))
                .summary(toSummary(summaryRow))
                .lines(lines)
                .items(movementRepository.movementReportByItem(normalizedItem, normalizedLocation, fromDate, toDate)
                        .stream().map(this::toItemLine).toList())
                .locations(movementRepository.movementReportByLocation(normalizedItem, normalizedLocation, fromDate, toDate)
                        .stream().map(this::toLocationLine).toList())
                .recentMovements(movementRepository.movementReportRecentMovements(normalizedItem, normalizedLocation, fromDate, toDate)
                        .stream().map(this::toMovementDetail).toList())
                .build();
    }

    @Override
    public String movementReportCsv(String itemCode, String locationCode, Instant fromDate, Instant toDate) {
        InventoryMovementReportResponse report = movementReport(itemCode, locationCode, fromDate, toDate);
        StringBuilder csv = new StringBuilder();

        csv.append(escapeCsv(report.getReportTitle())).append('\n');
        csv.append("Genere le:,").append(report.getGeneratedAt()).append('\n');
        csv.append('\n');

        csv.append("RESUME\n");
        csv.append("Total mouvements,Entrees (qte),Sorties (qte),Net (qte),Entrees (valeur),Sorties (valeur),Net (valeur)\n");
        InventoryMovementReportResponse.Summary s = report.getSummary();
        if (s != null) {
            csv.append(s.getMovementCount()).append(',')
                    .append(s.getTotalInQuantity()).append(',')
                    .append(s.getTotalOutQuantity()).append(',')
                    .append(s.getNetQuantity()).append(',')
                    .append(s.getTotalInValue()).append(',')
                    .append(s.getTotalOutValue()).append(',')
                    .append(s.getNetValue()).append('\n');
        }
        csv.append('\n');

        csv.append("PAR TYPE DE MOUVEMENT\n");
        csv.append("Type,Nb mouvements,Quantite totale,Valeur totale,Quantite signee,Valeur signee\n");
        for (InventoryMovementReportResponse.Line line : report.getLines()) {
            csv.append(line.getMovementType()).append(',')
                    .append(line.getMovementCount()).append(',')
                    .append(line.getTotalQuantity()).append(',')
                    .append(line.getTotalValue()).append(',')
                    .append(line.getSignedQuantity()).append(',')
                    .append(line.getSignedValue()).append('\n');
        }
        csv.append('\n');

        csv.append("PAR ARTICLE (TOP 20)\n");
        csv.append("Code article,Nom,Nb mouvements,Entrees,Sorties,Net,Valeur totale\n");
        for (InventoryMovementReportResponse.ItemLine item : report.getItems()) {
            csv.append(escapeCsv(item.getItemCode())).append(',')
                    .append(escapeCsv(item.getItemName())).append(',')
                    .append(item.getMovementCount()).append(',')
                    .append(item.getTotalInQuantity()).append(',')
                    .append(item.getTotalOutQuantity()).append(',')
                    .append(item.getNetQuantity()).append(',')
                    .append(item.getTotalValue()).append('\n');
        }
        csv.append('\n');

        csv.append("PAR EMPLACEMENT (TOP 20)\n");
        csv.append("Code emplacement,Nom,Nb mouvements,Entrees,Sorties,Net,Valeur totale\n");
        for (InventoryMovementReportResponse.LocationLine loc : report.getLocations()) {
            csv.append(escapeCsv(loc.getLocationCode())).append(',')
                    .append(escapeCsv(loc.getLocationName())).append(',')
                    .append(loc.getMovementCount()).append(',')
                    .append(loc.getTotalInQuantity()).append(',')
                    .append(loc.getTotalOutQuantity()).append(',')
                    .append(loc.getNetQuantity()).append(',')
                    .append(loc.getTotalValue()).append('\n');
        }
        csv.append('\n');

        csv.append("DETAIL DES MOUVEMENTS RECENTS\n");
        csv.append("Code,Type,Article,Nom article,De,Vers,Quantite,Cout unitaire,Cout total,Ref type,Ref code,Effectue par,Date\n");
        for (InventoryMovementReportResponse.MovementDetail mv : report.getRecentMovements()) {
            csv.append(escapeCsv(mv.getMovementCode())).append(',')
                    .append(mv.getMovementType()).append(',')
                    .append(escapeCsv(mv.getItemCode())).append(',')
                    .append(escapeCsv(mv.getItemName())).append(',')
                    .append(escapeCsv(mv.getLocationFromCode())).append(',')
                    .append(escapeCsv(mv.getLocationToCode())).append(',')
                    .append(mv.getQuantity()).append(',')
                    .append(mv.getUnitCost()).append(',')
                    .append(mv.getTotalCost()).append(',')
                    .append(escapeCsv(mv.getReferenceType())).append(',')
                    .append(escapeCsv(mv.getReferenceCode())).append(',')
                    .append(escapeCsv(mv.getPerformedBy())).append(',')
                    .append(escapeCsv(mv.getPerformedAt())).append('\n');
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    public byte[] movementReportPdf(String itemCode, String locationCode, Instant fromDate, Instant toDate) {
        InventoryMovementReportResponse report = movementReport(itemCode, locationCode, fromDate, toDate);
        return movementReportPdfRenderer.render(report);
    }

    @Override
    public InventoryAnomalyReportResponse anomalyReport() {
        return InventoryAnomalyReportResponse.builder()
                .negativeStockLevels(levelRepository.countByQuantityAvailableLessThanEqual(BigDecimal.ZERO))
                .negativeStockAlerts(alertRepository.countByStatusAndAlertType(InventoryAlertStatus.OPEN, InventoryAlertType.NEGATIVE_STOCK))
                .movementsWithNegativeOverride(movementRepository.countByAllowNegativeOverrideTrue())
                .suspiciousAdjustments(movementRepository.countByMovementTypeInAndQuantityGreaterThan(
                        List.of(StockMovementType.ADJUSTMENT_IN, StockMovementType.ADJUSTMENT_OUT), BigDecimal.valueOf(100)))
                .largeInventoryCountVariances(countItemRepository.countByVarianceQuantityGreaterThanOrVarianceQuantityLessThan(
                        BigDecimal.valueOf(100), BigDecimal.valueOf(-100)))
                .build();
    }

    private InventoryMovementReportResponse.Summary toSummary(Object[] row) {
        Object[] values = unwrapRow(row);
        return InventoryMovementReportResponse.Summary.builder()
                .movementCount(longAt(values, 0))
                .totalInQuantity(decimalAt(values, 1))
                .totalOutQuantity(decimalAt(values, 2))
                .netQuantity(decimalAt(values, 3))
                .totalInValue(longAt(values, 4))
                .totalOutValue(longAt(values, 5))
                .netValue(longAt(values, 6))
                .totalMovementValue(longAt(values, 7))
                .firstMovementAt(stringAt(values, 8))
                .lastMovementAt(stringAt(values, 9))
                .build();
    }

    private InventoryMovementReportResponse.ItemLine toItemLine(Object[] row) {
        return InventoryMovementReportResponse.ItemLine.builder()
                .itemCode(stringAt(row, 0))
                .itemName(stringAt(row, 1))
                .movementCount(longAt(row, 2))
                .totalInQuantity(decimalAt(row, 3))
                .totalOutQuantity(decimalAt(row, 4))
                .netQuantity(decimalAt(row, 5))
                .totalValue(longAt(row, 6))
                .build();
    }

    private InventoryMovementReportResponse.LocationLine toLocationLine(Object[] row) {
        return InventoryMovementReportResponse.LocationLine.builder()
                .locationCode(stringAt(row, 0))
                .locationName(stringAt(row, 1))
                .movementCount(longAt(row, 2))
                .totalInQuantity(decimalAt(row, 3))
                .totalOutQuantity(decimalAt(row, 4))
                .netQuantity(decimalAt(row, 5))
                .totalValue(longAt(row, 6))
                .build();
    }

    private InventoryMovementReportResponse.MovementDetail toMovementDetail(Object[] row) {
        return InventoryMovementReportResponse.MovementDetail.builder()
                .movementCode(stringAt(row, 0))
                .movementType(StockMovementType.valueOf(stringAt(row, 1)))
                .itemCode(stringAt(row, 2))
                .itemName(stringAt(row, 3))
                .locationFromCode(stringAt(row, 4))
                .locationToCode(stringAt(row, 5))
                .quantity(decimalAt(row, 6))
                .unitCost(nullableLongAt(row, 7))
                .totalCost(nullableLongAt(row, 8))
                .referenceType(stringAt(row, 9))
                .referenceCode(stringAt(row, 10))
                .performedBy(stringAt(row, 11))
                .performedAt(stringAt(row, 12))
                .build();
    }

    private BigDecimal signedQuantity(StockMovementType type, BigDecimal quantity) {
        if (quantity == null) {
            return BigDecimal.ZERO;
        }
        return switch (type) {
            case OUT, ADJUSTMENT_OUT -> quantity.negate();
            case IN, ADJUSTMENT_IN -> quantity;
            case TRANSFER -> BigDecimal.ZERO;
        };
    }

    private Long signedValue(StockMovementType type, Long value) {
        if (value == null) {
            return 0L;
        }
        return switch (type) {
            case OUT, ADJUSTMENT_OUT -> -value;
            case IN, ADJUSTMENT_IN -> value;
            case TRANSFER -> 0L;
        };
    }

    private BigDecimal decimalAt(Object[] row, int index) {
        if (row == null || row[index] == null) {
            return BigDecimal.ZERO;
        }
        if (row[index] instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(row[index].toString());
    }

    private long longAt(Object[] row, int index) {
        Long value = nullableLongAt(row, index);
        return value == null ? 0L : value;
    }

    private Long nullableLongAt(Object[] row, int index) {
        if (row == null || row[index] == null) {
            return null;
        }
        if (row[index] instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(row[index].toString());
    }

    private String stringAt(Object[] row, int index) {
        return row == null || row[index] == null ? null : row[index].toString();
    }

    private Object[] unwrapRow(Object[] row) {
        if (row != null && row.length == 1 && row[0] instanceof Object[] nested) {
            return nested;
        }
        return row;
    }

    private InventoryLabelResponse itemLabel(String code) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(code);
        return label("ITEM", item.getItemCode(), item.getName(), item.getItemCode());
    }

    private InventoryLabelResponse locationLabel(String code) {
        InventoryLocation location = locationRepository.findByLocationCode(normalize(code))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory location not found"));
        return label("LOCATION", location.getLocationCode(), location.getName(), location.getLocationCode());
    }

    private InventoryLabelResponse assetLabel(String code) {
        Asset asset = assetRepository.findByAssetCode(normalize(code))
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
        return label("ASSET", asset.getAssetCode(), asset.getAssetTag(), asset.getAssetCode());
    }

    private InventoryLabelResponse displayLabel(String code) {
        InventoryItem item = itemLookupService.findByItemCodeOrThrow(code);
        String displayCode = item.getDisplayCode() == null ? item.getItemCode() : item.getDisplayCode();
        return label("DISPLAY", displayCode, item.getName(), displayCode);
    }

    private InventoryLabelResponse label(String type, String code, String text, String value) {
        return InventoryLabelResponse.builder()
                .labelType(type)
                .code(code)
                .displayText(text)
                .barcodeValue(value)
                .qrValue(type + ":" + value)
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) throw new BadRequestException("Code is required");
        return value.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+", "-")
                .replaceAll("^-|-$", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : normalize(value);
    }

    private String nullSafe(String value) {
        return value == null ? "ALL" : value;
    }
}
