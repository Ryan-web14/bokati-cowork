package com.sni.bokaticowork.features.inventory.stock.service.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.request.InventoryLocationRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.InventoryLocationResponse;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;

import java.util.List;

public interface InventoryLocationService {

    InventoryLocationResponse create(InventoryLocationRequest request);

    InventoryLocationResponse update(String locationCode, InventoryLocationRequest request);

    InventoryLocationResponse get(String locationCode);

    List<InventoryLocationResponse> list(Boolean active);

    InventoryLocationResponse activate(String locationCode);

    InventoryLocationResponse deactivate(String locationCode);

    InventoryLocation findByLocationCodeOrThrow(String locationCode);
}
