package com.sni.bokaticowork.features.inventory.catalog.service.interfaces;

import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryUnitRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryUnitResponse;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryUnit;

import java.util.List;

public interface InventoryUnitService {

    InventoryUnitResponse create(InventoryUnitRequest request);

    InventoryUnitResponse update(String code, InventoryUnitRequest request);

    InventoryUnitResponse get(String code);

    List<InventoryUnitResponse> list(Boolean active);

    InventoryUnitResponse activate(String code);

    InventoryUnitResponse deactivate(String code);

    InventoryUnit findByCodeOrThrow(String code);
}
