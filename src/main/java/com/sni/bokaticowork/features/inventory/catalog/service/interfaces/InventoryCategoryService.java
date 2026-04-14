package com.sni.bokaticowork.features.inventory.catalog.service.interfaces;

import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryCategoryRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryCategoryResponse;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryCategory;

import java.util.List;

public interface InventoryCategoryService {

    InventoryCategoryResponse create(InventoryCategoryRequest request);

    InventoryCategoryResponse update(String code, InventoryCategoryRequest request);

    InventoryCategoryResponse get(String code);

    List<InventoryCategoryResponse> list(Boolean active);

    InventoryCategoryResponse activate(String code);

    InventoryCategoryResponse deactivate(String code);

    InventoryCategory findByCodeOrThrow(String code);
}
