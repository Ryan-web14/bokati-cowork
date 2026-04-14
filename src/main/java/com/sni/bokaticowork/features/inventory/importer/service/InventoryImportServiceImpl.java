package com.sni.bokaticowork.features.inventory.importer.service;

import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetRequest;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetCondition;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetStatus;
import com.sni.bokaticowork.features.inventory.asset.service.interfaces.AssetService;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemResponse;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemService;
import com.sni.bokaticowork.features.inventory.importer.dto.InventoryImportResponse;
import com.sni.bokaticowork.features.inventory.importer.dto.InventoryImportRowResult;
import com.sni.bokaticowork.features.inventory.importer.enums.InventoryImportType;
import com.sni.bokaticowork.features.inventory.procurement.dto.ProcurementDtos.SupplierRequest;
import com.sni.bokaticowork.features.inventory.procurement.dto.ProcurementDtos.SupplierResponse;
import com.sni.bokaticowork.features.inventory.procurement.enums.SupplierStatus;
import com.sni.bokaticowork.features.inventory.procurement.service.ProcurementService;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockInRequest;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryImportServiceImpl implements InventoryImportService {

    private final TabularInventoryFileReader fileReader;
    private final InventoryItemService itemService;
    private final StockService stockService;
    private final AssetService assetService;
    private final ProcurementService procurementService;

    @Override
    @Transactional
    public InventoryImportResponse importFile(InventoryImportType type, MultipartFile file, boolean dryRun) {
        List<Map<String, String>> rows = fileReader.read(file);
        List<InventoryImportRowResult> results = new ArrayList<>();
        int rowNumber = 2;
        for (Map<String, String> row : rows) {
            results.add(processRow(type, row, rowNumber++, dryRun));
        }
        int success = (int) results.stream().filter(InventoryImportRowResult::isSuccess).count();
        return InventoryImportResponse.builder()
                .importType(type)
                .fileName(file.getOriginalFilename())
                .dryRun(dryRun)
                .totalRows(results.size())
                .successCount(success)
                .errorCount(results.size() - success)
                .rows(results)
                .build();
    }

    private InventoryImportRowResult processRow(InventoryImportType type, Map<String, String> row, int rowNumber, boolean dryRun) {
        try {
            String reference = switch (type) {
                case ITEMS -> importItem(row, dryRun);
                case INITIAL_STOCK -> importInitialStock(row, dryRun);
                case SUPPLIERS -> importSupplier(row, dryRun);
                case ASSETS -> importAsset(row, dryRun);
            };
            return InventoryImportRowResult.builder()
                    .rowNumber(rowNumber)
                    .success(true)
                    .referenceCode(reference)
                    .message(dryRun ? "Validated" : "Imported")
                    .build();
        } catch (Exception ex) {
            return InventoryImportRowResult.builder()
                    .rowNumber(rowNumber)
                    .success(false)
                    .message(ex.getMessage())
                    .build();
        }
    }

    private String importItem(Map<String, String> row, boolean dryRun) {
        InventoryItemRequest request = new InventoryItemRequest();
        request.setItemCode(value(row, "item_code"));
        request.setName(required(row, "name"));
        request.setDescription(value(row, "description"));
        request.setPsku(value(row, "psku"));
        request.setShortCode(value(row, "short_code"));
        request.setDisplayCode(value(row, "display_code"));
        request.setIdentificationCode(value(row, "identification_code"));
        request.setSpecification(value(row, "specification"));
        request.setCategoryCode(value(row, "category_code"));
        request.setUnitCode(value(row, "unit_code"));
        request.setItemType(enumValue(row, "item_type", InventoryItemType.class, InventoryItemType.CONSUMABLE));
        request.setTrackingType(enumValue(row, "tracking_type", InventoryTrackingType.class, InventoryTrackingType.QUANTITY));
        request.setDefaultCost(longValue(row, "default_cost"));
        request.setSalePrice(longValue(row, "sale_price"));
        request.setTaxable(booleanValue(row, "taxable"));
        request.setAllowNegativeStock(booleanValue(row, "allow_negative_stock"));
        request.setRequiresExpiryDate(booleanValue(row, "requires_expiry_date"));
        request.setRequiresLotNumber(booleanValue(row, "requires_lot_number"));
        request.setRequiresSerialNumber(booleanValue(row, "requires_serial_number"));
        request.setActive(booleanValue(row, "active"));
        if (dryRun) return fallbackReference(request.getItemCode(), request.getName());
        InventoryItemResponse response = itemService.create(request);
        return response.getItemCode();
    }

    private String importInitialStock(Map<String, String> row, boolean dryRun) {
        StockInRequest request = new StockInRequest();
        request.setItemCode(required(row, "item_code"));
        request.setLocationCode(required(row, "location_code"));
        request.setQuantity(decimalValue(row, "quantity"));
        request.setUnitCost(longValue(row, "unit_cost"));
        request.setLotNumber(value(row, "lot_number"));
        request.setExpiryDate(dateValue(row, "expiry_date"));
        request.setReferenceType(StockReferenceType.MANUAL_ADJUSTMENT);
        request.setReferenceCode(value(row, "reference_code"));
        request.setReason(defaultValue(row, "reason", "Initial stock import"));
        request.setPerformedBy(value(row, "performed_by"));
        if (dryRun) return request.getItemCode() + "@" + request.getLocationCode();
        return stockService.receive(request).getMovementCode();
    }

    private String importSupplier(Map<String, String> row, boolean dryRun) {
        SupplierRequest request = new SupplierRequest();
        request.setSupplierCode(value(row, "supplier_code"));
        request.setName(required(row, "name"));
        request.setEmail(value(row, "email"));
        request.setPhone(value(row, "phone"));
        request.setTaxId(value(row, "tax_id"));
        request.setAddress(value(row, "address"));
        request.setStatus(enumValue(row, "status", SupplierStatus.class, SupplierStatus.ACTIVE));
        if (dryRun) return fallbackReference(request.getSupplierCode(), request.getName());
        SupplierResponse response = procurementService.createSupplier(request);
        return response.getSupplierCode();
    }

    private String importAsset(Map<String, String> row, boolean dryRun) {
        AssetRequest request = new AssetRequest();
        request.setAssetCode(value(row, "asset_code"));
        request.setItemCode(required(row, "item_code"));
        request.setSerialNumber(value(row, "serial_number"));
        request.setAssetTag(value(row, "asset_tag"));
        request.setStatus(enumValue(row, "status", AssetStatus.class, AssetStatus.AVAILABLE));
        request.setCondition(enumValue(row, "condition", AssetCondition.class, AssetCondition.GOOD));
        request.setLocationCode(value(row, "location_code"));
        request.setPurchaseDate(dateValue(row, "purchase_date"));
        request.setPurchaseCost(longValue(row, "purchase_cost"));
        request.setWarrantyEndDate(dateValue(row, "warranty_end_date"));
        request.setNotes(value(row, "notes"));
        if (dryRun) return fallbackReference(request.getAssetCode(), request.getAssetTag());
        return assetService.create(request).getAssetCode();
    }

    private String value(Map<String, String> row, String key) {
        String value = row.get(key);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String required(Map<String, String> row, String key) {
        String value = value(row, key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Missing required column: " + key);
        }
        return value;
    }

    private String defaultValue(Map<String, String> row, String key, String fallback) {
        String value = value(row, key);
        return StringUtils.hasText(value) ? value : fallback;
    }

    private BigDecimal decimalValue(Map<String, String> row, String key) {
        String value = required(row, key);
        return new BigDecimal(value.replace(" ", "").replace(",", "."));
    }

    private Long longValue(Map<String, String> row, String key) {
        String value = value(row, key);
        return StringUtils.hasText(value) ? Long.parseLong(value.replace(" ", "")) : null;
    }

    private Boolean booleanValue(Map<String, String> row, String key) {
        String value = value(row, key);
        if (!StringUtils.hasText(value)) return null;
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "y", "oui" -> true;
            case "false", "0", "no", "n", "non" -> false;
            default -> throw new IllegalArgumentException("Invalid boolean for " + key + ": " + value);
        };
    }

    private LocalDate dateValue(Map<String, String> row, String key) {
        String value = value(row, key);
        return StringUtils.hasText(value) ? LocalDate.parse(value) : null;
    }

    private <E extends Enum<E>> E enumValue(Map<String, String> row, String key, Class<E> enumType, E fallback) {
        String value = value(row, key);
        return StringUtils.hasText(value) ? Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT)) : fallback;
    }

    private String fallbackReference(String primary, String secondary) {
        return StringUtils.hasText(primary) ? primary : secondary;
    }
}
