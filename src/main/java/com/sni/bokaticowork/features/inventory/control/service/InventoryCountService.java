package com.sni.bokaticowork.features.inventory.control.service;

import com.sni.bokaticowork.features.inventory.control.dto.request.InventoryCountCreateRequest;
import com.sni.bokaticowork.features.inventory.control.dto.request.InventoryCountLineRequest;
import com.sni.bokaticowork.features.inventory.control.dto.response.InventoryCountResponse;
import com.sni.bokaticowork.features.inventory.control.enums.InventoryCountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryCountService {
    InventoryCountResponse create(InventoryCountCreateRequest request);

    InventoryCountResponse start(String countCode);

    InventoryCountResponse recordLine(String countCode, InventoryCountLineRequest request);

    InventoryCountResponse sendToReview(String countCode);

    InventoryCountResponse validate(String countCode, String performedBy);

    InventoryCountResponse cancel(String countCode);

    InventoryCountResponse get(String countCode);

    Page<InventoryCountResponse> list(InventoryCountStatus status, Pageable pageable);
}
