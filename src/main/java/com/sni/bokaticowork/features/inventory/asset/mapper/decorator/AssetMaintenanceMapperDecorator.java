package com.sni.bokaticowork.features.inventory.asset.mapper.decorator;

import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetMaintenanceRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetMaintenanceResponse;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetMaintenanceStatus;
import com.sni.bokaticowork.features.inventory.asset.mapper.interfaces.AssetMaintenanceMapper;
import com.sni.bokaticowork.features.inventory.asset.model.AssetMaintenance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class AssetMaintenanceMapperDecorator implements AssetMaintenanceMapper {

    @Autowired
    @Qualifier("delegate")
    private AssetMaintenanceMapper delegate;

    @Override
    public AssetMaintenance toEntity(AssetMaintenanceRequest request) {
        AssetMaintenance maintenance = delegate.toEntity(request);
        maintenance.setStatus(AssetMaintenanceStatus.PLANNED);
        return maintenance;
    }

    @Override
    public AssetMaintenanceResponse toResponse(AssetMaintenance maintenance) {
        AssetMaintenanceResponse response = delegate.toResponse(maintenance);
        if (maintenance != null && maintenance.getAsset() != null) {
            response.setAssetCode(maintenance.getAsset().getAssetCode());
        }
        return response;
    }
}
