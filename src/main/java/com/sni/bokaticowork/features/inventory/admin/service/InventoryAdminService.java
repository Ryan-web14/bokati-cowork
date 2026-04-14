package com.sni.bokaticowork.features.inventory.admin.service;

import com.sni.bokaticowork.features.inventory.admin.dto.InventoryDashboardResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryLabelResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryMovementReportResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryAnomalyReportResponse;

import java.time.Instant;

public interface InventoryAdminService {
    InventoryDashboardResponse dashboard();

    InventoryLabelResponse label(String type, String code);

    InventoryMovementReportResponse movementReport(String itemCode, String locationCode, Instant fromDate, Instant toDate);

    String movementReportCsv(String itemCode, String locationCode, Instant fromDate, Instant toDate);

    byte[] movementReportPdf(String itemCode, String locationCode, Instant fromDate, Instant toDate);

    InventoryAnomalyReportResponse anomalyReport();
}
