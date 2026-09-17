package com.sni.bokaticowork.features.inventory.catalog.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import com.sni.bokaticowork.features.inventory.asset.repository.AssetRepository;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryScanResponse;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryBarcode;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryPackaging;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryBarcodeRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryPackagingRepository;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryScanService;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryItemSerial;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import com.sni.bokaticowork.features.inventory.stock.repository.InventoryItemSerialRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.InventoryLocationRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryScanServiceImpl implements InventoryScanService {

    private final InventoryBarcodeRepository barcodeRepository;
    private final InventoryPackagingRepository packagingRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryLocationRepository locationRepository;
    private final AssetRepository assetRepository;
    private final InventoryItemSerialRepository serialRepository;
    private final StockLotRepository lotRepository;

    @Override
    public InventoryScanResponse resolve(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Scanned code is required");
        }
        String raw = code.trim();
        String normalized = normalizeCode(raw);
        String upper = raw.toUpperCase(Locale.ROOT);

        // L'ordre compte : du plus specifique au plus general, pour qu'un code-barres ne soit pas
        // confondu avec un code metier qui lui ressemblerait.
        return resolveBarcode(raw, upper)
                .or(() -> resolvePackaging(raw, upper))
                .or(() -> resolveItem(raw, normalized))
                .or(() -> resolveLocation(raw, normalized))
                .or(() -> resolveAsset(raw, normalized, upper))
                .or(() -> resolveSerial(raw, upper))
                .or(() -> resolveLot(raw, upper))
                .orElseGet(() -> unknown(raw));
    }

    private Optional<InventoryScanResponse> resolveBarcode(String raw, String upper) {
        return barcodeRepository.findByBarcodeValue(upper).map(barcode -> {
            InventoryItem item = barcode.getItem();
            return base(raw, "BARCODE", barcode.getBarcodeValue(), item.getName())
                    .itemCode(item.getItemCode())
                    .itemName(item.getName())
                    .quantityPerScan(barcode.getQuantity())
                    .unitCode(barcode.getUnitCode() != null ? barcode.getUnitCode() : unitCodeOf(item))
                    .suggestedActions(itemActions(item))
                    .message(barcodeWarning(barcode))
                    .build();
        });
    }

    private Optional<InventoryScanResponse> resolvePackaging(String raw, String upper) {
        return packagingRepository.findFirstByBarcodeValue(upper).map(packaging -> {
            InventoryItem item = packaging.getItem();
            return base(raw, "PACKAGING", packaging.getBarcodeValue(),
                    packagingLabel(packaging, item))
                    .itemCode(item.getItemCode())
                    .itemName(item.getName())
                    .quantityPerScan(packaging.getQuantity())
                    .unitCode(unitCodeOf(item))
                    .suggestedActions(itemActions(item))
                    .build();
        });
    }

    private Optional<InventoryScanResponse> resolveItem(String raw, String normalized) {
        return itemRepository.findByAnyCode(normalized).map(item ->
                base(raw, "ITEM", item.getItemCode(), item.getName())
                        .itemCode(item.getItemCode())
                        .itemName(item.getName())
                        .quantityPerScan(BigDecimal.ONE)
                        .unitCode(unitCodeOf(item))
                        .suggestedActions(itemActions(item))
                        .message(lifecycleWarning(item))
                        .build());
    }

    private Optional<InventoryScanResponse> resolveLocation(String raw, String normalized) {
        return locationRepository.findByLocationCode(normalized).map(location ->
                base(raw, "LOCATION", location.getLocationCode(), location.getName())
                        .locationCode(location.getLocationCode())
                        .suggestedActions(locationActions(location))
                        .message(Boolean.FALSE.equals(location.getActive())
                                ? "Emplacement inactif" : null)
                        .build());
    }

    private Optional<InventoryScanResponse> resolveAsset(String raw, String normalized, String upper) {
        Optional<Asset> asset = assetRepository.findByAssetCode(normalized);
        if (asset.isEmpty()) {
            asset = assetRepository.findFirstByAssetTag(upper);
        }
        if (asset.isEmpty()) {
            asset = assetRepository.findFirstBySerialNumber(upper);
        }
        return asset.map(found -> base(raw, "ASSET", found.getAssetCode(), assetLabel(found))
                .itemCode(found.getItem() == null ? null : found.getItem().getItemCode())
                .itemName(found.getItem() == null ? null : found.getItem().getName())
                .locationCode(found.getLocation() == null ? null : found.getLocation().getLocationCode())
                .suggestedActions(List.of("ASSET_DETAIL", "ASSIGN", "RETURN", "MAINTENANCE", "LOCATION_HISTORY"))
                .message("Statut: " + found.getStatus())
                .build());
    }

    private Optional<InventoryScanResponse> resolveSerial(String raw, String upper) {
        List<InventoryItemSerial> serials = serialRepository.findAllBySerialNumber(upper);
        if (serials.isEmpty()) {
            return Optional.empty();
        }
        InventoryItemSerial serial = serials.get(0);
        InventoryItem item = serial.getItem();
        return Optional.of(base(raw, "SERIAL", serial.getSerialNumber(), item.getName())
                .itemCode(item.getItemCode())
                .itemName(item.getName())
                .locationCode(serial.getLocation() == null ? null : serial.getLocation().getLocationCode())
                .quantityPerScan(BigDecimal.ONE)
                .unitCode(unitCodeOf(item))
                .suggestedActions(List.of("ITEM_DETAIL", "STOCK_OUT", "STOCK_TRANSFER"))
                .message(serials.size() > 1
                        ? "Numero de serie porte par " + serials.size() + " articles, premier retenu"
                        : "Statut: " + serial.getStatus())
                .build());
    }

    private Optional<InventoryScanResponse> resolveLot(String raw, String upper) {
        List<StockLot> lots = lotRepository.findAllByLotNumberAndActiveTrue(upper);
        if (lots.isEmpty()) {
            return Optional.empty();
        }
        StockLot lot = lots.get(0);
        InventoryItem item = lot.getItem();
        return Optional.of(base(raw, "LOT", lot.getLotNumber(), item.getName())
                .itemCode(item.getItemCode())
                .itemName(item.getName())
                .locationCode(lot.getLocation() == null ? null : lot.getLocation().getLocationCode())
                .quantityPerScan(lot.getRemainingQuantity())
                .unitCode(unitCodeOf(item))
                .suggestedActions(List.of("LOT_DETAIL", "STOCK_OUT", "STOCK_TRANSFER"))
                .message(lotMessage(lot, lots.size()))
                .build());
    }

    private InventoryScanResponse unknown(String raw) {
        return InventoryScanResponse.builder()
                .scannedCode(raw)
                .resolvedType("UNKNOWN")
                .resolved(false)
                .suggestedActions(List.of("CREATE_ITEM", "ASSIGN_BARCODE", "SEARCH"))
                .message("Code non reconnu. Il peut etre rattache a un article existant ou declencher une creation.")
                .build();
    }

    private InventoryScanResponse.InventoryScanResponseBuilder base(String raw, String type, String code, String label) {
        return InventoryScanResponse.builder()
                .scannedCode(raw)
                .resolvedType(type)
                .resolved(true)
                .code(code)
                .label(label);
    }

    private List<String> itemActions(InventoryItem item) {
        if (item.canReceiveStock() && item.canIssueStock()) {
            return List.of("ITEM_DETAIL", "STOCK_IN", "STOCK_OUT", "STOCK_TRANSFER", "STOCK_LEVELS");
        }
        if (item.canIssueStock()) {
            return List.of("ITEM_DETAIL", "STOCK_OUT", "STOCK_TRANSFER", "STOCK_LEVELS");
        }
        return List.of("ITEM_DETAIL", "STOCK_LEVELS");
    }

    private List<String> locationActions(InventoryLocation location) {
        if (Boolean.FALSE.equals(location.getActive())) {
            return List.of("LOCATION_DETAIL", "STOCK_LEVELS");
        }
        return List.of("LOCATION_DETAIL", "STOCK_LEVELS", "STOCK_IN", "STOCK_TRANSFER", "START_COUNT");
    }

    private String lifecycleWarning(InventoryItem item) {
        if (item.canReceiveStock() && item.canIssueStock()) {
            return null;
        }
        return "Article en statut " + item.getLifecycleStatus()
                + (item.canIssueStock() ? " : plus de reapprovisionnement" : " : aucun mouvement autorise");
    }

    private String barcodeWarning(InventoryBarcode barcode) {
        if (Boolean.FALSE.equals(barcode.getActive())) {
            return "Code-barres desactive";
        }
        return lifecycleWarning(barcode.getItem());
    }

    private String lotMessage(StockLot lot, int matches) {
        StringBuilder message = new StringBuilder();
        if (matches > 1) {
            message.append("Lot present sur ").append(matches).append(" emplacements, premier retenu. ");
        }
        if (Boolean.TRUE.equals(lot.getQuarantined())) {
            message.append("Lot en quarantaine. ");
        }
        if (lot.getExpiryDate() != null) {
            message.append("Peremption: ").append(lot.getExpiryDate());
        }
        String result = message.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private String assetLabel(Asset asset) {
        if (asset.getItem() != null && StringUtils.hasText(asset.getItem().getName())) {
            return asset.getItem().getName();
        }
        return asset.getAssetCode();
    }

    private String packagingLabel(InventoryPackaging packaging, InventoryItem item) {
        String name = StringUtils.hasText(packaging.getName())
                ? packaging.getName()
                : packaging.getPackagingLevel().name();
        return item.getName() + " (" + name + ")";
    }

    private String unitCodeOf(InventoryItem item) {
        return item.getUnit() == null ? null : item.getUnit().getCode();
    }

    private String normalizeCode(String value) {
        return value.trim().replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "").toUpperCase(Locale.ROOT);
    }
}
