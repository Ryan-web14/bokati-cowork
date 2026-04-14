package com.sni.bokaticowork.features.inventory.intelligence.service.interfaces;

import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovement;

public interface InventoryAutomationService {
    void afterStockMovement(StockMovement movement, StockLevel... impactedLevels);
    void publishAssetEvent(String eventType, String assetCode, Object payload);
}
