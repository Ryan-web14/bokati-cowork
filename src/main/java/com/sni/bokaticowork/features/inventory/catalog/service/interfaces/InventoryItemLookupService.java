package com.sni.bokaticowork.features.inventory.catalog.service.interfaces;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;

public interface InventoryItemLookupService {

    InventoryItem findByItemCodeOrThrow(String itemCode);
}
