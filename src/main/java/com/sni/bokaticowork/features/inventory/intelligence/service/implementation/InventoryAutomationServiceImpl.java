package com.sni.bokaticowork.features.inventory.intelligence.service.implementation;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertStatus;
import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertType;
import com.sni.bokaticowork.features.inventory.intelligence.model.InventoryAlert;
import com.sni.bokaticowork.features.inventory.intelligence.model.InventoryReorderRule;
import com.sni.bokaticowork.features.inventory.intelligence.repository.InventoryAlertRepository;
import com.sni.bokaticowork.features.inventory.intelligence.repository.InventoryReorderRuleRepository;
import com.sni.bokaticowork.features.inventory.intelligence.service.interfaces.InventoryAutomationService;
import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryAutomationServiceImpl implements InventoryAutomationService {

    private final InventoryAlertRepository alertRepository;
    private final InventoryReorderRuleRepository reorderRuleRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final OutboxService outboxService;

    @Override
    public void afterStockMovement(StockMovement movement, StockLevel... impactedLevels) {
        publishStockEvent(movement);
        if (impactedLevels == null) {
            return;
        }
        for (StockLevel level : impactedLevels) {
            if (level == null) continue;
            detectNegativeStock(level);
            detectOutOfStock(level);
            detectLowStock(level);
            detectOverstock(level);
            resolveRecoveredAlerts(level);
        }
    }

    @Override
    public void publishAssetEvent(String eventType, String assetCode, Object payload) {
        outboxService.publish(eventType, "INVENTORY_ASSET", assetCode, payload);
    }

    private void detectNegativeStock(StockLevel level) {
        if (level.getQuantityAvailable().compareTo(BigDecimal.ZERO) < 0) {
            openAlert(InventoryAlertType.NEGATIVE_STOCK, level, BigDecimal.ZERO,
                    "Stock negatif detecte pour " + level.getItem().getItemCode());
        }
    }

    private void detectOutOfStock(StockLevel level) {
        if (level.getQuantityAvailable().compareTo(BigDecimal.ZERO) <= 0) {
            openAlert(InventoryAlertType.OUT_OF_STOCK, level, BigDecimal.ZERO,
                    "Rupture de stock pour " + level.getItem().getItemCode());
        }
    }

    private void detectLowStock(StockLevel level) {
        InventoryReorderRule rule = reorderRuleRepository.findFirstByItemAndLocationAndActiveTrue(level.getItem(), level.getLocation())
                .or(() -> reorderRuleRepository.findFirstByItemAndLocationIsNullAndActiveTrue(level.getItem()))
                .orElse(null);
        if (rule == null) {
            return;
        }
        if (level.getQuantityAvailable().compareTo(rule.getMinQuantity()) <= 0) {
            long previousOccurrences = alertRepository.countByAlertTypeAndItemAndLocationAndStatus(
                    InventoryAlertType.LOW_STOCK, level.getItem(), level.getLocation(), InventoryAlertStatus.RESOLVED)
                    + alertRepository.countByAlertTypeAndItemAndLocationAndStatus(
                    InventoryAlertType.RECURRING_LOW_STOCK, level.getItem(), level.getLocation(), InventoryAlertStatus.RESOLVED);

            if (previousOccurrences >= 2) {
                openAlert(InventoryAlertType.RECURRING_LOW_STOCK, level, rule.getMinQuantity(),
                        "Stock bas recurrent (" + (previousOccurrences + 1) + "e occurrence) pour "
                                + level.getItem().getItemCode() + " : " + level.getQuantityAvailable()
                                + " <= seuil " + rule.getMinQuantity());
            } else {
                openAlert(InventoryAlertType.LOW_STOCK, level, rule.getMinQuantity(),
                        "Stock bas pour " + level.getItem().getItemCode()
                                + " : " + level.getQuantityAvailable() + " <= seuil " + rule.getMinQuantity());
            }
        }
    }

    private void detectOverstock(StockLevel level) {
        InventoryReorderRule rule = reorderRuleRepository.findFirstByItemAndLocationAndActiveTrue(level.getItem(), level.getLocation())
                .or(() -> reorderRuleRepository.findFirstByItemAndLocationIsNullAndActiveTrue(level.getItem()))
                .orElse(null);
        if (rule == null || rule.getMaxQuantity() == null) {
            return;
        }
        if (level.getQuantityAvailable().compareTo(rule.getMaxQuantity()) > 0) {
            openAlert(InventoryAlertType.OVERSTOCK, level, rule.getMaxQuantity(),
                    "Surstockage detecte pour " + level.getItem().getItemCode()
                            + " : " + level.getQuantityAvailable() + " > max " + rule.getMaxQuantity());
        } else {
            resolveOpen(level, InventoryAlertType.OVERSTOCK);
        }
    }

    private void resolveRecoveredAlerts(StockLevel level) {
        if (level.getQuantityAvailable().compareTo(BigDecimal.ZERO) > 0) {
            resolveOpen(level, InventoryAlertType.OUT_OF_STOCK);
            resolveOpen(level, InventoryAlertType.NEGATIVE_STOCK);
        }
        InventoryReorderRule rule = reorderRuleRepository.findFirstByItemAndLocationAndActiveTrue(level.getItem(), level.getLocation())
                .or(() -> reorderRuleRepository.findFirstByItemAndLocationIsNullAndActiveTrue(level.getItem()))
                .orElse(null);
        if (rule != null && level.getQuantityAvailable().compareTo(rule.getMinQuantity()) > 0) {
            resolveOpen(level, InventoryAlertType.LOW_STOCK);
            resolveOpen(level, InventoryAlertType.RECURRING_LOW_STOCK);
        }
    }

    private void openAlert(InventoryAlertType type, StockLevel level, BigDecimal threshold, String message) {
        if (alertRepository.findFirstByAlertTypeAndItemAndLocationAndStatusOrderByCreatedAtDesc(
                type, level.getItem(), level.getLocation(), InventoryAlertStatus.OPEN).isPresent()) {
            return;
        }
        InventoryAlert alert = InventoryAlert.builder()
                .alertCode(sequenceGenerator.next("inventory_alert", LocalDate.now()) + "-" + System.currentTimeMillis())
                .alertType(type)
                .status(InventoryAlertStatus.OPEN)
                .item(level.getItem())
                .location(level.getLocation())
                .currentQuantity(level.getQuantityAvailable())
                .thresholdQuantity(threshold)
                .message(message)
                .createdAt(Instant.now())
                .build();
        alertRepository.save(alert);
        outboxService.publish("inventory.alert.created", "INVENTORY_ALERT", alert.getAlertCode(), alertPayload(alert));
    }

    private void resolveOpen(StockLevel level, InventoryAlertType type) {
        alertRepository.findFirstByAlertTypeAndItemAndLocationAndStatusOrderByCreatedAtDesc(
                type, level.getItem(), level.getLocation(), InventoryAlertStatus.OPEN
        ).ifPresent(alert -> {
            alert.setStatus(InventoryAlertStatus.RESOLVED);
            alert.setResolvedAt(Instant.now());
            alertRepository.save(alert);
        });
    }

    private void publishStockEvent(StockMovement movement) {
        String eventType = switch (movement.getMovementType()) {
            case IN, ADJUSTMENT_IN -> "inventory.stock.increased";
            case OUT, ADJUSTMENT_OUT -> "inventory.stock.decreased";
            case TRANSFER -> "inventory.stock.transferred";
        };
        outboxService.publish(eventType, "STOCK_MOVEMENT", movement.getMovementCode(), movementPayload(movement));
    }

    private Map<String, Object> movementPayload(StockMovement movement) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("movementCode", movement.getMovementCode());
        payload.put("movementType", movement.getMovementType());
        payload.put("itemCode", movement.getItem().getItemCode());
        payload.put("quantity", movement.getQuantity());
        payload.put("locationFromCode", movement.getLocationFrom() == null ? null : movement.getLocationFrom().getLocationCode());
        payload.put("locationToCode", movement.getLocationTo() == null ? null : movement.getLocationTo().getLocationCode());
        payload.put("referenceType", movement.getReferenceType());
        payload.put("referenceCode", movement.getReferenceCode());
        return payload;
    }

    private Map<String, Object> alertPayload(InventoryAlert alert) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("alertCode", alert.getAlertCode());
        payload.put("alertType", alert.getAlertType());
        payload.put("itemCode", alert.getItem() == null ? null : alert.getItem().getItemCode());
        payload.put("locationCode", alert.getLocation() == null ? null : alert.getLocation().getLocationCode());
        payload.put("currentQuantity", alert.getCurrentQuantity());
        payload.put("thresholdQuantity", alert.getThresholdQuantity());
        return payload;
    }
}
