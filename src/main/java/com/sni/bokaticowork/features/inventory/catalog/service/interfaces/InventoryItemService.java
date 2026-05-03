package com.sni.bokaticowork.features.inventory.catalog.service.interfaces;

import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemResponse;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryItemService {

    InventoryItemResponse create(InventoryItemRequest request);

    InventoryItemResponse update(String itemCode, InventoryItemRequest request);

    InventoryItemResponse get(String itemCode);

    Page<InventoryItemResponse> search(String query, String categoryCode, InventoryItemType itemType, Boolean active, Pageable pageable);

    InventoryItemResponse activate(String itemCode);

    InventoryItemResponse deactivate(String itemCode);

    void delete(String itemCode);
}
