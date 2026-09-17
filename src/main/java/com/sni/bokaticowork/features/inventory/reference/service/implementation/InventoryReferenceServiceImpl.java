package com.sni.bokaticowork.features.inventory.reference.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssigneeType;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssignmentStatus;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetCondition;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetMaintenanceStatus;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetMaintenanceType;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetStatus;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryBarcodeType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryPackagingLevel;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryPriceType;
import com.sni.bokaticowork.features.inventory.catalog.enums.ItemLifecycleStatus;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryUnitType;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryCategoryRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryUnitRepository;
import com.sni.bokaticowork.features.inventory.control.enums.InventoryCountStatus;
import com.sni.bokaticowork.features.inventory.importer.enums.InventoryImportType;
import com.sni.bokaticowork.features.inventory.intelligence.enums.ConsumptionTrend;
import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertStatus;
import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertType;
import com.sni.bokaticowork.features.inventory.intelligence.enums.ReorderSuggestionReason;
import com.sni.bokaticowork.features.inventory.intelligence.enums.ReorderSuggestionSeverity;
import com.sni.bokaticowork.features.inventory.procurement.enums.GoodsReceiptStatus;
import com.sni.bokaticowork.features.inventory.procurement.enums.PurchaseApprovalLevel;
import com.sni.bokaticowork.features.inventory.procurement.enums.PurchaseOrderStatus;
import com.sni.bokaticowork.features.inventory.procurement.enums.PurchaseRequestStatus;
import com.sni.bokaticowork.features.inventory.procurement.enums.SupplierStatus;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierRepository;
import com.sni.bokaticowork.features.inventory.reference.dto.InventoryOptionResponse;
import com.sni.bokaticowork.features.inventory.reference.service.interfaces.InventoryReferenceService;
import com.sni.bokaticowork.features.inventory.stock.enums.InventoryLocationType;
import com.sni.bokaticowork.features.inventory.stock.enums.InventorySerialStatus;
import com.sni.bokaticowork.features.inventory.stock.enums.InventoryPeriodStatus;
import com.sni.bokaticowork.features.inventory.stock.enums.LotBlockReasonType;
import com.sni.bokaticowork.features.inventory.stock.enums.LotGenealogyRelation;
import com.sni.bokaticowork.features.inventory.stock.enums.NonConformanceDisposition;
import com.sni.bokaticowork.features.inventory.stock.enums.NonConformanceSeverity;
import com.sni.bokaticowork.features.inventory.stock.enums.ProductRecallStatus;
import com.sni.bokaticowork.features.inventory.stock.enums.QualityControlStage;
import com.sni.bokaticowork.features.inventory.stock.enums.QualityDecision;
import com.sni.bokaticowork.features.inventory.stock.enums.QualitySamplingMode;
import com.sni.bokaticowork.features.inventory.stock.enums.StockJournalDirection;
import com.sni.bokaticowork.features.inventory.stock.enums.ValuationMethod;
import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockOutReasonCode;
import com.sni.bokaticowork.features.inventory.stock.enums.StockOwnershipType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReservationStatus;
import com.sni.bokaticowork.features.inventory.stock.enums.StockTransferWorkflowStatus;
import com.sni.bokaticowork.features.inventory.stock.repository.InventoryLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryReferenceServiceImpl implements InventoryReferenceService {

    /**
     * Nom de groupe expose au front, associe a l'enumeration correspondante. L'ordre est celui du
     * parcours fonctionnel : catalogue, stock, equipements, inventaires, achats, pilotage.
     */
    private static final Map<String, Class<? extends Enum<?>>> ENUM_GROUPS = buildGroups();

    private final InventoryUnitRepository unitRepository;
    private final InventoryCategoryRepository categoryRepository;
    private final InventoryLocationRepository locationRepository;
    private final SupplierRepository supplierRepository;

    private static Map<String, Class<? extends Enum<?>>> buildGroups() {
        Map<String, Class<? extends Enum<?>>> groups = new LinkedHashMap<>();
        groups.put("itemTypes", InventoryItemType.class);
        groups.put("trackingTypes", InventoryTrackingType.class);
        groups.put("unitTypes", InventoryUnitType.class);
        groups.put("lifecycleStatuses", ItemLifecycleStatus.class);
        groups.put("barcodeTypes", InventoryBarcodeType.class);
        groups.put("packagingLevels", InventoryPackagingLevel.class);
        groups.put("priceTypes", InventoryPriceType.class);

        groups.put("locationTypes", InventoryLocationType.class);
        groups.put("movementTypes", StockMovementType.class);
        groups.put("stockOutReasons", StockOutReasonCode.class);
        groups.put("ownershipTypes", StockOwnershipType.class);
        groups.put("referenceTypes", StockReferenceType.class);
        groups.put("reservationStatuses", StockReservationStatus.class);
        groups.put("transferStatuses", StockTransferWorkflowStatus.class);
        groups.put("serialStatuses", InventorySerialStatus.class);
        groups.put("valuationMethods", ValuationMethod.class);
        groups.put("periodStatuses", InventoryPeriodStatus.class);
        groups.put("journalDirections", StockJournalDirection.class);
        groups.put("lotBlockReasonTypes", LotBlockReasonType.class);
        groups.put("lotGenealogyRelations", LotGenealogyRelation.class);
        groups.put("qualityControlStages", QualityControlStage.class);
        groups.put("qualitySamplingModes", QualitySamplingMode.class);
        groups.put("qualityDecisions", QualityDecision.class);
        groups.put("nonConformanceSeverities", NonConformanceSeverity.class);
        groups.put("nonConformanceDispositions", NonConformanceDisposition.class);
        groups.put("productRecallStatuses", ProductRecallStatus.class);

        groups.put("assetStatuses", AssetStatus.class);
        groups.put("assetConditions", AssetCondition.class);
        groups.put("assetAssigneeTypes", AssetAssigneeType.class);
        groups.put("assetAssignmentStatuses", AssetAssignmentStatus.class);
        groups.put("maintenanceTypes", AssetMaintenanceType.class);
        groups.put("maintenanceStatuses", AssetMaintenanceStatus.class);

        groups.put("countStatuses", InventoryCountStatus.class);
        groups.put("importTypes", InventoryImportType.class);

        groups.put("supplierStatuses", SupplierStatus.class);
        groups.put("purchaseRequestStatuses", PurchaseRequestStatus.class);
        groups.put("purchaseOrderStatuses", PurchaseOrderStatus.class);
        groups.put("goodsReceiptStatuses", GoodsReceiptStatus.class);
        groups.put("approvalLevels", PurchaseApprovalLevel.class);

        groups.put("alertTypes", InventoryAlertType.class);
        groups.put("alertStatuses", InventoryAlertStatus.class);
        groups.put("consumptionTrends", ConsumptionTrend.class);
        groups.put("reorderReasons", ReorderSuggestionReason.class);
        groups.put("reorderSeverities", ReorderSuggestionSeverity.class);
        return Collections.unmodifiableMap(groups);
    }

    @Override
    public Map<String, List<InventoryOptionResponse>> enums() {
        Map<String, List<InventoryOptionResponse>> result = new LinkedHashMap<>();
        ENUM_GROUPS.forEach((group, type) -> result.put(group, toOptions(type)));
        return result;
    }

    @Override
    public List<InventoryOptionResponse> enumGroup(String group) {
        Class<? extends Enum<?>> type = group == null ? null : ENUM_GROUPS.get(group.trim());
        if (type == null) {
            throw new ResourceNotFoundException("Unknown inventory reference group: " + group
                    + ". Available groups: " + String.join(", ", ENUM_GROUPS.keySet()));
        }
        return toOptions(type);
    }

    @Override
    public Map<String, List<InventoryOptionResponse>> data() {
        Map<String, List<InventoryOptionResponse>> result = new LinkedHashMap<>();

        result.put("units", unitRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(unit -> option(unit.getCode(), unit.getName(),
                        unit.getUnitType() == null ? null : InventoryReferenceLabels.labelFor(unit.getUnitType())))
                .toList());

        result.put("categories", categoryRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(category -> option(category.getCode(), category.getName(),
                        category.getParentCategory() == null ? null : category.getParentCategory().getName()))
                .toList());

        result.put("locations", locationRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(location -> option(location.getLocationCode(), location.getName(),
                        location.getLocationType() == null ? null : InventoryReferenceLabels.labelFor(location.getLocationType())))
                .toList());

        result.put("suppliers", supplierRepository.findAllByStatusOrderByNameAsc(SupplierStatus.ACTIVE).stream()
                .map(supplier -> option(supplier.getSupplierCode(), supplier.getName(), supplier.getPhone()))
                .toList());

        return result;
    }

    private List<InventoryOptionResponse> toOptions(Class<? extends Enum<?>> type) {
        return Arrays.stream(type.getEnumConstants())
                .map(constant -> InventoryOptionResponse.builder()
                        .value(constant.name())
                        .label(InventoryReferenceLabels.labelFor(constant))
                        .build())
                .toList();
    }

    private InventoryOptionResponse option(String value, String label, String description) {
        return InventoryOptionResponse.builder()
                .value(value)
                .label(label == null || label.isBlank() ? value : label)
                .description(description)
                .build();
    }

    @Override
    public List<String> groupNames() {
        return List.copyOf(ENUM_GROUPS.keySet());
    }
}
