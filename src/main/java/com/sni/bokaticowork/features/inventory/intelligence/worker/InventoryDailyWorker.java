package com.sni.bokaticowork.features.inventory.intelligence.worker;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssignmentStatus;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetMaintenanceStatus;
import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import com.sni.bokaticowork.features.inventory.asset.model.AssetAssignment;
import com.sni.bokaticowork.features.inventory.asset.model.AssetMaintenance;
import com.sni.bokaticowork.features.inventory.asset.repository.AssetAssignmentRepository;
import com.sni.bokaticowork.features.inventory.asset.repository.AssetMaintenanceRepository;
import com.sni.bokaticowork.features.inventory.asset.repository.AssetRepository;
import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertStatus;
import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertType;
import com.sni.bokaticowork.features.inventory.intelligence.model.InventoryAlert;
import com.sni.bokaticowork.features.inventory.intelligence.model.InventoryReorderRule;
import com.sni.bokaticowork.features.inventory.intelligence.repository.InventoryAlertRepository;
import com.sni.bokaticowork.features.inventory.intelligence.repository.InventoryReorderRuleRepository;
import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReservationStatus;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import com.sni.bokaticowork.features.inventory.stock.model.StockReservation;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLotRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockMovementRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryDailyWorker {

    private static final int EXPIRY_WINDOW_DAYS = 30;
    private static final int EXPIRY_IMMINENT_DAYS = 7;
    private static final int WARRANTY_WINDOW_DAYS = 30;
    private static final int SLOW_MOVING_DAYS = 30;

    private final StockLotRepository lotRepository;
    private final StockReservationRepository reservationRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository movementRepository;
    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final AssetMaintenanceRepository maintenanceRepository;
    private final InventoryAlertRepository alertRepository;
    private final InventoryReorderRuleRepository reorderRuleRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final OutboxService outboxService;

    /**
     * Expire les réservations périmées toutes les 30 minutes.
     */
    @Scheduled(cron = "${inventory.worker.reservation-cron:0 */30 * * * *}")
    @Transactional
    public void runReservationExpiryChecks() {
        int expired = expireReservations();
        log.info("Inventory reservation worker: expiredReservations={}", expired);
    }

    /**
     * Détecte les lots qui expirent bientôt et les lots en expiration imminente toutes les 2h.
     */
    @Scheduled(cron = "${inventory.worker.expiry-cron:0 0 */2 * * *}")
    @Transactional
    public void runExpirySoonChecks() {
        int soon = detectExpirySoon();
        int imminent = detectExpiryImminent();
        log.info("Inventory expiry worker: expirySoon={}, expiryImminent={}", soon, imminent);
    }

    /**
     * Vérifie les articles à rotation lente toutes les 6h.
     */
    @Scheduled(cron = "${inventory.worker.slow-moving-cron:0 0 */6 * * *}")
    @Transactional
    public void runSlowMovingChecks() {
        int slowMoving = detectSlowMoving();
        log.info("Inventory slow-moving worker: slowMoving={}", slowMoving);
    }

    /**
     * Vérifie les assets (garanties, maintenances, retards) toutes les 3h.
     */
    @Scheduled(cron = "${inventory.worker.asset-cron:0 0 */3 * * *}")
    @Transactional
    public void runAssetAlertChecks() {
        int warranties = detectWarrantySoon();
        int maintenance = detectMaintenanceDue();
        int overdueReturns = detectOverdueReturns();
        int returnReminders = detectReturnDueSoon();
        log.info("Inventory asset worker: warrantySoon={}, maintenanceDue={}, overdueReturns={}, returnDueSoon={}",
                warranties, maintenance, overdueReturns, returnReminders);
    }

    private int detectExpirySoon() {
        int created = 0;
        for (StockLot lot : lotRepository.findExpiringLots(LocalDate.now().plusDays(EXPIRY_WINDOW_DAYS), BigDecimal.ZERO)) {
            if (lot.getExpiryDate() != null && lot.getExpiryDate().isBefore(LocalDate.now().plusDays(EXPIRY_IMMINENT_DAYS))) {
                continue; // handled by detectExpiryImminent
            }
            if (alertRepository.findFirstByAlertTypeAndItemAndLocationAndStatusOrderByCreatedAtDesc(
                    InventoryAlertType.EXPIRY_SOON, lot.getItem(), lot.getLocation(), InventoryAlertStatus.OPEN).isPresent()) {
                continue;
            }
            InventoryAlert alert = baseAlert(InventoryAlertType.EXPIRY_SOON,
                    "Lot " + lot.getLotNumber() + " expire le " + lot.getExpiryDate()
                            + " (" + EXPIRY_WINDOW_DAYS + " jours) — qte: " + lot.getRemainingQuantity());
            alert.setItem(lot.getItem());
            alert.setLocation(lot.getLocation());
            alert.setCurrentQuantity(lot.getRemainingQuantity());
            alert.setThresholdQuantity(BigDecimal.ZERO);
            saveAndPublish(alert);
            created++;
        }
        return created;
    }

    private int detectExpiryImminent() {
        int created = 0;
        for (StockLot lot : lotRepository.findExpiringLots(LocalDate.now().plusDays(EXPIRY_IMMINENT_DAYS), BigDecimal.ZERO)) {
            if (alertRepository.findFirstByAlertTypeAndItemAndLocationAndStatusOrderByCreatedAtDesc(
                    InventoryAlertType.EXPIRY_IMMINENT, lot.getItem(), lot.getLocation(), InventoryAlertStatus.OPEN).isPresent()) {
                continue;
            }
            InventoryAlert alert = baseAlert(InventoryAlertType.EXPIRY_IMMINENT,
                    "URGENT — Lot " + lot.getLotNumber() + " expire le " + lot.getExpiryDate()
                            + " (sous " + EXPIRY_IMMINENT_DAYS + " jours) — qte: " + lot.getRemainingQuantity());
            alert.setItem(lot.getItem());
            alert.setLocation(lot.getLocation());
            alert.setCurrentQuantity(lot.getRemainingQuantity());
            alert.setThresholdQuantity(BigDecimal.ZERO);
            saveAndPublish(alert);
            created++;
        }
        return created;
    }

    private int detectSlowMoving() {
        int created = 0;
        Instant since = Instant.now().minus(SLOW_MOVING_DAYS, ChronoUnit.DAYS);
        List<InventoryReorderRule> activeRules = reorderRuleRepository.findAllActiveRules();
        for (InventoryReorderRule rule : activeRules) {
            List<StockLevel> levels = rule.getLocation() != null
                    ? stockLevelRepository.findByItemAndLocation(rule.getItem(), rule.getLocation())
                            .map(List::of).orElse(List.of())
                    : stockLevelRepository.findAllByItemIdOrderByQuantityAvailableAsc(rule.getItem().getId());
            for (StockLevel level : levels) {
                if (level.getQuantityAvailable().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                boolean hasRecentMovement = movementRepository.existsByItemIdAndMovementTypeInAndPerformedAtAfter(
                        rule.getItem().getId(),
                        List.of(StockMovementType.OUT, StockMovementType.ADJUSTMENT_OUT),
                        since);
                if (!hasRecentMovement) {
                    if (alertRepository.findFirstByAlertTypeAndItemAndLocationAndStatusOrderByCreatedAtDesc(
                            InventoryAlertType.SLOW_MOVING, level.getItem(), level.getLocation(), InventoryAlertStatus.OPEN).isPresent()) {
                        continue;
                    }
                    InventoryAlert alert = baseAlert(InventoryAlertType.SLOW_MOVING,
                            "Article a rotation lente : " + level.getItem().getItemCode()
                                    + " — aucune sortie depuis " + SLOW_MOVING_DAYS + " jours, stock: " + level.getQuantityAvailable());
                    alert.setItem(level.getItem());
                    alert.setLocation(level.getLocation());
                    alert.setCurrentQuantity(level.getQuantityAvailable());
                    alert.setThresholdQuantity(BigDecimal.ZERO);
                    saveAndPublish(alert);
                    created++;
                }
            }
        }
        return created;
    }

    private int expireReservations() {
        int expired = 0;
        for (StockReservation reservation : reservationRepository.findAllByStatusAndExpiresAtBefore(StockReservationStatus.ACTIVE, Instant.now())) {
            StockLevel level = stockLevelRepository.findByItemAndLocationForUpdate(reservation.getItem(), reservation.getLocation())
                    .orElse(null);
            if (level != null) {
                level.setQuantityReserved(level.getQuantityReserved().subtract(reservation.getQuantity()).max(BigDecimal.ZERO));
                level.recalculateAvailable();
                stockLevelRepository.save(level);
            }
            reservation.setStatus(StockReservationStatus.EXPIRED);
            reservation.setClosedAt(Instant.now());
            reservationRepository.save(reservation);
            expired++;
        }
        return expired;
    }

    private int detectWarrantySoon() {
        int created = 0;
        LocalDate maxDate = LocalDate.now().plusDays(WARRANTY_WINDOW_DAYS);
        for (Asset asset : assetRepository.findAll()) {
            if (asset.getWarrantyEndDate() == null || asset.getWarrantyEndDate().isAfter(maxDate)) {
                continue;
            }
            if (alertRepository.findFirstByAlertTypeAndAssetCodeAndStatusOrderByCreatedAtDesc(
                    InventoryAlertType.WARRANTY_SOON, asset.getAssetCode(), InventoryAlertStatus.OPEN).isPresent()) {
                continue;
            }
            InventoryAlert alert = baseAlert(InventoryAlertType.WARRANTY_SOON,
                    "Garantie expire le " + asset.getWarrantyEndDate() + " pour asset " + asset.getAssetCode());
            alert.setAssetCode(asset.getAssetCode());
            alert.setItem(asset.getItem());
            alert.setLocation(asset.getLocation());
            saveAndPublish(alert);
            created++;
        }
        return created;
    }

    private int detectMaintenanceDue() {
        int created = 0;
        for (AssetMaintenance maintenance : maintenanceRepository.findAllByStatusAndScheduledAtBefore(
                AssetMaintenanceStatus.PLANNED, Instant.now().plus(1, ChronoUnit.DAYS))) {
            Asset asset = maintenance.getAsset();
            if (alertRepository.findFirstByAlertTypeAndAssetCodeAndStatusOrderByCreatedAtDesc(
                    InventoryAlertType.MAINTENANCE_DUE, asset.getAssetCode(), InventoryAlertStatus.OPEN).isPresent()) {
                continue;
            }
            InventoryAlert alert = baseAlert(InventoryAlertType.MAINTENANCE_DUE,
                    "Maintenance planifiee pour asset " + asset.getAssetCode() + " avant 24h");
            alert.setAssetCode(asset.getAssetCode());
            alert.setItem(asset.getItem());
            alert.setLocation(asset.getLocation());
            saveAndPublish(alert);
            created++;
        }
        return created;
    }

    private int detectOverdueReturns() {
        int created = 0;
        for (AssetAssignment assignment : assignmentRepository.findAllByStatusAndExpectedReturnAtBefore(
                AssetAssignmentStatus.ACTIVE, Instant.now())) {
            Asset asset = assignment.getAsset();
            if (alertRepository.findFirstByAlertTypeAndAssetCodeAndStatusOrderByCreatedAtDesc(
                    InventoryAlertType.ASSET_RETURN_OVERDUE, asset.getAssetCode(), InventoryAlertStatus.OPEN).isPresent()) {
                continue;
            }
            InventoryAlert alert = baseAlert(InventoryAlertType.ASSET_RETURN_OVERDUE,
                    "Retour asset en retard — " + asset.getAssetCode() + " attendu le " + assignment.getExpectedReturnAt());
            alert.setAssetCode(asset.getAssetCode());
            alert.setItem(asset.getItem());
            alert.setLocation(asset.getLocation());
            saveAndPublish(alert);
            created++;
        }
        return created;
    }

    private int detectReturnDueSoon() {
        int created = 0;
        Instant from = Instant.now();
        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);
        for (AssetAssignment assignment : assignmentRepository.findAllByStatusAndExpectedReturnAtBetween(
                AssetAssignmentStatus.ACTIVE, from, to)) {
            Asset asset = assignment.getAsset();
            if (alertRepository.findFirstByAlertTypeAndAssetCodeAndStatusOrderByCreatedAtDesc(
                    InventoryAlertType.ASSET_RETURN_DUE_SOON, asset.getAssetCode(), InventoryAlertStatus.OPEN).isPresent()) {
                continue;
            }
            InventoryAlert alert = baseAlert(InventoryAlertType.ASSET_RETURN_DUE_SOON,
                    "Retour prévu dans moins de 24h — asset " + asset.getAssetCode()
                            + " attendu le " + assignment.getExpectedReturnAt());
            alert.setAssetCode(asset.getAssetCode());
            alert.setItem(asset.getItem());
            alert.setLocation(asset.getLocation());
            saveAndPublish(alert);
            created++;
        }
        return created;
    }

    private InventoryAlert baseAlert(InventoryAlertType type, String message) {
        return InventoryAlert.builder()
                .alertCode(sequenceGenerator.next("inventory_alert", LocalDate.now()) + "-" + System.currentTimeMillis())
                .alertType(type)
                .status(InventoryAlertStatus.OPEN)
                .message(message)
                .createdAt(Instant.now())
                .build();
    }

    private void saveAndPublish(InventoryAlert alert) {
        InventoryAlert saved = alertRepository.save(alert);
        Map<String, Object> payload = new HashMap<>();
        payload.put("alertCode", saved.getAlertCode());
        payload.put("alertType", saved.getAlertType());
        payload.put("itemCode", saved.getItem() == null ? null : saved.getItem().getItemCode());
        payload.put("locationCode", saved.getLocation() == null ? null : saved.getLocation().getLocationCode());
        payload.put("assetCode", saved.getAssetCode());
        outboxService.publish("inventory.alert.created", "INVENTORY_ALERT", saved.getAlertCode(), payload);
    }
}
