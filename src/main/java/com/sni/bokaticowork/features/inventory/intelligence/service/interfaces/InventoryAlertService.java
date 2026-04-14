package com.sni.bokaticowork.features.inventory.intelligence.service.interfaces;

import com.sni.bokaticowork.features.inventory.intelligence.dto.response.InventoryAlertResponse;
import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryAlertService {
    Page<InventoryAlertResponse> list(InventoryAlertStatus status, Pageable pageable);
    InventoryAlertResponse acknowledge(String alertCode);
    InventoryAlertResponse resolve(String alertCode);
    InventoryAlertResponse dismiss(String alertCode);
}
