package com.sni.bokaticowork.features.inventory.admin.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryDashboardResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryLabelResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryAnomalyReportResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryMovementReportResponse;
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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

    @Override
    public InventoryDashboardResponse dashboard() {
        return InventoryDashboardResponse.builder()
                .openAlerts(alertRepository.countByStatus(InventoryAlertStatus.OPEN))
                .lowStockAlerts(alertRepository.countByStatusAndAlertType(InventoryAlertStatus.OPEN, InventoryAlertType.LOW_STOCK))
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

    @Override
    public InventoryMovementReportResponse movementReport(String itemCode, String locationCode, Instant fromDate, Instant toDate) {
        String normalizedItem = normalizeOptional(itemCode);
        String normalizedLocation = normalizeOptional(locationCode);
        List<InventoryMovementReportResponse.Line> lines = movementRepository
                .movementReport(normalizedItem, normalizedLocation, fromDate, toDate)
                .stream()
                .map(row -> InventoryMovementReportResponse.Line.builder()
                        .movementType((StockMovementType) row[0])
                        .movementCount(((Number) row[1]).longValue())
                        .totalQuantity((BigDecimal) row[2])
                        .totalValue(((Number) row[3]).longValue())
                        .build())
                .toList();
        return InventoryMovementReportResponse.builder()
                .itemCode(normalizedItem)
                .locationCode(normalizedLocation)
                .fromDate(fromDate == null ? null : fromDate.toString())
                .toDate(toDate == null ? null : toDate.toString())
                .lines(lines)
                .build();
    }

    @Override
    public String movementReportCsv(String itemCode, String locationCode, Instant fromDate, Instant toDate) {
        InventoryMovementReportResponse report = movementReport(itemCode, locationCode, fromDate, toDate);
        StringBuilder csv = new StringBuilder("movementType,movementCount,totalQuantity,totalValue\n");
        for (InventoryMovementReportResponse.Line line : report.getLines()) {
            csv.append(line.getMovementType()).append(',')
                    .append(line.getMovementCount()).append(',')
                    .append(line.getTotalQuantity()).append(',')
                    .append(line.getTotalValue()).append('\n');
        }
        return csv.toString();
    }

    @Override
    public byte[] movementReportPdf(String itemCode, String locationCode, Instant fromDate, Instant toDate) {
        InventoryMovementReportResponse report = movementReport(itemCode, locationCode, fromDate, toDate);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 14);
                content.newLineAtOffset(50, 750);
                content.showText("Inventory movement report");
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(0, -20);
                content.showText("Item: " + nullSafe(report.getItemCode()) + " | Location: " + nullSafe(report.getLocationCode()));
                content.newLineAtOffset(0, -20);
                content.showText("Type | Count | Quantity | Value");
                for (InventoryMovementReportResponse.Line line : report.getLines()) {
                    content.newLineAtOffset(0, -16);
                    content.showText(line.getMovementType() + " | " + line.getMovementCount()
                            + " | " + line.getTotalQuantity() + " | " + line.getTotalValue());
                }
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BadRequestException("Unable to generate inventory movement PDF report");
        }
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
