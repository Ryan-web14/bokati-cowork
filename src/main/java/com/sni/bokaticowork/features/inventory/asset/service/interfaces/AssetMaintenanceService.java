package com.sni.bokaticowork.features.inventory.asset.service.interfaces;

import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetMaintenanceCompleteRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetMaintenanceRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetMaintenanceResponse;

import java.util.List;

public interface AssetMaintenanceService {

    AssetMaintenanceResponse plan(String assetCode, AssetMaintenanceRequest request);

    List<AssetMaintenanceResponse> list(String assetCode);

    AssetMaintenanceResponse start(String maintenanceCode);

    AssetMaintenanceResponse complete(String maintenanceCode, AssetMaintenanceCompleteRequest request);

    AssetMaintenanceResponse cancel(String maintenanceCode);
}
